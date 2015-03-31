package com.crowdmobile.kes.billing;

import android.content.Context;

import com.crowdmobile.kes.R;


public class CreditsItem implements Comparable<CreditsItem> {
    public String productId;
    public String price;
    public int nominal;
    
    public CreditsItem() {
        super();
    }
    
    public CreditsItem(SkuDetails details) {
        super();
        this.productId = details.getSku();
        this.price = details.getPrice();
        this.nominal = getNominal(productId);
    }
    
    public String getNominalText(Context context){
        return nominal + " " 
        		+ context.getResources().getQuantityString(R.plurals.credits_plurals, nominal);
    }

    private int getNominal(String productId){
        int n = 1;
        try {
            String[] split = productId.split("_");
            n = Integer.valueOf(split[1]).intValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return n;
    }

    public static boolean validateProductId(String productId, String creditsPrefix){
        return productId.startsWith(creditsPrefix);
    }

    @Override
    public boolean equals(Object o) {
        
        return super.equals(o);
    }

    @Override
    public int compareTo(CreditsItem another) {
        int nominalDiff = nominal - another.nominal;
        return nominalDiff;
    }
}
