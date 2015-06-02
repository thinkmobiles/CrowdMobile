package com.crowdmobile.kesapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.crowdmobile.kesapp.util.FacebookLogin;
import com.crowdmobile.kesapp.util.HockeyUtil;
import com.crowdmobile.kesapp.util.PreferenceUtils;
import com.crowdmobile.kesapp.util.TwitterUtil;
import com.crowdmobile.kesapp.widget.NavigationBar;
import com.kes.AccountManager;
import com.kes.KES;
import com.kes.model.User;
import com.kes.net.DataFetcher;
import com.urbanairship.analytics.Analytics;

public class AccountActivity extends Activity {

    private static final String TAG = AccountActivity.class.getSimpleName();
	private View btFacebook,btTwitter,btSkip;
	private ProgressDialog progressDialog;
	private AlertDialog alertDialog;

    private TwitterUtil.LoginManager twitterLogin;
    private FacebookLogin facebookLogin;
    private boolean openingMainActivity = false;

    public static void logout(Context context)
    {
        KES.shared().getAccountManager().logout();
        PreferenceUtils.setSkipLogin(context,false);
        TwitterUtil.getInstance(context).logout();
        //FacebookRegHelper.logout(mSession.getContext());

        AccountActivity.open(context);
        PreferenceUtils.setActiveFragment(context, NavigationBar.Attached.Feed.ordinal());
    }

	public static void open(Context context)
	{
		Intent intent = new Intent(context,AccountActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		context.startActivity(intent);
        if (context instanceof Activity)
            ((Activity)context).overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
	}

    /*
    private void getHash()
    {
        try {
            PackageInfo info =     getPackageManager().getPackageInfo(getPackageName(),     PackageManager.GET_SIGNATURES);
            for (android.content.pm.Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String sign= Base64.encodeToString(md.digest(), Base64.DEFAULT);
                Log.e("MY KEY HASH:", sign);
                //  Toast.makeText(getApplicationContext(),sign,     Toast.LENGTH_LONG).show();
            }
        } catch (PackageManager.NameNotFoundException e) {
        } catch (NoSuchAlgorithmException e) {
        }
    }
    */

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_login);
        twitterLogin = TwitterUtil.getInstance(this).getLoginManager(twitterCallback);
		facebookLogin = new FacebookLogin(this, fbCallback);
        btFacebook = findViewById(R.id.btFacebook);
		btFacebook.setOnClickListener(onClickListener);
        btTwitter = findViewById(R.id.btTwitter);
        btTwitter.setOnClickListener(onClickListener);
        btSkip = findViewById(R.id.btSkipLogin);
        btSkip.setOnClickListener(onClickListener);

		progressDialog = new ProgressDialog(this);

		progressDialog.setTitle(R.string.welcome);
		progressDialog.setCancelable(false);
		progressDialog.setIndeterminate(true);
		
		alertDialog = new AlertDialog.Builder(this)
		.setTitle(R.string.error)
		.setPositiveButton(R.string.ok, null)
		.create();
        HockeyUtil.onMainActivityCreate(this);
	}


	private boolean checkRegistered()
	{
        if (openingMainActivity)
            return true;
        if (KES.shared().getAccountManager().getUser().isRegistered())
		{
            openingMainActivity = true;
			MainActivity.open(this);
			return true;
		}
		else
			return false;
	}
	
	@Override
	public void onStart()
	{
		super.onStart();
        openingMainActivity = false;
        Analytics.activityStarted(this);
        if (!LandingActivity.hasGoogleAccount(this))
        {
            finish();
            return;
        }
        Log.d(TAG,"OnStart checkRegistered()");
        if (checkRegistered())
            return;
        KES.shared().getAccountManager().registerListener(accountListener);
	}

    @Override
	public void onStop()
	{
		super.onStop();
        Analytics.activityStopped(this);
        KES.shared().getAccountManager().unRegisterListener(accountListener);
	}

    @Override
    protected void onPause() {
        super.onPause();
        HockeyUtil.onMainActivityPause();
    }

    OnClickListener onClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
            progressDialog.show();

			if (v == btFacebook) {
                progressDialog.setMessage(getString(R.string.fb_login));
                facebookLogin.execute();
            }
            else if (v == btTwitter) {
                {
                    //startActivity(
                    //        new Intent(Intent.ACTION_VIEW, Uri.parse("oauth://twitterlogin")));

                    progressDialog.setMessage(getString(R.string.twitter_login));
                    twitterLogin.login(AccountActivity.this);
                }
            } else if (v == btSkip)
            {
                if (openingMainActivity)
                    return;
                openingMainActivity = true;
                PreferenceUtils.setActiveFragment(AccountActivity.this, NavigationBar.Attached.Feed.ordinal());
                PreferenceUtils.setSkipLogin(AccountActivity.this, true);
                MainActivity.open(AccountActivity.this);
            }

		}
		
	};

    FacebookLogin.FacebookCallback fbCallback = new FacebookLogin.FacebookCallback() {
        @Override
        public void onFail(FacebookLogin.Fail fail) {
            progressDialog.dismiss();
            if (fail == FacebookLogin.Fail.SessionOpen)
                showError(R.string.error_fb_session);
        }

        @Override
        public void onUserInfo(FacebookLogin.UserInfo userInfo) {
            KES.shared().getAccountManager().loginFacebook(userInfo.token,userInfo.uid);
        }
    };

    TwitterUtil.TwitterLoginCallback twitterCallback = new TwitterUtil.TwitterLoginCallback() {
        /*
        @Override
        public void onAuthUrl(String url) {
            if (url != null)
            {
                twitterLogin.loadUrl(url);
                twitterLogin.setVisibility(View.VISIBLE);
                progressDialog.show();
            } else
            {
                twitterLogin.loadUrl("about:blank");
                twitterLogin.setVisibility(View.GONE);
                progressDialog.hide();
            }
        }
        */

        @Override
        public void onLogin() {
            progressDialog.hide();
        }

        @Override
        public void onSuccess(String token, String secret, long uid) {
            KES.shared().getAccountManager().loginTwitter(token,secret,Long.toString(uid));
        }

        @Override
        public void onFailed() {
            //twitterLogin.loadUrl("about:blank");
            progressDialog.hide();
            alertDialog.show();
        }

        @Override
        public void onCanceled() {
            progressDialog.hide();
        }
    };

    AccountManager.AccountListener accountListener = new AccountManager.AccountListener() {
        @Override
        public void onUserLoadError(Exception e) {

        }

        @Override
        public void onUserChanged(User user) {
            progressDialog.dismiss();
            Log.d(TAG, "OnUserChanged checkRegistered()");
            checkRegistered();
        }

        @Override
        public void onLoggingIn() {

        }

        @Override
        public void onLoginFail(Exception e) {
            progressDialog.dismiss();
            if (e instanceof DataFetcher.KESNetworkException)
                alertDialog.setMessage(((DataFetcher.KESNetworkException)e).getError());
            alertDialog.show();
        }
    };

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (twitterLogin.onActivityResult(requestCode,resultCode,data)) {
            return;
        }

		if (facebookLogin.onActivityResult(this, requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	public void onDestroy()
	{
        facebookLogin.release();
		progressDialog.dismiss();
		alertDialog.dismiss();
		super.onDestroy();
	}

	
	
	private void showError(int resid)
	{
		progressDialog.hide();
		alertDialog.setMessage(getString(resid));
		alertDialog.show();
	}


}
