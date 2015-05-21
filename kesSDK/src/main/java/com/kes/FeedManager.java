package com.kes;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;

import com.kes.model.PhotoComment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Created by gadza on 2015.03.05..
 */
public class FeedManager {

    public interface OnChangeListener {
        public void onPageLoaded(FeedWrapper wrapper);
        public void onMarkAsReadResult(PhotoComment photoComment, Exception error);
        public void onLikeResult(int questionID, int commentID, Exception error);
        public void onReportResult(int questionID, Exception error);
        public void onDeleteResult(int questionID, int commentID, Exception error);
        public void onMarkAsPrivateResult(PhotoComment photoComment, Exception error);
        public void onPosting(PhotoComment photoComment);
        public void onPostResult(com.kes.model.PhotoComment photoComment, Exception error);
    }


    private static class FeedCache {

        PhotoComment photoComment;
        public boolean cacheConnected = false;
        public boolean lastItem = false;
    }

    private static final int DEFAULT_PAGE_SIZE = 10;

    private Session mSession;

    private WeakHashMap<OnChangeListener, Void> callbacks = new WeakHashMap<OnChangeListener, Void>();

    SparseArray<FeedCache> publicCache = new SparseArray<FeedCache>();
    SparseArray<FeedCache> myCache = new SparseArray<FeedCache>();
    boolean publicCacheLoaded = false;
    boolean myCacheLoaded = false;
    int minIDPublicFeed = 0;
    int minIDMyFeed = 0;

    int transactionIDPublicFeed = 0;
    int transactionIDMyFeed = 0;

    ArrayList<PhotoComment> pending = new ArrayList<PhotoComment>();

    public enum FeedType {Public, My};

    private SparseArray<FeedCache> cacheOf(FeedType type)
    {
        if (type == FeedType.Public)
            return publicCache;
        else if (type == FeedType.My)
            return myCache;
        return null;
    }

    private boolean getLoaded(FeedType type)
    {
        if (type == FeedType.Public)
            return publicCacheLoaded;
        else if (type == FeedType.My)
            return myCacheLoaded;
        return false;
    }

    public static class PhotoCommentResponseHolder extends ResultWrapper {
        public long internalid;
        public PhotoComment response;
    }

    public static class FeedWrapper extends ResultWrapper {
        protected Bundle extras;
        public FeedType feedType = FeedType.Public;
        public Integer since_id;
        public Integer max_id;
        public int page_size = DEFAULT_PAGE_SIZE;
        public String tags;
        public PhotoComment photoComments[];
        public boolean flag_feedBottomReached = false;
        protected int transactionid;
        public boolean unreadItems = false;
        public boolean appended = false;

        public boolean isEqual(FeedWrapper other) {
            if (feedType != other.feedType)
                return false;
            if (!Utils.strEqual(tags, other.tags))
                return false;
            return true;
        }
    }

    public static class QueryParams {
        private FeedManager manager;
        FeedWrapper feedWrapper;

        protected QueryParams(FeedManager manager) {
            this.manager = manager;
            this.feedWrapper = new FeedWrapper();
        }

        public QueryParams setMaxID(int max_id) {
            feedWrapper.max_id = Integer.valueOf(max_id);
            feedWrapper.appended = true;
            return this;
        }

        public QueryParams setAppended(boolean appended) {
            feedWrapper.appended = true;
            return this;
        }

        public QueryParams setSinceID(int since_id) {
            feedWrapper.since_id = Integer.valueOf(since_id);
            return this;
        }

        public QueryParams setPageSize(int pageSize) {
            feedWrapper.page_size = Integer.valueOf(pageSize);
            return this;
        }

        public QueryParams tags(String tags) {
            feedWrapper.tags = tags;
            return this;
        }

        public void clear() {
            if (feedWrapper.feedType == FeedType.My) {
                manager.myCacheLoaded = false;
                manager.pending.clear();
                manager.myCache.clear();
                manager.minIDMyFeed = 0;
                manager.transactionIDMyFeed++;
            }
            if (feedWrapper.feedType == FeedType.Public) {
                manager.minIDPublicFeed = 0;
                manager.publicCacheLoaded = false;
                manager.publicCache.clear();
                manager.transactionIDPublicFeed++;
            }
        }

