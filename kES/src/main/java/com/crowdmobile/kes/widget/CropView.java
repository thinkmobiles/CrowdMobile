package com.crowdmobile.kes.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.crowdmobile.kes.R;
import com.crowdmobile.kes.util.Graphic;

public class CropView extends ImageView {

    public interface CropViewListener {
        public void onLayout(int l,int t,int r,int b);
    }

    private enum AffectedSide {NONE,DRAG,LEFT,TOP,RIGHT,BOTTOM};
    private AffectedSide affectedSide = AffectedSide.NONE;
    private CropViewListener listener;
    Paint paint = new Paint();
    Paint paintRct = new Paint();
    private float rectWidth = 5;
    private int touchSize = 10;
    private int blursize = 5;
    private int initial_size = 0;
    private Point leftTop, rightBottom, center, previous;


    private int imageScaledWidth,imageScaledHeight;
    private Rect imgCoord = new Rect();
    private Bitmap bmBlur;
    private Bitmap bmDesk;
    private Rect dst = new Rect();
    private Rect dst2 = new Rect();
    private Rect dst3 = new Rect();
    // Adding parent class constructors

    public void setCropViewListener(CropViewListener listener)
    {
        this.listener = listener;
    }

    public CropView(Context context) {
        super(context);
        initCropView();
    }

