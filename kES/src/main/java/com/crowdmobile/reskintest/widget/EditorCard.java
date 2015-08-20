package com.crowdmobile.reskintest.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;

import com.crowdmobile.reskintest.R;

/**
 * Created by gadza on 2015.04.08..
 */
public class EditorCard extends CardView {

    public int maxHeight = 0;

    public EditorCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.KESWidget);

        final int N = a.getIndexCount();
        for (int i = 0; i < N; ++i)
        {
            int attr = a.getIndex(i);
            switch (attr)
            {
                case R.styleable.KESWidget_maxHeight:
                    maxHeight = (int)a.getDimension(attr, 0);
                    break;
                default:
                    break;
            }
        }
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (maxHeight != 0)
        {
            int height = MeasureSpec.getSize(heightMeasureSpec);
            if (height > maxHeight)
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST);
            /*
            widthMeasureSpec = MeasureSpec.getSize(widthMeasureSpec);
            heightMeasureSpec = MeasureSpec.getSize(heightMeasureSpec);
            if (heightMeasureSpec > maxHeight)
                heightMeasureSpec = maxHeight;
            setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
            //return;
            */
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
