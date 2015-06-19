package com.kes.billing;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;

import com.kes.BillingManager;
import com.kes.model.CreditResponse;
import com.kes.net.DataFetcher;
import com.kes.NetworkAPI;

import java.util.ArrayList;
import java.util.Arrays;

public class BillingService extends Service {
    private static final boolean TESTMODE = false;
    private static final String TAG = BillingService.class.getSimpleName();
    private static final String ACTION_PURCHASE = BillingService.class.getSimpleName() + "purchase";

    public static final String ACTION_CREDITLIST = BillingService.class.getSimpleName() + "creditlist";
    public static final String ACTION_CREDIT_STATUS = BillingService.class.getSimpleName() + "credit_status";
    public static final String ACTION_PURCHASED = BillingService.class.getSimpleName() + "credit_purchased";
    public static final String TAG_QUANTITY = "quantity";

    private static final String TAG_PURCHASE = "purchase";
    public static final String TAG_SIGNATURE = "signature";
    public static final String TAG_PRODUCTLIST = "productlist";
    public static final String TAG_TOKEN = "token";

    public static final String TAG_STATUS = "status";
    private BillingManager.BillingStatus status = BillingManager.BillingStatus.Idle;

    private Handler handler = new Handler();

    public interface OnBillingPreparedListener{
        void onPrepared(IabResult result);
    }

    public interface OnDetailsReceivedCallback{
        void onReceived(SkuDetails details, boolean isPurchased);
    }

    public interface OnCreditsReceivedCallback{
        void onReceived(ArrayList<CreditItem> items);
    }

    public interface OnCreditsUpdatedListener
    {
        public void updateBalance(int balance);
    }

    private static final int RC_REQUEST = 10101;

    private IabHelper iabHelper;
    private BillingServiceBinder mBinder = new BillingServiceBinder();

    private ArrayList<CreditItem> creditItems = null;
    private Purchase purchaseItems[] = null;

    private String signature = null;
    private String products[];
    private CompletePurchase completePurchase;
    private ArrayList<String>pendingOrder = null;
    private String token;

    public class BillingServiceBinder extends Binder {
        public BillingService getService() {
            return BillingService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Bundle extras = intent.getExtras();
        if (iabHelper == null) {
            signature = extras.getString(TAG_SIGNATURE);
            products = extras.getStringArray(TAG_PRODUCTLIST);
            token = extras.getString(TAG_TOKEN);
            setStatus(BillingManager.BillingStatus.Init);
            iabHelper = new com.kes.billing.IabHelper(BillingService.this, signature);
            iabHelper.enableDebugLogging(false);
            iabHelper.startSetup(iabSetupFinishedListener);
        }
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (completePurchase != null)
        {
            completePurchase.cancel(true);
            completePurchase = null;
        }

        iabHelper.dispose();
        setStatus(BillingManager.BillingStatus.Disconnected);
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
        if ((status != BillingManager.BillingStatus.PaymentProcess &&
            status != BillingManager.BillingStatus.Idle)
                || token == null)
        {
            stopSelf();
            return;
        }
        //Purchase purchase = extras.getParcelable(TAG_PURCHASE);

        Parcelable parcelable[] = extras.getParcelableArray(TAG_PURCHASE);
        Purchase purchase[] = Arrays.copyOf(parcelable, parcelable.length, Purchase[].class);
        completePurchase = new CompletePurchase(token);
        completePurchase.execute(purchase);
    }


    class CompletePurchase extends AsyncTask<Purchase, Integer, Integer> {
        String auth_token;
        ArrayList<String> localFailed = new ArrayList<String>();

        public CompletePurchase(String auth_token) {
            this.auth_token = auth_token;
        }

