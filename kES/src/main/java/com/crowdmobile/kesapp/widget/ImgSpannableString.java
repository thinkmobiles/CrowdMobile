package com.crowdmobile.kesapp.widget;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;

import com.crowdmobile.kesapp.YoutubeActivity;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gadza on 2015.07.07..
 */


//TextView which can have images
public class ImgSpannableString extends SpannableString {

    private Bitmap placeholder;
    private TextView holder;
    ArrayList<ImgDrawable> drawables = new ArrayList<ImgDrawable>();
    PicassoTarget pt;
    public ImgSpannableString(TextView holder,Bitmap placeholder, String source) {
        super(source);
        this.holder = holder;
        this.holder.setMovementMethod(LinkMovementMethod.getInstance());
        this.placeholder = placeholder;
        String [] parts = source.split("\\s");

        int startpos = 0;
        for( String item : parts ){
            String videoID;
            if (Patterns.WEB_URL.matcher(item).matches()) {
                if (!item.startsWith("http://") && !item.startsWith("https://")){
                    item = "http://" + item;
                }
                videoID = null;
                if (item.startsWith("https://www.youtube.com")) {
                    List<NameValuePair> parameters = null;
                    try {
                        parameters = URLEncodedUtils.parse(new URI(item), "UTF-8");
                        for (NameValuePair p : parameters) {
                            if ("v".equals(p.getName())) {
                                videoID = p.getValue();
                                break;
                            }

                        }
                    } catch (URISyntaxException e) {
                    }
                }
                if (videoID != null)
                {
                    ImgDrawable d = new ImgDrawable();
                    drawables.add(d);
                    d.setBounds(0, 0, 50, 50);
                    ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BASELINE);
                    setSpan(span, startpos, startpos + item.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                    LinkClick cs = new LinkClick(videoID);
                    setSpan(cs, startpos, startpos + item.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

                    try {
                        Picasso.with(holder.getContext()).load(extractYoutubeId(item)).into(new PicassoTarget(d));
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }

                }
            }
            startpos += item.length() + 1;
        }


        holder.setTransformationMethod(null);
        holder.setText(this);
    }

    class LinkClick extends ClickableSpan {

        String link;

        public LinkClick(String link)
        {
            this.link = link;
        }

        @Override
        public void onClick(View widget) {
            YoutubeActivity.openVideo(holder.getContext(),link);
        }
    };

    class PicassoTarget implements Target {

        private ImgDrawable imgDrawable;

        public PicassoTarget(ImgDrawable d)
        {
            imgDrawable = d;
            d.setTarget(this);
        }

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            imgDrawable.setBitmap(bitmap);
            holder.invalidate();
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            Log.d("Error","error");
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            Log.d("Error","error");
        }
    };

    class ImgDrawable extends Drawable {

        private PicassoTarget target;
        private Bitmap bitmap;
        private ImgDrawable()
        {
        }

        protected void setTarget(PicassoTarget t)
        {
            this.target = t;    //keep strong ref
        }

        public void setBitmap(Bitmap b)
        {
            bitmap = b;
        }

        @Override
        public void draw(Canvas canvas) {
            if (bitmap != null)
                canvas.drawBitmap(bitmap,null,getBounds(),null);
            else if (placeholder != null)
                canvas.drawBitmap(placeholder,null,getBounds(),null);


        }

        @Override
        public void setAlpha(int alpha) {

        }

        @Override
        public void setColorFilter(ColorFilter cf) {

        }

        @Override
        public int getOpacity() {
            return 0;
        }
    };

    public String extractYoutubeId(String url) throws MalformedURLException {
        String query = new URL(url).getQuery();
        String[] param = query.split("&");
        String id = null;
        for (String row : param) {
            String[] param1 = row.split("=");
            if (param1[0].equals("v")) {
                id = param1[1];
            }
        }
        return "http://img.youtube.com/vi/"+id+"/0.jpg";
    }


}
