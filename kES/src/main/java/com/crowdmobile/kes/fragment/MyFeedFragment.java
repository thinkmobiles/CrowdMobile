package com.crowdmobile.kes.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kes.FeedManager;

/**
 * Created by gadza on 2015.03.16..
 */
public class MyFeedFragment extends FeedBaseFragment {

    @Override
    public FeedManager.FeedType getFeedType() {
        return FeedManager.FeedType.My;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }
}