    public CropView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        initCropView();
    }

    public CropView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initCropView();
    }

    @Override
    public void layout(int l, int t, int r, int b) {
        super.layout(l, t, r, b);
        float[] f = new float[9];
        getImageMatrix().getValues(f);

        // Calculate the scaled dimensions
        bmDesk = null;
        if (initial_size == 0)
            initial_size = (r - l) / 2;
        resetPoints();

        Drawable d = getDrawable();
        if (d != null) {
            imageScaledWidth = Math.round(getDrawable().getIntrinsicWidth() * f[Matrix.MSCALE_X]);
            imageScaledHeight = Math.round(getDrawable().getIntrinsicHeight() * f[Matrix.MSCALE_Y]);
            int cx = (r - l) / 2;
            int cy = (b - t) / 2;
            imgCoord.set(cx - imageScaledWidth / 2, cy - imageScaledHeight / 2, cx + imageScaledWidth / 2, cy + imageScaledHeight / 2);
            if (listener != null)
                listener.onLayout(imgCoord.left,imgCoord.top,imgCoord.right,imgCoord.bottom);
        }
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        int w = getWidth();
        int h = getHeight();

        if (bmDesk == null)
        {
            int x = 0;
            int y = 0;
            int cw = w / 50;
            bmDesk = Bitmap.createBitmap(w, cw * 2, Bitmap.Config.ARGB_8888);
            bmDesk.eraseColor(Color.rgb(128, 128, 128));
            Canvas c = new Canvas(bmDesk);
            boolean indent = false;
            for (int i = 0; i < 2; i++) {
                indent = !indent;
                if (indent)
                    x = cw;
                else
                    x = 0;
                while (x < w) {
                    c.drawRect(x, y, x + cw, y + cw, paintRct);
                    x += cw * 2;
                }
                y += cw;
            }
        }
        if(leftTop.equals(0, 0))
            resetPoints();
        int top = 0;
        while (top < h)
        {
            canvas.drawBitmap(bmDesk,0,top,null);
            top += bmDesk.getHeight();
        }
        if (getDrawable() == null)
            return;
        if (bmBlur == null && getDrawable() != null)
        {
            int maxSize = w * h / 10;
            Bitmap src = ((BitmapDrawable)getDrawable()).getBitmap();
            Bitmap tmp = null;
            int srcSize = src.getWidth() * src.getHeight();
            if (srcSize > maxSize)
                tmp = Bitmap.createScaledBitmap(src, src.getWidth() * maxSize / srcSize, src.getHeight() * maxSize / srcSize,true);
            else
                tmp = src;
            bmBlur = Graphic.fastblur(tmp, blursize);
        }

        //super.onDraw(canvas);
        if (bmBlur != null)
            canvas.drawBitmap(bmBlur,null,imgCoord,null);
        canvas.drawColor(Color.argb(0x40, 0, 0, 0));

        if (affectedSide == AffectedSide.NONE)
            paint.setStrokeWidth(rectWidth);
        else
            paint.setStrokeWidth(rectWidth * 2);

        canvas.drawRect(leftTop.x, leftTop.y, rightBottom.x, rightBottom.y, paint);
        if (getDrawable() != null)
        {
            canvas.save();
            canvas.clipRect(leftTop.x, leftTop.y, rightBottom.x, rightBottom.y);
            super.onDraw(canvas);
            canvas.restore();
            /*
            Bitmap src = ((BitmapDrawable)getDrawable()).getBitmap();
            h = src.getHeight() * (rightBottom.y - leftTop.y) / imageScaledHeight;
            w = src.getWidth() * (rightBottom.x - leftTop.x) / imageScaledWidth;

            dst2.left = src.getWidth() * (leftTop.x - imgCoord.left) / imageScaledWidth;
            dst2.top = src.getHeight() * (leftTop.y - imgCoord.top) / imageScaledHeight;
            dst2.right = dst2.left + w;
            dst2.bottom = dst2.top + h;
            dst3.set(leftTop.x,leftTop.y,rightBottom.x,rightBottom.y);
            canvas.drawBitmap(src,dst2,dst3,null);
            */
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int eventaction = event.getAction();
        int x = (int)event.getX();
        int y = (int)event.getY();

        if (x < imgCoord.left)
            x = imgCoord.left;
        if (x > imgCoord.right)
            x = imgCoord.right;
        if (y < imgCoord.top)
            y = imgCoord.top;
        if (y > imgCoord.bottom)
            y = imgCoord.bottom;

        switch (eventaction) {
            case MotionEvent.ACTION_DOWN:
                previous.set(x, y);
                affectedSide = getAffectedSide((int)event.getX(),(int)event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                 if (affectedSide != AffectedSide.NONE) {
                     adjustRectangle(x, y);
                     invalidate(); // redraw rectangle
                 }
                 previous.set(x, y);
                break;
            case MotionEvent.ACTION_UP:
                affectedSide = AffectedSide.NONE;
                previous = new Point();
                invalidate();
                break;
        }
        return true;
    }

    private void initCropView() {
        rectWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getContext().getResources().getDisplayMetrics());
        touchSize = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10.0f, getContext().getResources().getDisplayMetrics());
        blursize = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.0f, getContext().getResources().getDisplayMetrics());
        paint.setColor(getResources().getColor(R.color.appTitleColor));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        paintRct.setColor(Color.rgb(160,160,160));
        paintRct.setStyle(Paint.Style.FILL);
        leftTop = new Point();
        rightBottom = new Point();
        center = new Point();
        previous = new Point();
    }

    public void resetPoints() {
        center.set(getWidth()/2, getHeight()/2);
        leftTop.set((getWidth()-initial_size)/2,(getHeight()-initial_size)/2);
        rightBottom.set(leftTop.x+initial_size, leftTop.y+initial_size);
    }

    private boolean isInImageRange(PointF point) {
        // Get image matrix values and place them in an array

        return (point.x>=(center.x-(imageScaledWidth/2))&&point.x<=(center.x+(imageScaledWidth/2))&&point.y>=(center.y-(imageScaledHeight/2))&&point.y<=(center.y+(imageScaledHeight/2)))?true:false;
    }

    private void adjustRectangle(int x, int y) {
        int movement;
        switch(affectedSide) {
            case LEFT:
                movement = x-leftTop.x;
                if(isInImageRange(new PointF(leftTop.x+movement,leftTop.y+movement)))
                    leftTop.set(leftTop.x+movement,leftTop.y+movement);
                break;
            case TOP:
                movement = y-leftTop.y;
                if(isInImageRange(new PointF(leftTop.x+movement,leftTop.y+movement)))
                    leftTop.set(leftTop.x+movement,leftTop.y+movement);
                break;
            case RIGHT:
                movement = x-rightBottom.x;
                if(isInImageRange(new PointF(rightBottom.x+movement,rightBottom.y+movement)))
                    rightBottom.set(rightBottom.x+movement,rightBottom.y+movement);
                break;
            case BOTTOM:
                movement = y-rightBottom.y;
                if(isInImageRange(new PointF(rightBottom.x+movement,rightBottom.y+movement)))
                    rightBottom.set(rightBottom.x+movement,rightBottom.y+movement);
                break;
            case DRAG:
                movement = x-previous.x;
                int movementY = y-previous.y;
                if(isInImageRange(new PointF(leftTop.x+movement,leftTop.y+movementY)) && isInImageRange(new PointF(rightBottom.x+movement,rightBottom.y+movementY))) {
                    leftTop.set(leftTop.x+movement,leftTop.y+movementY);
                    rightBottom.set(rightBottom.x+movement,rightBottom.y+movementY);
                }
                break;
        }
    }

    private AffectedSide getAffectedSide(float x, float y) {
        if (!(x>=(leftTop.x-touchSize)&&x<=(rightBottom.x+touchSize)&& y>=(leftTop.y-touchSize)&&y<=(rightBottom.y+touchSize)))
            return affectedSide.NONE;
        if(x>=(leftTop.x-touchSize)&&x<=(leftTop.x+touchSize))
            return affectedSide.LEFT;
        else if(y>=(leftTop.y-touchSize)&&y<=(leftTop.y+touchSize))
            return affectedSide.TOP;
        else if(x>=(rightBottom.x-touchSize)&&x<=(rightBottom.x+touchSize))
            return affectedSide.RIGHT;
        else if(y>=(rightBottom.y-touchSize)&&y<=(rightBottom.y+touchSize))
            return affectedSide.BOTTOM;
        else
            return affectedSide.DRAG;
    }

    public Bitmap getCroppedImage() {
        Bitmap src = ((BitmapDrawable)getDrawable()).getBitmap();
        int h = src.getHeight() * (rightBottom.y - leftTop.y) / imageScaledHeight;
        int w = src.getWidth() * (rightBottom.x - leftTop.x) / imageScaledWidth;
        Rect tmp = new Rect();
        tmp.left = src.getWidth() * (leftTop.x - imgCoord.left) / imageScaledWidth;
        tmp.top = src.getHeight() * (leftTop.y - imgCoord.top) / imageScaledHeight;
        tmp.right = tmp.left + w;
        tmp.bottom = tmp.top + h;
        return Bitmap.createBitmap(src,tmp.left,tmp.top,tmp.width(),tmp.height());
    }
}
