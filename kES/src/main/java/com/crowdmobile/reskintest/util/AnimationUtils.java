package com.crowdmobile.reskintest.util;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;

/**
 * Created by samson on 22.08.15.
 */
public class AnimationUtils {

    public static void expand(final View v, final int maxWidth, final int duration) {
        v.getLayoutParams().width = 0;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().width = (int) (maxWidth * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setInterpolator(new LinearInterpolator());
        a.setDuration(duration);
        v.startAnimation(a);
    }

    public static void collapse(final View v, final int maxWidth, final int duration, final boolean goneAfter) {
        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if(goneAfter && interpolatedTime == 1){
                    v.setVisibility(View.GONE);
                }else {
                    v.getLayoutParams().width = maxWidth - (int) (maxWidth * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setInterpolator(new LinearInterpolator());
        a.setDuration(duration);
        v.startAnimation(a);
    }
}
