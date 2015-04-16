package com.crowdmobile.kes.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.crowdmobile.kes.R;
import com.crowdmobile.kes.billing.BillingService;
import com.crowdmobile.kes.billing.CreditItem;
import com.crowdmobile.kes.list.PriceItem;
import com.kes.Session;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by gadza on 2015.03.11..
 */
public class CheckoutFragment extends Fragment {

    ListView lvPriceList;
    ArrayList<CreditItem> list;
    private PriceAdapter priceAdapter;
    View holderProgress;
    String currencyFormat;
    BillingService.BillingServiceListener billingServiceListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        billingServiceListener = (BillingService.BillingServiceListener)activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        billingServiceListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        list = new ArrayList<CreditItem>();
        priceAdapter = new PriceAdapter();
        currencyFormat = getString(R.string.credit_price_format);
    }


    private void updateCredits()
    {
        list.clear();
        BillingService billingService = billingServiceListener.getBillingService();
        if (billingService == null)
            return;
        if (!billingService.isCreditsReceived())
            return;
        billingService.getCreditItems(list);
        Collections.sort(list);
        priceAdapter.notifyDataSetChanged();
        holderProgress.setVisibility(View.GONE);
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateCredits();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_credit,container,false);
        lvPriceList = (ListView)result.findViewById(R.id.lvPricelist);
        lvPriceList.setAdapter(priceAdapter);
        lvPriceList.setOnItemClickListener(onItemClickListener);
        holderProgress = result.findViewById(R.id.holderProgress);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, new IntentFilter(ACTION_CREDIT_UPDATE));
        updateCredits();
        return result;
    }

    AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            CreditItem ci = list.get(position);
            billingServiceListener.getBillingService().buyCredits(getActivity(),ci.productId);
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        lvPriceList = null;
        holderProgress = null;
    }

    public class PriceAdapter extends ArrayAdapter<CreditItem> {

        private LayoutInflater inflater;
        private Session session;

        public PriceAdapter() {
            super(getActivity(), 0, list);
            inflater = getActivity().getLayoutInflater();
        }


    @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = PriceItem.createView(inflater, parent);
            PriceItem.updateView(convertView,getItem(position),currencyFormat);
            return convertView;
        }

    }

}
