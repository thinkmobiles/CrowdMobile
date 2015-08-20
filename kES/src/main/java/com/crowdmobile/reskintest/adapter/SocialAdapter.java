package com.crowdmobile.reskintest.adapter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.crowdmobile.reskintest.R;
import com.crowdmobile.reskintest.model.SocialPost;
import com.crowdmobile.reskintest.util.FacebookLogin;
import com.crowdmobile.reskintest.widget.FixedARImageView;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by john on 19.08.15.
 */
public class SocialAdapter extends RecyclerView.Adapter<SocialAdapter.ViewHolder> {

    private Activity activity;
    private ArrayList<SocialPost> items;
    private Bitmap avatar;

    public SocialAdapter(Activity activity, ArrayList<SocialPost> data){
        this.activity =activity;
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
        downloadAvatar(holder);
        if(post.getCreate_date() != null)
            holder.create_time.setText(dateParce(getTrueDateFormat(post.getCreate_date())));
        else
            holder.create_time.setText("2112-08-10");

        if(post.getImage()!= null)
            Picasso.with(activity.getApplicationContext())
                .load(post.getImage()).resize(600,600).placeholder(R.drawable.ic_feed_loading_image).error(R.drawable.ic_access_bongo).into(holder.image);


//        Picasso.with(activity.getApplicationContext())
//                .load("http://graph.facebook.com/" + FacebookLogin.KARDASJAN_ID + "/picture?type=large")
//                .fit().centerCrop().placeholder(R.drawable.ic_feed_loading_image).error(R.drawable.ic_access_bongo).into(holder.image);

        holder.description.setText(post.getDescription());

    }

    private synchronized void downloadAvatar(final ViewHolder viewHolder) {
        AsyncTask<Void, Void, Bitmap> task = new AsyncTask<Void, Void, Bitmap>() {

            @Override
            public Bitmap doInBackground(Void... params) {
                URL fbAvatarUrl = null;
                Bitmap fbAvatarBitmap = null;
                try {
                    fbAvatarUrl = new URL("https://graph.facebook.com/"+FacebookLogin.KARDASJAN_ID +"/picture?type=large");
                    fbAvatarBitmap = BitmapFactory.decodeStream(fbAvatarUrl.openConnection().getInputStream());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d("AVA", "http://graph.facebook.com/"+FacebookLogin.KARDASJAN_ID+"/picture");
                return fbAvatarBitmap;
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                viewHolder.avatar.setImageBitmap(result);
            }

        };
        task.execute();
    }


    private Date getTrueDateFormat(String _date) {
        Date date = null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        try {
            date = format.parse(_date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    private String dateParce(Date _date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String formatedDate = format.format(_date);
        return formatedDate;
    }

    public void updateData(ArrayList<SocialPost>socialPosts){
        items =socialPosts;
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

