package com.kes;

import android.content.Context;
import android.os.Bundle;
import android.util.SparseArray;

import com.kes.model.PhotoComment;
import com.kes.net.DataFetcher;

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
        public void onSuggestedQuestions(String[] questions, Exception error);
        public void onMarkAsReadResult(PhotoComment photoComment, Exception error);
        public void onLikeResult(int questionID, int commentID, Exception error);
        public void onReportResult(int questionID, Exception error);
        public void onDeleteResult(int questionID, int commentID, Exception error);
        public void onMarkAsPrivateResult(PhotoComment photoComment, Exception error);
        public void onPosting(PhotoComment photoComment);
        public void onPostResult(com.kes.model.PhotoComment photoComment, Exception error);
        public void onInsufficientCredit();
    }


    private static class FeedCache {

        PhotoComment photoComment;
        public boolean cacheConnected = false;
        public boolean lastItem = false;
    }

    private static final int DEFAULT_PAGE_SIZE = 10;

    private KES mSession;

    private WeakHashMap<OnChangeListener, Void> callbacks = new WeakHashMap<OnChangeListener, Void>();

    SparseArray<FeedCache> publicCache = new SparseArray<FeedCache>();
    SparseArray<FeedCache> myCache = new SparseArray<FeedCache>();
    boolean publicCacheLoaded = false;
    boolean myCacheLoaded = false;
    int minIDPublicFeed = 0;
    int minIDMyFeed = 0;

    //Drop received data if request happened before clearing cache
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

    private boolean isLoaded(FeedType type)
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

            //feedWrapper.page_size = 3;  //TODO:remove
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
            boolean isLoaded = manager.isLoaded(feedWrapper.feedType);
            SparseArray<FeedCache> cache = manager.cacheOf(feedWrapper.feedType);

            for (int i = cache.size() - 1; i >= 0; i--)
            {
                FeedCache item = cache.valueAt(i);
                if (item.cacheConnected) {
                    dest.add(item.photoComment);
                } else
                    break;
            }

            if (!isLoaded)
                return false;

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

        //Check if we have received data
        int commentsLength = feedWrapper.photoComments != null ? feedWrapper.photoComments.length : 0;

        if (!feedWrapper.unreadItems) {
            //If there is anything in the cache with higher ID than top of the feed, remove it
            if (feedWrapper.max_id == null && commentsLength > 0 && cache.size() > 0) {

                int lowestID = feedWrapper.photoComments[commentsLength - 1].getID(feedWrapper.feedType);
                for (int l = cache.size() - 1; l >= 0; l--)
                    if (cache.valueAt(l).photoComment.getID(feedWrapper.feedType) > lowestID)
                        cache.removeAt(l);
            }

            //When feed top is received, mark cached top item as "not connected"
            //If cached and received will overlap, they will be reconnected again few lines below
            if (feedWrapper.max_id == null && cache.size() > 0)
                cache.valueAt(cache.size() - 1).cacheConnected = false;
        }
        if (commentsLength < 1)
            return;

        //Add items to cache one by one
        for (int i = 0; i < feedWrapper.photoComments.length; i++) {
            FeedCache cachedItem = null;
            PhotoComment newItem = feedWrapper.photoComments[i];
            //Try to find it in the cache
            cachedItem = cache.get(newItem.getID(feedWrapper.feedType));
            if (cachedItem == null) {
                cachedItem = new FeedCache();
                cachedItem.photoComment = new PhotoComment();
                cache.put(newItem.getID(feedWrapper.feedType), cachedItem);
            }
            updateItemInCache(newItem); //replace item if different, so app can easily check if there is a change

            if (i != 0)
                cachedItem.cacheConnected = true;
            else {
                //Examine first item of the received feed
                if (feedWrapper.max_id == null)
                    cachedItem.cacheConnected = true;
                else
                {
                    FeedCache tmp = cache.get(feedWrapper.max_id + 1);
                    if (tmp != null)
                        cachedItem.cacheConnected = true;
                }
            }

        }
    }

    protected void updateData(FeedWrapper feedWrapper) {
        //Prevent updating data if clearCache was called after the request
        if (feedWrapper.feedType == FeedType.My && feedWrapper.transactionid < transactionIDMyFeed)
                return;
        if (feedWrapper.feedType == FeedType.Public && feedWrapper.transactionid < transactionIDPublicFeed)
            return;

        int commentsLength = feedWrapper.photoComments != null ? feedWrapper.photoComments.length : 0;

        //Safety feature : mark all public feed items as read to prevent UI from taking care of it
        if (feedWrapper.feedType == FeedType.Public)
            for (int i = 0; i < commentsLength; i++)
                feedWrapper.photoComments[i].setAsRead(true);

        addToCache(feedWrapper);

        //Check if received feed length is shorter than expected (EOF)
        if (feedWrapper.exception == null && feedWrapper.since_id == null && commentsLength < feedWrapper.page_size) {
            feedWrapper.flag_feedBottomReached = true;

            //Min id is last item id or max_id or 0
            int minID = 0;
            if (commentsLength > 0)
                minID = feedWrapper.photoComments[commentsLength - 1].getID(feedWrapper.feedType);
            else if (feedWrapper.max_id != null)
                minID = feedWrapper.max_id;

            if (feedWrapper.feedType == FeedType.Public)
                minIDPublicFeed = minID;
            else if (feedWrapper.feedType == FeedType.My)
                minIDMyFeed = minID;

            if (commentsLength > 0)
                cacheOf(feedWrapper.feedType).get(feedWrapper.photoComments[commentsLength - 1].getID(feedWrapper.feedType)).lastItem = true;
        }

        boolean loaded = (feedWrapper.exception == null && feedWrapper.max_id == null && feedWrapper.unreadItems == false);
        if (feedWrapper.feedType == FeedType.Public)
            publicCacheLoaded = loaded;
        else if (feedWrapper.feedType == FeedType.My)
            myCacheLoaded = loaded;

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


    protected FeedManager(KES session) {
        mSession = session;
        mSession.getDB().getAllPending(pending);
    }

    //http://kes-middletier-staging.elasticbeanstalk.com/api/wwjd/v1/photo_comments/?filter=feed&page=1&tags=word1%20word2

    protected void localeChanged()
    {
        feed(FeedType.Public).clear();
    }

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
        pending.add(0,p);
        mSession.getDB().addPending(p); //also sets id
        TaskPostQuestion.postQuestion(mSession.getContext(), mSession.getAccountManager().getToken(), p.getID(), question, picturePath, null, p.is_private);
        Iterator<OnChangeListener> iterator = callbacks.keySet().iterator();
        while (iterator.hasNext()) {
            tmp = iterator.next();
            tmp.onPosting(p);
        }
        tmp = null; //don't change, GC bug
    }

    protected void clearPendingDB()
    {
        mSession.getDB().clearPending(); //also sets id
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

    protected void updateSuggestions(SuggestionsHolder wrapper, boolean set)
    {
        if (set)
            suggestedQuestions = wrapper;
        Iterator<OnChangeListener> iterator = callbacks.keySet().iterator();
        while (iterator.hasNext()) {
            tmp = iterator.next();
            tmp.onSuggestedQuestions(wrapper.response,wrapper.exception);
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
        TaskOther.markAsPrivate(mSession.getContext(), mSession.getAccountManager().getToken(), questionID, isPrivate);
    }

    public void markAsRead(int questionID, int commentID)
    {
        //Log.d("FeedManager", "Marking as read" + questionID + "/" + commentID);
        TaskOther.execute(mSession.getContext(), mSession.getAccountManager().getToken(), ResultWrapper.ActionType.MarkAsRead, questionID, commentID);
    }

    public static class SuggestionsHolder extends ResultWrapper {
        public String[] response;
    }

    SuggestionsHolder suggestedQuestions;

    public void getSuggestedQuestions()
    {
        if (suggestedQuestions != null)
            updateSuggestions(suggestedQuestions, false);
        else
            TaskSuggestion.execute(mSession.getContext(), mSession.getAccountManager().getToken());
    }

    protected void updatePendingQuestion(PhotoCommentResponseHolder holder) {
        if (holder.user != null)
            mSession.getAccountManager().updateBalance(holder.user.balance);
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

            boolean noCredit = false;
            if (holder.exception != null && holder.exception instanceof DataFetcher.KESNetworkException)
            {
                DataFetcher.KESNetworkException ne = (DataFetcher.KESNetworkException)holder.exception;
                if (ne.error != null && ne.error.code == 3)
                    noCredit = true;
            }
            Iterator<OnChangeListener> iterator = callbacks.keySet().iterator();
            while (iterator.hasNext()) {
                tmp = iterator.next();
                tmp.onPostResult(p,holder.exception);
                if (noCredit)
                    tmp.onInsufficientCredit();
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
