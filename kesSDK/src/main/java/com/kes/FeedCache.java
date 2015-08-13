package com.kes;

import android.util.SparseArray;

import com.kes.model.PhotoComment;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.WeakHashMap;

/**
 * Created by gadza on 2015.07.31..
 */
public class FeedCache {

    public interface OnUpdateListener {
        void onItemInserted(int position);
        void onItemRemoved(int position);
        void onItemMoved(int fromPosition,int toPosition);
        void onItemUpdated(int position);
    }

    class FeedCacheItem {

        PhotoComment photoComment;
        public boolean cacheConnected = false;
        public boolean lastItem = false;
    }

    private FeedArray cache = new FeedArray();
    private FeedManager.FeedType mFeedType;
    private boolean mLoaded;
    private int eofID = 0;

    private ArrayList<PhotoComment> backCache = new ArrayList<PhotoComment>();

    public FeedCache(FeedManager.FeedType feedType)
    {
        mFeedType = feedType;
    }

    public FeedArray getCache()
    {
        return cache;
    }

    public boolean isLoaded()
    {
        return mLoaded;
    }

    public long get_EOF_ID()
    {
        return eofID;
    }

    protected Integer getHighestID()
    {
        int size = internalCache.size();
        if (size == 0)
            return null;
        return internalCache.valueAt(size - 1).photoComment.getID(mFeedType);
    }

    public boolean isEOF(int id)
    {
        return (internalCache.get(id).lastItem);
    }

    private SparseArray<FeedCacheItem> internalCache = new SparseArray<FeedCacheItem>();
    private ArrayList<PhotoComment> pendingItems = new ArrayList<PhotoComment>();

    protected ArrayList<PhotoComment> getPendingItems()
    {
        return pendingItems;
    }

    protected void insertPendingItem(PhotoComment p)
    {
        pendingItems.add(0, p);
        notifyListeners(UpdateType.insert, 0);
    }

    protected void updatePendingItem(PhotoComment p)
    {
        for (int i = 0; i < pendingItems.size(); i++)
        {
            PhotoComment tmp = pendingItems.get(i);
            if (tmp.getID() == p.getID() && Utils.strEqual(tmp.message,p.message) && Utils.strEqual(tmp.photo_url, p.photo_url))
            {
                notifyListeners(UpdateType.update,i);
                return;
            }
        }
        throw new IllegalStateException(idNotFound(p.getID()));
    }

    private String idNotFound(long internalID)
    {
        return "can't find pending question id=" + Long.toString(internalID);
    }

    protected PhotoComment getPendingItemByID(long internalID)
    {
        for (int i = 0; i < pendingItems.size(); i++) {
            PhotoComment tmp = pendingItems.get(i);
            if (tmp.getID() == internalID)
                return tmp;
        }
        throw new IllegalStateException(idNotFound(internalID));
    }

    protected void setPendingItemPosted(long internalID, PhotoComment updated)
    {
        int pendingIdx = -1;
        int permanentIdx = -1;
        for (int i = 0; i < pendingItems.size(); i++)
        {
            PhotoComment tmp = pendingItems.get(i);
            if (tmp.getID(mFeedType) == internalID)
            {
                pendingItems.remove(i);
                pendingIdx = i;
                break;
            }
        }
        if (pendingIdx == -1)
            throw new IllegalStateException("pending item " + internalID + " not found in cache");

        int currentID = updated.getID(mFeedType);
        FeedCacheItem item = internalCache.get(currentID);
        boolean moved = false;
        if (item == null)
        {
            item = new FeedCacheItem();
            internalCache.put(currentID, item);
            moved = true;
        }
        item.photoComment = updated;
        item.cacheConnected = true;
        permanentIdx = internalPosToExternal(internalCache.indexOfKey(currentID));
        if (moved)
            notifyListeners(UpdateType.move, pendingIdx, permanentIdx);
        else
            notifyListeners(UpdateType.remove, pendingIdx);

        notifyListeners(UpdateType.update, permanentIdx);
    }

    private WeakHashMap<OnUpdateListener, Void> updateListeners = new WeakHashMap<OnUpdateListener, Void>();

    public void registerOnUpdateListener(OnUpdateListener listener) {
        updateListeners.put(listener, null);
    }

    public void unRegisterOnUpdateListener(OnUpdateListener listener) {
        updateListeners.remove(listener);
    }


    OnUpdateListener updateTmp = null;    //don't change to local variable, GC register bug which I reported to google and fixing is in progress

