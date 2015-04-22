package com.crowdmobile.kes.list;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.crowdmobile.kes.R;
import com.kes.billing.CreditItem;

/**
 * Created by gadza on 2015.03.06..
 */
public class PriceItem {

    static class ViewHolder {
        TextView tvQuantity;
        TextView tvPrice;
    };


    public static View createView(LayoutInflater inflater, ViewGroup root)
    {
        View result = inflater.inflate(R.layout.item_credit,root,false);
        ViewHolder holder = new ViewHolder();
        result.setTag(holder);
        holder.tvQuantity = (TextView)result.findViewById(R.id.tvQuantity);
        holder.tvPrice = (TextView)result.findViewById(R.id.tvPrice);
        return result;
    }

    public static void updateView(View convertView, CreditItem item,String priceFormat)
    {
        ViewHolder holder = (ViewHolder)convertView.getTag();
        holder.tvQuantity.setText(Integer.toString(item.quantity));
        holder.tvPrice.setText(String.format(priceFormat, item.currency, item.price / item.quantity));
    }
}
