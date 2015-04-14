package com.crowdmobile.kes.billing;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.crowdmobile.kes.R;
import com.crowdmobile.kes.billing.IabHelper.OnConsumeMultiFinishedListener;
import com.crowdmobile.kes.billing.IabHelper.OnIabPurchaseFinishedListener;
import com.crowdmobile.kes.billing.IabHelper.QueryInventoryFinishedListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BillingManager {
    private static final String TAG = BillingManager.class.getSimpleName();
    
    public static final boolean TEST_MODE = true;
    
    private static final int RC_REQUEST = 10101;

    private boolean isPrepared = false;
    private Activity activity;
    private IabHelper iabHelper;
    
    private BillingListener billingListener;
    
    public interface OnRestoredPurchasesListenter{
    }
    
    public interface BillingListener {
        void onPrepared(IabResult result);
        void onRestored(boolean success, List<String> restoredItems);
        void onInventoryReceived(SkuDetails details, boolean isPurchased);
        void onCreditsReceived(ArrayList<CreditItem> items);
        void onIabPurchaseFinished(IabResult result, Purchase info);
    }
    

    public BillingManager(Activity activity, BillingListener listener) {
        if(activity == null){
            throw new IllegalArgumentException("activity must be not NULL");
        }
        if(listener == null){
            throw new IllegalArgumentException("preparedListener must be not NULL");
        }
        this.activity = activity;
        this.billingListener = listener;

        iabHelper = new com.crowdmobile.kes.billing.IabHelper(activity, activity.getString(R.string.signature_public));
        iabHelper.enableDebugLogging(true);
        iabHelper.startSetup(iabSetupFinishedListener);
    }

    IabHelper.OnIabSetupFinishedListener iabSetupFinishedListener = new IabHelper.OnIabSetupFinishedListener() {
        public void onIabSetupFinished(IabResult result) {
            isPrepared = result.isSuccess();
            billingListener.onPrepared(result);
        }
    };

    public void release(){
        if(iabHelper != null){
            iabHelper.dispose();
        }
        iabHelper = null;
    }

    public void requestCreditsList(String[] list){
        if(!isPrepared){
            Log.e(TAG, "requestCreditsList FAILED: manager is not prepared");
            return;
        }
        iabHelper.queryInventoryAsync(true,
                Arrays.asList(list),

                new QueryInventoryFinishedListener() {
                @Override
                public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
                    if (result.isFailure()) {
                        Log.e(TAG, "onQueryInventoryFinished " + result.getMessage());
                        billingListener.onCreditsReceived(null);
                        return;
                      }
                    ArrayList<CreditItem> items = new ArrayList<CreditItem>();
                    if(inventory == null || inventory.mSkuMap == null){
                        billingListener.onCreditsReceived(items);
                        return;
                    }
                    Log.d(TAG, "--- requestCreditsList: " + inventory.mSkuMap.keySet().size());
                    for(String productId : inventory.mSkuMap.keySet()){
                        Log.d(TAG, "productId: " + productId);
                        SkuDetails skuDetails = inventory.mSkuMap.get(productId);
                        if (skuDetails.mType != null)
                            items.add(new CreditItem(skuDetails));
                    }
                    Log.d(TAG, "---");
                    billingListener.onCreditsReceived(items);
                }
            });
    }

    public void requestItemDetails(final String productId){
        iabHelper.queryInventoryAsync(true, new ArrayList<String>(){
            {
                add(productId);
            }
        }, new QueryInventoryFinishedListener() {
            @Override
            public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                if(result.isSuccess()){
                    if(inv.hasDetails(productId)){
                        billingListener.onInventoryReceived(inv.getSkuDetails(productId), inv.hasPurchase(productId));
                    } else {
                        billingListener.onInventoryReceived(null, false);
                    }
                } else {
                    billingListener.onInventoryReceived(null, false);
                    Log.e("ERROR in Billing manage", "ERROR requestItemDetails: failed result");
                }
            }
        });
    }
    
    public void refundAll(final OnConsumeMultiFinishedListener l){
        iabHelper.queryInventoryAsync(new QueryInventoryFinishedListener() {
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

    public void buyCredits(String productId){
        iabHelper.launchPurchaseFlow(activity,productId, RC_REQUEST, new OnIabPurchaseFinishedListener() {
            @Override
            public void onIabPurchaseFinished(IabResult result, Purchase info) {
                if(result.isSuccess()){
                    try {
                        iabHelper.consume(info);
                    } catch (IabException e) {
                        e.printStackTrace();
                    }
                }
                billingListener.onIabPurchaseFinished(result, info);
            }
        });
    }

    public void buyCredits(CreditItem item, final OnIabPurchaseFinishedListener listener){
        iabHelper.launchPurchaseFlow(activity,item.productId, RC_REQUEST, new OnIabPurchaseFinishedListener() {
            @Override
            public void onIabPurchaseFinished(IabResult result, Purchase info) {
                if(result.isSuccess()){
                    try {
                        iabHelper.consume(info);
                    } catch (IabException e) {
                        e.printStackTrace();
                    }
                }
                listener.onIabPurchaseFinished(result, info);    
            }
        });
    }
    

    public void restorePurchases(final OnRestoredPurchasesListenter l){
        iabHelper.queryInventoryAsync(new QueryInventoryFinishedListener() {
            @Override
            public void onQueryInventoryFinished(IabResult result, Inventory inv) {
            }
        });
    }
    
    
    public void showBuyContentDialog(final OnIabPurchaseFinishedListener listener){
//        requestItemDetails("", new OnDetailsReceivedCallback() {
//            @Override
//            public void onReceived(SkuDetails details, boolean isPurchased) {
//                if(details == null){
//                } else {
//                    if(isPurchased){
//                        DialogHelper.showAlertDialog(activity, null, "Продукт уже приобретен. Используйте пунтом меню \"Восстановить\"");
//                    } else {
//                        new AlertDialog.Builder(activity)
//                            .setMessage(activity.getString(R.string.book_content_buy_message, details.getPrice()))
//                            .setPositiveButton(android.R.string.ok, new OnClickListener() {
//                                @Override
//                                public void onClickMarkAsPrivate(DialogInterface dialog, int which) {
//                                    activity.setProgressState(true);
//                                    if(SystemHelper.isConnected(activity)){
//                                        buyContent(listener);
//                                    } else {
//                                        activity.showConnectionLostDialog();
//                                    }
//                                }
//                            })
//                            .setNegativeButton(android.R.string.cancel, null)
//                            .setCancelable(false)
//                            .create().show();
//                    }
//                }
//            }
//        });
    }
    
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data){
        return iabHelper.handleActivityResult(requestCode, resultCode, data);
    }
    
}
