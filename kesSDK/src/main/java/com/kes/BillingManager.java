package com.kes;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.kes.billing.BillingService;
import com.kes.billing.CreditItem;

import java.util.ArrayList;

/**
 * Created by gadza on 2015.04.16..
 */
public class BillingManager {

    public enum BillingStatus {Idle, Init, InitFailed, InventoryRequest, InventoryFail, PaymentProcess,PaymentFail, Disconnected};

    public interface BillingListener {
        public void onStatus(BillingStatus status);
        public void onCreditList(ArrayList<CreditItem> list);
    }

    private String mProductList[];
    private String mSignature;
    private BillingService billingService;
    private boolean isBond = false;
    private Session mSession;
    private BillingListener mBillingListener;
    private BillingStatus mBillingStatus = BillingStatus.Idle;

    public ServiceConnection billingConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            billingService = ((BillingService.BillingServiceBinder)service).getService();
            mBillingListener.onStatus(billingService.getStatus());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBond = false;
            billingService = null;
        }
    };

    protected BillingManager(Session session)
    {
        mSession = session;
    }

    public void init(String signature_public, String productList[])
    {
        mSignature = signature_public;
        mProductList = productList;
    }

    public void onStart(Context context,BillingListener listener)
    {
        if (!mSession.getAccountManager().getUser().isRegistered())
            throw new IllegalStateException("User is not registered");
        LocalBroadcastManager.getInstance(mSession.getContext()).registerReceiver(receiver, new IntentFilter(BillingService.ACTION_CREDIT_STATUS));
        LocalBroadcastManager.getInstance(mSession.getContext()).registerReceiver(receiver, new IntentFilter(BillingService.ACTION_CREDITLIST));
        if (mProductList == null)
            throw new IllegalStateException("Product list is not initialised.Please call init() first.");
        mBillingListener = listener;
        listener.onStatus(BillingStatus.Init);
        Intent intent = new Intent(context, BillingService.class);
        intent.putExtra(BillingService.TAG_SIGNATURE, mSignature);
        intent.putExtra(BillingService.TAG_PRODUCTLIST, mProductList);
        isBond = context.bindService(intent, billingConnection, Context.BIND_AUTO_CREATE);
    }

    public void buyCredits(Activity activity, String productId) {
        billingService.buyCredits(activity,productId);
    }

    public void processPendingCredits()
    {
        billingService.processPendingCredits();
    }

    public ArrayList<String> getPendingOrders()
    {
        if (billingService == null)
            return null;
        return billingService.getPendingOrders();
    }

    public void onStop(Context context)
    {
        LocalBroadcastManager.getInstance(mSession.getContext()).unregisterReceiver(receiver);
        mBillingListener = null;
        if (isBond)
        {
            isBond = false;
            billingService = null;
            context.unbindService(billingConnection);
        }
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data){
        if (billingService != null)
            return billingService.handleActivityResult(requestCode, resultCode, data);
        else
            return false;
    }

    //Used receiver so related functions of SDK are not public
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (billingService == null)
                return;
            String action = intent.getAction();
            if (BillingService.ACTION_CREDITLIST.equals(action))
                mBillingListener.onCreditList(billingService.getCreditItems());
            if (BillingService.ACTION_CREDIT_STATUS.equals(action))
                mBillingListener.onStatus(BillingStatus.values()[intent.getIntExtra(BillingService.TAG_STATUS, 0)]);
        }
    };
}
