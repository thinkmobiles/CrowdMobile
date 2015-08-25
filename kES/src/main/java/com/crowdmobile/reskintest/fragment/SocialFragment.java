package com.crowdmobile.reskintest.fragment;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by john on 18.08.15.
 */
public class SocialFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = SocialFragment.class.getSimpleName();
    private static final String CHANNELID = "UCEeYPJ1GSYWf0RXS8nARHjg";
    private MainActivity activity;
    private TextView facebook,twitter, youtube;
    private ImageView arrowRight;
    private LinearLayout tabsContainer;
    private SwipeRefreshLayout refreshLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private SocialAdapter socialAdapter;
    private LinearLayoutManager mLayoutManager;
    private ArrayList<SocialPost> postsList;


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
        activity.executeYoutubeGetToken();

        return root;
    }

    public void setCallbackData(ArrayList<SocialPost> data){
        socialAdapter.updateData(data);
    }

    private void findUI(View v){
        facebook = (TextView)v.findViewById(R.id.btnFacebook);
        twitter = (TextView)v.findViewById(R.id.btnTwitter);
        youtube = (TextView)v.findViewById(R.id.btnYoutube);
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
    }

    private void setAdapter(){
        ArrayList<SocialPost> postsList=new ArrayList<>();
        for(int i=0;i<10; i++){
            PostOwner postOwner = new PostOwner(String.valueOf(i),"FN"+String.valueOf(i),null);
            SocialPost socialPost = new SocialPost(String.valueOf(i),"desc"+String.valueOf(i),null, null, postOwner);
            postsList.add(socialPost);
        }

        socialAdapter = new SocialAdapter(activity, postsList);
        recyclerView.setAdapter(socialAdapter);
    }

    public void updateList(ArrayList<SocialPost> list){
        postsList.addAll(list);
        socialAdapter.updateData(postsList);
    }


    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.btnFacebook:
                activity.executeFacebookGetPost();
                selectCurrentTab(facebook);

                break;
            case R.id.btnTwitter:
                activity.executeTwitterGetPost();
                selectCurrentTab(twitter);

                break;
            case R.id.btnYoutube:
                activity.executeYoutubeGetPost(this);
                selectCurrentTab(youtube);
                break;
            case R.id.btnArrowRight:
                setTabsVisibility(true);
                break;

        }
    }

    private void selectCurrentTab(TextView textView){
        facebook.setTextColor(activity.getResources().getColor(R.color.black));
        twitter.setTextColor(activity.getResources().getColor(R.color.black));
        youtube.setTextColor(activity.getResources().getColor(R.color.black));

        textView.setTextColor(activity.getResources().getColor(R.color.white));
    }

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

    RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            setTabsVisibility(false);
        }
    };
}
