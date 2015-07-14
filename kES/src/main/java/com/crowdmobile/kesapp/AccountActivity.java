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
    private boolean isForeground = false;
    private boolean loginInProgress = false;

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
        KES.shared().getAccountManager().registerListener(accountListener);
	}


	private boolean checkRegistered()
	{
        if (openingMainActivity) {
            Log.d(TAG, "already opening main activity");
            return true;
        }
        if (KES.shared().getAccountManager().getUser().isRegistered())
		{
            openMainActivity();
			return true;
		}
		else {
            Log.d(TAG, "user is not registered");
            return false;
        }
	}
	
	@Override
	public void onStart()
	{
		super.onStart();
        isForeground = true;
        openingMainActivity = false;
        Analytics.activityStarted(this);
        if (!LandingActivity.hasGoogleAccount(this))
        {
            finish();
            return;
        }
        Log.d(TAG, "OnStart checkRegistered()");
        if (checkRegistered())
            return;
	}

    @Override
	public void onStop()
	{
        Log.d(TAG, "onStop()");
		super.onStop();
        isForeground = false;
        Analytics.activityStopped(this);
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
                if (loginInProgress)
                    return;
                loginInProgress = true;
                progressDialog.setMessage(getString(R.string.fb_login));
                facebookLogin.execute();
            }
            else if (v == btTwitter) {
                {
                    if (loginInProgress)
                        return;
                    //startActivity(
                    //        new Intent(Intent.ACTION_VIEW, Uri.parse("oauth://twitterlogin")));
                    loginInProgress = true;
                    progressDialog.setMessage(getString(R.string.twitter_login));
                    twitterLogin.login(AccountActivity.this);
                }
            } else if (v == btSkip)
            {
                if (openingMainActivity)
                    return;
                PreferenceUtils.setActiveFragment(AccountActivity.this, NavigationBar.Attached.Feed.ordinal());
                PreferenceUtils.setSkipLogin(AccountActivity.this, true);
                openMainActivity();
            }

		}
		
	};

    private void openMainActivity()
    {
        if (openingMainActivity)
            return;
        openingMainActivity = true;
        MainActivity.open(AccountActivity.this);
    }

    FacebookLogin.FacebookCallback fbCallback = new FacebookLogin.FacebookCallback() {
        @Override
        public void onFail(FacebookLogin.Fail fail) {
            //Log.d(TAG,"Facebook fail");
            loginInProgress = false;
            progressDialog.hide();
            //if (fail == FacebookLogin.Fail.SessionOpen)
            showError(R.string.error_fb_login);
        }

        @Override
        public void onUserInfo(FacebookLogin.UserInfo userInfo) {
            Log.d(TAG,"Facebook success");
            KES.shared().getAccountManager().loginFacebook(userInfo.token,userInfo.uid);
        }
    };

    TwitterUtil.TwitterLoginCallback twitterCallback = new TwitterUtil.TwitterLoginCallback() {

        @Override
        public void onSuccess(String token, String secret, long uid) {
            KES.shared().getAccountManager().loginTwitter(token,secret,Long.toString(uid));
        }

        @Override
        public void onFailed(String message) {
            //twitterLogin.loadUrl("about:blank");
            loginInProgress = false;
            progressDialog.hide();
            alertDialog.setMessage(message);
            alertDialog.show();
        }

        @Override
        public void onCanceled() {
            loginInProgress = false;
            progressDialog.hide();
        }
    };

    AccountManager.AccountListener accountListener = new AccountManager.AccountListener() {
        @Override
        public void onUserChanged(User user) {
            {
                Log.d(TAG, "onUserChanged");
                Log.d(TAG, user.first_name);
                Log.d(TAG, user.auth_token);
                Log.d(TAG, user.toString());
            }
            loginInProgress = false;
            if (user != null && user.isRegistered() && isForeground)
            {
                progressDialog.hide();
                openMainActivity();
            }
        }

        @Override
        public void onLoggingIn() {
            progressDialog.setMessage(getString(R.string.twitter_login));
            progressDialog.show();
        }

        @Override
        public void onLoginFail(Exception e) {
            loginInProgress = false;
            progressDialog.hide();
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
        loginInProgress = false;
        facebookLogin.release();
		progressDialog.dismiss();
		alertDialog.dismiss();
        KES.shared().getAccountManager().unRegisterListener(accountListener);
		super.onDestroy();
	}

	
	
	private void showError(int resID)
	{
		progressDialog.hide();
		alertDialog.setMessage(getString(resID));
		alertDialog.show();
	}


}
