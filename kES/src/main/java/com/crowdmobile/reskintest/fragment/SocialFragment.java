package com.crowdmobile.reskintest.fragment;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.crowdmobile.reskintest.MainActivity;
import com.crowdmobile.reskintest.R;
import com.crowdmobile.reskintest.adapter.SocialAdapter;
import com.crowdmobile.reskintest.model.PostOwner;
import com.crowdmobile.reskintest.model.SocialPost;
import com.crowdmobile.reskintest.model.YoutubeResponse;
import com.crowdmobile.reskintest.util.AnimationUtils;
import com.crowdmobile.reskintest.util.PreferenceUtils;
import com.crowdmobile.reskintest.util.YoutubeUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by john on 18.08.15.
 */
public class SocialFragment extends Fragment implements View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = SocialFragment.class.getSimpleName();
    private static final String CHANNELID = "UCEeYPJ1GSYWf0RXS8nARHjg";
    private MainActivity activity;
    private ImageView facebook, twitter, youtube;
    private ImageView arrowRight;
    private LinearLayout tabsContainer;
    private SwipeRefreshLayout refreshLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private SocialAdapter socialAdapter;
    private LinearLayoutManager mLayoutManager;
    private ArrayList<SocialPost> postsList, feedFacebook, feedTwitter, feedYoutube;
    private State state = State.FACEBOOK;
    private int twittre_paging = 1;
    private boolean isRefresh = false;

    private enum State {FACEBOOK, TWITTER, YOUTUBE}

    @Override
    public void onRefresh() {
        isRefresh = true;
        switch (state){
            case FACEBOOK:
                activity.clearFacebookNextInfo();
                activity.executeFacebookGetPost(this);
                break;
            case TWITTER:
                activity.clearTwitterNextInfo();
                activity.executeTwitterGetPost(this,1);
                break;
            case YOUTUBE:
                activity.executeYoutubeGetPost(this);
                break;
        }
    }

    public void cancelRefresh() {
        refreshLayout.setRefreshing(false);
    }

    public void updateFeedFacebook(ArrayList<SocialPost> list){
        feedFacebook.addAll(list);
        postsList = feedFacebook;
        updateList(postsList);
    }

    public void updateFeedTwitter(ArrayList<SocialPost> list) {
        feedTwitter.addAll(list);
        postsList = feedTwitter;
        updateList(postsList);
    }

    public void updateFeedYoutube(ArrayList<SocialPost> list) {
        feedYoutube.addAll(list);
        postsList = feedYoutube;
        updateList(feedYoutube);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (MainActivity) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_social, container, false);
        findUI(root);
        setListener();
        setAdapter();
        initFeeds();
        selectTab(State.FACEBOOK, feedFacebook);

        return root;
    }

    private void findUI(View v){
        facebook = (ImageView)v.findViewById(R.id.btnFacebook);
        twitter = (ImageView)v.findViewById(R.id.btnTwitter);
        youtube = (ImageView)v.findViewById(R.id.btnYoutube);
        arrowRight = (ImageView) v.findViewById(R.id.btnArrowRight);
        refreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_container);
        recyclerView = (RecyclerView) v.findViewById(R.id.rvFeed);
        recyclerView.setHasFixedSize(false);
        mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        progressBar = (ProgressBar) v.findViewById(R.id.progressLoading);
        tabsContainer = (LinearLayout) v.findViewById(R.id.tabsSocial);
    }

    private void setListener() {
        facebook.setOnClickListener(this);
        twitter.setOnClickListener(this);
        youtube.setOnClickListener(this);
        arrowRight.setOnClickListener(this);
        recyclerView.addOnScrollListener(scrollListener);
        refreshLayout.setOnRefreshListener(this);
    }

    private void setAdapter() {
        postsList = new ArrayList<>();
        socialAdapter = new SocialAdapter(activity, postsList);
        recyclerView.setAdapter(socialAdapter);
    }

    private void initFeeds() {
        feedFacebook = new ArrayList<>();
        feedTwitter = new ArrayList<>();
        feedYoutube = new ArrayList<>();
    }

    public void clearFeed(){
        if(isRefresh) {
            switch (state) {
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
    }

    private void updateList(ArrayList<SocialPost> list){
        socialAdapter.setIsLoading(false);
        progressBar.setVisibility(View.GONE);
        socialAdapter.updateData(list);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btnFacebook:
                selectTab(State.FACEBOOK, feedFacebook);
                break;
            case R.id.btnTwitter:
                selectTab(State.TWITTER, feedTwitter);
                break;
            case R.id.btnYoutube:
                selectTab(State.YOUTUBE, feedYoutube);
                break;            case R.id.btnArrowRight:
                setTabsVisibility(true);
                break;
        }
    }

    private void selectTab(State _state, ArrayList<SocialPost> feedList){
        state = _state;
        if(isFilledList(feedList)){
            updateList(feedList);
            mLayoutManager.scrollToPositionWithOffset(0,0);
        } else {
            progressBar.setVisibility(View.VISIBLE);
            switch (state) {
                case FACEBOOK:
                    activity.executeFacebookGetPost(this);
                    break;
                case TWITTER:
                    activity.executeTwitterGetPost(this, twittre_paging);
                    break;
                case YOUTUBE:
                    activity.executeYoutubeGetPost(this);
                    break;
            }

        }
    }

    private boolean isFilledList(ArrayList<SocialPost> list){
        return (list != null && list.size() != 0);
    }

    private void setTabsVisibility(boolean isVisible){
        if(isVisible){
            AnimationUtils.expand(tabsContainer, 90, 300);
            AnimationUtils.collapse(arrowRight, 150, 300, true);
        } else {
            if (arrowRight.getVisibility() != View.VISIBLE) {
                AnimationUtils.collapse(tabsContainer, 90, 300, true);
                AnimationUtils.expand(arrowRight, 150, 300);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            setTabsVisibility(false);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            Log.e("pos", mLayoutManager.findLastVisibleItemPosition() + " " + socialAdapter.getItemCount());
            if(mLayoutManager.findLastVisibleItemPosition() == socialAdapter.getItemCount() - 2 && !socialAdapter.isLoading()){
                socialAdapter.setIsLoading(true);
                loadMorePosts();
            }
        }
    };

    private void loadMorePosts(){
        isRefresh = false;
        switch (state){
            case FACEBOOK:
                activity.executeFacebookGetPost(this);
                break;
            case TWITTER:
                activity.executeTwitterGetPost(this,++twittre_paging);
                break;
            case YOUTUBE:
                activity.executeYoutubeGetNextPost(this);
                break;
        }
    }
}