        public boolean isLastItem(int id)
        {
            return (manager.cacheOf(feedWrapper.feedType).get(id).lastItem);
        }

        public void load() {
            String token = null;

            feedWrapper.page_size = 3;  //TODO:remove
            if (feedWrapper.max_id != null)
            {
                int compareID = 0;
                if (feedWrapper.feedType == FeedType.Public)
                    compareID = manager.minIDPublicFeed;
                else if (feedWrapper.feedType == FeedType.My)
                    compareID = manager.minIDMyFeed;

                if (feedWrapper.max_id <= compareID)
                {
                    feedWrapper.flag_feedBottomReached = true;
                    manager.postChange(feedWrapper);
                    return;
                }
            }

            if (feedWrapper.feedType != FeedType.Public) {
                token = manager.mSession.getAccountManager().getToken();
            }

            if (feedWrapper.feedType == FeedType.Public)
                feedWrapper.transactionid = manager.transactionIDPublicFeed;
            else if (feedWrapper.feedType == FeedType.My)
                feedWrapper.transactionid = manager.transactionIDMyFeed;

            TaskLoadFeed.loadFeed(manager.mSession.getContext(), token, feedWrapper);
        }

        public boolean getCache(List<PhotoComment> dest)
        {
            boolean isLoaded = manager.getLoaded(feedWrapper.feedType);
            SparseArray<FeedCache> cache = manager.cacheOf(feedWrapper.feedType);

            //return feed between two end points
            int addCount = 0;
            boolean foundFirst = false;
            for (int i = cache.size() - 1; i >= 0; i--)
            {
                FeedCache item = cache.valueAt(i);
                if (item.cacheConnected) {
                    dest.add(item.photoComment);
                    addCount++;
                } else
                    break;
            }

            //Todo : re-enable below
            //if (addCount == 0)
            //    return isLoaded;

            if (feedWrapper.feedType == FeedType.Public.My)
                dest.addAll(0,manager.pending);

            return isLoaded;
        }

    }

    public void checkUnread()
    {
        Integer maxID = null;
        if (cacheOf(FeedType.My).size() > 0)
            maxID = cacheOf(FeedType.My).keyAt(0);
        TaskCheckUnread.loadFeed(mSession.getContext(), mSession.getAccountManager().getToken(), maxID);
    }

    protected void updateUnread(Context context, FeedWrapper feedWrapper)
    {
        if (feedWrapper.exception == null)
            mSession.getAccountManager().updateUnread(feedWrapper.user.unread_count);
        updateData(feedWrapper);
    }

    private void addToCache(FeedWrapper feedWrapper) {
        SparseArray<FeedCache> cache = cacheOf(feedWrapper.feedType);
        if (feedWrapper.max_id == null && cache.size() > 0)
            cache.valueAt(cache.size() - 1).cacheConnected = false;

        int commentsLength = feedWrapper.photoComments != null ? feedWrapper.photoComments.length : 0;
        if (commentsLength < 1)
            return;

        int previousCacheSize = cache.size();

        for (int i = 0; i < feedWrapper.photoComments.length; i++) {
            FeedCache item = null;
            PhotoComment newItem = feedWrapper.photoComments[i];
            int currentID = feedWrapper.photoComments[i].getID(feedWrapper.feedType);
            item = cache.get(currentID);
            if (item == null) {
                item = new FeedCache();
                item.photoComment = new PhotoComment();
                cache.put(newItem.getID(feedWrapper.feedType), item);
            }
            if (i == 0)
            {
                if (feedWrapper.max_id != null) {
                    FeedCache tmp = cache.get(feedWrapper.max_id + 1);
                    if (tmp != null)
                        item.cacheConnected = true;
                } else
                    item.cacheConnected = true;
            } else
                item.cacheConnected = true;

            updateItemInCache(newItem);
        }
    }

