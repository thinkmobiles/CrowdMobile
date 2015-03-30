package com.crowdmobile.kes.widget;

import android.app.Activity;
import android.app.Fragment;
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
	private Activity mActivity;
	private Attached attached = Attached.Empty;
	
	public NavigationBar(Activity activity, View v)
	{
		mActivity = activity;
		btFeed = (ImageView)v.findViewById(R.id.btFeed);
		btMyFeed = (ImageView)v.findViewById(R.id.btMyFeed);
		btCompose = (ImageView)v.findViewById(R.id.btCompose);
		btCheckout = (ImageView)v.findViewById(R.id.btCheckout);

		btFeed.setOnClickListener(onClickListener);
		btMyFeed.setOnClickListener(onClickListener);
		btCompose.setOnClickListener(onClickListener);
		btCheckout.setOnClickListener(onClickListener);
	}

    public Attached getAttached()
    {
        return attached;
    }

    public void saveState()
    {
        PreferenceUtils.setActiveFragment(mActivity, attached.ordinal());
    }

	public void navigateTo(Attached dest)
	{
		if (attached == dest)
			return;
		attached = dest;
		Fragment f = null;

		if (attached == Attached.Feed) {
            btFeed.setImageResource(R.drawable.ic_tabbar_newsfeed_pressed);
            f = new NewsFeedFragment();
        } else
            btFeed.setImageResource(R.drawable.ic_tabbar_newsfeed);

        if (attached == Attached.MyFeed) {
            btMyFeed.setImageResource(R.drawable.ic_tabbar_myfeed_pressed);
            if (Session.getInstance(mActivity).getAccountManager().getUser().isRegistered())
                f = new MyFeedFragment();
            else
                f = new NotRegisteredFragment();
        } else
            btMyFeed.setImageResource(R.drawable.ic_tabbar_myfeed);

		if (attached == Attached.Checkout) {
            btCheckout.setImageResource(R.drawable.ic_tabbar_shop_pressed);
            if (Session.getInstance(mActivity).getAccountManager().getUser().isRegistered())
                f = new CheckoutFragment();
            else
                f = new NotRegisteredFragment();
        } else
            btCheckout.setImageResource(R.drawable.ic_tabbar_shop);

        if (attached == Attached.Compose) {
            btCompose.setImageResource(R.drawable.ic_tabbar_post_pressed);
            if (Session.getInstance(mActivity).getAccountManager().getUser().isRegistered())
                f = new ComposeFragment();
            else
                f = new NotRegisteredFragment();
        } else
            btCompose.setImageResource(R.drawable.ic_tabbar_post);


		mActivity.getFragmentManager().beginTransaction()
//    	.setCustomAnimations(R.anim.oa_fade_in, R.anim.oa_fade_out)
    	.replace(R.id.fragmentHolder, f)
    	.commit();
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

}
