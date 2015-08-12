package com.kes;

import android.content.Context;
import android.os.Bundle;

import com.kes.model.PhotoComment;
import com.kes.net.DataFetcher;

import java.io.File;
import java.util.Iterator;
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




    private static final int DEFAULT_PAGE_SIZE = 10;

    private KES mSession;

    private WeakHashMap<OnChangeListener, Void> callbacks = new WeakHashMap<OnChangeListener, Void>();

    int minIDPublicFeed = 0;
    int minIDMyFeed = 0;


    private FeedCache publicCache;
    private FeedCache myCache;

    public enum FeedType {Public, My};

    public FeedCache cacheOf(FeedType type)
    {
        if (type == FeedType.Public)
            return publicCache;
        else if (type == FeedType.My)
            return myCache;
        return null;
    }

    public static class PhotoCommentResponseHolder extends ResultWrapper {
        public long internalid;
        public String filePath;
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


        public void load() {
            //feedWrapper.page_size = 3;  //TODO:remove
            //If we already know the bottom end of the feed, reject lower id requests
            if (feedWrapper.max_id != null && feedWrapper.max_id <= manager.cacheOf(feedWrapper.feedType).get_EOF_ID())
            {
                feedWrapper.flag_feedBottomReached = true;
                manager.postChange(feedWrapper);
                return;
            }

            TaskLoadFeed.loadFeed(manager.mSession.getContext(),
                    manager.mSession.getAccountManager().getToken(feedWrapper.feedType == FeedType.Public),
                    feedWrapper);
        }
    }

    public void checkUnread()
    {
        TaskCheckUnread.loadFeed(mSession.getContext(), mSession.getAccountManager().getToken(), myCache.getHighestID());
    }

    protected void updateUnread(Context context, FeedWrapper feedWrapper)
    {
        if (feedWrapper.exception == null)
            mSession.getAccountManager().updateUnread(feedWrapper.user.unread_count);
        updateData(feedWrapper);
    }


    protected void updateData(FeedWrapper feedWrapper) {

        int commentsLength = feedWrapper.photoComments != null ? feedWrapper.photoComments.length : 0;

        //Safety feature : mark all public feed items as read to prevent UI from taking care of it
        if (feedWrapper.feedType == FeedType.Public)
            for (int i = 0; i < commentsLength; i++)
                feedWrapper.photoComments[i].setAsRead(true);

        cacheOf(feedWrapper.feedType).updateData(feedWrapper);

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
        publicCache = new FeedCache(FeedType.Public);
        myCache = new FeedCache(FeedType.My);
        mSession.getDB().getAllPending(myCache.getPendingItems());
    }

    //http://kes-middletier-staging.elasticbeanstalk.com/api/wwjd/v1/photo_comments/?filter=feed&page=1&tags=word1%20word2

    protected void localeChanged()
    {
        cacheOf(FeedType.Public).clear();
        suggestedQuestions = null;
    }

    public QueryParams feed(FeedType feedType) {
        QueryParams result = new QueryParams(this);
        result.feedWrapper.feedType = feedType;
        return result;
    }

    int dstid = 310000;
    public void postQuestion(PhotoComment p) {
        /*
        PhotoComment p2 = new PhotoComment();
        p2.copyFrom(p);
        p2.status = PhotoComment.PostStatus.Posted;
        p2.setID(dstid++);
        myCache.moveInsertedItem(p.getID(FeedType.My), p2);
        */
        p.status = PhotoComment.PostStatus.Pending;
        myCache.updatePendingItem(p);
        TaskPostQuestion.postQuestion(mSession.getContext(), mSession.getAccountManager().getToken(), p.getID(), p.message, p.photo_url, null, p.is_private);
    }


    public void postQuestion(boolean isPrivate, String question, String picturePath) {
        if (!Utils.strHasValue(question) && !Utils.strHasValue(picturePath))
            throw new IllegalStateException("nothing to post");
        PhotoComment p = new PhotoComment();
        p.status = PhotoComment.PostStatus.Pending;
        p.message = question;
        p.is_private = isPrivate;
        if (picturePath != null)
            p.photo_url = picturePath;
        myCache.insertPendingItem(p);
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


    protected void updateAction(ResultWrapper wrapper)
    {

        switch (wrapper.actionType) {
            case MarkAsPrivate:
                if (wrapper.photoComment != null)
                {
                    if (!wrapper.photoComment.is_private)
                        publicCache.addItem(wrapper.photoComment);
                    else
                        publicCache.removeItem(wrapper.photoComment);
                }
                myCache.updateItem(wrapper.photoComment);
                break;
            case MarkAsRead:
                if (wrapper.user != null)
                    mSession.getAccountManager().updateUnread(wrapper.user.unread_count);
                publicCache.updateItem(wrapper.photoComment);
                myCache.updateItem(wrapper.photoComment);
                break;
            default:
                break;
        }

        Iterator<OnChangeListener> iterator = callbacks.keySet().iterator();
        while (iterator.hasNext()) {
            tmp = iterator.next();
            switch (wrapper.actionType) {
                case Like:
                    tmp.onLikeResult(wrapper.questionID,wrapper.commentID,wrapper.exception);
                    break;
                case MarkAsPrivate:
                    tmp.onMarkAsPrivateResult(wrapper.photoComment, wrapper.exception);
                    break;
                case MarkAsRead:
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
        if (holder.exception == null && holder.photoComment != null) {
            mSession.getDB().updatePendingQuestion(holder.internalid, true);
            myCache.setPendingItemPosted(holder.internalid, holder.photoComment);
            if (holder.filePath != null)
            {
                File f = new File(holder.filePath);
                f.delete();
            }
            return;
        }

        mSession.getDB().updatePendingQuestion(holder.internalid, false);
        PhotoComment item = myCache.getPendingItemByID(holder.internalid);
        item.status = PhotoComment.PostStatus.Error;
        myCache.updatePendingItem(item);


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
            tmp.onPostResult(item, holder.exception);
            if (noCredit)
                tmp.onInsufficientCredit();
        }
        tmp = null; //don't change, GC bug
        return;

   }


}