    protected void updateData(FeedWrapper feedWrapper) {
        if (feedWrapper.feedType == FeedType.My && feedWrapper.transactionid < transactionIDMyFeed)
                return;
        if (feedWrapper.feedType == FeedType.Public && feedWrapper.transactionid < transactionIDPublicFeed)
            return;

        int commentsLength = feedWrapper.photoComments != null ? feedWrapper.photoComments.length : 0;

        if (feedWrapper.feedType == FeedType.Public)
            for (int i = 0; i < commentsLength; i++)
                feedWrapper.photoComments[i].setAsRead();

        if (feedWrapper.exception == null && commentsLength == 0)
        {
            int maxid = 0;
            if (feedWrapper.max_id != null)
                maxid = feedWrapper.max_id;

            if (feedWrapper.feedType == FeedType.Public)
                minIDPublicFeed = maxid;
            else if (feedWrapper.feedType == FeedType.My)
                minIDMyFeed = maxid;
        }

        addToCache(feedWrapper);
        if (feedWrapper.exception == null && feedWrapper.since_id == null && commentsLength < feedWrapper.page_size) {
            feedWrapper.flag_feedBottomReached = true;
            if (commentsLength > 0)
                cacheOf(feedWrapper.feedType).get(feedWrapper.photoComments[commentsLength - 1].getID(feedWrapper.feedType)).lastItem = true;
        }

        if (feedWrapper.feedType == FeedType.Public)
            publicCacheLoaded = feedWrapper.exception == null;
        else if (feedWrapper.feedType == FeedType.My)
            myCacheLoaded = feedWrapper.exception == null;

        postChange(feedWrapper);
    }

    OnChangeListener tmp = null;    //don't change to local variable, GC register bug which I reported to google and fixing is in progress

    protected void postChange(FeedWrapper holder) {
        Iterator<OnChangeListener> iterator = callbacks.keySet().iterator();
        while (iterator.hasNext()) {
            tmp = iterator.next();
            tmp.onPageLoaded(holder);
        }
        tmp = null; //don't change, GC bug
    }

    public void registerOnChangeListener(OnChangeListener listener) {
        callbacks.put(listener, null);
    }

    public void unRegisterOnChangeListener(OnChangeListener listener) {
        callbacks.remove(listener);
    }


    protected FeedManager(Session session) {
        mSession = session;
        mSession.getDB().getAllPending(pending);
    }

    //http://kes-middletier-staging.elasticbeanstalk.com/api/wwjd/v1/photo_comments/?filter=feed&page=1&tags=word1%20word2

    public QueryParams feed(FeedType feedType) {
        QueryParams result = new QueryParams(this);
        result.feedWrapper.feedType = feedType;
        return result;
    }

    public void postQuestion(PhotoComment p) {
        for (int i = 0; i < pending.size(); i++)
        {
            PhotoComment tmp = pending.get(i);
            if (tmp.getID() == p.getID() && Utils.strEqual(tmp.message,p.message) && Utils.strEqual(tmp.photo_url, p.photo_url))
            {
                p.status = PhotoComment.PostStatus.Pending;
                TaskPostQuestion.postQuestion(mSession.getContext(), mSession.getAccountManager().getToken(), p.getID(), p.message, p.profile_photo_url, null, p.is_private);
                return;
            }
        }
        throw new IllegalStateException("can't find pending question id=" + Integer.toString(p.getID()));
    }


    public void postQuestion(boolean isPrivate, String question, String picturePath) {
        if (!Utils.strHasValue(question) && !Utils.strHasValue(picturePath))
            throw new IllegalStateException("nothing to post");
        PhotoComment p = new PhotoComment();
        p.status = PhotoComment.PostStatus.Pending;
        p.message = question;
        p.is_private = isPrivate;
        if (picturePath != null)
            p.photo_url = "file://" + picturePath;
        pending.add(p);
        mSession.getDB().addPending(p); //also sets id
        TaskPostQuestion.postQuestion(mSession.getContext(), mSession.getAccountManager().getToken(), p.getID(), question, picturePath, null, p.is_private);
        Iterator<OnChangeListener> iterator = callbacks.keySet().iterator();
        while (iterator.hasNext()) {
            tmp = iterator.next();
            tmp.onPosting(p);
        }
        tmp = null; //don't change, GC bug
    }

