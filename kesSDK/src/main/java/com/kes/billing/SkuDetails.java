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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents an in-app product's listing details.
 */
public class SkuDetails {
    String mItemType;
    String mSku;
    String mType;
    String mPrice;
    String mTitle;
    String mDescription;
    String mJson;
    long mPriceAmountMicros;
    String mCurrencyCode;

    public SkuDetails(String jsonSkuDetails) throws JSONException {
        this(IabHelper.ITEM_TYPE_INAPP, jsonSkuDetails);
    }

    public SkuDetails(String itemType, String jsonSkuDetails) throws JSONException {
        mItemType = itemType;
        mJson = jsonSkuDetails;
        JSONObject o = new JSONObject(mJson);
        mTitle = o.optString("title");
        mPrice = o.optString("price");
        mType = o.optString("type");
        mDescription = o.optString("description");
        mPriceAmountMicros = o.optLong("price_amount_micros");
        mCurrencyCode = o.optString("price_currency_code");
        mSku = o.optString("productId");
    }

    public String getSku() { return mSku; }
    public String getType() { return mType; }
    public String getPriceStr() { return mPrice; }
    public String getTitle() { return mTitle; }
    public String getDescription() { return mDescription; }
    public long getPriceMicros() { return mPriceAmountMicros; }
    public String getCurrency() {return mCurrencyCode;}

    @Override
    public String toString() {
        return "SkuDetails:" + mJson;
    }
}
