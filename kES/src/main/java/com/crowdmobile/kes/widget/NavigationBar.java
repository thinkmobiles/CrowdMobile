package com.crowdmobile.kes.widget;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.crowdmobile.kes.R;
import com.crowdmobile.kes.fragment.CheckoutFragment;
import com.crowdmobile.kes.fragment.ComposeFragment;
import com.crowdmobile.kes.fragment.MyFeedFragment;
import com.crowdmobile.kes.fragment.NewsFeedFragment;
import com.crowdmobile.kes.fragment.NotRegisteredFragment;
import com.crowdmobile.kes.util.PreferenceUtils;
import com.kes.Session;

public class NavigationBar {

	public static interface NavigationCallback {
		public NavigationBar getNavigationBar();
	}

	public enum Attached {Empty, Feed, MyFeed, Compose, Checkout};
	private ImageView btFeed,btMyFeed,btCompose,btCheckout;
	private ActionBarActivity mActivity;
	private Attached attached = Attached.Empty;
	private ViewPager mViewPager;

	public NavigationBar(ActionBarActivity activity, View v,ViewPager viewPager)
	{
		mActivity = activity;
        mViewPager = viewPager;
        mViewPager.setAdapter(new NavbarAdapter(activity.getSupportFragmentManager()));
        mViewPager.setOnPageChangeListener(pageChangeListener);
		btFeed = (ImageView)v.findViewById(R.id.btFeed);
		btMyFeed = (ImageView)v.findViewById(R.id.btMyFeed);
		btCompose = (ImageView)v.findViewById(R.id.btCompose);
		btCheckout = (ImageView)v.findViewById(R.id.btCheckout);

		btFeed.setOnClickListener(onClickListener);
		btMyFeed.setOnClickListener(onClickListener);
		btCompose.setOnClickListener(onClickListener);
		btCheckout.setOnClickListener(onClickListener);
	}

    ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            updateIcons(Attached.values()[position + 1]);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

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
            return new NotRegisteredFragment();

        if (src == Attached.MyFeed)
            return new MyFeedFragment();

        if (src == Attached.Checkout)
            return new CheckoutFragment();

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

		if (attached == Attached.Feed) {
            mActivity.getSupportActionBar().setTitle(R.string.newsfeed);
            btFeed.setImageResource(R.drawable.ic_tabbar_newsfeed_pressed);
        }
        else
            btFeed.setImageResource(R.drawable.ic_tabbar_newsfeed);

        if (attached == Attached.MyFeed) {
            btMyFeed.setImageResource(R.drawable.ic_tabbar_myfeed_pressed);
            mActivity.getSupportActionBar().setTitle(R.string.myfeed);
        }
        else
            btMyFeed.setImageResource(R.drawable.ic_tabbar_myfeed);

		if (attached == Attached.Checkout) {
            btCheckout.setImageResource(R.drawable.ic_tabbar_shop_pressed);
            mActivity.getSupportActionBar().setTitle(R.string.fragment_credit_title);
        }
        else
            btCheckout.setImageResource(R.drawable.ic_tabbar_shop);

        if (attached == Attached.Compose) {
            btCompose.setImageResource(R.drawable.ic_tabbar_post_pressed);
            mActivity.getSupportActionBar().setTitle(R.string.compose_title);
        }
        else
            btCompose.setImageResource(R.drawable.ic_tabbar_post);
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
        if (attached == dest)
            return;
        mViewPager.setCurrentItem(dest.ordinal() - 1);
    }

    class NavbarAdapter extends FragmentPagerAdapter {


        public NavbarAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            return getFragment(Attached.values()[position + 1]);
        }

        @Override
        public int getCount() {
            return Attached.values().length - 1;
        }

    }

}
