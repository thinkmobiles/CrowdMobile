package com.crowdmobile.kes.billing;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.crowdmobile.kes.R;

import java.util.ArrayList;
import java.util.Arrays;

public class BillingService extends Service {

    private static final String TAG = BillingService.class.getSimpleName();
    private static final String ACTION_PURCHASE = "purchase";
    public static final String ACTION_CREDIT_UPDATE = "credit_update";
    public static final String ACTION_CREDIT_PROCESSING = "credit_processing";

    private static final String TAG_PURCHASE = "purchase";
    CompletePurchase completePurchase = null;

    public static interface BillingServiceListener {
        public BillingService getBillingService();
    }

    public interface OnBillingPreparedListener{
        void onPrepared(IabResult result);
    }

    public interface OnDetailsReceivedCallback{
        void onReceived(SkuDetails details, boolean isPurchased);
    }

    public interface OnCreditsReceivedCallback{
        void onReceived(ArrayList<CreditItem> items);
    }

    private static final int RC_REQUEST = 10101;

    private boolean isPrepared = false;
    private IabHelper iabHelper;
    private BillingServiceBinder mBinder = new BillingServiceBinder();
    private boolean priceListReceived = false;

    private ArrayList<CreditItem> creditItems = null;
    private Purchase purchaseItems[] = null;

    public class BillingServiceBinder extends Binder {
        public BillingService getService() {
            return BillingService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        iabHelper = new com.crowdmobile.kes.billing.IabHelper(this, getString(R.string.signature_public));
        iabHelper.enableDebugLogging(true);
        iabHelper.startSetup(iabSetupFinishedListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        iabHelper.dispose();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null)
            return START_NOT_STICKY;
        Bundle extras = intent.getExtras();
        if (ACTION_PURCHASE.equals(intent.getAction())) {
            purchase(extras);
        }
        return START_NOT_STICKY;
    }

    private void purchase(Bundle extras)
    {
        //Purchase purchase = extras.getParcelable(TAG_PURCHASE);
        Purchase purchase[] = (Purchase[])extras.getParcelableArray(TAG_PURCHASE);
        new CompletePurchase().execute(purchase);
    }

    class CompletePurchase extends AsyncTask<Purchase, Void, Purchase> {

        protected Purchase doInBackground(Purchase... params) {
            return null;
        }

        @Override
        protected void onPostExecute(Purchase purchase) {
            if (purchase == null)
                return;
            try {
                iabHelper.consume(purchase);
            } catch (IabException e) {
                e.printStackTrace();
            }
        }
    };

    IabHelper.OnIabSetupFinishedListener iabSetupFinishedListener = new IabHelper.OnIabSetupFinishedListener() {
        public void onIabSetupFinished(IabResult result) {
            if (isPrepared = result.isSuccess())
                iabHelper.queryInventoryAsync(true, Arrays.asList(getResources().getStringArray(R.array.credits_list)), queryInventoryFinished);
            else {
                priceListReceived = true;
                LocalBroadcastManager.getInstance(BillingService.this).sendBroadcast(new Intent(ACTION_CREDIT_UPDATE));
            }
        }
    };

    IabHelper.QueryInventoryFinishedListener queryInventoryFinished = new IabHelper.QueryInventoryFinishedListener() {
        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            priceListReceived = true;
            if (!result.isFailure()) {
                creditItems = new ArrayList<CreditItem>();
                if (inventory != null || inventory.mSkuMap != null) {
                    Log.d(TAG, "--- requestCreditsList: " + inventory.mSkuMap.keySet().size());
                    for (String productId : inventory.mSkuMap.keySet()) {
                        Log.d(TAG, "productId: " + productId);
                        SkuDetails skuDetails = inventory.mSkuMap.get(productId);
                        if (skuDetails.mType != null)
                            creditItems.add(new CreditItem(skuDetails));
                    }
                }

                if (inventory != null || inventory.mPurchaseMap != null) {
                    purchaseItems = new Purchase[inventory.mPurchaseMap.size()];
                    Log.d(TAG, "--- requestCreditsList: " + inventory.mPurchaseMap.keySet().size());
                    int idx = 0;
                    for (String productId : inventory.mPurchaseMap.keySet())
                        purchaseItems[idx++] = inventory.mPurchaseMap.get(productId);
                    Intent intent = new Intent(BillingService.this,BillingService.class);
                    intent.setAction(ACTION_PURCHASE);
                    intent.putExtra(TAG_PURCHASE,purchaseItems);
                    startService(intent);
                    LocalBroadcastManager.getInstance(BillingService.this).sendBroadcast(new Intent(ACTION_CREDIT_PROCESSING));
                }

            }
            LocalBroadcastManager.getInstance(BillingService.this).sendBroadcast(new Intent(ACTION_CREDIT_UPDATE));
        }
    };

    public boolean isCreditsReceived()
    {
        return priceListReceived;
    }

    public void getCreditItems(ArrayList<CreditItem> dest)
    {
        if (creditItems != null)
            for (int i = 0; i < creditItems.size(); i++)
                dest.add(creditItems.get(i));
    }

    public void refundAll(final IabHelper.OnConsumeMultiFinishedListener l){
        iabHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
            @Override
            public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                if(result.isSuccess()){
                    iabHelper.consumeAsync(inv.getAllPurchases(), l);
//                    iabHelper.consumeAsync(inv.getPurchase("android.test.purchased"), new OnConsumeFinishedListener() {
//
//                        @Override
//                        public void onConsumeFinished(Purchase purchase, IabResult result) {
//                            if(result.isSuccess()){
//                                Log.d(TAG, "!!! success refund TEST");
//                            } else {
//                                Log.d(TAG, "!!! failed refund TEST");
//                            }
//                        }
//                    });
                }
            }
        });
    }

    public void buyCredits(Activity activity, String productId){
        if (completePurchase != null)
            return;
        productId = "android.test.purchased";
        iabHelper.launchPurchaseFlow(activity,productId, RC_REQUEST, new PurchaseListener());
    }



    class PurchaseListener implements IabHelper.OnIabPurchaseFinishedListener {
        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase info) {
            if(result.isSuccess()) {
                Intent intent = new Intent(BillingService.this,BillingService.class);
                intent.setAction(ACTION_PURCHASE);
                intent.putExtra(TAG_PURCHASE,new Purchase[] {info});
                startService(intent);
            }
        }
    };

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data){
        return iabHelper.handleActivityResult(requestCode, resultCode, data);
    }

}
