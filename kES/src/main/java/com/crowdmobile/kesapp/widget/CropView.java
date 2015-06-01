package com.crowdmobile.kesapp.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.crowdmobile.kesapp.R;
import com.crowdmobile.kesapp.util.Graphic;

public class CropView extends View {

    public interface CropViewListener {
        public void onLayout(int l,int t,int r,int b);
    }

    private enum AffectedSide {NONE,DRAG,LEFT,TOP,RIGHT,BOTTOM};
    private AffectedSide affectedSide = AffectedSide.NONE;
    private CropViewListener listener;
    Paint paint = new Paint();
    Paint paintRct = new Paint();
    private float rectMinSize = 50;
    private float rectWidth = 5;
    private int touchSize = 10;
    private int blursize = 5;
    private int initial_size = 0;
    private Point leftTop, rightBottom, center, previous;


    private Bitmap bitmap;
    private int imageScaledWidth,imageScaledHeight;
    private Rect imageScaledRect = new Rect();
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

    public void setBitmap(Bitmap bitmap)
    {
        this.bitmap = bitmap;
        invalidate();
    }

    @Override
    public void layout(int l, int t, int r, int b) {
        super.layout(l, t, r, b);

        int w = r - l;
        int h = b - t;
        // Calculate the scaled dimensions
        bmDesk = null;

        if (bitmap != null) {
            float f = Math.min((float)w / bitmap.getWidth(),(float)h / bitmap.getHeight());
            imageScaledWidth = (int)(f * bitmap.getWidth());
            imageScaledHeight = (int)(f * bitmap.getHeight());
            //imageScaledWidth = Math.round(getDrawable().getIntrinsicWidth() * f[Matrix.MSCALE_X]);
            //imageScaledHeight = Math.round(getDrawable().getIntrinsicHeight() * f[Matrix.MSCALE_Y]);

            imageScaledRect.set(
                    center.x-(imageScaledWidth/2),
                    center.y-(imageScaledHeight/2),
                    center.x+(imageScaledWidth/2),
                    center.y+(imageScaledHeight/2));

            int cx = (r - l) / 2;
            int cy = (b - t) / 2;
            imgCoord.set(cx - imageScaledWidth / 2, cy - imageScaledHeight / 2, cx + imageScaledWidth / 2, cy + imageScaledHeight / 2);
            if (listener != null)
                listener.onLayout(imgCoord.left,imgCoord.top,imgCoord.right,imgCoord.bottom);
            if (initial_size == 0)
                initial_size = Math.min(imgCoord.width(), imgCoord.height());
            resetPoints();
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
        if (bitmap == null)
            return;
        if (bmBlur == null && bitmap != null)
        {
            int maxSize = (int)Math.sqrt(w * h / (blursize * 2));
            Bitmap tmp = null;
            int srcSize = (int)Math.sqrt(bitmap.getWidth() * bitmap.getHeight());
            if (srcSize > maxSize) {
                int nw = bitmap.getWidth() * maxSize / srcSize;
                int nh = bitmap.getHeight() * maxSize / srcSize;
                tmp = Bitmap.createScaledBitmap(bitmap, nw, nh, true);
            }
            else
                tmp = bitmap;
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
        if (bitmap != null)
        {
            canvas.save();
            canvas.clipRect(leftTop.x, leftTop.y, rightBottom.x, rightBottom.y);
            canvas.drawBitmap(bitmap,null,imageScaledRect,null);
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
        rectMinSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getContext().getResources().getDisplayMetrics());
        rectWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getContext().getResources().getDisplayMetrics());
        touchSize = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10.0f, getContext().getResources().getDisplayMetrics());
        blursize = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2.0f, getContext().getResources().getDisplayMetrics());
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

        return (point.x>=(center.x-(imageScaledWidth/2))&&
                point.x<=(center.x+(imageScaledWidth/2))&&
                point.y>=(center.y-(imageScaledHeight/2))&&
                point.y<=(center.y+(imageScaledHeight/2)))?true:false;
    }

    private void adjustRectangle(int x, int y) {
        int movement;
        int minSize = Math.min(imageScaledWidth,imageScaledHeight);
        minSize = Math.min((int)rectMinSize,minSize);
        switch(affectedSide) {
            case LEFT:
                movement = x-leftTop.x;
                if(isInImageRange(new PointF(leftTop.x+movement,leftTop.y+movement)))
                    leftTop.set(leftTop.x+movement,leftTop.y+movement);
                    if (rightBottom.x - leftTop.x < minSize)
                        leftTop.set(rightBottom.x - minSize,rightBottom.y - minSize);
                break;
            case TOP:
                movement = y-leftTop.y;
                if(isInImageRange(new PointF(leftTop.x+movement,leftTop.y+movement)))
                    leftTop.set(leftTop.x+movement,leftTop.y+movement);
                if (rightBottom.x - leftTop.x < minSize)
                    leftTop.set(rightBottom.x - minSize,rightBottom.y - minSize);
                break;
            case RIGHT:
                movement = x-rightBottom.x;
                if(isInImageRange(new PointF(rightBottom.x+movement,rightBottom.y+movement)))
                    rightBottom.set(rightBottom.x+movement,rightBottom.y+movement);
                if (rightBottom.x - leftTop.x < minSize)
                    rightBottom.set(leftTop.x + minSize,leftTop.y + minSize);
                break;
            case BOTTOM:
                movement = y-rightBottom.y;
                if(isInImageRange(new PointF(rightBottom.x+movement,rightBottom.y+movement)))
                    rightBottom.set(rightBottom.x+movement,rightBottom.y+movement);
                if (rightBottom.x - leftTop.x < minSize)
                    rightBottom.set(leftTop.x + minSize,leftTop.y + minSize);
                break;
            case DRAG:
                movement = x-previous.x;
                int movementY = y-previous.y;
                if (leftTop.x + movement < imageScaledRect.left)
                    movement = imageScaledRect.left - leftTop.x;
                if (rightBottom.x + movement > imageScaledRect.right)
                    movement = imageScaledRect.right - rightBottom.x;

                if (leftTop.y + movementY < imageScaledRect.top)
                    movementY = imageScaledRect.top - leftTop.y;

                if (rightBottom.y + movementY > imageScaledRect.bottom)
                    movementY = imageScaledRect.bottom - rightBottom.y;


                leftTop.set(leftTop.x+movement,leftTop.y+movementY);
                rightBottom.set(rightBottom.x+movement,rightBottom.y+movementY);
                /*
                if(isInImageRange(new PointF(leftTop.x+movement,leftTop.y+movementY)))
                    leftTop.set(leftTop.x+movement,leftTop.y+movementY);
                if (isInImageRange(new PointF(rightBottom.x+movement,rightBottom.y+movementY)))
                    rightBottom.set(rightBottom.x+movement,rightBottom.y+movementY);
                    */
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
        int h = bitmap.getHeight() * (rightBottom.y - leftTop.y) / imageScaledHeight;
        int w = bitmap.getWidth() * (rightBottom.x - leftTop.x) / imageScaledWidth;
        Rect tmp = new Rect();
        tmp.left = bitmap.getWidth() * (leftTop.x - imgCoord.left) / imageScaledWidth;
        tmp.top = bitmap.getHeight() * (leftTop.y - imgCoord.top) / imageScaledHeight;
        tmp.right = tmp.left + w;
        tmp.bottom = tmp.top + h;
        return Bitmap.createBitmap(bitmap,tmp.left,tmp.top,tmp.width(),tmp.height());
    }
}
