package com.crowdmobile.kes;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import com.crowdmobile.kes.fragment.AccessFragment;

/**
 * Created by gadza on 2015.04.27..
 */
public class LandingActivity extends Activity {

    private AccessFragment.AccessViewHolder accessViewHolder;
    private Handler mHandler;

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
            if (hasGoogleAccount(LandingActivity.this))
                MainActivity.open(LandingActivity.this);
            else
                showNoAccount();
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
