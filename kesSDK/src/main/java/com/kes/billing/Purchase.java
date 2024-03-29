/* Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kes.billing;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents an in-app billing purchase.
 */
public class Purchase implements Parcelable {
    String mItemType;  // ITEM_TYPE_INAPP or ITEM_TYPE_SUBS
    String mOrderId;
    String mPackageName;
    String mSku;
    long mPurchaseTime;
    int mPurchaseState;
    String mDeveloperPayload;
    String mToken;
    String mOriginalJson;
    String mSignature;

    public static final Parcelable.Creator<Purchase> CREATOR =
            new Parcelable.Creator<Purchase>(){

                @Override
                public Purchase createFromParcel(Parcel source) {
                    return new Purchase(source);
                }

                @Override
                public Purchase[] newArray(int size) {
                    return new Purchase[size];
                }
            };

    public Purchase(Parcel source)
    {
        mItemType = source.readString();
        mOrderId = source.readString();
        mPackageName = source.readString();
        mSku = source.readString();
        mPurchaseTime = source.readLong();
        mPurchaseState = source.readInt();
        mDeveloperPayload = source.readString();
        mToken = source.readString();
        mOriginalJson = source.readString();
        mSignature = source.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mItemType);
        dest.writeString(mOrderId);
        dest.writeString(mPackageName);
        dest.writeString(mSku);
        dest.writeLong(mPurchaseTime);
        dest.writeInt(mPurchaseState);
        dest.writeString(mDeveloperPayload);
        dest.writeString(mToken);
        dest.writeString(mOriginalJson);
        dest.writeString(mSignature);
    }



    @Override
    public String toString() { return "PurchaseInfo(type:" + mItemType + "):" + mOriginalJson; }

    @Override
    public int describeContents() {
        return 0;
    }

    public Purchase(String itemType, String jsonPurchaseInfo, String signature) throws JSONException {
        mItemType = itemType;
        mOriginalJson = jsonPurchaseInfo;
        JSONObject o = new JSONObject(mOriginalJson);
        mOrderId = o.optString("orderId");
        mPackageName = o.optString("packageName");
        mSku = o.optString("productId");
        mPurchaseTime = o.optLong("purchaseTime");
        mPurchaseState = o.optInt("purchaseState");
        mDeveloperPayload = o.optString("developerPayload");
        mToken = o.optString("token", o.optString("purchaseToken"));
        mSignature = signature;
    }

    public String getItemType() { return mItemType; }
    public String getOrderId() { return mOrderId; }
    public String getPackageName() { return mPackageName; }
    public String getSku() { return mSku; }
    public long getPurchaseTime() { return mPurchaseTime; }
    public int getPurchaseState() { return mPurchaseState; }
    public String getDeveloperPayload() { return mDeveloperPayload; }
    public String getToken() { return mToken; }
    public String getOriginalJson() { return mOriginalJson; }
    public String getSignature() { return mSignature; }

}
