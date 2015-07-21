package com.crowdmobile.kesapp.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.crowdmobile.kesapp.MainActivity;
import com.crowdmobile.kesapp.MainActivityInterface;
import com.crowdmobile.kesapp.R;
import com.crowdmobile.kesapp.adapter.FeedAdapter;
import com.crowdmobile.kesapp.widget.NavigationBar;
import com.kes.FeedManager;
import com.kes.KES;
import com.kes.model.PhotoComment;

import java.util.ArrayList;

public abstract class FeedBaseFragment extends Fragment {

    private static final int CHECK_UNREAD_INTERVAL = 1000;  //msec
    private static final int READ_DELAY = 5000;  //msec

    private static final String TAG = FeedBaseFragment.class.getSimpleName();
    protected RecyclerView rvFeed;
    private LinearLayoutManager mLayoutManager;
    private FeedAdapter adapter;
    private ArrayList<PhotoComment> list;
    //    View itemTitle;
//    View itemShare;
    private boolean scrollInitialized;
    private boolean titleVisible = false;
    //    FeedItem.ShareController shareController;

    private FeedManager.QueryParams refreshNetworkAction = null;
    private FeedManager.QueryParams nextPageNetworkAction = null;

    private SwipeRefreshLayout swipeContainer;
    private int minID = 0;
    private boolean hasFooterView = false;
    private boolean bottomReached = false;
    private AccessFragment.AccessViewHolder accessViewHolder;
    private ProgressBar progressBar;
    private ReadHandler mHandler;
    private boolean isStarted = false;
    private boolean isVisibleToUser = false;
    private Rect scrollBounds;
    private boolean firstShow;
    private boolean loadingNextPage;
    private MainActivityInterface mainActivityInterface;

    class ReadTag {
        public long oldShownAt;
        public long shownAt;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mainActivityInterface = (MainActivityInterface)activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mainActivityInterface = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scrollBounds = new Rect();
        list = new ArrayList<PhotoComment>();
        adapter = new FeedAdapter(getActivity(), getFeedType(), list, feedAdapterListener);
        mHandler = new ReadHandler();
    }

