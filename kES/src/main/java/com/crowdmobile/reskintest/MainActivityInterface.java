package com.crowdmobile.reskintest;

import android.os.Handler;
import android.widget.ImageView;

/**
 * Created by gadza on 2015.07.01..
 */
public interface MainActivityInterface {

    void zoomImageFromThumb(ImageView view);
    void showNoCreditDialog();
    Handler getHandler();
}
