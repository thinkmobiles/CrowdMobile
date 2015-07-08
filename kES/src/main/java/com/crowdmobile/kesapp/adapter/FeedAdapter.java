package com.crowdmobile.kesapp.adapter;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crowdmobile.kesapp.R;
import com.kes.FeedManager;
import com.kes.model.PhotoComment;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by gadza on 2015.04.01..
 */
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.ItemHolder> {

    private static final int TYPE_ITEM = 100;
    private static final int TYPE_FOOTER = 101;

    private boolean footerLoading = true;
    private FeedAdapterListener listener = null;
    private Activity activity;
    private Handler mHandler = new Handler();
    private Bitmap placeHolder;

    public interface FeedAdapterListener {
//        public void onLastItemReached();
        public void retryLoadClick();
        public void retryPostClick(PhotoComment p);
        public void report(PhotoComment p);
        public void markAsPrivate(PhotoComment p);
        public void onItemViewed(PhotoComment p);
        public void onImageClick(ImageView v);
    }

    public void hideFooter()
    {
        int s = list.size();
        if (s == 0)
            return;
        s--;
        if (list.get(s) != null)
            return;
        list.remove(s);
        notifyItemRemoved(s);
    }

    public void setFooterLoading(boolean enabled)
    {
        footerLoading = enabled;
        int s = list.size();
        if (s > 0 && list.get(s - 1) == null) {
            notifyItemChanged(s - 1);
            return;
        }
        list.add(null);
        notifyItemInserted(s);
    }

    public static class ItemHolder extends RecyclerView.ViewHolder {
        public int itemID;
        int viewType;
        View itemCard;
        TextView tvTimeQuestion;
        TextView tvQuestion;
        View messagePlaceholder;
        ImageView imgFeedPic;
        View imgOpenShare;

        TextView tvAnswer;
        View tvAnswerLabel;
        ImageView ivAnswerLeft;
        ImageView ivAnswerCenter;
        ImageView ivAnswerRight;
        //Footer
        View btRetry;
        View progress;
        View holderBackground;
        TextView tvLikeCount;
        View detailHolder;

        public View answerBackground;
        public ValueAnimator backgroundAnimator;

        public ItemHolder(View view, int viewType, View.OnClickListener itemClick, View.OnClickListener report_privateClick, View.OnClickListener retryClick,View.OnClickListener retryPostClick,View.OnClickListener imgClick) {
            super(view);
            this.viewType = viewType;
            if (viewType == TYPE_ITEM) {
                itemCard = view.findViewById(R.id.itemCard);
                itemCard.setOnClickListener(itemClick);
                holderBackground = view.findViewById(R.id.holderBackground);
                answerBackground = view.findViewById(R.id.answerBackground);
                tvTimeQuestion = (TextView) view.findViewById(R.id.tvTimeQuestion);
                tvQuestion = (TextView) view.findViewById(R.id.tvMessage);
                messagePlaceholder = view.findViewById(R.id.messagePlaceholder);
                imgFeedPic = (ImageView) view.findViewById(R.id.imgFeedPic);
                imgFeedPic.setOnClickListener(imgClick);
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

                detailHolder = view.findViewById(R.id.detailHolder);
                tvLikeCount = (TextView) view.findViewById(R.id.tvLikeCount);

                btRetry = view.findViewById(R.id.btRetry);
                btRetry.setOnClickListener(retryPostClick);
                ivAnswerRight.setOnClickListener(report_privateClick);
                backgroundAnimator = new ValueAnimator();
                backgroundAnimator.setDuration(500);
                backgroundAnimator.setFloatValues(1, 0);
                backgroundAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        answerBackground.setAlpha((float) valueAnimator.getAnimatedValue());
                    }
                });
                backgroundAnimator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        answerBackground.setVisibility(View.INVISIBLE);
                        answerBackground.setAlpha(1);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
            } else {
                progress = view.findViewById(R.id.progress);
                btRetry = (Button) view.findViewById(R.id.btRetry);
                btRetry.setOnClickListener(retryClick);
            }

        }
    };

    private LayoutInflater inflater;
    private Resources resources;
    private List<PhotoComment> list;
    FeedManager.FeedType feedType;

    public FeedAdapter(Activity activity, FeedManager.FeedType feedType, List<PhotoComment> list,FeedAdapterListener listener) {
        inflater = activity.getLayoutInflater();
        resources = activity.getResources();
        this.feedType = feedType;
        this.activity = activity;
        this.list = list;
        this.listener = listener;
        this.placeHolder = BitmapFactory.decodeResource(activity.getResources(), R.drawable.ic_youtube);
    }

    View.OnClickListener itemClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ItemHolder holder = (ItemHolder)v.getTag();
            PhotoComment p = findByID(holder.itemID);
            if (p == null)
                return;
            if (!p.isUnread())
                return;
            listener.onItemViewed(p);
            holder.backgroundAnimator.start();
        }
    };

    private PhotoComment findByID(int id)
    {
        for (int i = 0; i < list.size(); i++)
            if (list.get(i).getID(feedType) == id)
                return list.get(i);
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        if (list.get(position) == null)
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
        Log.d("TAG","OnCreateViewHolder");
        return new ItemHolder(result, viewType, itemClick, report_privateClick, retryClick, retryPostClick, imgClick);
    }

    @Override
    public void onBindViewHolder(ItemHolder holder, int i) {
        if (getItemViewType(i) == TYPE_FOOTER) {
            if (footerLoading) {
                holder.progress.setVisibility(View.VISIBLE);
                holder.btRetry.setVisibility(View.GONE);
                /*
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onLastItemReached();
                    }
                });
                */
            } else {
                holder.progress.setVisibility(View.GONE);
                holder.btRetry.setVisibility(View.VISIBLE);
            }
            return;
        }

        PhotoComment item = list.get(i);
        holder.itemID = item.getID(feedType);
        holder.itemCard.setTag(holder);

        /*
        if (feedType == FeedManager.FeedType.My)
            listener.onItemViewed(item);
        */

        holder.backgroundAnimator.cancel();
        if (item.isUnread()) {
            holder.answerBackground.setVisibility(View.VISIBLE);
            holder.answerBackground.setAlpha(1);
        }
        else
            holder.answerBackground.setVisibility(View.INVISIBLE);

        //holder.backgroundAnimator.start();
        holder.imgOpenShare.setTag(item.share_url);

        if (item.status == PhotoComment.PostStatus.Posted) {
            /*
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
            */
            holder.tvTimeQuestion.setText(R.string.feed_question);
            holder.tvTimeQuestion.setVisibility(View.VISIBLE);
        }
        else if (item.status == PhotoComment.PostStatus.Pending) {
            holder.tvTimeQuestion.setText(R.string.feed_posting);
            holder.tvTimeQuestion.setVisibility(View.VISIBLE);
        } else
            holder.tvTimeQuestion.setVisibility(View.INVISIBLE);

        //holder.tvTimeQuestion.setText(Integer.toString(item.id));

        if (TextUtils.isEmpty(item.message)) {
            holder.messagePlaceholder.setVisibility(View.VISIBLE);
            holder.tvQuestion.setVisibility(View.GONE);
        }
        else {
            holder.tvQuestion.setText(item.message);
            /*
            SpannableString ss = new SpannableString(item.message);
            Drawable d = holder.tvQuestion.getResources().getDrawable(R.drawable.ic_camera);
            d.setBounds(0, 0, 20, 20);
            ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BASELINE);
            ss.setSpan(span, 2, 3, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            holder.tvQuestion.setTransformationMethod(null);
            holder.tvQuestion.setText(ss);
            */
            //String tmp = "this is a video1 https://www.youtube.com/watch?v=-eH_4TMDuqw and a video2 https://www.youtube.com/watch?v=sZq87CNwY3w link in this feed and any https://www.youtube.com/watch?v=i3ZZkX1LYbc";
            //new ImgSpannableString(holder.tvQuestion,placeHolder, tmp);
            holder.tvQuestion.setVisibility(View.VISIBLE);
            holder.messagePlaceholder.setVisibility(View.GONE);
        }

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


            if (holder.detailHolder != null) {
                int likeCount = item.responses[0].likes_count;
                if (likeCount == 0)
                    holder.detailHolder.setVisibility(View.GONE);
                else {
                    holder.detailHolder.setVisibility(View.VISIBLE);
                    if (holder.tvLikeCount != null) {
                        if (likeCount == 0)
                            holder.tvLikeCount.setVisibility(View.GONE);
                        else {
                            holder.tvLikeCount.setVisibility(View.VISIBLE);
                            holder.tvLikeCount.setText(holder.tvLikeCount.getContext().getResources().getQuantityString(R.plurals.like, likeCount, likeCount));
                        }
                    }
                }
            }

            holder.tvAnswerLabel.setVisibility(View.VISIBLE);
            holder.ivAnswerLeft.setVisibility(View.VISIBLE);
            holder.ivAnswerCenter.setVisibility(View.INVISIBLE);
            holder.ivAnswerRight.setVisibility(View.VISIBLE);
            if (feedType == FeedManager.FeedType.My) {
                if (item.is_private)
                    holder.ivAnswerRight.setImageResource(R.drawable.ic_private);
                else
                    holder.ivAnswerRight.setImageResource(R.drawable.ic_public);
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
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    View.OnClickListener imgClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            listener.onImageClick((ImageView)v);

        }
    };

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
