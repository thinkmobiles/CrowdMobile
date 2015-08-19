package com.crowdmobile.kesapp.fragment;


import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.crowdmobile.kesapp.R;

import java.util.ArrayList;

/**
 * Created by john on 18.08.15.
 */
public class SocialFragment extends Fragment implements View.OnClickListener {

    private Activity activity;
    private View root;
    private TextView facebook,twitter;
    private SwipeRefreshLayout refreshLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_social,container,false);
        findUI(root);
        setListener();
        return root;
    }

    private void findUI(View v){
        facebook = (TextView)v.findViewById(R.id.btnFacebook);
        twitter = (TextView)v.findViewById(R.id.btnTwitter);
        refreshLayout = (SwipeRefreshLayout)v.findViewById(R.id.swipe_container);
        recyclerView = (RecyclerView)v.findViewById(R.id.rvFeed);
        progressBar = (ProgressBar)v.findViewById(R.id.progressLoading);
    }

    private void setListener(){
        facebook.setOnClickListener(this);
        twitter.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.btnFacebook:

                break;
            case R.id.btnTwitter:

                break;

        }
    }

    public class AsynkFacebookFeed extends AsyncTask<String, Void, ArrayList>{

        @Override
        protected ArrayList doInBackground(String... params) {
            return null;
        }
    }
}
