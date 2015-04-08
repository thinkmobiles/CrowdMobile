package com.crowdmobile.kes;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.crowdmobile.kes.util.PreferenceUtils;
import com.crowdmobile.kes.widget.NavigationBar;
import com.kes.AccountManager;
import com.kes.Session;
import com.kes.model.User;
import com.kes.net.DataFetcher;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends ActionBarActivity implements NavigationBar.NavigationCallback {

    private Handler mHandler;
    private Toolbar toolbar;
    private Session mSession;
    private NavigationBar navigationBar;
    private MenuItem mCredit;
    ArrayAdapter<String> networkAdapter;
    ArrayList<String> networkComm = new ArrayList<String>();
    LogCatThread logCatThread;
    ListView lvLogcat;
    boolean networkVisible = false;
    ViewPager viewPager;

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
        mHandler = new Handler();

        mSession = Session.getInstance(this);
		if (!mSession.getAccountManager().getUser().isRegistered() && !PreferenceUtils.getSkipLogin(this))
		{
			AccountActivity.open(this);
			return;
		}
        mSession.getAccountManager().registerListener(accountListener);
        setContentView(R.layout.activity_main);
        viewPager = (ViewPager)findViewById(R.id.viewPager);
        lvLogcat = (ListView)findViewById(R.id.lvLogcat);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        navigationBar = new NavigationBar(this,findViewById(R.id.navigationBar),viewPager);

        navigationBar.navigateTo(NavigationBar.Attached.values()[PreferenceUtils.getActiveFragment(this,NavigationBar.Attached.Feed.ordinal())]);

        networkAdapter = new ArrayAdapter<String>(this,
                R.layout.item_logcat, android.R.id.text1, networkComm);
        lvLogcat.setAdapter(networkAdapter);


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
        logCatThread = new LogCatThread();
        logCatThread.start();
        if (KesApplication.enableHockey)
            CrashManager.register(this, KesApplication.HOCKEYAPP_ID);
    }

    @Override
    protected void onPause() {
        super.onPause();
        logCatThread.interrupt();
        logCatThread = null;
        if (navigationBar != null)
            navigationBar.saveState();
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
        if (id == R.id.action_crediticon) {
            networkVisible = !networkVisible;
            if (networkVisible)
                lvLogcat.setVisibility(View.VISIBLE);
            else
                lvLogcat.setVisibility(View.INVISIBLE);
            return true;
		} else if (id == R.id.action_logout) {
            AccountActivity.logout(MainActivity.this);
            navigationBar = null;
			return true;
		} else if (id == R.id.action_support) {
            openURL("http://www.askbongo.com");
            return true;
        } else if (id == R.id.action_about) {
            openURL("http://www.askbongo.com");
            return true;
        } else if (id == R.id.action_privacy) {
            openURL("http://www.askbongo.com");
            return true;
        }
        return super.onOptionsItemSelected(item);
	}

    private void openURL(String url)
    {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No application can handle this request."
                    + " Please install a webbrowser", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public NavigationBar getNavigationBar() {
        return navigationBar;
    }

    class UIUpdate implements Runnable {
        String data[];
        public UIUpdate(String[] data) {
            super();
            this.data = data;
        }

        @Override
        public void run() {
            for (int i = 0; i < data.length; i++)
                networkComm.add(0,data[i]);
            networkAdapter.notifyDataSetChanged();
        }
    };

    class LogCatThread extends Thread
    {
        @Override
        public void run() {
            BufferedReader bufferedReader = null;
            Process process = null;
            try {
                process = Runtime.getRuntime().exec("logcat");
                bufferedReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                String line = "";
                while (!interrupted())
                {
                    Thread.sleep(1000);
                    String[] log = DataFetcher.getNetworkLog();
                    if (log == null)
                        continue;
                    mHandler.post(new UIUpdate(log));
                }
            } catch (IOException | InterruptedException e) {
            }
            finally {
                try {
                    bufferedReader.close();
                } catch (IOException ignored) {}
                if (process != null)
                    process.destroy();
            }
        }
    }



}
