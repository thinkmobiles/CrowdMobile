package com.kes;

import android.content.Context;
import android.os.Handler;

import com.kes.model.User;
import com.kes.net.DataFetcher;
import com.kes.net.ServerNavigator;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by gadza on 2015.03.02..
 */
public class KES {

    private Context mContext;
    private Handler mHandler;
    private ArrayList<Transaction> transactions;
    private User user;
    private static KES sInstance;
    private AccountManager accountManager;
    private BillingManager billingManager;
    private FeedManager feedManager;
    private KesDB kesDB;
    private KesConfigOptions mConfigOptions;


    protected interface SessionCallback
    {
        public Context getContext();
    }

    protected Handler getHandler()
    {
        return mHandler;
    }

    protected void networkError(DataFetcher.KESNetworkException exception)
    {
        if (exception.error.code == DataFetcher.KESNetworkException.CODE_Invalid_authentication_code)
            accountManager.logout();
    }

    public static void initialize(Context context,KesConfigOptions configOptions) {
        if (sInstance == null) {
            verifyConfigOptions(configOptions);
            sInstance = new KES(context, configOptions);
        }
        else
            throw new IllegalStateException("KES has been already initialized.This should be called @ Application.onCreate()");
    }

    protected static KES getInstance()
    {
        if (sInstance == null)
            throw new IllegalStateException("Kes has not been initialized.Call initialize @ Application.onCreate()");
        return sInstance;
    }

    public static KESShared shared()
    {
        return getInstance().kesShared;
    }

    private static void verifyConfigOptions(KesConfigOptions configOptions)
    {
        if (configOptions == null)
            throw new IllegalStateException("KesConfigOptions can''t be null");
        if (!Utils.strHasValue(configOptions.serverURL))
            throw new IllegalStateException("Server URL of KESConfigOptions can''t be bull");
    }

    private static void updateConfigOptions(KesConfigOptions options)
    {
        String tmp = options.serverURL;
        if (tmp.charAt(options.serverURL.length() - 1) != '/')
            tmp = options.serverURL + "/";
        ServerNavigator.BASE_URL = tmp;
    }

    private KES(Context context,KesConfigOptions configOptions) {
        mConfigOptions = configOptions;
        updateConfigOptions(mConfigOptions);
        mContext = context.getApplicationContext();
        mHandler = new Handler();
    }

    protected Context getContext()
    {
        return mContext;
    }

    protected KesDB getDB()
    {
        if (kesDB == null)
            kesDB = new KesDB(mContext);
        return kesDB;
    }

    public void shutDown()
    {
        for (int i = transactions.size(); i > 0; i--)
            transactions.get(i - 1).cancel();
        transactions.clear();
        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;
    }

    //Do some cleanup here
    protected void serviceShutdown()
    {
        if (kesDB != null)
        {
            kesDB.close();
            kesDB = null;
        }
    }

    protected AccountManager getAccountManager() {
        if (accountManager == null)
            accountManager = new AccountManager(KES.this);
        return accountManager;
    }

    protected BillingManager getBillingManager() {
        if (billingManager == null)
            billingManager = new BillingManager(KES.this);
        return billingManager;
    }

    protected FeedManager getFeedManager() {
        if (feedManager == null)
            feedManager = new FeedManager(KES.this);
        return feedManager;
    }

    protected void setLocale(Locale locale)
    {
        DataFetcher.locale = locale.toString();
        if (feedManager != null)
            feedManager.localeChanged();
    }


    private KESShared kesShared = new KESShared() {
        @Override
        public AccountManager getAccountManager() {
            return KES.this.getAccountManager();
        }

        @Override
        public BillingManager getBillingManager() {
            return KES.this.getBillingManager();
        }

        @Override
        public FeedManager getFeedManager() {
            return KES.this.getFeedManager();
        }
    };

}
