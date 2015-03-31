package com.crowdmobile.kes.list;

import android.animation.Animator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crowdmobile.kes.AccountActivity;
import com.crowdmobile.kes.R;
import com.kes.Session;
import com.kes.model.PhotoComment;
import com.squareup.picasso.Picasso;

/**
 * Created by gadza on 2015.03.06..
 */
public class FeedItem {

    public interface ClickController {
//      public void onOpenShareClick();
    };

    public static class ShareController implements View.OnClickListener {
        private View item;
        private View share;
        private View openShare;
        private View closeShare;
        private View btFlag, btFacebook, btTwitter, btCopy;

        private Animator openAnim;
        private Animator closeAnim;
        private boolean open = false;

        public boolean isOpen()
        {
            return open;
        }
        int itemIndex = -1;
        int sharedItemIndex = -1;
        PhotoComment photoComment;

        public void setItemIndex(int idx)
        {
            this.itemIndex = idx;
        }

        public int getSharedItemIndex()
        {
            return sharedItemIndex;
        }

        Animator.AnimatorListener animatorListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (animation == openAnim)
                    share.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                animation.removeListener(this);
                if (animation == closeAnim) {
                    share.setVisibility(View.INVISIBLE);
                    closeAnim = null;
                    open = false;
                    sharedItemIndex = -1;
                } else
                {
                    open = true;
                    openAnim = null;
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        };

        public ShareController(View item,View share)
        {
            this.item = item;
            this.share = share;
            this.openShare = item.findViewById(R.id.imgOpenShare);
            this.closeShare = share.findViewById(R.id.btShareClose);
            this.btFlag = share.findViewById(R.id.btShareFlag);
            this.btFacebook = share.findViewById(R.id.btShareFB);
            this.btTwitter = share.findViewById(R.id.btShareTwitter);
            this.btCopy = share.findViewById(R.id.btShareCopy);

            this.openShare.setOnClickListener(this);
            this.closeShare.setOnClickListener(this);
            this.btFlag.setOnClickListener(this);
            this.btFacebook.setOnClickListener(this);
            this.btTwitter.setOnClickListener(this);
            this.btCopy.setOnClickListener(this);
        }

        public void setItem(PhotoComment photoComment)
        {
            this.photoComment = photoComment;
        }

