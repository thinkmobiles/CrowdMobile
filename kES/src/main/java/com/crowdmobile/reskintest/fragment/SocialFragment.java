package com.crowdmobile.reskintest.fragment;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.crowdmobile.reskintest.MainActivity;
import com.crowdmobile.reskintest.R;
import com.crowdmobile.reskintest.adapter.SocialAdapter;
import com.crowdmobile.reskintest.model.SocialPost;
import com.crowdmobile.reskintest.util.AnimationUtils;

import java.util.ArrayList;

/**
 * Created by john on 18.08.15.
 */
public class SocialFragment extends Fragment implements View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = SocialFragment.class.getSimpleName();
    private static final String CHANNELID = "UCEeYPJ1GSYWf0RXS8nARHjg";
    private MainActivity activity;
    private ImageView facebook,twitter, youtube;
    private ImageView arrowRight;
    private LinearLayout tabsContainer;
    private SwipeRefreshLayout refreshLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private SocialAdapter socialAdapter;
    private LinearLayoutManager mLayoutManager;
    private ArrayList<SocialPost> postsList, feedFacebook, feedTwitter, feedYoutube;
    private State state = State.FACEBOOK;
    private int twittre_paging =1;

    private enum State{FACEBOOK, TWITTER, YOUTUBE}

    @Override
    public void onRefresh() {
        if(state == State.FACEBOOK) {
            activity.clearFacebookNextInfo();
            activity.executeFacebookGetPost();
        }
        else {
            activity.clearTwitterNextInfo();
            activity.executeTwitterGetPost(twittre_paging);
        }
    }

    public void cancelRefresh(){
        refreshLayout.setRefreshing(false);
    }

    public void updateFeedFacebook(ArrayList<SocialPost> list){
        feedFacebook.addAll(list);
        postsList = feedFacebook;
        updateList(postsList);
    }

    public void updateFeedTwitter(ArrayList<SocialPost> list){
        feedTwitter.addAll(list);
        postsList = feedTwitter;
        updateList(postsList);
    }

    public void updateFeedYoutube(ArrayList<SocialPost> list){
        feedYoutube.addAll(list);
        updateList(feedYoutube);
    }

//    private SocialFragment(){
//    }
//
//    public static SocialFragment getInstance(){
//        if
//        SocialFragment socialFragment = new SocialFragment()
//    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (MainActivity) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_social,container,false);
        findUI(root);
        setListener();
        setAdapter();
        initFeeds();
        activity.executeYoutubeGetToken();
        selectFacebook();

        return root;
    }

    public void setCallbackData(ArrayList<SocialPost> data){
        socialAdapter.updateData(data);
    }

    private void findUI(View v){
        facebook = (ImageView)v.findViewById(R.id.btnFacebook);
        twitter = (ImageView)v.findViewById(R.id.btnTwitter);
        youtube = (ImageView)v.findViewById(R.id.btnYoutube);
        arrowRight = (ImageView) v.findViewById(R.id.btnArrowRight);
        refreshLayout = (SwipeRefreshLayout)v.findViewById(R.id.swipe_container);
        recyclerView = (RecyclerView)v.findViewById(R.id.rvFeed);
        recyclerView.setHasFixedSize(false);
        mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        progressBar = (ProgressBar)v.findViewById(R.id.progressLoading);
        tabsContainer = (LinearLayout) v.findViewById(R.id.tabsSocial);
    }

    private void setListener(){
        facebook.setOnClickListener(this);
        twitter.setOnClickListener(this);
        youtube.setOnClickListener(this);
        arrowRight.setOnClickListener(this);
        recyclerView.addOnScrollListener(scrollListener);
        refreshLayout.setOnRefreshListener(this);
    }

    private void setAdapter(){
        postsList=new ArrayList<>();
        socialAdapter = new SocialAdapter(activity, postsList);
        recyclerView.setAdapter(socialAdapter);
    }

    private void initFeeds(){
        feedFacebook = new ArrayList<>();
        feedTwitter = new ArrayList<>();
        feedYoutube = new ArrayList<>();
    }

    public void clearFeed(){
        switch (state){
            case FACEBOOK:
                feedFacebook.clear();
                break;
            case TWITTER:
                feedTwitter.clear();
                break;
            case YOUTUBE:
                feedYoutube.clear();
                break;
        }
    }

    private void updateList(ArrayList<SocialPost> list){
        socialAdapter.updateData(list);
    }


    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.btnFacebook:
                selectFacebook();
                break;
            case R.id.btnTwitter:
                selectTwitter();
                break;
            case R.id.btnYoutube:
                selectYoutube();
                break;
            case R.id.btnArrowRight:
                setTabsVisibility(true);
                break;

        }
    }

    private void selectFacebook(){
        state = State.FACEBOOK;
        if(isFilledList(feedFacebook)){
            updateList(feedFacebook);
        } else {
            activity.executeFacebookGetPost();
        }

//        selectCurrentTab(facebook);
    }

    private void selectTwitter(){
        state = State.TWITTER;
        if(isFilledList(feedTwitter)){
            updateList(feedTwitter);
        } else {
            activity.executeTwitterGetPost(twittre_paging);
        }
//        selectCurrentTab(twitter);
    }

    private void selectYoutube(){
        state = State.YOUTUBE;
        activity.executeYoutubeGetPost(this);
//        selectCurrentTab(youtube);
    }

    private boolean isFilledList(ArrayList<SocialPost> list){
        return (list != null && list.size() != 0);
    }

//    private void selectCurrentTab(TextView textView){
//        facebook.setTextColor(activity.getResources().getColor(R.color.black));
//        twitter.setTextColor(activity.getResources().getColor(R.color.black));
//        youtube.setTextColor(activity.getResources().getColor(R.color.black));
//
//        textView.setTextColor(activity.getResources().getColor(R.color.white));
//    }

    private void setTabsVisibility(boolean isVisible){
        if(isVisible){
            AnimationUtils.expand(tabsContainer, 90, 300);
            AnimationUtils.collapse(arrowRight, 150, 300, true);
        } else {
            if(arrowRight.getVisibility() != View.VISIBLE) {
                AnimationUtils.collapse(tabsContainer, 90, 300, true);
                AnimationUtils.expand(arrowRight, 150, 300);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume() {
        super.onResume();
        activity.setFragment(this);
    }

    @Override
    public void onStop() {
        super.onStop();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        activity.closeSession();
    }

    RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            setTabsVisibility(false);
        }
    };
}
