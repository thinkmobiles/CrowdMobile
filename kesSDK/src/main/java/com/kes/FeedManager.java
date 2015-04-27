package com.kes;

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
        public void onUnread(FeedWrapper wrapper);
        public void onPageLoaded(FeedWrapper wrapper);
        public void onMarkAsReadResult(int questionID, int commentID, Exception error);
        public void onLikeResult(int questionID, int commentID, Exception error);
        public void onReportResult(int questionID, Exception error);
        public void onDeleteResult(int questionID, int commentID, Exception error);
        public void onMarkAsPrivateResult(int questionID, Exception error);
        public void onPostResult(int questionID, Exception error);
    }


    private int internalID = 0;
    private Session mSession;

    private WeakHashMap<OnChangeListener, Void> callbacks = new WeakHashMap<OnChangeListener, Void>();

    SparseArray<PhotoComment> publicCache = new SparseArray<PhotoComment>();
    SparseArray<PhotoComment> myCache = new SparseArray<PhotoComment>();
    boolean publicCacheLoaded = false;
    boolean myCacheLoaded = false;
    int minIDPublicFeed = 0;
    int minIDMyFeed = 0;

    int transactionIDPublicFeed = 0;
    int transactionIDMyFeed = 0;

    ArrayList<PhotoComment> pending = new ArrayList<PhotoComment>();

    public enum FeedType {Public, My};

    private SparseArray<PhotoComment>getCache(FeedType type)
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
        public FeedType feedType = FeedType.Public;
        public Integer since_id;
        public Integer max_id;
        public Integer page_size = 10;
        public String tags;
        public PhotoComment comments[];
        public boolean flag_feedBottomReached = false;
        protected int transactionid;

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
            SparseArray<PhotoComment> cache = manager.getCache(feedWrapper.feedType);
            int len = cache.size();

            if (feedWrapper.feedType == FeedType.Public.My)
                len += manager.pending.size();

            if (len == 0)
                return manager.getLoaded(feedWrapper.feedType);
            int idx = 0;

            if (feedWrapper.feedType == FeedType.Public.My)
                for (int i = manager.pending.size() ; i > 0; i--)
                    dest.add(manager.pending.get(i - 1));

            for (int i = 0, l = cache.size(); i < l ; i++) {
                PhotoComment item = cache.valueAt(l - i - 1);
                dest.add(cache.valueAt(l - i - 1));
                if (item.flag_last)
                    break;
            }
            return manager.getLoaded(feedWrapper.feedType);
        }
    }

    public void checkUnread()
    {
        TaskCheckUnread.loadFeed(mSession.getContext(), mSession.getAccountManager().getToken());
    }

    protected void updateUnread(FeedWrapper feedWrapper)
    {
        Iterator<OnChangeListener> iterator = callbacks.keySet().iterator();
        while (iterator.hasNext()) {
            tmp = iterator.next();
            tmp.onUnread(feedWrapper);
        }
        tmp = null; //don't change, GC bug

    }

    protected void updateData(FeedWrapper feedWrapper) {
        if (feedWrapper.feedType == FeedType.My && feedWrapper.transactionid < transactionIDMyFeed)
                return;
        if (feedWrapper.feedType == FeedType.Public && feedWrapper.transactionid < transactionIDPublicFeed)
            return;

        if (feedWrapper.exception == null && (feedWrapper.comments == null || feedWrapper.comments.length == 0))
        {
            int maxid = 0;
            if (feedWrapper.max_id != null)
                maxid = feedWrapper.max_id;

            if (feedWrapper.feedType == FeedType.Public)
                minIDPublicFeed = maxid;
            else if (feedWrapper.feedType == FeedType.My)
                minIDMyFeed = maxid;
            feedWrapper.flag_feedBottomReached = true;
        }

        SparseArray<PhotoComment> cache = getCache(feedWrapper.feedType);
        if (feedWrapper.feedType == FeedType.Public)
            publicCacheLoaded = feedWrapper.exception == null;
        else if (feedWrapper.feedType == FeedType.My)
            myCacheLoaded = feedWrapper.exception == null;

        if (feedWrapper.comments != null && feedWrapper.comments.length > 0 && feedWrapper.max_id != null)
        {
            PhotoComment item = cache.get(feedWrapper.max_id + 1);
            if (item != null)
                item.flag_last = false;
        }

        if (feedWrapper.comments != null && feedWrapper.comments.length > 0) {

            feedWrapper.comments[0].flag_first = true;
            feedWrapper.comments[feedWrapper.comments.length - 1].flag_last = true;

            for (int i = 0; i < feedWrapper.comments.length; i++) {
                PhotoComment newItem = feedWrapper.comments[i];
                PhotoComment oldItem = cache.get(feedWrapper.comments[i].id);
                if (oldItem != null)
                {
                    oldItem.flag_first &= newItem.flag_first;
                    oldItem.flag_last &= newItem.flag_last;
                    continue;
                }
                cache.put(newItem.id, newItem);
            }
        }
        postChange(feedWrapper);
    }

    OnChangeListener tmp = null;    //don't change to local variable, GC register bug which I reported to google and fixing is in progress

    public void postChange(FeedWrapper holder) {
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


    public void postQuestion(String question, String picturePath) {
        if (!Utils.strHasValue(question) && !Utils.strHasValue(picturePath))
            throw new IllegalStateException("nothing to post");
        PhotoComment p = new PhotoComment();
        p.status = PhotoComment.PostStatus.Pending;
        p.id = internalID ++;
        p.message = question;
        if (picturePath != null)
            p.photo_url = "file://" + picturePath;
        pending.add(p);
        TaskPostQuestion.postQuestion(mSession.getContext(), mSession.getAccountManager().getToken(), p.id, question, picturePath, null, false);
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
                    tmp.onMarkAsPrivateResult(wrapper.questionID, wrapper.exception);
                    break;
                case MarkAsRead:
                    tmp.onMarkAsReadResult(wrapper.questionID, wrapper.commentID, wrapper.exception);
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
        TaskOther.execute(mSession.getContext(), mSession.getAccountManager().getToken(), ResultWrapper.ActionType.MarkAsRead, questionID, commentID);
    }

    protected void updatePostedQuestion(PhotoCommentResponseHolder holder) {
        for (int i = 0; i < pending.size(); i++)
        {
            PhotoComment p = pending.get(i);
            if (p.id == holder.internalid)
            {
                if (holder.exception == null && holder.response != null) {
                    p.copyFrom(holder.response);
                    pending.remove(p);
                    getCache(FeedType.My).put(holder.response.id, p);
                }
                else
                    p.status = PhotoComment.PostStatus.Error;
            }
            break;
        }

        Iterator<OnChangeListener> iterator = callbacks.keySet().iterator();
        while (iterator.hasNext()) {
            tmp = iterator.next();
            tmp.onPostResult(holder.questionID,holder.exception);
        }
        tmp = null; //don't change, GC bug

        /*
        mSession.getDB().addPending(question, picturePath);
        mSession.getDB().updatePending(holder.sqlid, holder.exception == null);
        */
   }

}
