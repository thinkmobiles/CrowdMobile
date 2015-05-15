package com.crowdmobile.kes.fragment;

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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;

import com.crowdmobile.kes.MainActivity;
import com.crowdmobile.kes.R;
import com.crowdmobile.kes.adapter.FeedAdapter;
import com.crowdmobile.kes.widget.NavigationBar;
import com.kes.FeedManager;
import com.kes.Session;
import com.kes.model.PhotoComment;

import java.util.ArrayList;

public abstract class FeedBaseFragment extends Fragment {

    private static final int CHECK_UNREAD_INTERVAL = 1000;  //msec
    private static final int READ_DELAY = 5000;  //msec

    private static final String TAG = FeedBaseFragment.class.getSimpleName();
    private Session session;
    protected RecyclerView rvFeed;
    private LinearLayoutManager mLayoutManager;
    private FeedAdapter adapter;
    private ArrayList<PhotoComment> list;
    //    View itemTitle;
//    View itemShare;
    private boolean scrollInitialized;
    private boolean titleVisible = false;
    //    FeedItem.ShareController shareController;
    private FeedManager.QueryParams lastNetworkAction = null;
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
    private boolean loading;

    class ReadTag {
        public long oldShownAt;
        public long shownAt;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = Session.getInstance(getActivity());
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
    }

