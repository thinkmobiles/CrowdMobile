package com.crowdmobile.kesapp;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import com.crowdmobile.kesapp.fragment.AccessFragment;
import com.crowdmobile.kesapp.util.PreferenceUtils;
import com.kes.KES;
import com.urbanairship.analytics.Analytics;

/**
 * Created by gadza on 2015.04.27..
 */
public class LandingActivity extends Activity {

    private AccessFragment.AccessViewHolder accessViewHolder;
    private Handler mHandler;
    private TextView tvVersion;

    public static boolean hasGoogleAccount(Context context)
    {
        AccountManager manager = AccountManager.get(context);
        Account[] accounts = manager.getAccountsByType("com.google");
        return (accounts != null && accounts.length > 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);
        tvVersion = (TextView)findViewById(R.id.tvVersion);
        boolean ds = false;
        String s = "";
        s = "Version " + BuildConfig.VERSION_NAME;
        if (BuildConfig.DEBUG) {
            s += "\r\nDebug signature";
            ds = true;
        }
        if (AppCfg.staging) {
            s += "\r\nDEMO / Staging";
            ds = true;
        }
        tvVersion.setText(s);
        if (ds)
            tvVersion.setTextColor(getResources().getColor(R.color.splash_text_development));
        mHandler = new Handler();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Analytics.activityStarted(this);
        if (BuildConfig.DEBUG)
            mHandler.postDelayed(startSystem, 1000);
        else
            mHandler.postDelayed(startSystem, 1500);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Analytics.activityStopped(this);
        mHandler.removeCallbacks(startSystem);
    }

    Runnable startSystem = new Runnable() {
        @Override
        public void run() {
            if (!hasGoogleAccount(LandingActivity.this)) {
                showNoAccount();
                return;
            }

            if (!KES.shared().getAccountManager().getUser().isRegistered() && !PreferenceUtils.getSkipLogin(LandingActivity.this))
                AccountActivity.open(LandingActivity.this);
            else
                MainActivity.open(LandingActivity.this);
        }
    };

    private void showNoAccount()
    {
        setContentView(R.layout.fragment_access);
        accessViewHolder = AccessFragment.getViews(findViewById(R.id.accessFragment));
        accessViewHolder.tvTitle.setText(getString(R.string.noaccount_title));
        accessViewHolder.tvMessage.setText(getString(R.string.noaccount_message));
        accessViewHolder.btAccess.setText(getString(R.string.noaccount_btquit));
        accessViewHolder.btAccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

}
