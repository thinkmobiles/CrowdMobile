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

import com.crowdmobile.kes.util.NotificationUtil;
import com.crowdmobile.kes.util.PreferenceUtils;
import com.crowdmobile.kes.widget.NavigationBar;
import com.kes.AccountManager;
import com.kes.BaseNotificationCreator;
import com.kes.FeedManager;
import com.kes.PushHandler;
import com.kes.Session;
import com.kes.model.PhotoComment;
import com.kes.model.User;
import com.kes.net.DataFetcher;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends ActionBarActivity implements NavigationBar.NavigationCallback {

    public static String TAG = ActionBarActivity.class.getSimpleName();
    public static String TAG_MYPOSTS = "myposts";
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
    ReaderViewPagerTransformer pagerTransformer;

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
            navigationBar.setUnreadCount(user.unread_count);
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
//        Configuration configuration = getResources().getConfiguration();
//        Log.d("HEIGHT",Float.toString(configuration.screenHeightDp));
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        }
		super.onCreate(savedInstanceState);
        Session.getInstance(this).getBillingManager().init(getString(R.string.signature_public),getResources().getStringArray(R.array.credits_list));

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
            finish();
			return;
		}
        mSession.getAccountManager().registerListener(accountListener);
        setContentView(R.layout.activity_main);
        viewPager = (ViewPager)findViewById(R.id.viewPager);
//        viewPager.setPageTransformer(false,new RotationPageTransformer(90,0.5f));
        pagerTransformer = new ReaderViewPagerTransformer(TransformType.ZOOM);
        viewPager.setPageTransformer(false,pagerTransformer);
        lvLogcat = (ListView)findViewById(R.id.lvLogcat);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        navigationBar = new NavigationBar(this,findViewById(R.id.navigationBar),viewPager);

        networkAdapter = new ArrayAdapter<String>(this,
                R.layout.item_logcat, android.R.id.text1, networkComm);
        lvLogcat.setAdapter(networkAdapter);

        /*
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		setContentView(R.layout.activity_main);
		*/
