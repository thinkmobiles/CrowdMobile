package com.crowdmobile.kes.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.crowdmobile.kes.R;
import com.crowdmobile.kes.adapter.FeedAdapter;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = Session.getInstance(getActivity());
        list = new ArrayList<PhotoComment>();
        adapter = new FeedAdapter(getActivity(),list,feedAdapterListener);
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
        public void retryClick() {
            if (lastNetworkAction != null)
            {
                adapter.setFooterLoading(true);
                lastNetworkAction.load();
            }
        }
    };

    public abstract FeedManager.FeedType getFeedType();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_feed, container,false);
        swipeContainer = (SwipeRefreshLayout)result.findViewById(R.id.swipe_container);
        swipeContainer.setOnRefreshListener(onRefreshListener);
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
        holderNoPost = result.findViewById(R.id.holderNoPost);
        scrollInitialized = false;
        titleVisible = false;
        //shareController = new FeedItem.ShareController(itemTitle, itemShare);
        return result;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        list.clear();
        session.getFeedManager().feed(getFeedType()).getCache(list);
        if (list.size() == 0) {
            lastNetworkAction = session.getFeedManager().feed(getFeedType());
            lastNetworkAction.setMaxID(400).load();
        }
    }

    SwipeRefreshLayout.OnRefreshListener onRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            if (getFeedType() == FeedManager.FeedType.My)
            {
                holderNoPost.setVisibility(View.GONE);
                session.getFeedManager().feed(getFeedType()).clear();
                list.clear();
                adapter.notifyDataSetChanged();
            }
            session.getFeedManager().feed(getFeedType()).load();
        }
    };

    FeedManager.OnChangeListener onFeedChange = new FeedManager.OnChangeListener()
    {
        @Override
        public void onPageLoaded(FeedManager.FeedWrapper wrapper) {

            if (wrapper.feedType != getFeedType())
                return;

            if (wrapper.max_id == null) {
                swipeContainer.setRefreshing(false);
            }

            if (wrapper.exception != null)
            {
                adapter.setFooterLoading(false);
                return;
            }

            if (wrapper.flag_feedBottomReached)
                adapter.setFooterVisible(false);

            list.clear();
            session.getFeedManager().feed(getFeedType()).getCache(list);

            adapter.notifyDataSetChanged();
            if (getFeedType() == FeedManager.FeedType.My)
            {
                if (list.size() == 0 && wrapper.exception == null)
                    holderNoPost.setVisibility(View.VISIBLE);
                else
                    holderNoPost.setVisibility(View.GONE);
            }

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
    }


}
