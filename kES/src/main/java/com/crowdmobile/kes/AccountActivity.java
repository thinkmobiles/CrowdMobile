package com.crowdmobile.kes;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.crowdmobile.kes.util.FacebookLogin;
import com.crowdmobile.kes.util.PreferenceUtils;
import com.crowdmobile.kes.util.TwitterUtil;
import com.crowdmobile.kes.widget.NavigationBar;
import com.kes.AccountManager;
import com.kes.Session;
import com.kes.model.User;

import net.hockeyapp.android.CrashManager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AccountActivity extends Activity {

	private View btFacebook,btTwitter,btSkip;
	private ProgressDialog progressDialog;
	private AlertDialog alertDialog;
    private Session mSession;

    private TwitterUtil.LoginManager twitterLogin;
    private FacebookLogin facebookLogin;

    public static void logout(Context context)
    {
        Session.getInstance(context).getAccountManager().logout();
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


	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
        if (KesApplication.enableHockey) {
            CrashManager.register(this, KesApplication.HOCKEYAPP_ID);
        }
        getHash();
		this.setContentView(R.layout.activity_login);
        mSession = Session.getInstance(this);
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
	}


	private boolean checkRegistered()
	{
        if (mSession.getAccountManager().getUser().isRegistered())
		{
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
        if (checkRegistered())
            return;
        mSession.getAccountManager().registerListener(accountListener);
	}
	
	@Override
	public void onStop()
	{
		super.onStop();
        mSession.getAccountManager().unRegisterListener(accountListener);
	}
	
	OnClickListener onClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
            progressDialog.show();

			if (v == btFacebook)
                facebookLogin.execute();
            else if (v == btTwitter) {
                {
                    //startActivity(
                    //        new Intent(Intent.ACTION_VIEW, Uri.parse("oauth://twitterlogin")));

                    twitterLogin.login(AccountActivity.this);
                }
            } else if (v == btSkip)
            {
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
            mSession.getAccountManager().loginFacebook(userInfo.token,null);
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
            mSession.getAccountManager().loginTwitter(token,secret,null);
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
            checkRegistered();
        }

        @Override
        public void onLoggingIn() {

        }

        @Override
        public void onLoginFail(Exception e) {
            progressDialog.dismiss();
            alertDialog.show();
        }
    };

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (twitterLogin.onActivityResult(requestCode,resultCode,data))
            return;

		if (facebookLogin.onActivityResult(this,requestCode,resultCode,data))
            return;
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


    /*
    AccountManager.OnChangeListener onChangeListener = new AccountManager.OnChangeListener() {

        @Override
        public void onChange(com.kes.model.User user) {
            checkRegistered();
        }

        @Override
        public void onConnectionStatus(Connection connection, AccountManager.ConnectionStatus status, Exception exception) {
            if (status == AccountManager.ConnectionStatus.OPEN) {
                if (connection == Connection.FB)
                    progressDialog.setMessage(getString(R.string.fb_login));
                else if (connection == Connection.KES)
                    progressDialog.setMessage(getString(R.string.connecting));
                progressDialog.show();
            } else
            {
                progressDialog.hide();
                if (status == AccountManager.ConnectionStatus.ERROR) {
                    if (connection == Connection.FB)
                        showError(R.string.error_fb_session);
                    if (connection == Connection.KES)
                        showError(R.string.error_kes_login);
                }
            }
        }
    };
    */

    /*
	BroadcastReceiver receiver = new BroadcastReceiver()
	{

		@Override
		public void onReceive(Context context, Intent intent) {
			if (RegisterUser.ACTION_LOGINRESULT.equals(intent.getAction()))
			{
				progressDialog.hide();
				checkRegistered();
			}
		}

	};
    */

    /*
	FacebookRegHelper.FacebookCallback fbCallback = new FacebookRegHelper.FacebookCallback() { 
	
		@Override
		public Activity getActivity() {
			return AccountActivity.this;
		}
	
		@Override
		public void onSessionActive() {
			
		}
	
		@Override
		public void onSessionOpen() {
			progressDialog.setMessage(getString(R.string.fb_login));
			progressDialog.show();
		}
	
		@Override
		public void onSessionOpenFail() {
			showError(R.string.error_fb_session);
		}
	
		@Override
		public void onSessionClose() {
		}

		@Override
		public void onLoginResult(boolean success) {
			if (!success)
			{
				showError(R.string.error_fb_login);
				return;
			}
		}
		
		@Override
		public void onUserInfo(FacebookRegHelper.UserInfo userInfo) {
			if (userInfo == null)
			{
				showError(R.string.error_fb_nouserinfo);
				return;
			}
				
			String ua_token = null;
			progressDialog.setMessage(getString(R.string.connecting));
			RegisterUser.loginFacebook(AccountActivity.this, 
					userInfo.token, 
					ua_token, 
					PreferenceUtils.getAuthToken(AccountActivity.this));
		}
	
	};
	*/
}
