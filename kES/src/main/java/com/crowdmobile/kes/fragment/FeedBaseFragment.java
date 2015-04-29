package com.crowdmobile.kes.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.crowdmobile.kes.MainActivity;
import com.crowdmobile.kes.R;
import com.crowdmobile.kes.adapter.FeedAdapter;
import com.crowdmobile.kes.widget.NavigationBar;
import com.kes.FeedManager;
import com.kes.Session;
import com.kes.model.PhotoComment;

import java.util.ArrayList;

public abstract class FeedBaseFragment extends Fragment {


    private static final String TAG = FeedBaseFragment.class.getSimpleName();
    Session session;
    RecyclerView lvFeed;
    LinearLayoutManager mLayoutManager;
    FeedAdapter adapter;
    ArrayList<PhotoComment> list;
//    View itemTitle;
//    View itemShare;
    boolean scrollInitialized;
    boolean titleVisible = false;
//    FeedItem.ShareController shareController;
    FeedManager.QueryParams lastNetworkAction = null;
    SwipeRefreshLayout swipeContainer;
    View holderNoPost;
    int minID = 0;
    boolean hasFooterView = false;
    boolean isViewCreated = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = Session.getInstance(getActivity());
        list = new ArrayList<PhotoComment>();
        adapter = new FeedAdapter(getActivity(),getFeedType(), list,feedAdapterListener);
    }

    FeedAdapter.FeedAdapterListener feedAdapterListener = new FeedAdapter.FeedAdapterListener() {
        @Override
        public void onLastItemReached() {
            if (list.size() > 0) {
                lastNetworkAction =
                        session.getFeedManager().feed(getFeedType())
                                .setMaxID(list.get(list.size() - 1).id - 1);
                lastNetworkAction.load();
            }
        }

        @Override
        public void retryLoadClick() {
            if (lastNetworkAction != null)
            {
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
            Session.getInstance(getActivity()).getFeedManager().report(p.id);
        }

        @Override
        public void markAsPrivate(PhotoComment p) {
            p.is_private = !p.is_private;
            Session.getInstance(getActivity()).getFeedManager().markAsPrivate(p.id);
        }
    };

    public abstract FeedManager.FeedType getFeedType();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_feed, container,false);
        holderNoPost = result.findViewById(R.id.holderNoPost);
        ((TextView)holderNoPost.findViewById(R.id.tvAccessTitle)).setText(R.string.myfeed_noposts_title);
        ((TextView)holderNoPost.findViewById(R.id.tvAccessMessage)).setText(R.string.myfeed_noposts_message);
        holderNoPost.findViewById(R.id.btGetStarted).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity)getActivity()).getNavigationBar().navigateTo(NavigationBar.Attached.Compose);
            }
        });
        swipeContainer = (SwipeRefreshLayout)result.findViewById(R.id.swipe_container);
        swipeContainer.setOnRefreshListener(onRefreshListener);
        swipeContainer.setEnabled(false);
//        itemTitle = result.findViewById(R.id.itemTitle);
//        itemShare = result.findViewById(R.id.itemShare);
//        shareController = FeedItem.createHolder(itemTitle,itemShare).shareController;
        lvFeed = (RecyclerView)result.findViewById(R.id.lvFeed);
        lvFeed.setHasFixedSize(false);
        mLayoutManager = new LinearLayoutManager(getActivity());
        lvFeed.setLayoutManager(mLayoutManager);
        lvFeed.setAdapter(adapter);
//        lvFeed.setOnScrollListener(listScroll);
        session.getFeedManager().registerOnChangeListener(onFeedChange);
        scrollInitialized = false;
        titleVisible = false;
        isViewCreated = true;
        reload();
        return result;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void setEmptyLayoutVisibility(boolean visible)
    {
        if (getFeedType() == FeedManager.FeedType.My)
        {
            if (visible)
                holderNoPost.setVisibility(View.VISIBLE);
            else
                holderNoPost.setVisibility(View.GONE);
        }
    }

    public void reload()
    {
        if (!isViewCreated)
            return;
        list.clear();
        boolean loaded = session.getFeedManager().feed(getFeedType()).getCache(list);
        setEmptyLayoutVisibility(list.size() == 0 && loaded);
        if (list.size() == 0) {
            lastNetworkAction = session.getFeedManager().feed(getFeedType());
            lastNetworkAction./*setMaxID(400).*/load();
        } else {
            lvFeed.scrollToPosition(0);
            adapter.notifyDataSetChanged();
        }
        swipeContainer.setEnabled(list.size() > 0);
    }

    SwipeRefreshLayout.OnRefreshListener onRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            swipeContainer.setEnabled(false);
            if (getFeedType() == FeedManager.FeedType.My)
            {
                holderNoPost.setVisibility(View.GONE);
                session.getFeedManager().feed(getFeedType()).clear();
                list.clear();
                adapter.notifyDataSetChanged();
            }
            lastNetworkAction = session.getFeedManager().feed(getFeedType());
            lastNetworkAction.load();
        }
    };

    FeedManager.OnChangeListener onFeedChange = new FeedManager.OnChangeListener()
    {
        @Override
        public boolean onUnread(FeedManager.FeedWrapper wrapper) {
            return false;
        }

        @Override
        public void onPageLoaded(FeedManager.FeedWrapper wrapper) {

            if (wrapper.feedType != getFeedType())
                return;

            if (wrapper.max_id == null) {
                swipeContainer.setRefreshing(false);
            }

            if (wrapper.exception != null)
            {
                setEmptyLayoutVisibility(false);
                adapter.setFooterLoading(false);
                swipeContainer.setEnabled(false);
                return;
            }

            if (wrapper.flag_feedBottomReached) {
                adapter.hideFooter();
                swipeContainer.setEnabled(true);
            }

            list.clear();
            session.getFeedManager().feed(getFeedType()).getCache(list);

            adapter.notifyDataSetChanged();
            swipeContainer.setEnabled(list.size() > 0);

            setEmptyLayoutVisibility(list.size() == 0 && wrapper.exception == null);

            if (wrapper.max_id == null) {
                lvFeed.scrollToPosition(0);
            }

//            if (!scrollInitialized)
//                listScroll.onScroll(lvFeed,0,0,0);
        }


        @Override
        public void onMarkAsReadResult(int questionID, int commentID, Exception error) {

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
        public void onMarkAsPrivateResult(int questionID, Exception error) {

        }

        @Override
        public void onPostResult(int questionID, Exception error) {
            adapter.notifyDataSetChanged();
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
    public void onDestroyView() {
        super.onDestroyView();
        swipeContainer = null;
        lvFeed = null;
        mLayoutManager = null;
        holderNoPost = null;
        session.getFeedManager().unRegisterOnChangeListener(onFeedChange);
        list.clear();
        isViewCreated = false;
    }


}
