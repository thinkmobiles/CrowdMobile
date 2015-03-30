package com.crowdmobile.kes.fragment;

import android.app.Fragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.crowdmobile.kes.R;
import com.crowdmobile.kes.list.FeedItem;
import com.kes.FeedManager;
import com.kes.Session;
import com.kes.model.PhotoComment;

import java.util.ArrayList;

public abstract class FeedBaseFragment extends Fragment {


    private static final String TAG = FeedBaseFragment.class.getSimpleName();
    Session session;
    ListView lvFeed;
    FeedAdapter adapter;
    ArrayList<PhotoComment> list;
//    View itemTitle;
//    View itemShare;
    boolean scrollInitialized;
    boolean titleVisible = false;
//    FeedItem.ShareController shareController;
    FeedFooter footer;
    FeedManager.QueryParams lastNetworkAction = null;
    SwipeRefreshLayout swipeContainer;
    int minID = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = Session.getInstance(getActivity());
        list = new ArrayList<PhotoComment>();
        adapter = new FeedAdapter();
    }

    public abstract FeedManager.FeedType getFeedType();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_feed, container,false);
        swipeContainer = (SwipeRefreshLayout)result.findViewById(R.id.swipe_container);
        swipeContainer.setOnRefreshListener(onRefreshListener);
//        itemTitle = result.findViewById(R.id.itemTitle);
//        itemShare = result.findViewById(R.id.itemShare);
//        shareController = FeedItem.createHolder(itemTitle,itemShare).shareController;
        lvFeed = (ListView)result.findViewById(R.id.lvFeed);
        footer = new FeedFooter(inflater,lvFeed, footerListener);
        lvFeed.setAdapter(adapter);
//        lvFeed.setOnScrollListener(listScroll);
        session.getFeedManager().registerOnChangeListener(onFeedChange);
        scrollInitialized = false;
        titleVisible = false;
        //shareController = new FeedItem.ShareController(itemTitle, itemShare);
        return result;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        list.clear();
        PhotoComment cached[] = session.getFeedManager().feed(getFeedType()).getCache();
        if (cached != null) {
            for (int i = 0; i < cached.length; i++)
                list.add(cached[i]);
        } else {
            lastNetworkAction = session.getFeedManager().feed(getFeedType());
            lastNetworkAction.setMaxID(164).load();
        }
    }

    SwipeRefreshLayout.OnRefreshListener onRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            if (getFeedType() == FeedManager.FeedType.My)
            {
                session.getFeedManager().feed(getFeedType()).clear();
                list.clear();
                adapter.notifyDataSetChanged();
            }
            if (list.size() > 0)
                session.getFeedManager().feed(getFeedType())
                        .setSinceID(list.get(0).id).load();
            else {
                swipeContainer.setRefreshing(false);
                footer.setLoading(true);
                session.getFeedManager().feed(getFeedType()).load();
            }
        }
    };

    FeedManager.OnChangeListener onFeedChange = new FeedManager.OnChangeListener()
    {
        @Override
        public void onPageLoaded(FeedManager.FeedWrapper wrapper) {

            if (wrapper.feedType != getFeedType())
                return;

            if (wrapper.flag_feedBottomReached)
            {
                footer.hide();
                return;
            }

            if (wrapper.since_id != null)
            {
                swipeContainer.setRefreshing(false);
                if (wrapper.comments != null && wrapper.comments.length > 0) {
                    int index = lvFeed.getFirstVisiblePosition();
                    View v = lvFeed.getChildAt(lvFeed.getHeaderViewsCount());
                    int top = (v == null) ? 0 : v.getTop();

                    PhotoComment[] cached = session.getFeedManager().feed(getFeedType()).getCache();
                    int diff = cached.length - list.size();
                    list.clear();

                    for (int i = 0; i < cached.length; i++)
                        list.add(cached[i]);
                    adapter.notifyDataSetChanged();
                    lvFeed.setSelectionFromTop(index + diff, top);
                }
                return;
            }

            if (wrapper.exception != null)
            {
                footer.setLoading(false);
                return;
            }
            if (wrapper.comments == null || wrapper.comments.length == 0) {
                footer.hide();
                return;
            }
            list.clear();
            PhotoComment[] cached = session.getFeedManager().feed(getFeedType()).getCache();

            for (int i = 0; i < cached.length; i++)
                list.add(cached[i]);
            adapter.notifyDataSetChanged();
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
        footer = null;
        swipeContainer = null;
        lvFeed = null;
        session.getFeedManager().unRegisterOnChangeListener(onFeedChange);
        list.clear();
    }


    public class FeedAdapter extends ArrayAdapter<PhotoComment> {

        private LayoutInflater inflater;
        private Resources resources;

        public FeedAdapter() {
            super(getActivity(), 0, list);
            inflater = getActivity().getLayoutInflater();
            resources = getActivity().getResources();
        }


        View.OnClickListener retryClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PhotoComment p = (PhotoComment)v.getTag();
                session.getFeedManager().postQuestion(p);
                notifyDataSetChanged();
            }
        };

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = FeedItem.createView(inflater, parent, retryClick);
            FeedItem.updateView(resources, convertView,getItem(position));
            int l = list.size();
            if (l > 0 && position == l - 1)
            {
                lastNetworkAction =
                        session.getFeedManager().feed(getFeedType())
                                .setMaxID(list.get(list.size() - 1).id - 1);
                lastNetworkAction.load();
            }
            return convertView;
        }

    }

    FeedFooter.FooterListener footerListener = new FeedFooter.FooterListener() {
        @Override
        public void onRetry() {
            if (lastNetworkAction != null)
            {
                footer.setLoading(true);
                lastNetworkAction.load();
            }
        }
    };

    private static class FeedFooter {
        interface FooterListener {
            void onRetry();
        }

        boolean visible = false;
        ListView listView;
        FooterListener footerListener;
        View footerView;
        TextView tvFooterStatus;
        Button btRetry;
        View progress;

        View getView()
        {
            return footerView;
        }

        void setLoading(boolean enabled)
        {
            show();
            if (enabled) {
                progress.setVisibility(View.VISIBLE);
                btRetry.setVisibility(View.GONE);
                tvFooterStatus.setText(R.string.item_loading);
            } else
            {
                progress.setVisibility(View.GONE);
                btRetry.setVisibility(View.VISIBLE);
                tvFooterStatus.setText(R.string.item_loaderror);
            }
        }

        public void show()
        {
            if (visible)
                return;
            visible = true;
            listView.addFooterView(footerView);
        }

        public void hide()
        {
            if (!visible)
                return;
            visible = false;
            listView.removeFooterView(footerView);
        }

        public FeedFooter(LayoutInflater inflater,ListView listView, FooterListener footerChange)
        {
            this.listView = listView;
            this.footerListener = footerChange;
            footerView = inflater.inflate(R.layout.footer_feed, null, false);
            tvFooterStatus = (TextView)footerView.findViewById(R.id.tvFooterStatus);
            btRetry = (Button)footerView.findViewById(R.id.btRetry);
            btRetry.setOnClickListener(onClickListener);
            progress = footerView.findViewById(R.id.progress);
            setLoading(true);
        }

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLoading(true);
                footerListener.onRetry();
            }
        };
    }
}
