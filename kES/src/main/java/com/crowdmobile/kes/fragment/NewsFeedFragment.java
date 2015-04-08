package com.crowdmobile.kes.fragment;

import android.app.Activity;
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
public class NewsFeedFragment extends FeedBaseFragment {
    @Override
    public FeedManager.FeedType getFeedType() {
        return FeedManager.FeedType.Public;
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
