package com.kes;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;

import com.kes.model.PhotoComment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.WeakHashMap;

/**
 * Created by gadza on 2015.03.05..
 */
public class FeedManager {

    public interface OnChangeListener {
        public boolean onUnread(FeedWrapper wrapper);
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
        public static int ID_NONE = -1;
        public static int ID_END = -2;

        PhotoComment photoComment;
        public int highID = ID_NONE;
        public int lowID = ID_NONE;
    }

    private static final int DEFAULT_PAGE_SIZE = 10;

    private int internalID = 0;
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

    private SparseArray<FeedCache>getCache(FeedType type)
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
        protected boolean unreadOnly = false;

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
            return (manager.getCache(feedWrapper.feedType).get(id).lowID == FeedCache.ID_END);
        }

        public void load() {
            String token = null;

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


        public boolean getCache(ArrayList<PhotoComment> dest)
        {
            SparseArray<FeedCache> cache = manager.getCache(feedWrapper.feedType);
            int len = cache.size();

            if (feedWrapper.feedType == FeedType.Public.My)
                len += manager.pending.size();

            if (len == 0)
                return manager.getLoaded(feedWrapper.feedType);
            int idx = 0;

            if (feedWrapper.feedType == FeedType.Public.My)
                for (int i = manager.pending.size() ; i > 0; i--)
                    dest.add(manager.pending.get(i - 1));

            //return feed between two end points
            boolean foundFirst = false;
            for (int i = cache.size(); i > 0; i--)
            {
                FeedCache item = cache.valueAt(i - 1);
                if (!foundFirst)
                {
                    if (item.highID == FeedCache.ID_END)
                        foundFirst = true;
                    else
                        continue;
                }
                dest.add(item.photoComment);
                if (item.lowID == FeedCache.ID_END)
                    break;
            }

            return manager.getLoaded(feedWrapper.feedType);
        }
    }

    public void checkUnread()
    {
        Integer maxID = null;
        if (getCache(FeedType.My).size() > 0)
            maxID = getCache(FeedType.My).keyAt(0);
        TaskCheckUnread.loadFeed(mSession.getContext(), mSession.getAccountManager().getToken(), maxID);
    }

    protected void updateUnread(Context context, FeedWrapper feedWrapper)
    {
        if (!feedWrapper.unreadOnly)
        {
            updateData(feedWrapper);
            return;
        }

        if (feedWrapper.photoComments == null || feedWrapper.photoComments.length == 0)
            return;

        for (int i = 0; i < feedWrapper.photoComments.length; i++) {
            FeedCache item = myCache.get(feedWrapper.photoComments[i].id);
            if (item == null)
                continue;
            item.photoComment.responses = feedWrapper.photoComments[i].responses;
        }

        boolean handled = false;
        Iterator<OnChangeListener> iterator = callbacks.keySet().iterator();
        while (iterator.hasNext()) {
            tmp = iterator.next();
            handled |= tmp.onUnread(feedWrapper);
        }
        tmp = null; //don't change, GC bug

        //compare highest unread
        //if (feedWrapper.max_id <= PreferenceUtil.getHighestUnreadID(context))
        //    return;
        PreferenceUtil.setHighestUnreadID(context,feedWrapper.max_id);

        if (handled)
            return;
    }

    private void addToCache(FeedWrapper feedWrapper)
    {
        int commentsLength = feedWrapper.photoComments != null ? feedWrapper.photoComments.length : 0;
        if (commentsLength < 1)
            return;

        SparseArray<FeedCache> cache = getCache(feedWrapper.feedType);
        int lowID = 0;
        int highID = 0;

        for (int i = 0; i < feedWrapper.photoComments.length; i++) {
            FeedCache item = null;
            PhotoComment newItem = feedWrapper.photoComments[i];
            item = cache.get(feedWrapper.photoComments[i].id);
            if (item == null)
            {
                item = new FeedCache();
                item.photoComment = new PhotoComment();
                cache.put(newItem.id, item);
            }
            updateItemInCache(newItem);
        }

        for (int i = 0; i < commentsLength - 1; i++) {
            highID = feedWrapper.photoComments[i].id;
            lowID = feedWrapper.photoComments[i + 1].id;

            cache.get(highID).lowID = lowID;
            cache.get(lowID).highID = highID;
        }


        //Check start
        if (feedWrapper.max_id == null && feedWrapper.since_id == null) {
            for (int i = 0; i < cache.size(); i++)
                if (cache.valueAt(i).highID == FeedCache.ID_END)
                    cache.valueAt(i).highID = FeedCache.ID_NONE;
            cache.get(feedWrapper.photoComments[0].id).highID = FeedCache.ID_END;
        }

        lowID = feedWrapper.photoComments[0].id;
        highID = feedWrapper.photoComments[commentsLength - 1].id;
        if (lowID > highID) {
            int tmp = lowID;
            lowID = highID;
            highID = tmp;
        }

        FeedCache tmp = cache.get(lowID - 1);
        if (tmp != null)
            cache.get(lowID).lowID = lowID - 1;
        tmp = cache.get(highID + 1);
        if (tmp != null)
            cache.get(highID).highID = highID + 1;

    }

    protected void updateData(FeedWrapper feedWrapper) {
        if (feedWrapper.feedType == FeedType.My && feedWrapper.transactionid < transactionIDMyFeed)
                return;
        if (feedWrapper.feedType == FeedType.Public && feedWrapper.transactionid < transactionIDPublicFeed)
            return;

        int commentsLength = feedWrapper.photoComments != null ? feedWrapper.photoComments.length : 0;

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
                getCache(feedWrapper.feedType).get(feedWrapper.photoComments[commentsLength - 1].id).lowID = FeedCache.ID_END;
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
    //    mSession.getDB().getAllPending(pending);
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
            if (tmp.id == p.id && Utils.strEqual(tmp.message,p.message) && Utils.strEqual(tmp.photo_url, p.photo_url))
            {
                p.status = PhotoComment.PostStatus.Pending;
                TaskPostQuestion.postQuestion(mSession.getContext(), mSession.getAccountManager().getToken(), p.id, p.message, p.profile_photo_url, null, p.is_private);
                return;
            }
        }
        throw new IllegalStateException("can't find pending question id=" + Integer.toString(p.id));
    }


    public void postQuestion(boolean isPrivate, String question, String picturePath) {
        if (!Utils.strHasValue(question) && !Utils.strHasValue(picturePath))
            throw new IllegalStateException("nothing to post");
        PhotoComment p = new PhotoComment();
        p.status = PhotoComment.PostStatus.Pending;
        p.id = internalID ++;
        p.message = question;
        p.is_private = isPrivate;
        if (picturePath != null)
            p.photo_url = "file://" + picturePath;
        pending.add(p);
        TaskPostQuestion.postQuestion(mSession.getContext(), mSession.getAccountManager().getToken(), p.id, question, picturePath, null, p.is_private);
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
        FeedCache c = publicCache.get(photoComment.id);
        if (c != null)
            c.photoComment.copyFrom(photoComment);
        c = myCache.get(photoComment.id);
        if (c != null)
            c.photoComment.copyFrom(photoComment);
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

    public void markAsPrivate(int questionID)
    {
        TaskOther.execute(mSession.getContext(), mSession.getAccountManager().getToken(), ResultWrapper.ActionType.MarkAsPrivate, questionID, 0);
    }

    public void markAsRead(int questionID, int commentID)
    {
        Log.d("FeedManager", "Marking as read" + questionID + "/" + commentID);
        TaskOther.execute(mSession.getContext(), mSession.getAccountManager().getToken(), ResultWrapper.ActionType.MarkAsRead, questionID, commentID);
    }

    protected void updatePostedQuestion(PhotoCommentResponseHolder holder) {
        for (int i = 0; i < pending.size(); i++)
        {
            PhotoComment p = pending.get(i);
            if (p.id != holder.internalid)
                continue;
            if (holder.exception == null && holder.response != null) {
                p.copyFrom(holder.response);
                pending.remove(p);
                FeedCache item = new FeedCache();
                item.photoComment = p;
                getCache(FeedType.My).put(holder.response.id, item);
            }
            else
                p.status = PhotoComment.PostStatus.Error;

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
