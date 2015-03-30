package com.crowdmobile.kes.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.crowdmobile.kes.R;
import com.crowdmobile.kes.list.PriceItem;
import com.kes.Session;
import com.kes.model.CreditOption;

import java.util.ArrayList;

/**
 * Created by gadza on 2015.03.11..
 */
public class CheckoutFragment extends Fragment {

    ListView lvPriceList;
    ArrayList<CreditOption> list;
    private String discountFormat;
    private PriceAdapter priceAdapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        discountFormat = getResources().getString(R.string.discountformat);
        list = new ArrayList<CreditOption>();
        CreditOption co = new CreditOption();
        co.currency = "€";
        co.quantity = 25;
        co.price = 1.99f;
        co.discount = 0;
        list.add(co);
        co = new CreditOption();
        co.currency = "€";
        co.quantity = 75;
        co.price = 4.99f;
        co.discount = 5;
        list.add(co);
        co = new CreditOption();
        co.currency = "€";
        co.quantity = 175;
        co.price = 9.99f;
        co.discount = 10;
        list.add(co);
        co = new CreditOption();
        co.currency = "€";
        co.quantity = 400;
        co.price = 19.99f;
        co.discount = 15;
        list.add(co);
        priceAdapter = new PriceAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(R.string.fragment_credit_title);
        View result = inflater.inflate(R.layout.fragment_credit,container,false);
        lvPriceList = (ListView)result.findViewById(R.id.lvPricelist);
        lvPriceList.setAdapter(priceAdapter);
        return result;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        lvPriceList = null;
    }

    public class PriceAdapter extends ArrayAdapter<CreditOption> {

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
            PriceItem.updateView(convertView,getItem(position),discountFormat);
            return convertView;
        }

    }

}
