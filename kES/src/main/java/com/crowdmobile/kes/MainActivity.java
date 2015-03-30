package com.crowdmobile.kes;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.Window;
import android.view.WindowManager;

import com.crowdmobile.kes.util.PreferenceUtils;
import com.crowdmobile.kes.widget.NavigationBar;
import com.kes.AccountManager;
import com.kes.Session;
import com.kes.model.User;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

public class MainActivity extends ActionBarActivity implements NavigationBar.NavigationCallback {


    private Toolbar toolbar;
    private boolean profileVisible = false;
    private Session mSession;
    private NavigationBar navigationBar;
    private MenuItem mCredit;

	public static void open(Context context)
	{
		Intent intent = new Intent(context,MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		context.startActivity(intent);
        if (context instanceof Activity)
            ((Activity)context).overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
	}

    AccountManager.AccountListener accountListener = new AccountManager.AccountListener() {
        @Override
        public void onUserLoadError(Exception e) {

        }

        @Override
        public void onUserChanged(User user) {
            if (mCredit != null)
                mCredit.setTitle(Integer.toString(mSession.getAccountManager().getUser().balance));
        }

        @Override
        public void onLoggingIn() {

        }

        @Override
        public void onLoginFail(Exception e) {

        }
    };


	@Override
	protected void onCreate(Bundle savedInstanceState) {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        }
		super.onCreate(savedInstanceState);
        if (KesApplication.enableHockey) {
            CrashManager.register(this, KesApplication.HOCKEYAPP_ID);
            UpdateManager.register(this, KesApplication.HOCKEYAPP_ID);
        }

       // if (true)
       //     throw new IllegalStateException("HockeyAPP Crash test onCreate2()");

        mSession = Session.getInstance(this);
		if (!mSession.getAccountManager().getUser().isRegistered() && !PreferenceUtils.getSkipLogin(this))
		{
			AccountActivity.open(this);
			return;
		}
        mSession.getAccountManager().registerListener(accountListener);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        navigationBar = new NavigationBar(this,findViewById(R.id.navigationBar));

        navigationBar.navigateTo(NavigationBar.Attached.values()[PreferenceUtils.getActiveFragment(this)]);
        /*
        //Todo: remember last fragment on exit
        getFragmentManager().
                beginTransaction().
                replace(R.id.fragmentHolder, new NewsFeedFragment()).
                commit();
        */
        /*
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		setContentView(R.layout.activity_main);
		*/
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UpdateManager.unregister();
        mSession.getAccountManager().unRegisterListener(accountListener);
    }


    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
        mCredit = menu.findItem(R.id.action_credit);
        mCredit.setTitle(Integer.toString(mSession.getAccountManager().getUser().balance));
		return true;
	}

    @Override
    protected void onResume() {
        super.onResume();
        if (KesApplication.enableHockey)
            CrashManager.register(this, KesApplication.HOCKEYAPP_ID);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (navigationBar != null)
            navigationBar.saveState();
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
            profileVisible = true;
            if (Build.VERSION.SDK_INT >= 21) {
                View myView = findViewById(R.id.holderProfile);

// get the center for the clipping circle
                int cx = myView.getRight();
                int cy = myView.getTop();

// get the final radius for the clipping circle
                int finalRadius = Math.max(myView.getWidth(), myView.getHeight());

// create the animator for this view (the start radius is zero)
                Animator anim =
                        ViewAnimationUtils.createCircularReveal(myView, cx, cy, 0, finalRadius);

// make the view visible and start the animation
                myView.setVisibility(View.VISIBLE);
                anim.start();
            }
		} else if (id == R.id.action_logout) {
            AccountActivity.logout(MainActivity.this);
            navigationBar = null;
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

    @Override
    public void onBackPressed() {
        if (profileVisible)
        {
            if (Build.VERSION.SDK_INT >= 21) {
                final View myView = findViewById(R.id.holderProfile);

// get the center for the clipping circle
                int cx = myView.getRight();
                int cy = myView.getTop();

// get the initial radius for the clipping circle
                int initialRadius = myView.getWidth();

// create the animation (the final radius is zero)
                Animator anim =
                        ViewAnimationUtils.createCircularReveal(myView, cx, cy, initialRadius, 0);

// make the view invisible when the animation is done
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        myView.setVisibility(View.INVISIBLE);
                    }
                });

// start the animation
                anim.start();
                profileVisible = false;
                return;
            }
        }

        super.onBackPressed();
    }

    @Override
    public NavigationBar getNavigationBar() {
        return navigationBar;
    }
}
