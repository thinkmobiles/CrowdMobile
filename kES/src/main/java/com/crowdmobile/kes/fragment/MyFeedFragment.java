package com.crowdmobile.kes.fragment;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.crowdmobile.kes.R;
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
        ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(R.string.myfeed);
        return super.onCreateView(inflater, container, savedInstanceState);
    }
}
