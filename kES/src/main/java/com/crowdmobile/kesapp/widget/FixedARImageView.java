//Fixed aspect ratio ImageView
package com.crowdmobile.kesapp.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class FixedARImageView extends ImageView {

	float fscale = 1;
	
    public FixedARImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setAspectRatio(float scale)
    {
    	fscale = scale;
    }
    
    @Override 
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
    	int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = (int)((float)width * fscale);
        setMeasuredDimension(width, height);
    }

}