    private void updateItemInCache(PhotoComment photoComment)
    {
        if (photoComment == null)
            return;
        FeedCache c = null;
        try {
            c = publicCache.get(photoComment.getID(FeedType.Public));
            if (c != null && !c.photoComment.equals(photoComment))
                c.photoComment = photoComment;
        } catch (NullPointerException ignored) {}

        try {
        c = myCache.get(photoComment.getID(FeedType.My));
        if (c != null && !c.photoComment.equals(photoComment))
            c.photoComment = photoComment;
        } catch (NullPointerException ignored) {}
    }

    protected void updateAction(ResultWrapper wrapper)
    {
        Iterator<OnChangeListener> iterator = callbacks.keySet().iterator();
        while (iterator.hasNext()) {
            tmp = iterator.next();
            switch (wrapper.actionType) {
                case Like:
                    tmp.onLikeResult(wrapper.questionID,wrapper.commentID,wrapper.exception);
                    break;
                case MarkAsPrivate:
                    updateItemInCache(wrapper.photoComment);
                    tmp.onMarkAsPrivateResult(wrapper.photoComment, wrapper.exception);
                    break;
                case MarkAsRead:
                    if (wrapper.user != null)
                        mSession.getAccountManager().updateUnread(wrapper.user.unread_count);
                    if (wrapper.photoComment != null)
                        updateItemInCache(wrapper.photoComment);
                    tmp.onMarkAsReadResult(wrapper.photoComment, wrapper.exception);
                    break;
                case Report:
                    tmp.onReportResult(wrapper.questionID, wrapper.exception);
                    break;
                case Delete:
                    tmp.onDeleteResult(wrapper.questionID, wrapper.commentID, wrapper.exception);
                    break;
                default:
                    break;
            };
        }
        tmp = null; //don't change, GC bug
    }

    public void delete(int questionID,int commentID)
    {
        TaskOther.execute(mSession.getContext(), mSession.getAccountManager().getToken(), ResultWrapper.ActionType.Delete, questionID, commentID);
    }

    public void report(int questionID)
    {
        TaskOther.execute(mSession.getContext(), mSession.getAccountManager().getToken(), ResultWrapper.ActionType.Report, questionID, 0);
    }

    public void like(int questionID, int commentID)
    {
        TaskOther.execute(mSession.getContext(), mSession.getAccountManager().getToken(), ResultWrapper.ActionType.Like, questionID, commentID);
    }

    public void markAsPrivate(int questionID,boolean isPrivate)
    {
        TaskOther.execute(mSession.getContext(), mSession.getAccountManager().getToken(), ResultWrapper.ActionType.MarkAsPrivate, questionID, 0);
    }

    public void markAsRead(int questionID, int commentID)
    {
        Log.d("FeedManager", "Marking as read" + questionID + "/" + commentID);
        TaskOther.execute(mSession.getContext(), mSession.getAccountManager().getToken(), ResultWrapper.ActionType.MarkAsRead, questionID, commentID);
    }

    protected void updatePendingQuestion(PhotoCommentResponseHolder holder) {
        for (int i = 0; i < pending.size(); i++)
        {
            PhotoComment p = pending.get(i);
            if (p.getID() != holder.internalid)
                continue;
            if (holder.exception == null && holder.response != null) {
                p.copyFrom(holder.response);
                pending.remove(p);

                int currentID = holder.response.getID(FeedType.My);

                SparseArray<FeedCache> cache = cacheOf(FeedType.My);
                FeedCache item = cache.get(currentID);

                if (item == null)
                {
                    item = new FeedCache();
                    cache.put(currentID, item);
                }
                item.photoComment = p;
                item.cacheConnected = true;

                mSession.getDB().updatePendingQuestion(holder.internalid, true);
            }
            else {
                p.status = PhotoComment.PostStatus.Error;
                mSession.getDB().updatePendingQuestion(holder.internalid, false);
            }

            Iterator<OnChangeListener> iterator = callbacks.keySet().iterator();
            while (iterator.hasNext()) {
                tmp = iterator.next();
                tmp.onPostResult(p,holder.exception);
            }
            tmp = null; //don't change, GC bug
            return;
        }


        /*
        mSession.getDB().addPending(question, picturePath);
        mSession.getDB().updatePending(holder.sqlid, holder.exception == null);
        */
   }

}
