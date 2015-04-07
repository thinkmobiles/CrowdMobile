package com.crowdmobile.kes.adapter;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.crowdmobile.kes.R;
import com.kes.Session;
import com.kes.model.PhotoComment;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by gadza on 2015.04.01..
 */
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.ItemHolder> {

    private static final int TYPE_ITEM = 100;
    private static final int TYPE_FOOTER = 101;

    private boolean footerVisible = true;
    private boolean footerLoading = true;
    private FeedAdapterListener listener = null;
    private Activity activity;
    private Handler mHandler = new Handler();

    public interface FeedAdapterListener {
        public void onLastItemReached();
        public void retryClick();
    }

    public void setFooterVisible(boolean visible)
    {
        footerVisible = visible;
        notifyDataSetChanged();
    }

    public void setFooterLoading(boolean enabled)
    {
        footerLoading = enabled;
        notifyDataSetChanged();
    }

    static class ItemHolder extends RecyclerView.ViewHolder {
        TextView tvTimeQuestion;
        TextView tvQuestion;
        ImageView imgFeedPic;
        View imgOpenShare;
        ImageView ivTitle;

        TextView tvAnswer;
        TextView tvTimeAnswer;

        //Footer
        TextView tvFooterStatus;
        Button btRetry;
        View progress;
        View layerBrightness;

        public ItemHolder(View view, View.OnClickListener retryClick) {
            super(view);
            tvTimeQuestion = (TextView)view.findViewById(R.id.tvTimeQuestion);
            tvQuestion = (TextView)view.findViewById(R.id.tvMessage);
            imgFeedPic = (ImageView)view.findViewById(R.id.imgFeedPic);
            imgOpenShare = view.findViewById(R.id.imgOpenShare);
            tvAnswer = (TextView)view.findViewById(R.id.tvAnswer);
            tvTimeAnswer = (TextView)view.findViewById(R.id.tvTimeAnswer);
            ivTitle = (ImageView)view.findViewById(R.id.ivTitle);
            layerBrightness = view.findViewById(R.id.layerBrightness);

            tvFooterStatus = (TextView)view.findViewById(R.id.tvFooterStatus);
            btRetry = (Button)view.findViewById(R.id.btRetry);
            btRetry.setOnClickListener(retryClick);
            progress = view.findViewById(R.id.progress);
        }
    };

    private LayoutInflater inflater;
    private Resources resources;
    private ArrayList<PhotoComment> list;

    public FeedAdapter(Activity activity,ArrayList<PhotoComment> list,FeedAdapterListener listener) {
        inflater = activity.getLayoutInflater();
        resources = activity.getResources();
        this.activity = activity;
        this.list = list;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (footerVisible && position == getItemCount() - 1)
            return TYPE_FOOTER;
        return TYPE_ITEM;
    }


    @Override
    public ItemHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View result;
        if (viewType == TYPE_ITEM)
            result = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.item_feed, viewGroup, false);
        else
            result = LayoutInflater.
                    from(viewGroup.getContext()).
                    inflate(R.layout.footer_feed, viewGroup, false);

        return new ItemHolder(result, retryClick);
    }

    @Override
    public void onBindViewHolder(ItemHolder holder, int i) {
        if (getItemViewType(i) == TYPE_FOOTER) {
            if (footerLoading) {
                holder.progress.setVisibility(View.VISIBLE);
                holder.btRetry.setVisibility(View.GONE);
                holder.tvFooterStatus.setText(R.string.item_loading);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onLastItemReached();
                    }
                });
            } else {
                holder.progress.setVisibility(View.GONE);
                holder.btRetry.setVisibility(View.VISIBLE);
                holder.tvFooterStatus.setText(R.string.item_loaderror);
            }
            return;
        }

        PhotoComment item = list.get(i);
        holder.tvTimeQuestion.setText(Integer.toString(item.id));
        holder.tvQuestion.setText(item.message);

        holder.btRetry.setTag(item);

        if (item.status == PhotoComment.PostStatus.Posted || item.status == PhotoComment.PostStatus.Pending) {
            holder.ivTitle.setVisibility(View.VISIBLE);
            if (item.status == PhotoComment.PostStatus.Pending) {
                holder.ivTitle.setImageResource(R.drawable.ic_navbar_credit_icon);
            } else
                holder.ivTitle.setImageResource(R.drawable.ic_feed_item_header_icon);
        } else
            holder.ivTitle.setVisibility(View.INVISIBLE);

        holder.btRetry.setVisibility(item.status == PhotoComment.PostStatus.Error ? View.VISIBLE : View.GONE);

        if (item.photo_url != null && item.photo_url.length() > 0) {
            holder.imgFeedPic.setVisibility(View.VISIBLE);
            Picasso.with(holder.imgFeedPic.getContext()).load(item.photo_url).fit().centerCrop().placeholder(R.drawable.ic_feed_loading_image).into(holder.imgFeedPic);
        } else
            holder.imgFeedPic.setVisibility(View.GONE);

        if (item.responses == null || item.responses.length == 0) {
//            holder.fadeLayer.setVisibility(View.VISIBLE);
            holder.tvAnswer.setText(R.string.item_noanswer);
            holder.tvTimeAnswer.setVisibility(View.INVISIBLE);
            holder.layerBrightness.setVisibility(View.VISIBLE);
//            holder.layout.setMaskColor(resources.getColor(R.color.item_fade_layer));
        } else {
            holder.layerBrightness.setVisibility(View.GONE);
//            holder.fadeLayer.setVisibility(View.INVISIBLE);
//            holder.layout.setMaskColor(0);
            holder.tvAnswer.setText(item.responses[0].comment);
            holder.tvTimeAnswer.setVisibility(View.VISIBLE);
            String elapsedStr = null;

            long elapsed = (System.currentTimeMillis() - item.responses[0].created_at) / 1000;
            int days = (int) (elapsed / 86400);
            if (days > 0)
                elapsedStr = String.format(resources.getString(R.string.timeformat_day), days);
            else {
                int hour = (int) elapsed / 3600;
                int min = (int) elapsed / 60;
                min -= (hour * 60);
                if (hour > 0)
                    elapsedStr = String.format(resources.getString(R.string.timeformat_hour_min), hour, min);
                else
                    elapsedStr = String.format(resources.getString(R.string.timeformat_min), min);
            }
            holder.tvTimeAnswer.setText(elapsedStr);
        }
//        holder.fadeLayer.setVisibility(View.VISIBLE);


    }
    @Override
    public int getItemCount() {
        int result = list.size();
        if (footerVisible)
            result++;
        return result;
    }

    View.OnClickListener retryClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            listener.retryClick();
            PhotoComment p = (PhotoComment) v.getTag();
            Session.getInstance(v.getContext()).getFeedManager().postQuestion(p);
            notifyDataSetChanged();
        }
    };


    /*
FooterListener footerListener = new FooterListener() {
    @Override
    public void onRetry() {
        if (lastNetworkAction != null)
        {
            footer.setLoading(true);
            lastNetworkAction.load();
        }
    }
};
*/

}