        protected Integer doInBackground(Purchase... params) {
            boolean processed = false;
            int totalCount = 0;
            CreditResponse cr = null;

            try {
                for (int i = 0; i < params.length; i++) {
                    Purchase p = params[i];
                    processed = false;
                    cr = null;
                    try {
                        cr = NetworkAPI.addCredit(auth_token, p.getOriginalJson());
                        processed = true;
                    } catch (DataFetcher.KESNetworkException e) {
                        if (e.error.code == 10)
                            processed = true;   //already processed item
                    }
                    if (processed) {
                        try {
                            //if (false)
                            iabHelper.consume(p);
                            totalCount++;
                            if (cr != null)
                                publishProgress(cr.credit_points);
                            continue;
                        } catch (IabException ignored) {}
                    }
                    localFailed.add(p.getOrderId());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return params.length - totalCount;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            Intent intent = new Intent(ACTION_PURCHASED);
            intent.putExtra(TAG_QUANTITY, values[0]);
            LocalBroadcastManager.getInstance(BillingService.this).sendBroadcastSync(new Intent(intent));
        }

        @Override
        protected void onPostExecute(Integer count) {
            completePurchase = null;
            pendingOrder = localFailed;
            if (count == 0)
                setStatus(BillingManager.BillingStatus.Idle);
            else
                setStatus(BillingManager.BillingStatus.PaymentFail);
            stopSelf();
        }
    };

    public ArrayList<String> getPendingOrders()
    {
        return pendingOrder;
    }

    IabHelper.OnIabSetupFinishedListener iabSetupFinishedListener = new IabHelper.OnIabSetupFinishedListener() {
        public void onIabSetupFinished(IabResult result) {
            if (result.isSuccess())
                iabHelper.queryInventoryAsync(true, Arrays.asList(products), queryInventoryFinished);
            else {
                LocalBroadcastManager.getInstance(BillingService.this).sendBroadcast(new Intent(ACTION_CREDIT_STATUS));
                setStatus(BillingManager.BillingStatus.InitFailed);
            }
        }
    };

    public BillingManager.BillingStatus getStatus()
    {
        return status;
    }

    private void setStatus(BillingManager.BillingStatus status)
    {
        if (this.status == status)
            return;
        this.status = status;
        Intent intent = new Intent(ACTION_CREDIT_STATUS);
        intent.putExtra(TAG_STATUS, status.ordinal());
        LocalBroadcastManager.getInstance(BillingService.this).sendBroadcastSync(new Intent(intent));
    }

    IabHelper.QueryInventoryFinishedListener queryInventoryFinished = new IabHelper.QueryInventoryFinishedListener() {
        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            BillingManager.BillingStatus newStatus = BillingManager.BillingStatus.InventoryFail;
            if (!result.isFailure()) {
                newStatus = BillingManager.BillingStatus.Idle;
                creditItems = new ArrayList<CreditItem>();
                if (inventory != null && inventory.mSkuMap != null && inventory.mSkuMap.size() > 0) {
                    //Log.d(TAG, "--- requestCreditsList: " + inventory.mSkuMap.keySet().size());
                    for (String productId : inventory.mSkuMap.keySet()) {
                        //Log.d(TAG, "productId: " + productId);
                        SkuDetails skuDetails = inventory.mSkuMap.get(productId);
                        if (skuDetails.mType != null)
                            creditItems.add(new CreditItem(skuDetails));
                    }
                }

                if (inventory != null && inventory.mPurchaseMap != null && inventory.mPurchaseMap.size() > 0) {
                    newStatus = BillingManager.BillingStatus.PaymentProcess;
                    purchaseItems = new Purchase[inventory.mPurchaseMap.size()];
                    //Log.d(TAG, "--- requestCreditsList: " + inventory.mPurchaseMap.keySet().size());
                    int idx = 0;
                    for (String productId : inventory.mPurchaseMap.keySet())
                        purchaseItems[idx++] = inventory.mPurchaseMap.get(productId);
                    Intent intent = new Intent(BillingService.this,BillingService.class);
                    intent.setAction(ACTION_PURCHASE);
                    intent.putExtra(TAG_PURCHASE, purchaseItems);
                    //TODO:enable
                    startService(intent);
                }

            }
            LocalBroadcastManager.getInstance(BillingService.this).sendBroadcast(new Intent(ACTION_CREDITLIST));
            setStatus(newStatus);
        }
    };

    public ArrayList<CreditItem> getCreditItems()
    {
        return creditItems;
    }

    /*
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
    */
    public void buyCredits(Activity activity, String productId){
        if (status != BillingManager.BillingStatus.Idle)
            return;
        if (TESTMODE) productId = "android.test.purchased";
        iabHelper.launchPurchaseFlow(activity,productId, RC_REQUEST, new PurchaseListener());
    }

    public void processPendingCredits()
    {
        if (status != BillingManager.BillingStatus.Idle && status != BillingManager.BillingStatus.PaymentFail)
            return;
        setStatus(BillingManager.BillingStatus.InventoryRequest);
        iabHelper.queryInventoryAsync(true, Arrays.asList(products), queryInventoryFinished);
    }

    class PurchaseListener implements IabHelper.OnIabPurchaseFinishedListener {
        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase info) {
            if(result.isSuccess()) {
                setStatus(BillingManager.BillingStatus.PaymentProcess);
                //Log.d("PURCHASE JSON",info.getOriginalJson());
                Intent intent = new Intent(BillingService.this,BillingService.class);
                intent.setAction(ACTION_PURCHASE);
                intent.putExtra(TAG_PURCHASE,new Purchase[] {info});
                startService(intent);
            } else {
                setStatus(BillingManager.BillingStatus.Idle);
                if (result.getResponse() == 7)
                    processPendingCredits();
            }
        }
    };

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data){
        if (iabHelper != null)
            return iabHelper.handleActivityResult(requestCode, resultCode, data);
        return false;
    }

}
