package com.kes;

import android.content.Context;

import com.kes.model.User;
import com.kes.net.ModelFactory;

import java.util.Iterator;
import java.util.WeakHashMap;

/**
 * Created by gadza on 2015.03.02..
 */
public class AccountManager {

    private static final String OPERATION_IN_PROGRESS = "network operation is in progress";
    protected static final String TOKEN_NULL = "token can't be null";

    public interface AccountListener {
        public void onUserChanged(User user);
        public void onLoggingIn();
        public void onLoginFail(Exception e);
    }

    static protected class UserWrapper extends ResultWrapper {
        User user;
    }

    private boolean login_progress = false;

    private KES mKES;
    private User user;
    private WeakHashMap<AccountListener, Void> userCallbacks = new WeakHashMap<AccountListener, Void>();
    private String ua_token;

    public void unRegisterListener(Object object)
    {
        userCallbacks.remove(object);
    }

    public void registerListener(AccountListener listener)
    {
        userCallbacks.put(listener, null);
        if (login_progress)
            listener.onLoggingIn();
    }

    AccountListener tmpUserListener = null;    //don't change to local, GC bug

    private void postUserChanged()
    {
        Iterator<AccountListener> iterator = userCallbacks.keySet().iterator();
        while (iterator.hasNext()) {
            tmpUserListener = iterator.next();
            tmpUserListener.onUserChanged(user);
        }
        tmpUserListener = null;
    }

    protected void postLoginResult(UserWrapper wrapper)
    {
        login_progress = false;
        user = wrapper.user;
        if (user != null)
        {
            updateUser(wrapper);
            return;
        }
        Iterator<AccountListener> iterator = userCallbacks.keySet().iterator();
        while (iterator.hasNext()) {
            tmpUserListener = iterator.next();
            tmpUserListener.onLoginFail(wrapper.exception);
        }
        tmpUserListener = null;
    }

    public AccountManager(KES session)
    {
        mKES = session;
    }

    protected void updateUser(UserWrapper userWrapper)
    {
        if (userWrapper.exception == null)
        {
            getCachedUser(mKES.getContext());
            user.id = userWrapper.user.id;
            user.first_name = userWrapper.user.first_name;
            user.last_name = userWrapper.user.last_name;
            user.show_last_name = userWrapper.user.show_last_name;
            user.show_profile_photo = userWrapper.user.show_profile_photo;
            user.profile_photo_url = userWrapper.user.profile_photo_url;
            user.balance = userWrapper.user.balance;
            user.unread_count = userWrapper.user.unread_count;
            user.login_type = userWrapper.user.login_type;
            user.auth_token = userWrapper.user.auth_token;
            user.upToDate = true;
            PreferenceUtil.setUser(mKES.getContext(), user);
            postUserChanged();
        }
    }

    protected void updateBalance(int newBalance)
    {
        getCachedUser(mKES.getContext());
        if (user.auth_token == null)
            return;
        user.balance = newBalance;
        PreferenceUtil.setBalance(mKES.getContext(), user.balance);
        postUserChanged();
    }

    protected void updateUnread(int count)
    {
        getCachedUser(mKES.getContext());
        if (user.auth_token == null)
            return;
        user.unread_count = count;
        PreferenceUtil.setUnreadCount(mKES.getContext(), user.balance);
        postUserChanged();
    }

    public void setUAChannelID(String channelID)
    {
        ua_token = channelID;
        if (getCachedUser(mKES.getContext()).isRegistered())
            TaskPostToken.updatePushToken(mKES.getContext(), user.auth_token,ua_token);
    }

    public User getUser()
    {
        getCachedUser(mKES.getContext());
        if (user.isRegistered() && !user.upToDate)
            TaskLoadUser.loadUser(mKES.getContext(), user.auth_token);
        return user;
    }

    protected User getCachedUser(Context context)
    {
        if (user == null)
            user = PreferenceUtil.getUser(context);
        return user;
    }

    protected String getToken()
    {
        getUser();
        if (user.auth_token == null)
            throw new IllegalStateException(TOKEN_NULL);
        return user.auth_token;
    }

    public void loginFacebook(String facebook_token, String facebook_uid)
    {
        if (login_progress)
            throw new IllegalStateException(OPERATION_IN_PROGRESS);
        login_progress = true;
        TaskLogin.login(mKES.getContext(), ModelFactory.LoginType.Facebook,facebook_token,null, facebook_uid);
    }

    public void loginTwitter(String twitter_token, String twitter_secret, String twitter_uid)
    {
        if (login_progress)
            throw new IllegalStateException(OPERATION_IN_PROGRESS);
        login_progress = true;
        TaskLogin.login(mKES.getContext(), ModelFactory.LoginType.Twitter,twitter_token,twitter_secret,twitter_uid);
    }

    public void logout()
    {
        if (login_progress)
            throw new IllegalStateException(OPERATION_IN_PROGRESS);
        if (user == null || !user.isRegistered())
            return;
        mKES.getFeedManager().feed(FeedManager.FeedType.My).clear();
        mKES.getFeedManager().clearPendingDB();
        PreferenceUtil.clearUser(mKES.getContext());
        user = null;
        postUserChanged();
    }

}
