package com.crowdmobile.reskintest.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.crowdmobile.reskintest.R;
import com.crowdmobile.reskintest.model.SocialPost;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by john on 19.08.15.
 */
public class SocialAdapter extends RecyclerView.Adapter<SocialAdapter.ViewHolder> {

    private static final int TYPE_FOOTER = 0;
    private static final int TYPE_ITEM = 1;

    private Activity activity;
    private ArrayList<SocialPost> items;
    private boolean isLoading = false;
    private int countItems = 0;

    public SocialAdapter(Activity activity, ArrayList<SocialPost> data) {
        this.activity = activity;
        items = data;
        countItems = items.size();
    }

    public void setIsLoading(boolean isLoading) {
        this.isLoading = isLoading;
    }

    public boolean isLoading() {
        return isLoading;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if(viewType == TYPE_ITEM)
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_social, parent, false);
        else
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.footer_feed, parent, false);
        return new ViewHolder(view, viewType);
    }

    @Override
    public int getItemViewType(int position) {
        if(items.get(position).getId().equals("-1"))
            return TYPE_FOOTER;
        return TYPE_ITEM;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        if(getItemViewType(position) == TYPE_FOOTER){
            if(isLoading){
                holder.progressBar.setVisibility(View.VISIBLE);
            } else {
                holder.progressBar.setVisibility(View.GONE);
            }
        } else {

            SocialPost post = items.get(position);
            holder.ownerName.setText(post.getPostOwner().getName());
            Picasso.with(activity.getApplicationContext())
                    .load(post.getPostOwner().getIcon()).into(holder.avatar);
            if (post.getCreate_date() != null)
                holder.create_time.setText(post.getCreate_date());
            else
                holder.create_time.setText("2112-08-10");

            if (post.getImage() != null) {
                holder.image.setVisibility(View.VISIBLE);
                Picasso.with(activity.getApplicationContext())
                        .load(post.getImage()).resize(600, 600).placeholder(R.drawable.ic_feed_loading_image).error(R.drawable.ic_access_bongo).into(holder.image);
            } else
                holder.image.setVisibility(View.GONE);

            holder.description.setText(post.getDescription());
        }
    }

    SocialPost footer = new SocialPost();

    public void updateData(ArrayList<SocialPost>socialPosts){
        socialPosts.add(footer);
        items = socialPosts;
        countItems = items.size();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return countItems;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView ownerName, description, create_time;
        ImageView avatar, image;
        ProgressBar progressBar;

        public ViewHolder(View itemView, int type) {
            super(itemView);
            if(type == TYPE_ITEM) {
                ownerName = (TextView) itemView.findViewById(R.id.tvNameOwner);
                description = (TextView) itemView.findViewById(R.id.tvDescription);
                avatar = (ImageView) itemView.findViewById(R.id.imgAvatar);
                image = (ImageView) itemView.findViewById(R.id.imgFeedPic);
                create_time = (TextView) itemView.findViewById(R.id.createDate);
            } else {
                progressBar = (ProgressBar) itemView.findViewById(R.id.progress);
                itemView.findViewById(R.id.btRetry).setVisibility(View.GONE);
            }
        }


    }
}

