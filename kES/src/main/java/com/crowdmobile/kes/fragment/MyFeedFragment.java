package com.crowdmobile.kes.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.crowdmobile.kes.MainActivity;
import com.crowdmobile.kes.widget.NavigationBar;
import com.kes.FeedManager;
import com.kes.Session;
import com.kes.model.PhotoComment;

/**
 * Created by gadza on 2015.03.16..
 */
public class MyFeedFragment extends FeedBaseFragment {

    private static final String TAG = FeedBaseFragment.class.getSimpleName();
    boolean isVisibleToUser = false;
    SparseArray<PhotoComment> itemViewed = new SparseArray<PhotoComment>();

    @Override
    public FeedManager.FeedType getFeedType() {
        return FeedManager.FeedType.My;
    }

    @Override
    public void onItemViewed(PhotoComment p) {
//        Log.d(TAG,"-----item viewed" + p.id);
        itemViewed.put(p.id, p);
        if (lvFeed.getScrollState() == RecyclerView.SCROLL_STATE_IDLE)
            flushViewed();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = super.onCreateView(inflater, container, savedInstanceState);
        lvFeed.setOnScrollListener(listScroll);
        return result;
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(navigationChange, new IntentFilter(NavigationBar.ACTION_CHANGE));
        navigationChange.onReceive(null, null);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(navigationChange);
    }

    BroadcastReceiver navigationChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            isVisibleToUser =
                    ((MainActivity) getActivity()).getNavigationBar().getAttached() == NavigationBar.Attached.MyFeed;
            flushViewed();
        }
    };

    private void flushViewed() {
        if(!isVisibleToUser)
            return;
        FeedManager feedManager = Session.getInstance(getActivity()).getFeedManager();
        for (int i = 0; i < itemViewed.size(); i++)
        {
            PhotoComment pc = itemViewed.valueAt(i);
            itemViewed.removeAt(i);
            pc.markAsRead(feedManager);
            //Log.d(TAG,"Item viewed"+Integer.toString(pc.id));
        }
    }

    RecyclerView.OnScrollListener listScroll = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState != RecyclerView.SCROLL_STATE_IDLE)
                return;
            flushViewed();
            //Log.d(TAG, "Scroll state idle");
        }
    };

}
