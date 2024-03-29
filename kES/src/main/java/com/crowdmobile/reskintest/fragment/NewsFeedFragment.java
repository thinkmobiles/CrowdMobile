package com.crowdmobile.reskintest.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kes.FeedManager;
import com.kes.model.PhotoComment;

/**
 * Created by gadza on 2015.03.16..
 */
public class NewsFeedFragment extends FeedBaseFragment {
    @Override
    public FeedManager.FeedType getFeedType() {
        return FeedManager.FeedType.Public;
    }

    @Override
    public void onItemViewed(PhotoComment p) {
        //ignore
        p.markAsRead();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }
    
}
