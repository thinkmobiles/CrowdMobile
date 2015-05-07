package com.crowdmobile.kes;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

/**
 * Created by gadza on 2015.04.27..
 */
public class LandingActivity extends Activity {

    Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);
        mHandler = new Handler();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mHandler.postDelayed(startSystem,500);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mHandler.removeCallbacks(startSystem);
    }

    Runnable startSystem = new Runnable() {
        @Override
        public void run() {
            MainActivity.open(LandingActivity.this);
        }
    };
}
