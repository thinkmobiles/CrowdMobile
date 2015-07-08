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

    public enum BillingStatus {Idle, Init, InitFailed, InventoryRequest, InventoryFail, PaymentGoogle, PaymentProcess,PaymentFail, Disconnected};

    public interface BillingListener {
        public void onPurchased(int quantity);
        public void onStatus(BillingStatus status);
        public void onCreditList(ArrayList<CreditItem> list);
    }

    class ContextWrapper {
        Activity activity;
        BillingListener listener;
        boolean isBond;
    }

    private String mProductList[];
    private String mSignature;
    private BillingService billingService;
    private KES mKES;
    //private BillingStatus mBillingStatus = BillingStatus.Idle;
    ArrayList<ContextWrapper> bondList = new ArrayList<ContextWrapper>();
    ArrayList<CreditItem> mCreditList = null;

    public ServiceConnection billingConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            billingService = ((BillingService.BillingServiceBinder)service).getService();
            for (int i = 0; i < bondList.size(); i++)
                bondList.get(i).listener.onStatus(billingService.getStatus());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            billingService = null;
            for (int i = 0; i < bondList.size(); i++)
                bondList.get(i).listener.onStatus(BillingStatus.Disconnected);
        }
    };

    protected BillingManager(KES kes)
    {
        mKES = kes;
    }

    public void init(String signature_public, String productList[])
    {
        mSignature = signature_public;
        mProductList = productList;
    }


    public void onStart(Activity activity,BillingListener listener)
    {
        if (mProductList == null)
            throw new IllegalStateException("Product list is not initialised.Please call init() first.");
        if (!mKES.getAccountManager().getUser().isRegistered())
            throw new IllegalStateException("User is not registered");

        if (bondList.size() == 0) {
            LocalBroadcastManager.getInstance(mKES.getContext()).registerReceiver(receiver, new IntentFilter(BillingService.ACTION_PURCHASED));
            LocalBroadcastManager.getInstance(mKES.getContext()).registerReceiver(receiver, new IntentFilter(BillingService.ACTION_CREDIT_STATUS));
            LocalBroadcastManager.getInstance(mKES.getContext()).registerReceiver(receiver, new IntentFilter(BillingService.ACTION_CREDITLIST));
        }

        for (int i = 0; i < bondList.size(); i++)
        {
            ContextWrapper cw = bondList.get(i);
            if (cw.activity == activity)
                throw new IllegalStateException("OnStart has already been called from this Activity");
        }
        ContextWrapper cw = new ContextWrapper();
        bondList.add(cw);
        cw.activity = activity;
        cw.listener = listener;
        Intent intent = new Intent(activity, BillingService.class);
        intent.putExtra(BillingService.TAG_SIGNATURE, mSignature);
        intent.putExtra(BillingService.TAG_PRODUCTLIST, mProductList);
        intent.putExtra(BillingService.TAG_TOKEN, mKES.getAccountManager().getToken());
        cw.isBond = activity.bindService(intent, billingConnection, Context.BIND_AUTO_CREATE);
        listener.onStatus(BillingStatus.Init);
        if (mCreditList != null)
            listener.onCreditList(mCreditList);
    }

    public void buyCredits(Activity activity, String productId) {
        billingService.buyCredits(activity, productId);
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

    public void onStop(Activity activity)
    {
        for (int i = 0; i < bondList.size(); i++)
        {
            ContextWrapper cw = bondList.get(i);
            if (cw.activity == activity)
            {
                bondList.remove(cw);
                if (cw.isBond)
                    cw.activity.unbindService(billingConnection);
                if (bondList.size() == 0)
                {
                    billingService = null;
                    LocalBroadcastManager.getInstance(mKES.getContext()).unregisterReceiver(receiver);
                }
                return;
            }
        }
        throw new IllegalStateException("OnStart has already been called from this Activity");
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
            if (BillingService.ACTION_CREDITLIST.equals(action)) {
                mCreditList = billingService.getCreditItems();
                for (int i = 0; i < bondList.size(); i++)
                    bondList.get(i).listener.onCreditList(mCreditList);
            }
            if (BillingService.ACTION_CREDIT_STATUS.equals(action))
                for (int i = 0; i < bondList.size(); i++)
                    bondList.get(i).listener.onStatus(BillingStatus.values()[intent.getIntExtra(BillingService.TAG_STATUS, 0)]);
            if (BillingService.ACTION_PURCHASED.equals(action)) {
                int quantity = intent.getIntExtra(BillingService.TAG_QUANTITY, 0);
                mKES.getAccountManager().updateBalance(quantity);
                for (int i = 0; i < bondList.size(); i++)
                    bondList.get(i).listener.onPurchased(quantity);
            }
        }
    };
}
