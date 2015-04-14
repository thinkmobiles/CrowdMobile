package com.crowdmobile.kes.billing;

public class CreditItem implements Comparable<CreditItem> {
    public String productId;
    public int quantity;
    public double price;
    public String currency;

    public CreditItem() {
        super();
    }
    
    public CreditItem(SkuDetails details) {
        super();
        this.productId = details.getSku();
        this.quantity = Integer.valueOf(productId.substring(7));
        this.price = details.getPriceMicros() / 1000000;
        this.currency = details.getCurrency();
    }

    /*
    public String getNominalText(Context context){
        return nominal + " " 
        		+ context.getResources().getQuantityString(R.plurals.credits_plurals, nominal);
    }
    */

    @Override
    public int compareTo(CreditItem another) {
        if (quantity == another.quantity)
            return 0;
        if (quantity > another.quantity)
            return 1;
        else
            return -1;
    }

}