    class ReadHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d("Readmessage", "Message read" + msg.what);
        }
    };

    FeedAdapter.FeedAdapterListener feedAdapterListener = new FeedAdapter.FeedAdapterListener() {
        /*
        @Override
        public void onLastItemReached() {
            if (list.size() > 0) {
                lastNetworkAction =
                        session.getFeedManager().feed(getFeedType())
                                .setMaxID(list.get(list.size() - 1).id - 1);
                lastNetworkAction.load();
            }
        }
        */

        @Override
        public void retryLoadClick() {
            if (refreshNetworkAction != null) {
                adapter.setFooterLoading(true);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshNetworkAction.load();

                    }
                },2000);
            }
        }

        @Override
        public void retryPostClick(PhotoComment p) {
            p.status = PhotoComment.PostStatus.Pending;
            KES.shared().getFeedManager().postQuestion(p);
        }

        @Override
        public boolean like(PhotoComment p) {
            if (p.liked)
                return false;
            if (!KES.shared().getAccountManager().getUser().isRegistered())
                return false;
            p.liked = true;
            p.responses[0].likes_count ++;
           // KES.shared().getFeedManager().like(p.getID(),p.responses[0].id);
            return true;
        }

        @Override
        public boolean report(PhotoComment p) {
            if (p.reported)
                return false;
            p.reported = true;
            if (!KES.shared().getAccountManager().getUser().isRegistered())
                return false;
            KES.shared().getFeedManager().report(p.getID(getFeedType()));
            return true;
        }

        @Override
        public void markAsPrivate(PhotoComment p) {
            p.is_private = !p.is_private;
            KES.shared().getFeedManager().markAsPrivate(p.getID(getFeedType()),p.is_private);
        }

        @Override
        public void onItemViewed(PhotoComment p) {
            FeedBaseFragment.this.onItemViewed(p);
        }

        @Override
        public void onImageClick(ImageView v) {
            mainActivityInterface.zoomImageFromThumb(v);
        }
    };

    public abstract FeedManager.FeedType getFeedType();

    public abstract void onItemViewed(PhotoComment p);

    public void showNoPost() {
        if (getFeedType() == FeedManager.FeedType.My) {
            accessViewHolder.tvTitle.setText(R.string.myfeed_noposts_title);
            accessViewHolder.tvMessage.setText(R.string.myfeed_noposts_message);
            accessViewHolder.btAccess.setText(R.string.myfeed_noposts_btn);
            accessViewHolder.btAccess.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((MainActivity) getActivity()).getNavigationBar().navigateTo(NavigationBar.Attached.Compose);
                }
            });
        } else
        {
            accessViewHolder.tvTitle.setText(R.string.publicfeed_noposts_title);
            accessViewHolder.tvMessage.setText(R.string.publicfeed_noposts_message);
            accessViewHolder.btAccess.setText(R.string.publicfeed_noposts_btn);
            accessViewHolder.btAccess.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showMainProgressbar(true);
                    accessViewHolder.setVisibility(View.GONE);
                    onRefreshListener.onRefresh();
                }
            });
        }
        accessViewHolder.setVisibility(View.VISIBLE);
    }

    public void showLoadError() {
        accessViewHolder.tvTitle.setText(R.string.feed_error_title);
        accessViewHolder.tvMessage.setText(R.string.feed_error_message);
        accessViewHolder.btAccess.setText(R.string.feed_error_btn);
        accessViewHolder.btAccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMainProgressbar(true);
                accessViewHolder.setVisibility(View.GONE);
                onRefreshListener.onRefresh();
            }
        });
        accessViewHolder.setVisibility(View.VISIBLE);
    }


    public class PreCachingLayoutManager extends LinearLayoutManager {
        private static final int DEFAULT_EXTRA_LAYOUT_SPACE = 600;
        private int extraLayoutSpace = -1;
        private Context context;

        public PreCachingLayoutManager(Context context) {
            super(context);
            this.context = context;
        }

        public PreCachingLayoutManager(Context context, int extraLayoutSpace) {
            super(context);
            this.context = context;
            this.extraLayoutSpace = extraLayoutSpace;
        }

        public PreCachingLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
            this.context = context;
        }

        public void setExtraLayoutSpace(int extraLayoutSpace) {
            this.extraLayoutSpace = extraLayoutSpace;
        }

        @Override
        protected int getExtraLayoutSpace(RecyclerView.State state) {
            if (extraLayoutSpace > 0) {
                return extraLayoutSpace;
            }
            return DEFAULT_EXTRA_LAYOUT_SPACE;
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View result = inflater.inflate(R.layout.fragment_feed, container, false);
        loadingNextPage = false;
        firstShow = true;
        accessViewHolder = AccessFragment.getViews(result.findViewById(R.id.holderNoPost));
        progressBar = (ProgressBar) result.findViewById(R.id.progressLoading);
        swipeContainer = (SwipeRefreshLayout) result.findViewById(R.id.swipe_container);
        swipeContainer.setOnRefreshListener(onRefreshListener);
        swipeContainer.setEnabled(false);
//        itemTitle = result.findViewById(R.id.itemTitle);
//        itemShare = result.findViewById(R.id.itemShare);
//        shareController = FeedItem.createHolder(itemTitle,itemShare).shareController;
        rvFeed = (RecyclerView) result.findViewById(R.id.rvFeed);
        //rvFeed.setItemViewCacheSize(10);
        rvFeed.setHasFixedSize(false);
        mLayoutManager = new LinearLayoutManager(getActivity());
        rvFeed.setLayoutManager(mLayoutManager);
        rvFeed.setAdapter(adapter);
        rvFeed.setOnScrollListener(endlessRecyclerOnScrollListener);
        KES.shared().getFeedManager().registerOnChangeListener(onFeedChange);
        scrollInitialized = false;
        titleVisible = false;
        showMainProgressbar(false);
        initFeed();
        return result;
    }

    private void initFeed() {
        adapter.removeAllItems();
        boolean loaded = KES.shared().getFeedManager().feed(getFeedType()).getCache(list);
        if (list.size() == 0) {
            if (loaded)
                showNoPost();
            else {
                refreshNetworkAction = KES.shared().getFeedManager().feed(getFeedType());
                refreshNetworkAction./*setMaxID(2).*/load();
                showMainProgressbar(true);
            }
        } else {
            addAllItems();
        }
        swipeContainer.setEnabled(list.size() > 0);
    }

    private void addAllItems()
    {
        for (int i = 0; i < list.size(); i++)
            adapter.notifyItemInserted(i);
    }

    private void showMainProgressbar(boolean enabled) {
        progressBar.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
        progressBar.setIndeterminate(enabled);
    }

    protected ReadTag getPhotoCommentTag(PhotoComment p) {
        ReadTag tag = (ReadTag) p.getTag();
        if (tag == null) {
            tag = new ReadTag();
            p.setTag(tag);
        }
        return tag;
    }

    /*
    //Test visible answers
    long lastTested = 0;
    private boolean hasUnread = false;

    private void testVisibleAnswers(boolean force) {
        //if (getFeedType() != FeedManager.FeedType.My)
        //    return;
        if (list.size() == 0)
            return;
        if (force)
            lastTested = 0;
        if (!isVisibleToUser || !isStarted)
            return;
        long now = System.currentTimeMillis();
        long elapsed = now - lastTested;
        if (elapsed < CHECK_UNREAD_INTERVAL)
            return;
        findUncoveredItems();
        if (firstUncoveredItem < 0 && !force)
            return;
        Log.d("TAG", "Testing visible answers");
        hasUnread = false;
        lastTested = now;

        ReadTag tag = null;
        PhotoComment p = null;
        for (int i = 0; i < list.size(); i++) {
            p = list.get(i);
            if (p == null)
                continue;
            tag = getPhotoCommentTag(p);
            tag.oldShownAt = tag.shownAt;
            tag.shownAt = 0;
        }

        if (firstUncoveredItem < 0)
            return;

        for (int i = firstUncoveredItem; i < lastUncoveredItem + 1; i++)
        {
                p = list.get(i);
                tag = getPhotoCommentTag(p);
                tag.shownAt = tag.oldShownAt;
                if (tag.shownAt == 0)
                    tag.shownAt = now;
                else if (now - tag.shownAt > READ_DELAY) {
                    FeedAdapter.ItemHolder itemHolder = (FeedAdapter.ItemHolder)rvFeed.getChildViewHolder(mLayoutManager.findViewByPosition(i));
                    if (itemHolder.backgroundAnimator != null) {
                        itemHolder.backgroundAnimator.start();
                    }
                    onItemViewed(p);
                    Log.d("TAG", "Mark as read" + i);
                }
                hasUnread |= p.isUnread();
        }
        if (hasUnread && isVisibleToUser && isStarted) {
            mHandler.removeCallbacks(rTestViewed);
            mHandler.postDelayed(rTestViewed, CHECK_UNREAD_INTERVAL);
        }
        else
            mHandler.removeCallbacks(rTestViewed);
    }


    int firstVisibleItem = -1;
    int firstUncoveredItem = -1;
    int lastUncoveredItem = -1;

    private PhotoComment findByID(int id)
    {
        for (int i = 0; i < list.size(); i++)
            if (list.get(i).getID(getFeedType()) == id)
                return list.get(i);
        return null;
    }

    private void findUncoveredItems()
    {
        if (list.size() == 0)
            return;
        firstUncoveredItem = -1;
        lastUncoveredItem = -1;
        boolean firstUnset = true;

        firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();
        int lastVisibleItem = mLayoutManager.findLastVisibleItemPosition();
        if (firstVisibleItem != RecyclerView.NO_POSITION) {
            for (int i = firstVisibleItem; i < lastVisibleItem; i++) {
                FeedAdapter.ItemHolder itemHolder = (FeedAdapter.ItemHolder)rvFeed.getChildViewHolder(mLayoutManager.findViewByPosition(i));
                if (itemHolder.answerBackground == null ||
                        !itemHolder.answerBackground.getLocalVisibleRect(scrollBounds) ||
                        itemHolder.answerBackground.getHeight() != scrollBounds.height())
                    continue;
                PhotoComment p = findByID(itemHolder.itemID);
                if (p == null || !p.isUnread())
                    continue;
                if (firstUnset) {
                    firstUnset = false;
                    firstUncoveredItem = i;
                }
                lastUncoveredItem = i;
            }
        }
    }
    */

    RecyclerView.OnScrollListener endlessRecyclerOnScrollListener = new RecyclerView.OnScrollListener() {

        private int previousTotal = 0; // The total number of items in the dataset after the last load
//        private boolean loading = true; // True if we are still waiting for the last set of data to load.
        private int visibleThreshold = 1; // The minimum amount of items to have below your current scroll position before loading more.
        int visibleItemCount, totalItemCount;
        private int current_page = 1;

        /*
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                int tfirst = firstUncoveredItem;
                int tlast = lastUncoveredItem;
                findUncoveredItems();
                Log.d("TAGXXXXXXXXXXXXXXX","ONSCROLLED");
                if (tfirst != firstUncoveredItem || tlast != lastUncoveredItem)
                    testVisibleAnswers(true);
            }
        }
        */

        int firstVisibleItem;

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            visibleItemCount = recyclerView.getChildCount();
            totalItemCount = mLayoutManager.getItemCount();
            firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();

            /*
            if (loading) {
                if (totalItemCount > previousTotal+1) {
                    loading = false;
                    previousTotal = totalItemCount;
                }
            }
            */

            if (!loadingNextPage && (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
                // End has been reached
                // Do something
                current_page++;
                onLoadMore(current_page);
                loadingNextPage = true;
            }
        }

        public void onLoadMore(int current_page)
        {
            if (bottomReached || list.size() == 0)
                return;
            PhotoComment item = list.get(list.size() - 1);
            if (item == null)
                return;
            if (item.status != PhotoComment.PostStatus.Posted)
                return;
            if (item.status == PhotoComment.PostStatus.Posted && KES.shared().getFeedManager().feed(getFeedType()).isLastItem(item.getID(getFeedType())))
                return;
            adapter.setFooterLoading(true);
            refreshNetworkAction = KES.shared().getFeedManager().feed(getFeedType());
            if (item.status == PhotoComment.PostStatus.Posted)
                refreshNetworkAction.setMaxID(item.getID(getFeedType()) - 1);
            refreshNetworkAction.setAppended(true);
            refreshNetworkAction.load();

        }
    };

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }



    SwipeRefreshLayout.OnRefreshListener onRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            swipeContainer.setEnabled(false);
            /*
            if (getFeedType() == FeedManager.FeedType.My)
            {
                session.getFeedManager().feed(getFeedType()).clear();
                removeAllItems();
            }
            */
            refreshNetworkAction = KES.shared().getFeedManager().feed(getFeedType());
            refreshNetworkAction.load();
        }
    };

    FeedManager.OnChangeListener onFeedChange = new FeedManager.OnChangeListener()
    {

        @Override
        public void onPageLoaded(FeedManager.FeedWrapper wrapper) {

            if (wrapper.feedType != getFeedType())
                return;

            loadingNextPage = false;
            if (!wrapper.unreadItems)
                showMainProgressbar(false);

            if (wrapper.max_id == null)
                swipeContainer.setRefreshing(false);

            int listSize = list.size();

            if (wrapper.exception != null) {
                swipeContainer.setEnabled(false);
                if (listSize == 0 || wrapper.appended == false)
                    showLoadError();
                else
                    adapter.setFooterLoading(false);
                return;
            }

            adapter.hideFooter();
            if (wrapper.flag_feedBottomReached)
                bottomReached = true;

            SparseArray<PhotoComment> indexedPosted = new SparseArray<PhotoComment>();
            SparseArray<PhotoComment> indexedLocal = new SparseArray<PhotoComment>();

            //Get all items to a temporary cache
            //Index and separate them by type
            ArrayList<PhotoComment> cacheTmp = new ArrayList<PhotoComment>();
            KES.shared().getFeedManager().feed(getFeedType()).getCache(cacheTmp);
            for (int i = 0; i < cacheTmp.size(); i++) {
                PhotoComment pc = cacheTmp.get(i);
                if (pc.status == PhotoComment.PostStatus.Posted)
                    indexedPosted.put(pc.getID(getFeedType()), pc);
                else
                    indexedLocal.put(pc.getID(), pc);
            }

            //Find first posted item ID
            int firstPostedID = -1;
            for (int i = 0; i < list.size(); i++)
                if (list.get(i).status == PhotoComment.PostStatus.Posted)
                {
                    firstPostedID = list.get(i).getID(getFeedType());
                    break;
                }

            //New data is valid but empty or new data can't be connected to the list
            if (cacheTmp.size() == 0 || (firstPostedID < 0 && indexedPosted.size() > 0) || firstPostedID + 1 < indexedPosted.keyAt(0))
            {
                bottomReached = false;
                adapter.removeAllItems();
                for (int i = 0; i < cacheTmp.size(); i++) {
                    list.add(cacheTmp.get(i));
                    adapter.notifyItemInserted(i);
                }
                rvFeed.scrollToPosition(0);
                if (list.size() == 0)
                    showNoPost();
                else {
                    accessViewHolder.setVisibility(View.GONE);
                    swipeContainer.setEnabled(true);
                }
                showListAnimation();
                return;
            }


            //first remove missing items and update changed items
            for (int i = list.size() - 1; i >= 0; i --)
            {
                PhotoComment oldItem = list.get(i);
                int oldID = oldItem.getID(getFeedType());
                PhotoComment newItem = null;

                if (oldItem.status == PhotoComment.PostStatus.Posted) {
                    newItem = indexedPosted.get(oldID);
                    if (newItem != null)
                        indexedPosted.delete(oldID);
                }
                else {
                    newItem = indexedLocal.get(oldID);
                    if (newItem != null)
                        indexedLocal.remove(oldID);
                }

                if (newItem == null)
                {
                    list.remove(i);
                    adapter.notifyItemRemoved(i);
                } else
                {
                    if (oldItem != newItem) {
                        list.set(i, newItem);
                        adapter.notifyItemChanged(i);
                    }
                }
            }

            //Insert pending items
            for (int j = 0; j < indexedLocal.size(); j++)
            {
                list.add(0,indexedLocal.valueAt(j));
                adapter.notifyItemInserted(0);
            }
            //Insert posted items
            boolean insertedToTop = false;
            int startSearch = 0;
            int newPosition;
            for (int j = indexedPosted.size() - 1; j >= 0; j--)
            {
                newPosition = list.size();
                PhotoComment newItem = indexedPosted.valueAt(j);
                int newID = newItem.getID(getFeedType());
                for (int i = startSearch; i < list.size(); i++)
                {
                    startSearch = i;
                    PhotoComment oldItem = list.get(i);
                    if (oldItem.status != PhotoComment.PostStatus.Posted)
                        continue;
                    int oldID = oldItem.getID(getFeedType());
                    if (oldID < newID) {
                        newPosition = startSearch;
                        break;
                    }
                }
                if (newPosition == 0)
                    insertedToTop = true;
                list.add(newPosition, newItem);
                adapter.notifyItemInserted(newPosition);
            }

            /*
            //first insert higher id items
            int highID = Integer.MIN_VALUE;
            if (list.size() > 0)
                highID = list.get(0).getID(getFeedType());

            for (int i = 0; i < tmp.size(); i++) {
                PhotoComment p = tmp.valueAt(i);
                if (p.getID(getFeedType()) > highID) {
                    list.add(0,p);
                    adapter.notifyItemInserted(0);
                    inserted = true;
                }
            }
            */
            if (insertedToTop)
                rvFeed.scrollToPosition(0);
            /*
            //add lower id items
            int lowID = Integer.MAX_VALUE;
            if (list.size() > 0)
                lowID = list.get(list.size() - 1).getID(getFeedType());

            for (int i = tmp.size(); i > 0; i--) {
                PhotoComment p = tmp.valueAt(i - 1);
                if (p.getID(getFeedType()) < lowID) {
                    list.add(p);
                    adapter.notifyItemInserted(list.size() - 1);
                }
            }
            */
            swipeContainer.setEnabled(list.size() > 0);

            if (list.size() == 0)
                showNoPost();
            else
                accessViewHolder.setVisibility(View.GONE);

            //testVisibleAnswers(true);
            /*
            NavigationBar.Attached attached = ((MainActivity) getActivity()).getNavigationBar().getAttached();
            if ((attached == NavigationBar.Attached.Feed && getFeedType() == FeedManager.FeedType.Public) ||
                (attached == NavigationBar.Attached.MyFeed && getFeedType() == FeedManager.FeedType.My))
            */


            showListAnimation();

            //if (wrapper.unreadItems)
            //    testVisibleAnswers(true);
        }

        @Override
        public void onSuggestedQuestions(String[] questions, Exception error) {

        }

        private void showListAnimation()
        {
            if (!firstShow)
                return;
            firstShow = false;
            if (!isVisibleToUser)
                return;
            if (list.size() == 0)
                return;
            /*
            Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.feedanimation);
            animation.setFillAfter(false);
            rvFeed.startAnimation(animation);
            */
        }

        @Override
        public void onMarkAsReadResult(PhotoComment photoComment, Exception error) {

        }

        @Override
        public void onLikeResult(int questionID, int commentID, Exception error) {

        }

        @Override
        public void onReportResult(int questionID, Exception error) {

        }

        @Override
        public void onDeleteResult(int questionID, int commentID, Exception error) {

        }

        @Override
        public void onMarkAsPrivateResult(PhotoComment p, Exception error) {
            if (p == null)
                return;
            if (getFeedType() == FeedManager.FeedType.Public)
            {
                int id = p.getID(FeedManager.FeedType.Public);
                int idx = -1;
                for (int i = 0; i < list.size(); i++)
                    if (list.get(i).getID(FeedManager.FeedType.Public) == id)
                    {
                        idx = i;
                        break;
                    }
                if (p.is_private && idx >= 0)
                {
                    list.remove(idx);
                    adapter.notifyItemRemoved(idx);
                }
            }
        }

        @Override
        public void onPosting(PhotoComment photoComment) {
            if (getFeedType() != FeedManager.FeedType.My)
                return;
            accessViewHolder.setVisibility(View.GONE);
            list.add(0,photoComment);
            adapter.notifyItemInserted(0);
            rvFeed.scrollToPosition(0);
            if (list.size() == 1)
                swipeContainer.setEnabled(true);
        }

        @Override
        public void onPostResult(PhotoComment photoComment, Exception error) {
            if (getFeedType() != FeedManager.FeedType.My)
                return;
            accessViewHolder.setVisibility(View.GONE);
            if (error != null)
            {
                int i = list.indexOf(photoComment);
                if (i >= 0)
                    adapter.notifyItemChanged(i);
                return;
            }
            //Check if already updated with pull down to refresh
            int last = list.lastIndexOf(photoComment);
            int first = list.indexOf(photoComment);
            if (first != last)
            {
                list.remove(last);
                adapter.notifyItemRemoved(last);
                return;
            }

            //Move to new position

            int newPosition = list.size();
            for (int i = 0, l = list.size(); i < l; i++)
            {
                PhotoComment pc = list.get(i);
                if (pc == null || pc == photoComment || pc.status != PhotoComment.PostStatus.Posted)
                    continue;
                if (pc.getID(FeedManager.FeedType.My) > photoComment.getID(FeedManager.FeedType.My))
                    continue;
                newPosition = i;
                break;
            }
            if (newPosition > first)
                newPosition --;

            if (first != newPosition) {
                list.remove(first);
                //adapter.notifyItemRemoved(first);
                list.add(newPosition, photoComment);
                //adapter.notifyItemInserted(newPosition);
                adapter.notifyItemMoved(first,newPosition);
            }
            adapter.notifyItemChanged(newPosition);
        }

        @Override
        public void onInsufficientCredit() {

        }

    };


    /*
    ListView.OnScrollListener listScroll = new ListView.OnScrollListener() {
        int firstvisible = -1;

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }

        @Override
        public void onScroll(AbsListView listView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (list.size() == 0)
            {
                itemTitle.setVisibility(View.INVISIBLE);
                itemShare.setVisibility(View.INVISIBLE);
                return;
            } else
                itemTitle.setVisibility(View.VISIBLE);

            if (!scrollInitialized && list.size() >0)
                scrollInitialized = true;
            if (firstVisibleItem != firstvisible) {
                shareController.setItemIndex(firstVisibleItem);
                firstvisible = firstVisibleItem;
                if (list.size() > firstVisibleItem)
                    FeedItem.updateViewTitle(itemTitle,list.get(firstVisibleItem));
            }
            View item = listView.getChildAt(1);
            if (item != null) {
                int ofs = (int) item.getY() - itemTitle.getHeight();
                if (ofs > 0) ofs = 0;
                itemTitle.setTranslationY(ofs);
            }
            if (shareController.isOpen())
            {
                item = listView.getChildAt(shareController.getSharedItemIndex() + 1 - firstVisibleItem);
                if (item != null) {
                    int ofs = (int) item.getY() - itemTitle.getHeight();
                    if (ofs > 0) ofs = 0;
                    itemShare.setTranslationY(ofs);
                }
            }
        }
    };
    */

    @Override
    public void onStart() {
        super.onStart();
        isStarted = true;
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(navigationChange, new IntentFilter(NavigationBar.ACTION_CHANGE));
        navigationChange.onReceive(null, null);
        refreshNetworkAction = KES.shared().getFeedManager().feed(getFeedType());
        refreshNetworkAction.load();
    }

    @Override
    public void onStop() {
        super.onStop();
        isStarted = false;
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(navigationChange);
        mHandler.removeCallbacksAndMessages(null);
    }

    BroadcastReceiver navigationChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            NavigationBar.Attached attached = NavigationBar.Attached.Empty;
            if (getFeedType() == FeedManager.FeedType.Public)
                attached = NavigationBar.Attached.Feed;
            else
            if (getFeedType() == FeedManager.FeedType.My)
                attached = NavigationBar.Attached.MyFeed;
            isVisibleToUser =
                    ((MainActivity) getActivity()).getNavigationBar().getAttached() == attached;
            //lastTested = 0;
            //testVisibleAnswers(true);
        }
    };

/*
    Runnable rTestViewed = new Runnable() {
        @Override
        public void run() {
            testVisibleAnswers(false);
        }
    };
*/
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        progressBar = null;
        swipeContainer = null;
        rvFeed = null;
        mLayoutManager = null;
        accessViewHolder = null;
        KES.shared().getFeedManager().unRegisterOnChangeListener(onFeedChange);
        list.clear();
    }

    @Override
    public void onDestroy() {
        scrollBounds = null;
        list = null;
        adapter = null;
        mHandler = null;
        super.onDestroy();
    }


}