    enum UpdateType {insert,remove,move,update};

    protected void notifyListeners(UpdateType updateType, int position) {
        notifyListeners(updateType, position, 0);
    }

    protected void notifyListeners(UpdateType updateType, int position, int position2) {
        Iterator<OnUpdateListener> iterator = updateListeners.keySet().iterator();
        while (iterator.hasNext()) {
            updateTmp = iterator.next();
            switch (updateType) {
                case insert:
                    updateTmp.onItemInserted(position);
                    break;
                case remove:
                    updateTmp.onItemRemoved(position);
                    break;
                case move:
                    if (position != position2)
                        updateTmp.onItemMoved(position,position2);
                    break;
                case update:
                    updateTmp.onItemUpdated(position);
                    break;
                default:
                    break;
            }

        }
        updateTmp = null; //don't change, GC bug
    }



    protected boolean updateItem(PhotoComment photoComment)
    {
        if (photoComment == null)
            return false;
        int idx = internalCache.indexOfKey(photoComment.getID(mFeedType));
        if (idx < 0)
            return false;
        FeedCacheItem item = internalCache.valueAt(idx);
        if (!item.photoComment.equals(photoComment)) {
            item.photoComment = photoComment;
            notifyListeners(UpdateType.update, internalPosToExternal(idx));
        }
        return true;
    }


    public void clear() {
        mLoaded = false;
        cache.removeFooter();
        for (int i = 0; i < cache.size(); i++)
            notifyListeners(UpdateType.remove, 0);
        pendingItems.clear();
        internalCache.clear();
    }

    private void cutInvalid()
    {
        int found = -1;
        for (int i = internalCache.size(); i > 0; i--)
        {
            FeedCacheItem item = internalCache.valueAt(i - 1);
            if (item.cacheConnected == false)
            {
                found = i;
                break;
            }
        }
        if (found == -1)
            return;
        for (int i = found; i > 0; i--)
        {
            int notifyPos = internalPosToExternal(i - 1);
            internalCache.removeAt(i - 1);
            notifyListeners(UpdateType.remove,notifyPos);
        }
    }

    private int internalPosToExternal(int internalPos)
    {
        return (internalCache.size() - 1) - internalPos + pendingItems.size();
    }

    private static String cache_exception = "cache can't be updated directly";

    public class FeedArray extends AbstractList<PhotoComment> {
        private boolean footer = false;

        @Override
        public void add(int location, PhotoComment object) {
            throw new IllegalStateException(cache_exception);
        }

        @Override
        public boolean add(PhotoComment object) {
            throw new IllegalStateException(cache_exception);
        }

        @Override
        public boolean addAll(int location, Collection<? extends PhotoComment> collection) {
            throw new IllegalStateException(cache_exception);
        }

        @Override
        public PhotoComment remove(int location) {
            throw new IllegalStateException(cache_exception);
        }

        @Override
        protected void removeRange(int start, int end) {
            throw new IllegalStateException(cache_exception);
        }

        @Override
        public PhotoComment get(int location) {
            int maxPos = size();
            if (location < 0 || location > maxPos - 1)
                throw new IndexOutOfBoundsException();
            if (location < pendingItems.size())
                return pendingItems.get(location);
            if (footer && location == maxPos - 1)
                    return null;
            location -= pendingItems.size();
            return internalCache.valueAt(internalCache.size() - location - 1).photoComment;
        }

        @Override
        public int size() {
            int size = pendingItems.size() + internalCache.size();
            if (footer)
                size ++;
            return size;
        }

        public void insertFooter()
        {
            if (footer)
                notifyListeners(UpdateType.update, size() - 1);
            else {
                footer = true;
                notifyListeners(UpdateType.insert,size() - 1);
            }
        }

        public void removeFooter()
        {
            if (!footer)
                return;
            footer = false;
            notifyListeners(UpdateType.remove,size() - 1);
        }

    }

    protected void addItem(PhotoComment p)
    {
        if (p == null)
            return;
        if (updateItem(p))
            return;

        int id = p.getID(mFeedType);
        if (internalCache.size() == 0)
            return;
        int maxid = internalCache.valueAt(internalCache.size() - 1).photoComment.getID(mFeedType);
        int minid = internalCache.valueAt(0).photoComment.getID(mFeedType);
        if (id > maxid + 1|| id < minid - 1)
            return;
        FeedCacheItem item = new FeedCacheItem();
        item.photoComment = p;
        item.cacheConnected = true;
        internalCache.put(id, item);
        notifyListeners(UpdateType.insert, internalPosToExternal(internalCache.indexOfKey(id)));
    }

