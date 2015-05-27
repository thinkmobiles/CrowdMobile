package com.kes;

import android.content.Context;
import android.os.Handler;

import com.kes.model.User;
import com.kes.net.DataFetcher;

import java.util.ArrayList;

/**
 * Created by gadza on 2015.03.02..
 */
public class Session {

    private Context mContext;
    private Handler mHandler;
    private ArrayList<Transaction> transactions;
    private User user;
    private static Session sInstance;
    private AccountManager accountManager;
    private BillingManager billingManager;
    private FeedManager feedManager;
    private KesDB kesDB;

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
        //if (exception.error.code == DataFetcher.KESNetworkException.CODE_Invalid_authentication_code)
        //    accountManager.logout();
    }

    public static Session getInstance(Context context) {
        if (sInstance == null)
            sInstance = new Session(context);
        return sInstance;
    }

    private Session(Context context) {
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

    public AccountManager getAccountManager()
    {
        if (accountManager == null)
            accountManager = new AccountManager(this);
        return accountManager;
    }

    public BillingManager getBillingManager()
    {
        if (billingManager == null)
            billingManager = new BillingManager(this);
        return billingManager;
    }

    public FeedManager getFeedManager()
    {
        if (feedManager == null)
            feedManager = new FeedManager(this);
        return feedManager;
    }

    public Transaction beginTransaction()
    {
//        Transaction retval = new Transaction(this);
//        return retval;\
        return null;
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

}
