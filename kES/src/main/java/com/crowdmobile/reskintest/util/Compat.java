package com.crowdmobile.reskintest.util;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * Created by gadza on 2015.07.20..
 */
public class Compat {

    @SuppressLint("NewApi")
    public static void setDrawable(View v,Drawable d) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            v.setBackgroundDrawable(d);
        } else {
            v.setBackground(d);
        }
    }

}
