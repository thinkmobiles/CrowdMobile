package com.crowdmobile.kes.fragment;

import com.kes.FeedManager;
import com.kes.Session;
import com.kes.model.PhotoComment;

/**
 * Created by gadza on 2015.03.16..
 */
public class MyFeedFragment extends FeedBaseFragment {

    private static final String TAG = FeedBaseFragment.class.getSimpleName();

    @Override
    public FeedManager.FeedType getFeedType() {
        return FeedManager.FeedType.My;
    }

    @Override
    public void onItemViewed(PhotoComment p) {
//        Log.d(TAG,"-----item viewed" + p.id);
        p.markAsRead(Session.getInstance(getActivity()).getFeedManager());
    }


}
