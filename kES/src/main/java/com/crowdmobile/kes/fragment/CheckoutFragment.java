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
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.crowdmobile.kes.MainActivity;
import com.crowdmobile.kes.R;
import com.crowdmobile.kes.billing.CreditItem;
import com.crowdmobile.kes.list.PriceItem;
import com.kes.Session;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by gadza on 2015.03.11..
 */
public class CheckoutFragment extends Fragment {

    public static String ACTION_CREDIT = "action_credit";

    ListView lvPriceList;
    ArrayList<CreditItem> list;
    private PriceAdapter priceAdapter;
    View holderProgress;
    String currencyFormat;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
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
        ((MainActivity)getActivity()).updatePriceList(list);
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
        holderProgress = result.findViewById(R.id.holderProgress);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, new IntentFilter(ACTION_CREDIT));
        if (((MainActivity)getActivity()).updatePriceList(list))
            updateCredits();
        return result;
    }

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
