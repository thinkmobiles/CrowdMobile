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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.crowdmobile.reskintest.MainActivity;
import com.crowdmobile.reskintest.R;
import com.crowdmobile.reskintest.adapter.SocialAdapter;
import com.crowdmobile.reskintest.model.PostOwner;
import com.crowdmobile.reskintest.model.SocialPost;

import java.util.ArrayList;

/**
 * Created by john on 18.08.15.
 */
public class SocialFragment extends Fragment implements View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = SocialFragment.class.getSimpleName();
    private MainActivity activity;
    private View root;
    private TextView facebook,twitter;
    private SwipeRefreshLayout refreshLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private ArrayList <SocialPost> socialPosts;
    private SocialAdapter socialAdapter;
    private ArrayList<SocialPost> posts;
    private LinearLayoutManager mLayoutManager;

    @Override
    public void onRefresh() {
        if(state == State.FACEBOOK)
            activity.executeFacebookGetPost();
        else
            activity.executeTwitterGetPost();
    }

    public void cancelRefresh(){
        refreshLayout.setRefreshing(false);
    }

    private enum State {FACEBOOK, TWITTER};
    private State state = State.FACEBOOK;


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
        root = inflater.inflate(R.layout.fragment_social,container,false);
        findUI(root);
        setListener();

        posts=new ArrayList<>();
        for(int i=0;i<10; i++){
            PostOwner postOwner = new PostOwner(String.valueOf(i),"FN"+String.valueOf(i),null);
            SocialPost socialPost =new SocialPost(String.valueOf(i),"desc"+String.valueOf(i),null, null, postOwner);
            posts.add(socialPost);
        }

        socialAdapter = new SocialAdapter(activity,posts);
        recyclerView.setAdapter(socialAdapter);
        return root;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    public void setCallbackData(ArrayList<SocialPost> data){
        socialAdapter.updateData(data);
    }

    private void findUI(View v){
        facebook = (TextView)v.findViewById(R.id.btnFacebook);
        twitter = (TextView)v.findViewById(R.id.btnTwitter);
        refreshLayout = (SwipeRefreshLayout)v.findViewById(R.id.swipe_container);
        recyclerView = (RecyclerView)v.findViewById(R.id.rvFeed);
        recyclerView.setHasFixedSize(false);
        mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        progressBar = (ProgressBar)v.findViewById(R.id.progressLoading);
    }

    private void setListener(){
        facebook.setOnClickListener(this);
        twitter.setOnClickListener(this);
        refreshLayout.setOnRefreshListener(this);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.btnFacebook:
                state = State.FACEBOOK;
                activity.executeFacebookGetPost();
                break;
            case R.id.btnTwitter:
                state = State.TWITTER;
                activity.executeTwitterGetPost();
                break;

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
}