    protected void removeItem(PhotoComment p)
    {
        if (p == null)
            return;
        int i = internalCache.indexOfKey(p.getID(mFeedType));
        if (i < 0)
            return;
        int notifyPos = internalPosToExternal(i);
        internalCache.removeAt(i);
        notifyListeners(UpdateType.remove, notifyPos);
    }

    SparseArray<PhotoComment> tmpArray = new SparseArray<PhotoComment>();

    //Called from FeedManager
    protected void updateData(FeedManager.FeedWrapper feedWrapper)
    {
        int commentsLength = feedWrapper.photoComments != null ? feedWrapper.photoComments.length : 0;

        mLoaded |= (feedWrapper.exception == null && feedWrapper.max_id == null && feedWrapper.unreadItems == false);

        if (mFeedType != feedWrapper.feedType && feedWrapper.photoComments != null)
        {
            for (int i = 0; i < feedWrapper.photoComments.length; i++)
                updateItem(feedWrapper.photoComments[i]);
            return;
        }

        //if cache has higher items than top of the feed, remove them
        if (feedWrapper.max_id == null && !feedWrapper.unreadItems && feedWrapper.photoComments != null && feedWrapper.photoComments.length > 0)
        {
            int highestID = feedWrapper.photoComments[0].getID(mFeedType);
            for (int i = internalCache.size(); i > 0; i--)
            {
                if (internalCache.valueAt(i - 1).photoComment.getID(mFeedType) > highestID) {
                    int notifyPos = internalPosToExternal(i - 1);
                    internalCache.removeAt(i - 1);
                    notifyListeners(UpdateType.remove, notifyPos);
                } else
                    break;
            }
        }

        if (feedWrapper.photoComments != null && feedWrapper.photoComments.length > 2)
        {
            tmpArray.clear();
            for (int i = 0; i < feedWrapper.photoComments.length; i++)
                tmpArray.put(feedWrapper.photoComments[i].getID(feedWrapper.feedType), feedWrapper.photoComments[i]);
        }

        //Check if we have received data

        if (feedWrapper.exception == null && !feedWrapper.unreadItems && feedWrapper.max_id == null && internalCache.size() > 0)
            internalCache.valueAt(internalCache.size() - 1).cacheConnected = false;

        if (commentsLength < 1) {
            cutInvalid();
            return;
        }

        //Add items to cache one by one
        for (int i = 0; i < feedWrapper.photoComments.length; i++) {
            FeedCacheItem cachedItem = null;
            PhotoComment newItem = feedWrapper.photoComments[i];
            //Try to find it in the cache
            int newID = newItem.getID(feedWrapper.feedType);
            cachedItem = internalCache.get(newID);
            if (cachedItem == null) {
                cachedItem = new FeedCacheItem();
                cachedItem.photoComment = new PhotoComment();
                internalCache.put(newID, cachedItem);
                cachedItem.photoComment = newItem;
                notifyListeners(UpdateType.insert, internalPosToExternal(internalCache.indexOfKey(newID)));
            } else {
                if (!cachedItem.photoComment.equals(newItem)) {
                    cachedItem.photoComment = newItem;
                    notifyListeners(UpdateType.update, internalPosToExternal(internalCache.indexOfKey(newID)));
                }
            }

            if (i != 0)
                cachedItem.cacheConnected = true;
            else {
                //Examine first item of the received feed
                if (feedWrapper.max_id == null)
                    cachedItem.cacheConnected = true;
                else
                {
                    FeedCacheItem tmp = internalCache.get(feedWrapper.max_id + 1);
                    if (tmp != null)
                        cachedItem.cacheConnected = true;
                }
            }

        }

        //Check if received feed length is shorter than expected (EOF)
        if (feedWrapper.exception == null && feedWrapper.since_id == null && commentsLength < feedWrapper.page_size) {
            feedWrapper.flag_feedBottomReached = true;

            //Min id is last item id or max_id or 0
            eofID = 0;
            if (commentsLength > 0)
                eofID = feedWrapper.photoComments[commentsLength - 1].getID(feedWrapper.feedType);
            else if (feedWrapper.max_id != null)
                eofID = feedWrapper.max_id;

            if (commentsLength > 0)
                internalCache.get(feedWrapper.photoComments[commentsLength - 1].getID(feedWrapper.feedType)).lastItem = true;
        }
        cutInvalid();
    }

}