        public PhotoComment getItem()
        {
            return photoComment;
        }

        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AccountActivity.logout(item.getContext());
            }
        };

        @Override
        public void onClick(View v) {
            if (openAnim != null || closeAnim != null)
                return;

            if (v == openShare) {
                sharedItemIndex = itemIndex;
                share.setVisibility(View.VISIBLE);

                /*
                if (Build.VERSION.SDK_INT >= 21) {
                    int cx = item.getRight();
                    int cy = item.getTop();
                    openAnim = ViewAnimationUtils.createCircularReveal(share, cx, cy, 0, item.getWidth());
                    openAnim.addListener(animatorListener);
                    openAnim.start();
                }
                */
            } else if (v == closeShare) {
                /*
                if (Build.VERSION.SDK_INT >= 21) {
                    int cx = item.getRight();
                    int cy = item.getTop();
                    closeAnim = ViewAnimationUtils.createCircularReveal(share, cx, cy, item.getWidth(), 0);
                    closeAnim.addListener(animatorListener);
                    closeAnim.start();
                }
                */
                share.setVisibility(View.INVISIBLE);
            } else if (v == btFlag)
            {
                Session s = Session.getInstance(v.getContext());
                if (s.getAccountManager().getUser().isRegistered())
                    Session.getInstance(v.getContext()).getFeedManager().report(photoComment.id);
                else
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                    builder
                            .setTitle(R.string.not_registered_title)
                            .setMessage(R.string.not_registered_dialog_message)
                            .setPositiveButton(R.string.not_registered_btstart,onClickListener)
                            .setNegativeButton(android.R.string.cancel,null)
                            .show();

                }
            }  else if (v == btFacebook)
            {

            }  else if (v == btTwitter)
            {

            }  else if (v == btCopy)
            {
                if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setText(photoComment.share_url);
                } else {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText(v.getContext().getResources().getString(R.string.clipboard_label), photoComment.share_url);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(v.getContext(),R.string.copied_to_clipboard,Toast.LENGTH_SHORT).show();
                }
            }

                /*
                DropDownAnim ta = new DropDownAnim(holderShare,200,true);
                ta.setDuration(500);
                holderShare.startAnimation(ta);
                */
        }
    };


    public static class ViewHolder {
        TextView tvTimeQuestion;
        TextView tvQuestion;
        ImageView imgFeedPic;
        View imgOpenShare;
        View holderShare;
        View btShareClose;
        ImageView ivTitle;
        View btRetry;

        TextView tvAnswer;
        TextView tvTimeAnswer;
        public ShareController shareController;
    };


    public static View createView(LayoutInflater inflater, ViewGroup root, View.OnClickListener retryClick)
    {
        View result = inflater.inflate(R.layout.item_feed,root,false);
        ViewHolder holder = createHolder(result,result.findViewById(R.id.itemShare));
        holder.btRetry.setOnClickListener(retryClick);
        return result;
    }

    public static ViewHolder createHolder(View view,View share)
    {
        ViewHolder holder = new ViewHolder();
        view.setTag(holder);
        holder.holderShare = share;
        holder.tvTimeQuestion = (TextView)view.findViewById(R.id.tvTimeQuestion);
        holder.tvQuestion = (TextView)view.findViewById(R.id.tvMessage);
        holder.imgFeedPic = (ImageView)view.findViewById(R.id.imgFeedPic);
        holder.imgOpenShare = view.findViewById(R.id.imgOpenShare);
        holder.btShareClose = holder.holderShare.findViewById(R.id.btShareClose);
        holder.shareController = new ShareController(view, holder.holderShare);
        holder.tvAnswer = (TextView)view.findViewById(R.id.tvAnswer);
        holder.tvTimeAnswer = (TextView)view.findViewById(R.id.tvTimeAnswer);
        holder.ivTitle = (ImageView)view.findViewById(R.id.ivTitle);
        holder.btRetry = view.findViewById(R.id.btRetry);
        return holder;
    }

    public static void updateViewTitle(View convertView, PhotoComment item)
    {
        ViewHolder holder = (ViewHolder)convertView.getTag();
        holder.tvTimeQuestion.setText(Integer.toString(item.id));
        holder.tvQuestion.setText(item.message);
    }

    public static void updateView(Resources resources, View convertView, PhotoComment item)
    {
        updateViewTitle(convertView, item);
        ViewHolder holder = (ViewHolder)convertView.getTag();
        holder.btRetry.setTag(item);
        holder.shareController.setItem(item);

        if (item.status == PhotoComment.PostStatus.Posted || item.status == PhotoComment.PostStatus.Pending)
        {
            holder.ivTitle.setVisibility(View.VISIBLE);
            if (item.status == PhotoComment.PostStatus.Pending)
                holder.ivTitle.setImageResource(R.drawable.ic_navbar_credit_icon);
            else
                holder.ivTitle.setImageResource(R.drawable.ic_feed_item_header_icon);
        } else
            holder.ivTitle.setVisibility(View.INVISIBLE);
        holder.btRetry.setVisibility(item.status == PhotoComment.PostStatus.Error ? View.VISIBLE : View.GONE);

        if (item.photo_url != null && item.photo_url.length() > 0) {
            holder.imgFeedPic.setVisibility(View.VISIBLE);
            Picasso.with(convertView.getContext()).load(item.photo_url).resize(400,400).centerCrop().placeholder(R.drawable.ic_settings_logo).into(holder.imgFeedPic);
        }
        else
            holder.imgFeedPic.setVisibility(View.GONE);

        if (item.responses == null || item.responses.length == 0)
        {
//            holder.fadeLayer.setVisibility(View.VISIBLE);
            holder.tvAnswer.setText(R.string.item_noanswer);
            holder.tvTimeAnswer.setVisibility(View.INVISIBLE);
        } else
        {
//            holder.fadeLayer.setVisibility(View.INVISIBLE);
            holder.tvAnswer.setText(item.responses[0].comment);
            holder.tvTimeAnswer.setVisibility(View.VISIBLE);
            String elapsedStr = null;

            long elapsed = (System.currentTimeMillis() - item.responses[0].created_at) / 1000;
            int days = (int)(elapsed / 86400);
            if (days > 0)
                elapsedStr = String.format(resources.getString(R.string.timeformat_day), days);
            else
            {
                int hour = (int)elapsed / 3600;
                int min = (int)elapsed / 60;
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


    static class DropDownAnim extends Animation {
        private final int targetHeight;
        private final View view;
        private final boolean down;

        public DropDownAnim(View view, int targetHeight, boolean down) {
            this.view = view;
            this.targetHeight = targetHeight;
            this.down = down;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            int newHeight;
            if (down) {
                newHeight = (int) (targetHeight * interpolatedTime);
            } else {
                newHeight = (int) (targetHeight * (1 - interpolatedTime));
            }
            view.getLayoutParams().height = newHeight;
            view.requestLayout();
        }

        @Override
        public void initialize(int width, int height, int parentWidth,
                               int parentHeight) {
            super.initialize(width, height, parentWidth, parentHeight);
        }

        @Override
        public boolean willChangeBounds() {
            return true;
        }
    }
}
