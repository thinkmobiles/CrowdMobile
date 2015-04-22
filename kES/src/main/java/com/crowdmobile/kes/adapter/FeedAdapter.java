package com.crowdmobile.kes.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crowdmobile.kes.R;
import com.kes.FeedManager;
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
        public void retryLoadClick();
        public void retryPostClick(PhotoComment p);
        public void report(PhotoComment p);
        public void markAsPrivate(PhotoComment p);
    }

    public void hideFooter()
    {
        footerVisible = false;
        notifyDataSetChanged();
    }

    public void setFooterLoading(boolean enabled)
    {
        footerVisible = true;
        footerLoading = enabled;
        notifyDataSetChanged();
    }

    static class ItemHolder extends RecyclerView.ViewHolder {
        TextView tvTimeQuestion;
        TextView tvQuestion;
        ImageView imgFeedPic;
        View imgOpenShare;

        TextView tvAnswer;
        View tvAnswerLabel;
        ImageView ivAnswerLeft;
        ImageView ivAnswerCenter;
        ImageView ivAnswerRight;
        //Footer
        TextView tvFooterStatus;
        Button btRetry;
        View progress;
        View holderBackground;

        public ItemHolder(View view, int viewType, View.OnClickListener report_privateClick, View.OnClickListener retryClick,View.OnClickListener retryPostClick) {
            super(view);
            if (viewType == TYPE_ITEM) {
                holderBackground = view.findViewById(R.id.holderBackground);
                tvTimeQuestion = (TextView) view.findViewById(R.id.tvTimeQuestion);
                tvQuestion = (TextView) view.findViewById(R.id.tvMessage);
                imgFeedPic = (ImageView) view.findViewById(R.id.imgFeedPic);
                imgOpenShare = view.findViewById(R.id.imgOpenShare);
                if (imgOpenShare != null) {
                    imgOpenShare.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String url = (String) v.getTag();
                            shareUrl(v.getContext(), url);
                        }
                    });
                }
                tvAnswer = (TextView) view.findViewById(R.id.tvAnswer);
                tvAnswerLabel = (TextView) view.findViewById(R.id.tvAnswerLabel);
                ivAnswerLeft = (ImageView) view.findViewById(R.id.imgAnswerLeft);
                ivAnswerCenter = (ImageView) view.findViewById(R.id.imgAnswerCenter);
                ivAnswerRight = (ImageView) view.findViewById(R.id.imgAnswerRight);

                btRetry = (Button) view.findViewById(R.id.btRetry);
                btRetry.setOnClickListener(retryPostClick);
                ivAnswerRight.setOnClickListener(report_privateClick);
            } else {
                tvFooterStatus = (TextView) view.findViewById(R.id.tvFooterStatus);
                progress = view.findViewById(R.id.progress);
                btRetry = (Button) view.findViewById(R.id.btRetry);
                btRetry.setOnClickListener(retryClick);
            }

        }
    };

    private LayoutInflater inflater;
    private Resources resources;
    private ArrayList<PhotoComment> list;
    FeedManager.FeedType feedType;

    public FeedAdapter(Activity activity, FeedManager.FeedType feedType, ArrayList<PhotoComment> list,FeedAdapterListener listener) {
        inflater = activity.getLayoutInflater();
        resources = activity.getResources();
        this.feedType = feedType;
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

        return new ItemHolder(result, viewType, report_privateClick, retryClick, retryPostClick);
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
        holder.imgOpenShare.setTag(item.share_url);

        if (item.status == PhotoComment.PostStatus.Posted) {
            String elapsedStr = null;
            long elapsed = (System.currentTimeMillis() - item.created_at) / 1000;
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
            holder.tvTimeQuestion.setText(elapsedStr);
            holder.tvTimeQuestion.setVisibility(View.VISIBLE);
        }
        else if (item.status == PhotoComment.PostStatus.Pending) {
            holder.tvTimeQuestion.setText(R.string.feed_posting);
            holder.tvTimeQuestion.setVisibility(View.VISIBLE);
        } else
            holder.tvTimeQuestion.setVisibility(View.INVISIBLE);

        //holder.tvTimeQuestion.setText(Integer.toString(item.id));

        holder.tvQuestion.setText(item.message);

        holder.ivAnswerRight.setTag(item);
        holder.btRetry.setTag(item);
        holder.btRetry.setVisibility(item.status == PhotoComment.PostStatus.Error ? View.VISIBLE : View.GONE);

        if (item.photo_url != null && item.photo_url.length() > 0) {
            holder.imgFeedPic.setVisibility(View.VISIBLE);
            Picasso.with(holder.imgFeedPic.getContext()).load(item.photo_url).fit().centerCrop().placeholder(R.drawable.ic_feed_loading_image).into(holder.imgFeedPic);
        } else
            holder.imgFeedPic.setVisibility(View.GONE);

        if (item.responses == null || item.responses.length == 0) {
//            holder.fadeLayer.setVisibility(View.VISIBLE);
            holder.tvAnswer.setText(R.string.item_noanswer);
            holder.tvAnswerLabel.setVisibility(View.INVISIBLE);
            holder.ivAnswerLeft.setVisibility(View.INVISIBLE);
            holder.ivAnswerCenter.setVisibility(View.VISIBLE);
            holder.ivAnswerRight.setVisibility(View.INVISIBLE);
//            holder.holderBackground.setBackgroundColor(R.color.item_background_disabled);
//            holder.layout.setMaskColor(resources.getColor(R.color.item_fade_layer));
        } else {
//            holder.holderBackground.setBackgroundColor(R.color.item_background);
//            holder.fadeLayer.setVisibility(View.INVISIBLE);
//            holder.layout.setMaskColor(0);
            holder.tvAnswerLabel.setVisibility(View.VISIBLE);
            holder.ivAnswerLeft.setVisibility(View.VISIBLE);
            holder.ivAnswerCenter.setVisibility(View.INVISIBLE);
            holder.ivAnswerRight.setVisibility(View.VISIBLE);
            if (feedType == FeedManager.FeedType.My) {
                if (item.is_private)
                    holder.ivAnswerRight.setImageResource(R.drawable.ic_feed_item_header_private);
                else
                    holder.ivAnswerRight.setImageResource(R.drawable.ic_feed_item_header_public);
            } else
            {
                if (item.reported) {
                    holder.ivAnswerRight.setVisibility(View.INVISIBLE);
                } else
                {
                    holder.ivAnswerRight.setVisibility(View.VISIBLE);
                    holder.ivAnswerRight.setImageResource(R.drawable.ic_feed_item_header_flag);
                }
            }
            holder.tvAnswer.setText(item.responses[0].comment);

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
            listener.retryLoadClick();
            /*
            PhotoComment p = (PhotoComment) v.getTag();
            Session.getInstance(v.getContext()).getFeedManager().postQuestion(p);
            */
            notifyDataSetChanged();
        }
    };

    View.OnClickListener retryPostClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            listener.retryPostClick((PhotoComment) v.getTag());
            notifyDataSetChanged();
        }
    };

    View.OnClickListener report_privateClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (feedType == FeedManager.FeedType.My)
                listener.markAsPrivate((PhotoComment) v.getTag());
            else
                listener.report((PhotoComment) v.getTag());
            notifyDataSetChanged();
        }
    };

    private static void shareUrl(Context context, String url)
    {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, url);
            intent.putExtra(android.content.Intent.EXTRA_SUBJECT, context.getResources().getString(R.string.share_subject));
            context.startActivity(Intent.createChooser(intent, context.getResources().getString(R.string.share_title)));

        } catch (Exception e)
        {
            Toast.makeText(context,R.string.share_unavailable,Toast.LENGTH_SHORT).show();
        }
    }

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
