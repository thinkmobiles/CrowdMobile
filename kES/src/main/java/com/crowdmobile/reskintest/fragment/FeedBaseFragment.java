package com.crowdmobile.reskintest.fragment;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.crowdmobile.reskintest.MainActivity;
import com.crowdmobile.reskintest.MainActivityInterface;
import com.crowdmobile.reskintest.R;
import com.crowdmobile.reskintest.adapter.FeedAdapter;
import com.crowdmobile.reskintest.widget.NavigationBar;
import com.kes.FeedCache;
import com.kes.FeedManager;
import com.kes.KES;
import com.kes.model.PhotoComment;


public abstract class FeedBaseFragment extends Fragment {

    private static final int CHECK_UNREAD_INTERVAL = 1000;  //msec
    private static final int READ_DELAY = 5000;  //msec

    private static final String TAG = FeedBaseFragment.class.getSimpleName();
    protected RecyclerView rvFeed;
    private LinearLayoutManager mLayoutManager;
    private FeedAdapter adapter;
    private FeedCache.FeedArray list;
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
            if (p.responses == null || p.responses.length == 0)
                return false;
            if (p.responses[0].liked)
                return false;
            if (!KES.shared().getAccountManager().getUser().isRegistered())
                return false;
            p.responses[0].liked = true;
            p.responses[0].likes_count ++;
            KES.shared().getFeedManager().like(p.getID(),p.responses[0].id);
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
        rvFeed.addOnScrollListener(endlessRecyclerOnScrollListener);
        KES.shared().getFeedManager().registerOnChangeListener(onFeedChange);
        KES.shared().getFeedManager().cacheOf(getFeedType()).registerOnUpdateListener(onUpdateListener);
        scrollInitialized = false;
        titleVisible = false;
        showMainProgressbar(false);
        initFeed();
        rvFeed.setAdapter(adapter);
        return result;
    }

    FeedCache.OnUpdateListener onUpdateListener = new FeedCache.OnUpdateListener() {
        @Override
        public void onItemInserted(int position) {
            adapter.notifyItemInserted(position);
            if (position == 0)
                rvFeed.scrollToPosition(0);
        }

        @Override
        public void onItemRemoved(int position) {
            adapter.notifyItemRemoved(position);
        }

        @Override
        public void onItemMoved(int fromPosition, int toPosition) {
            adapter.notifyItemMoved(fromPosition,toPosition);
        }

        @Override
        public void onItemUpdated(int position) {
            adapter.notifyItemChanged(position);
        }
    };

    private void initFeed() {
        FeedCache cache = KES.shared().getFeedManager().cacheOf(getFeedType());
        list = cache.getCache();
        adapter = new FeedAdapter(getActivity(), getFeedType(), list, feedAdapterListener);
        if (list.size() == 0) {
            if (cache.isLoaded())
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
            if (item.status == PhotoComment.PostStatus.Posted && KES.shared().getFeedManager().cacheOf(getFeedType()).isEOF(item.getID(getFeedType())))
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

            if (!wrapper.unreadItems)
                showMainProgressbar(false);

            //Feed top load result
            if (wrapper.max_id == null && wrapper.unreadItems == false)
            {
                swipeContainer.setRefreshing(false);

                if (wrapper.exception != null) {
                    swipeContainer.setEnabled(false);
                    if (list.size() == 0 || wrapper.appended == false)
                        showLoadError();
                    else
                        adapter.setFooterLoading(false);
                    return;
                } else {
                    swipeContainer.setEnabled(true);
                    rvFeed.scrollToPosition(0);
                }

            } else
            //Next page load result
            {
                loadingNextPage = false;
                if (wrapper.exception != null)
                    adapter.setFooterLoading(false);
            }

            bottomReached |= wrapper.flag_feedBottomReached;
            if (wrapper.exception == null)
                adapter.hideFooter();

            if (list.size() == 0 && KES.shared().getFeedManager().cacheOf(getFeedType()).isLoaded())
                    showNoPost();
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
            /*
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
            */
        }

        @Override
        public void onPosting(PhotoComment photoComment) {
            if (getFeedType() != FeedManager.FeedType.My)
                return;
            accessViewHolder.setVisibility(View.GONE);
            if (list.size() == 1)
                swipeContainer.setEnabled(true);
        }

        @Override
        public void onPostResult(PhotoComment photoComment, Exception error) {
            if (getFeedType() != FeedManager.FeedType.My)
                return;
            accessViewHolder.setVisibility(View.GONE);
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
        KES.shared().getFeedManager().cacheOf(getFeedType()).unRegisterOnUpdateListener(onUpdateListener);
        rvFeed.removeOnScrollListener(endlessRecyclerOnScrollListener);

        progressBar = null;
        swipeContainer = null;
        rvFeed = null;
        mLayoutManager = null;
        accessViewHolder = null;
        KES.shared().getFeedManager().unRegisterOnChangeListener(onFeedChange);
        list = null;
        adapter = null;
    }

    @Override
    public void onDestroy() {
        scrollBounds = null;
        mHandler = null;
        super.onDestroy();
    }


}
