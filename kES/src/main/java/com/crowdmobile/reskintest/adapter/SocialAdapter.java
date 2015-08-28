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
import com.crowdmobile.reskintest.fragment.SocialFragment;
import com.crowdmobile.reskintest.model.SocialPost;
import com.crowdmobile.reskintest.util.DateParser;
import com.crowdmobile.reskintest.widget.FixedARImageView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by john on 19.08.15.
 */
public class SocialAdapter extends RecyclerView.Adapter<SocialAdapter.ViewHolder> {

    private static final int TYPE_FOOTER = 0;
    private static final int TYPE_ITEM = 1;

    private OnItemClick mOnItemClickListener;

    private Activity activity;
    private ArrayList<SocialPost> items;
    private boolean isLoading = false;
    private int countItems = 0;
    private SocialFragment.State state = SocialFragment.State.FACEBOOK;

    public SocialAdapter(Activity activity) {
        this.activity = activity;
        items = new ArrayList<>();
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
        if(items.get(position).getId() != null &&
                items.get(position).getId().equals("-1"))
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

            holder.setPosition(position);

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

                if(state == SocialFragment.State.YOUTUBE){
                    holder.image.setVisibility(View.GONE);
                    holder.image.setAspectRatio(3f / 4f);
                    holder.image.setVisibility(View.VISIBLE);
                    if (post.getDuration() != null && !post.getDuration().equals("0")) {
                        holder.duration.setText(DateParser.parseDuration(post.getDuration()));
                    }
                    holder.duration.setVisibility(View.VISIBLE);
                } else {
                    holder.image.setVisibility(View.GONE);
                    holder.image.setAspectRatio(1f);
                    holder.image.setVisibility(View.VISIBLE);
                    holder.duration.setVisibility(View.GONE);
                }
            } else
                holder.image.setVisibility(View.GONE);

            holder.description.setText(post.getDescription());
        }
    }

    SocialPost footer = new SocialPost();

    public void updateData(ArrayList<SocialPost>socialPosts, SocialFragment.State state){
        this.state = state;

        footer = new SocialPost();
        items = new ArrayList<>();
        items.addAll(socialPosts);
        items.add(footer);
        countItems = items.size();
        notifyDataSetChanged();
    }

    public  void setOnItemClickListener(final OnItemClick _onItemClick) {
        mOnItemClickListener = _onItemClick;
    }

    @Override
    public int getItemCount() {
        return countItems;
    }

    public SocialPost getItem(int position){
        return items.get(position);
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView ownerName, description, create_time, duration;
        FixedARImageView image;
        ImageView avatar;
        ProgressBar progressBar;
        int posItem;

        public ViewHolder(View itemView, int type) {
            super(itemView);
            if(type == TYPE_ITEM) {
                ownerName = (TextView) itemView.findViewById(R.id.tvNameOwner);
                description = (TextView) itemView.findViewById(R.id.tvDescription);
                avatar = (ImageView) itemView.findViewById(R.id.imgAvatar);
                image = (FixedARImageView) itemView.findViewById(R.id.imgFeedPic);
                create_time = (TextView) itemView.findViewById(R.id.createDate);
                duration = (TextView) itemView.findViewById(R.id.tvDuration);
                image.setOnClickListener(this);
            } else {
                progressBar = (ProgressBar) itemView.findViewById(R.id.progress);
                itemView.findViewById(R.id.btRetry).setVisibility(View.GONE);
            }

        }

        @Override
        public void onClick(View v) {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClicked(posItem);
            }
        }

        public void setPosition(int position){
            posItem = position;
        }

    }

    public interface OnItemClick {
        void onItemClicked(int position);
    }
}

