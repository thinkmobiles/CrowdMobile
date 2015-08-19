package com.crowdmobile.kesapp.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by john on 19.08.15.
 */
public class SocialAdapter extends RecyclerView.Adapter {

    private Activity activity;
    private ArrayList items;

    public SocialAdapter(Activity activity, ArrayList<HashMap> data){
        this.activity =activity;
        items = data;
    }
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }
}