//        mHandler.postDelayed(refreshTread, 10000);
	}

    private void navigate(Intent intent)
    {
        NavigationBar.Attached a[] = NavigationBar.Attached.values();
        int saved = PreferenceUtils.getActiveFragment(this, NavigationBar.Attached.Feed.ordinal());
        if (saved < 0 || saved > a.length -1)
            saved = 0;
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                if (extras.getBoolean(TAG_MYPOSTS, false))
                    saved = NavigationBar.Attached.MyFeed.ordinal();
                    PreferenceUtils.setActiveFragment(this,saved);
            }
        }
        navigationBar.navigateTo(a[saved],false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
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
    protected void onStart() {
        super.onStart();
        Session.getInstance(this).getFeedManager().registerOnChangeListener(onFeedChange);
        logCatThread = new LogCatThread();
        logCatThread.start();
        navigate(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        navigate(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Session.getInstance(this).getFeedManager().unRegisterOnChangeListener(onFeedChange);
        logCatThread.interrupt();
        logCatThread = null;

    }

    public static class NotificationCreator extends BaseNotificationCreator {

        @Override
        public void createNotification(Context context, Bundle extras) {
            NotificationUtil.createNotification(context);
        }
    };

    Runnable refreshTread = new Runnable() {
        @Override
        public void run() {
            Bundle bundle = new Bundle();
            PushHandler.handlePush(getApplicationContext(),NotificationCreator.class, bundle);
            mHandler.postDelayed(this,10000);
            Toast.makeText(MainActivity.this,"Refreshing tread",Toast.LENGTH_SHORT).show();
        }
    };

    FeedManager.OnChangeListener onFeedChange = new FeedManager.OnChangeListener() {
        @Override
        public boolean onUnread(FeedManager.FeedWrapper wrapper) {
            int count = 0;
            if (wrapper.comments != null)
                count = wrapper.comments.length;
            navigationBar.setUnreadCount(count);
            return true;
        }

        @Override
        public void onPageLoaded(FeedManager.FeedWrapper wrapper) {

        }

        @Override
        public void onMarkAsReadResult(PhotoComment photoComment, Exception error) {

        }

        @Override
        public void onLikeResult(int questionID, int commentID, Exception error) {

        }

        @Override
        public void onReportResult(int questionID, Exception error) {

        }

        @Override
        public void onDeleteResult(int questionID, int commentID, Exception error) {

        }

        @Override
        public void onMarkAsPrivateResult(PhotoComment photoComment, Exception error) {

        }

        @Override
        public void onPostResult(int questionID, Exception error) {

        }
    };

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

    public class RotationPageTransformer implements ViewPager.PageTransformer{
        private float minAlpha;
        private int degrees;
        private float distanceToCentreFactor;

        /**
         * Creates a RotationPageTransformer
         * @param degrees the inner angle between two edges in the "polygon" that the pages are on.
         * Note, this will only work with an obtuse angle
         */
        public RotationPageTransformer(int degrees){
            this(degrees, 0.7f);
        }

        /**
         * Creates a RotationPageTransformer
         * @param degrees the inner angle between two edges in the "polygon" that the pages are on.
         * Note, this will only work with an obtuse angle
         * @param minAlpha the least faded out that the side
         */
        public RotationPageTransformer(int degrees, float minAlpha){
            this.degrees = degrees;
            distanceToCentreFactor = (float) Math.tan(Math.toRadians(degrees / 2))/2;
            this.minAlpha = minAlpha;
        }

        public void transformPage(View view, float position){
            int pageWidth = view.getWidth();
            int pageHeight = view.getHeight();
            view.setPivotX((float) pageWidth / 2);
            view.setPivotY((float) (pageHeight + pageWidth * distanceToCentreFactor));

            if(position < -1){ //[-infinity,1)
                //off to the left by a lot
                view.setRotation(0);
                view.setAlpha(0);
            }else if(position <= 1){ //[-1,1]
                view.setTranslationX((-position) * pageWidth); //shift the view over
                view.setRotation(position * (180 - degrees)); //rotate it
                // Fade the page relative to its distance from the center
                view.setAlpha(Math.max(minAlpha, 1 - Math.abs(position)/3));
            }else{ //(1, +infinity]
                //off to the right by a lot
                view.setRotation(0);
                view.setAlpha(0);
            }
        }
    }

    public class PageTransformer implements ViewPager.PageTransformer {

        public void transformPage(View view, float position) {

            int pageWidth = view.getWidth();
            if (position > 1)
            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0);

            } else if (position <= 1) { // [-1,1]

                view.setAlpha(Math.max(0.5f,1 - Math.abs(position)/3));
                view.setTranslationX(-position * (pageWidth / 2)); //Half the normal speed

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0);
            }

        }
    }

    static enum TransformType {
        FLOW,
        DEPTH,
        ZOOM,
        SLIDE_OVER
    }

    class ReaderViewPagerTransformer implements ViewPager.PageTransformer {
        private final TransformType mTransformType;

        ReaderViewPagerTransformer(TransformType transformType) {
            mTransformType = transformType;
        }

        private static final float MIN_SCALE_DEPTH = 0.75f;
        private static final float MIN_SCALE_ZOOM = 0.85f;
        private static final float MIN_ALPHA_ZOOM = 0.5f;
        private static final float SCALE_FACTOR_SLIDE = 0.85f;
        private static final float MIN_ALPHA_SLIDE = 0.35f;

        public void transformPage(View page, float position) {
            final float alpha;
            float scale;
            final float translationX;

            switch (mTransformType) {
                case FLOW:
                    page.setRotationY(position * -30f);
                    return;

                case SLIDE_OVER:
                    if (position < 0 && position > -1) {
                        // this is the page to the left
                        scale = Math.abs(Math.abs(position) - 1) * (1.0f - SCALE_FACTOR_SLIDE) + SCALE_FACTOR_SLIDE;
                        alpha = Math.max(MIN_ALPHA_SLIDE, 1 - Math.abs(position));
                        int pageWidth = page.getWidth();
                        float translateValue = position * -pageWidth;
                        if (translateValue > -pageWidth) {
                            translationX = translateValue;
                        } else {
                            translationX = 0;
                        }
                    } else {
                        alpha = 1;
                        scale = 1;
                        translationX = 0;
                    }
                    break;

                case DEPTH:
                    if (position > 0 && position < 1) {
                        // moving to the right
                        alpha = (1 - position);
                        scale = MIN_SCALE_DEPTH + (1 - MIN_SCALE_DEPTH) * (1 - Math.abs(position));
                        translationX = (page.getWidth() * -position);
                    } else {
                        // use default for all other cases
                        alpha = 1;
                        scale = 1;
                        translationX = 0;
                    }
                    break;

                case ZOOM:
                    if (position >= -1 && position <= 1) {
                        scale = Math.max(MIN_SCALE_ZOOM, 1 - Math.abs(position));
                        alpha = MIN_ALPHA_ZOOM +
                                (scale - MIN_SCALE_ZOOM) / (1 - MIN_SCALE_ZOOM) * (1 - MIN_ALPHA_ZOOM);
                        float vMargin = page.getHeight() * (1 - scale) / 2;
                        float hMargin = page.getWidth() * (1 - scale) / 2;
                        if (position < 0) {
                            translationX = (hMargin - vMargin / 2);
                        } else {
                            translationX = (-hMargin + vMargin / 2);
                        }
                    } else {
                        alpha = 1;
                        scale = 1;
                        translationX = 0;
                    }
                    break;

                default:
                    return;
            }

            page.setAlpha(alpha);
            page.setTranslationX(translationX);
            page.setScaleX(scale);
            page.setScaleY(scale);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Session.getInstance(this).getBillingManager().handleActivityResult(requestCode,resultCode,data))
            return;
        super.onActivityResult(requestCode,resultCode,data);
    }

}