    ;

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
            if (lastNetworkAction != null) {
                adapter.setFooterLoading(true);
                lastNetworkAction.load();
            }
        }

        @Override
        public void retryPostClick(PhotoComment p) {
            p.status = PhotoComment.PostStatus.Pending;
            Session.getInstance(getActivity()).getFeedManager().postQuestion(p);
        }

        @Override
        public void report(PhotoComment p) {
            p.reported = true;
            Session.getInstance(getActivity()).getFeedManager().report(p.getID(getFeedType()));
        }

        @Override
        public void markAsPrivate(PhotoComment p) {
            p.is_private = !p.is_private;
            Session.getInstance(getActivity()).getFeedManager().markAsPrivate(p.getID(getFeedType()),p.is_private);
        }

        @Override
        public void onItemViewed(PhotoComment p) {
            FeedBaseFragment.this.onItemViewed(p);
        }
    };

    public abstract FeedManager.FeedType getFeedType();

    public abstract void onItemViewed(PhotoComment p);

    public void showNoPost() {
        accessViewHolder.tvTitle.setText(R.string.myfeed_noposts_title);
        accessViewHolder.tvMessage.setText(R.string.myfeed_noposts_message);
        accessViewHolder.btAccess.setText(R.string.myfeed_noposts_btn);
        accessViewHolder.btAccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).getNavigationBar().navigateTo(NavigationBar.Attached.Compose);
            }
        });
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
                lastNetworkAction.load();
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
        loading = false;
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
        session.getFeedManager().registerOnChangeListener(onFeedChange);
        scrollInitialized = false;
        titleVisible = false;
        showMainProgressbar(false);
        initFeed();
        return result;
    }

    private void initFeed() {
        list.clear();
        boolean loaded = session.getFeedManager().feed(getFeedType()).getCache(list);
        if (list.size() == 0) {
            if (loaded)
                showNoPost();
            else {
                lastNetworkAction = session.getFeedManager().feed(getFeedType());
                lastNetworkAction./*setMaxID(2).*/load();
                showMainProgressbar(true);
            }
        } else {
            adapter.notifyDataSetChanged();
        }
        swipeContainer.setEnabled(list.size() > 0);
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

    //Test visible answers
    long lastTested = 0;
    private boolean hasUnread = false;

    private void testVisibleAnswers(boolean force) {
        if (getFeedType() != FeedManager.FeedType.My)
            return;
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

    private void findUncoveredItems()
    {
        if (list.size() == 0)
            return;
        firstUncoveredItem = -1;
        firstUncoveredItem = -1;
        lastUncoveredItem = -1;
        boolean firstUnset = true;
        firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();
        int lastVisibleItem = mLayoutManager.findLastVisibleItemPosition();

        if (firstVisibleItem >= 0) {
            int lastItem = mLayoutManager.findLastVisibleItemPosition() + 1;
            for (int i = firstVisibleItem; i < lastVisibleItem; i++) {
                FeedAdapter.ItemHolder itemHolder = (FeedAdapter.ItemHolder)rvFeed.getChildViewHolder(mLayoutManager.findViewByPosition(i));
                if (itemHolder.answerBackground == null ||
                        !itemHolder.answerBackground.getLocalVisibleRect(scrollBounds) ||
                        itemHolder.answerBackground.getHeight() != scrollBounds.height())
                    continue;
                PhotoComment p = list.get(i);
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


    RecyclerView.OnScrollListener endlessRecyclerOnScrollListener = new RecyclerView.OnScrollListener() {

        private int previousTotal = 0; // The total number of items in the dataset after the last load
//        private boolean loading = true; // True if we are still waiting for the last set of data to load.
        private int visibleThreshold = 1; // The minimum amount of items to have below your current scroll position before loading more.
        int visibleItemCount, totalItemCount;
        private int current_page = 1;

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            visibleItemCount = recyclerView.getChildCount();
            totalItemCount = mLayoutManager.getItemCount();

            int tfirst = firstUncoveredItem;
            int tlast = lastUncoveredItem;
            findUncoveredItems();

            if (tfirst != firstUncoveredItem || tlast != lastUncoveredItem)
                testVisibleAnswers(true);

            /*
            if (loading) {
                if (totalItemCount > previousTotal+1) {
                    loading = false;
                    previousTotal = totalItemCount;
                }
            }
            */

            if (!loading && (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
                // End has been reached
                // Do something
                current_page++;
                onLoadMore(current_page);
                loading = true;
            }
        }

        public void onLoadMore(int current_page)
        {
            if (bottomReached || list.size() == 0)
                return;
            PhotoComment item = list.get(list.size() - 1);
            if (item == null)
                return;
            if (session.getFeedManager().feed(getFeedType()).isLastItem(item.getID(getFeedType())))
                return;
            adapter.setFooterLoading(true);
            lastNetworkAction = session.getFeedManager().feed(getFeedType())
                                .setMaxID(item.getID(getFeedType()) - 1);
            lastNetworkAction.load();

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
                list.clear();
                adapter.notifyDataSetChanged();
            }
            */
            lastNetworkAction = session.getFeedManager().feed(getFeedType());
            lastNetworkAction.load();
        }
    };

    FeedManager.OnChangeListener onFeedChange = new FeedManager.OnChangeListener()
    {

        @Override
        public void onPageLoaded(FeedManager.FeedWrapper wrapper) {

            if (wrapper.feedType != getFeedType())
                return;

            loading = false;
            showMainProgressbar(false);

            if (wrapper.max_id == null)
                swipeContainer.setRefreshing(false);

            int listSize = list.size();

            if (wrapper.exception != null) {
                swipeContainer.setEnabled(false);
                if (listSize == 0 || wrapper.max_id == null)
                    showLoadError();
                else
                    adapter.setFooterLoading(false);
                return;
            }

            if (wrapper.flag_feedBottomReached)
                bottomReached = true;

            SparseArray<PhotoComment> tmp = new SparseArray<PhotoComment>();
            ArrayList<PhotoComment> ltmp = new ArrayList<PhotoComment>();
            session.getFeedManager().feed(getFeedType()).getCache(ltmp);
            for (int i = 0; i < ltmp.size(); i++) {
                PhotoComment p = ltmp.get(i);
                tmp.put(p.getID(getFeedType()), p);
            }

            ltmp.clear();
            ltmp = null;

            //New data can't be connected to the list
            if (tmp.size() == 0 || (listSize > 0 && list.get(0).getID(getFeedType()) + 1 < tmp.valueAt(0).getID(getFeedType())))
            {
                bottomReached = false;
                list.clear();
                for (int i = tmp.size(); i > 0; i--)
                    list.add(tmp.valueAt(i - 1));
                adapter.notifyDataSetChanged();
                rvFeed.scrollToPosition(0);
            } else
            {
                adapter.hideFooter();

                //first update existing items
                for (int i = list.size(); i > 0; i--)
                {
                    PhotoComment c = list.get(i - 1);
                    PhotoComment newItem = tmp.get(c.getID(getFeedType()));
                    if (newItem == null)
                    {
                        list.remove(i - 1);
                        adapter.notifyItemRemoved(i - 1);
                    } else
                    {
                        if (c != newItem) {
                            list.set(i - 1, newItem);
                            adapter.notifyItemChanged(i - 1);
                        }
                    }
                }

                //first insert higher id items
                int highID = Integer.MIN_VALUE;
                if (list.size() > 0)
                    highID = list.get(0).getID(getFeedType());

                boolean inserted = false;
                for (int i = 0; i < tmp.size(); i++) {
                    PhotoComment p = tmp.valueAt(i);
                    if (p.getID(getFeedType()) > highID) {
                        list.add(0,p);
                        adapter.notifyItemInserted(0);
                        inserted = true;
                    }
                }
                if (inserted)
                    rvFeed.scrollToPosition(0);

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
            }
            swipeContainer.setEnabled(list.size() > 0);

            if (list.size() == 0)
                showNoPost();
            else
                accessViewHolder.setVisibility(View.GONE);

            testVisibleAnswers(true);
            NavigationBar.Attached attached = ((MainActivity) getActivity()).getNavigationBar().getAttached();
            if ((attached == NavigationBar.Attached.Feed && getFeedType() == FeedManager.FeedType.Public) ||
                (attached == NavigationBar.Attached.MyFeed && getFeedType() == FeedManager.FeedType.My))


            if (firstShow && list.size() > 0 && listSize == 0)
            {
                firstShow = false;
                Animation animation = AnimationUtils.loadAnimation(getActivity(),R.anim.feedanimation);
                animation.setFillAfter(false);
                rvFeed.startAnimation(animation);
            }

            //if (wrapper.unreadItems)
            //    testVisibleAnswers(true);
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
        public void onMarkAsPrivateResult(PhotoComment photoComment, Exception error) {

        }

        @Override
        public void onPosting(PhotoComment photoComment) {
            if (getFeedType() != FeedManager.FeedType.My)
                return;
            accessViewHolder.setVisibility(View.GONE);
            list.add(0,photoComment);
            adapter.notifyItemInserted(0);
            rvFeed.scrollToPosition(0);
        }

        @Override
        public void onPostResult(PhotoComment photoComment, Exception error) {
            if (getFeedType() != FeedManager.FeedType.My)
                return;
            accessViewHolder.setVisibility(View.GONE);
            int idx = list.indexOf(photoComment);
            if (idx >= 0)
                adapter.notifyItemChanged(idx);
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
            lastTested = 0;
            testVisibleAnswers(true);
        }
    };


    Runnable rTestViewed = new Runnable() {
        @Override
        public void run() {
            testVisibleAnswers(false);
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        progressBar = null;
        swipeContainer = null;
        rvFeed = null;
        mLayoutManager = null;
        accessViewHolder = null;
        session.getFeedManager().unRegisterOnChangeListener(onFeedChange);
        list.clear();
    }

    @Override
    public void onDestroy() {
        session = null;
        scrollBounds = null;
        list = null;
        adapter = null;
        mHandler = null;
        super.onDestroy();
    }

}
