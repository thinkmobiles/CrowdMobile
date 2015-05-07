package com.crowdmobile.kes.widget;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.crowdmobile.kes.R;
import com.crowdmobile.kes.fragment.ComposeFragment;
import com.crowdmobile.kes.fragment.CreditFragment;
import com.crowdmobile.kes.fragment.MyFeedFragment;
import com.crowdmobile.kes.fragment.NewsFeedFragment;
import com.crowdmobile.kes.fragment.AccessFragment;
import com.crowdmobile.kes.util.PreferenceUtils;
import com.kes.Session;

import java.util.ArrayList;

public class NavigationBar {

    public static String ACTION_CHANGE = NavigationBar.class.getSimpleName() + "change";
	public static interface NavigationCallback {
		public NavigationBar getNavigationBar();
	}

	public enum Attached {Empty, Feed, MyFeed, Compose, Checkout};
	private ImageView btFeed,btMyFeed,btCompose,btCheckout;
	private ActionBarActivity mActivity;
	private Attached attached = Attached.Empty;
	private ViewPager mViewPager;
    private NavbarAdapter adapter;
    private TextView tvUnreadCount;

	public NavigationBar(ActionBarActivity activity, View v,ViewPager viewPager)
	{
		mActivity = activity;
        mViewPager = viewPager;
        mViewPager.setOffscreenPageLimit(3);
        adapter = new NavbarAdapter(activity.getSupportFragmentManager());
        mViewPager.setAdapter(adapter);
        mViewPager.setOnPageChangeListener(pageChangeListener);
		btFeed = (ImageView)v.findViewById(R.id.btFeed);
		btMyFeed = (ImageView)v.findViewById(R.id.btMyFeed);
		btCompose = (ImageView)v.findViewById(R.id.btCompose);
		btCheckout = (ImageView)v.findViewById(R.id.btCheckout);

		btFeed.setOnClickListener(onClickListener);
		btMyFeed.setOnClickListener(onClickListener);
		btCompose.setOnClickListener(onClickListener);
		btCheckout.setOnClickListener(onClickListener);
        tvUnreadCount = (TextView)v.findViewById(R.id.tvUnreadCount);
        setUnreadCount(Session.getInstance(activity).getAccountManager().getUser().unread_count);
	}

    public void setUnreadCount(int count)
    {
        if (count < 1)
            tvUnreadCount.setVisibility(View.INVISIBLE);
        else {
            tvUnreadCount.setVisibility(View.VISIBLE);
            tvUnreadCount.setText(Integer.toString(count));
        }
    }

    private void selectPage(int position)
    {
        Attached newPage = Attached.values()[position + 1];
        updateIcons(newPage);
        LocalBroadcastManager.getInstance(mActivity).sendBroadcast(new Intent(ACTION_CHANGE));
        attached = newPage;

        if (attached != Attached.Compose) {
            final InputMethodManager imm = (InputMethodManager)mActivity.getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mViewPager.getWindowToken(), 0);
        }
    }

    ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
        Attached attached = Attached.Empty;

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            selectPage(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (attached == Attached.Compose) {
                final InputMethodManager imm = (InputMethodManager)mActivity.getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mViewPager.getWindowToken(), 0);
            }
            /*
            if (state == ViewPager.SCROLL_STATE_DRAGGING && attached == Attached.Compose)
            {
                InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            }
            */
        }
    };

    public Attached getAttached()
    {
        return attached;
    }

    public void saveState()
    {
        PreferenceUtils.setActiveFragment(mActivity, attached.ordinal());
    }


    private Fragment getFragment(Attached src)
    {
        if (src == Attached.Feed)
            return new NewsFeedFragment();


        if (!Session.getInstance(mActivity).getAccountManager().getUser().isRegistered())
            return new AccessFragment();

        if (src == Attached.Checkout)
            return new CreditFragment();

        if (src == Attached.MyFeed)
            return new MyFeedFragment();


        if (src == Attached.Compose)
            return new ComposeFragment();

        return null;
    }

	public void updateIcons(Attached dest)
	{
		if (attached == dest)
			return;
		attached = dest;
		Fragment f = null;
        View destIcon = null;

		if (attached == Attached.Feed) {
            mActivity.getSupportActionBar().setTitle(R.string.newsfeed);
            btFeed.setImageResource(R.drawable.ic_tabbar_newsfeed_pressed);
            destIcon = btFeed;
        }
        else
            btFeed.setImageResource(R.drawable.ic_tabbar_newsfeed);

        if (attached == Attached.MyFeed) {
            btMyFeed.setImageResource(R.drawable.ic_tabbar_myfeed_pressed);
            mActivity.getSupportActionBar().setTitle(R.string.myfeed);
            destIcon = btMyFeed;
        }
        else
            btMyFeed.setImageResource(R.drawable.ic_tabbar_myfeed);

		if (attached == Attached.Checkout) {
            btCheckout.setImageResource(R.drawable.ic_tabbar_shop_pressed);
            mActivity.getSupportActionBar().setTitle(R.string.fragment_credit_title);
            destIcon = btCheckout;
        }
        else
            btCheckout.setImageResource(R.drawable.ic_tabbar_shop);

        if (attached == Attached.Compose) {
            btCompose.setImageResource(R.drawable.ic_tabbar_post_pressed);
            mActivity.getSupportActionBar().setTitle(R.string.compose_title);
            destIcon = btCompose;
        }
        else
            btCompose.setImageResource(R.drawable.ic_tabbar_post);

        Animation a = AnimationUtils.loadAnimation(mActivity,R.anim.bounce);
        destIcon.startAnimation(a);
	}
	
	OnClickListener onClickListener = new OnClickListener()
	{

		@Override
		public void onClick(View v) {
			if (v == btFeed)
				navigateTo(Attached.Feed);
            else if (v == btMyFeed)
                navigateTo(Attached.MyFeed);
			else if (v == btCheckout)
				navigateTo(Attached.Checkout);
            else if (v == btCompose)
                navigateTo(Attached.Compose);
		}
		
	};

    public void navigateTo(Attached dest)
    {
        navigateTo(dest,true);
    }

    public void navigateTo(Attached dest,boolean smooth)
    {
        if (attached == dest)
            return;
        mViewPager.setCurrentItem(dest.ordinal() - 1,smooth);
        selectPage(dest.ordinal() - 1);
    }

    class NavbarAdapter extends FragmentStatePagerAdapter {

        ArrayList<Fragment> fragments = new ArrayList<Fragment>();
        public NavbarAdapter(FragmentManager fm) {
            super(fm);
            Attached[] a = Attached.values();
            for (int i = 1; i < a.length; i++)
                fragments.add(getFragment(a[i]));
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

    }

}
