package com.crowdmobile.reskintest.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.crowdmobile.reskintest.R;
import com.crowdmobile.reskintest.model.SocialPost;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by john on 19.08.15.
 */
public class SocialAdapter extends RecyclerView.Adapter<SocialAdapter.ViewHolder> {

    private Activity activity;
    private ArrayList<SocialPost> items;

    public SocialAdapter(Activity activity, ArrayList<SocialPost> data) {
        this.activity = activity;
        items = data;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_social, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

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

    public void updateData(ArrayList<SocialPost> socialPosts) {
        items = socialPosts;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView ownerName, description, create_time;
        ImageView avatar, image;

        public ViewHolder(View itemView) {
            super(itemView);
            ownerName = (TextView) itemView.findViewById(R.id.tvNameOwner);
            description = (TextView) itemView.findViewById(R.id.tvDescription);
            avatar = (ImageView) itemView.findViewById(R.id.imgAvatar);
            image = (ImageView) itemView.findViewById(R.id.imgFeedPic);
            create_time = (TextView) itemView.findViewById(R.id.createDate);
        }


    }
}

