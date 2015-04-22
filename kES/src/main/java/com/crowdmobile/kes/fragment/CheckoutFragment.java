package com.crowdmobile.kes.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crowdmobile.kes.R;
import com.crowdmobile.kes.list.PriceItem;
import com.kes.BillingManager;
import com.kes.Session;
import com.kes.billing.CreditItem;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by gadza on 2015.03.11..
 */
public class CheckoutFragment extends Fragment {

    ListView lvPriceList;
    ArrayList<CreditItem> priceList = new ArrayList<CreditItem>();
    private PriceAdapter priceAdapter;
    View holderProgress;
    ProgressBar billingProgress;
    TextView tvBillingStatus;
    View holderButtons, btCreditRetry,btCreditRefund;
    String currencyFormat;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        priceAdapter = new PriceAdapter();
        currencyFormat = getString(R.string.credit_price_format);

    }

    BillingManager.BillingListener billingListener = new BillingManager.BillingListener() {
        @Override
        public void onStatus(BillingManager.BillingStatus status) {
            if (status == BillingManager.BillingStatus.PaymentFail)
            {
                holderProgress.setVisibility(View.VISIBLE);
                tvBillingStatus.setText(R.string.credit_status_processing_fail);
                holderButtons.setVisibility(View.VISIBLE);
                billingProgress.setVisibility(View.GONE);
                return;
            } else
            {
                holderButtons.setVisibility(View.GONE);
                billingProgress.setVisibility(View.VISIBLE);
            }

            if (status == BillingManager.BillingStatus.Idle)
                holderProgress.setVisibility(View.GONE);
            else
            {
                if (status == BillingManager.BillingStatus.Init)
                tvBillingStatus.setText(R.string.credit_status_init);
                else if (status == BillingManager.BillingStatus.InitFailed)
                    tvBillingStatus.setText(R.string.credit_status_initfail);
                else if (status == BillingManager.BillingStatus.PaymentProcess)
                    tvBillingStatus.setText(R.string.credit_status_processing);
                else if (status == BillingManager.BillingStatus.PaymentFail)
                    tvBillingStatus.setText(R.string.credit_status_processing_fail);
                else if (status == BillingManager.BillingStatus.Disconnected)
                    tvBillingStatus.setText(R.string.credit_status_disconnected);
                holderProgress.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onCreditList(ArrayList<CreditItem> list) {
            priceList.clear();
            if (list != null) {
                for (int i = 0; i < list.size(); i++)
                    priceList.add(list.get(i));
                Collections.sort(priceList);
            }
            priceAdapter.notifyDataSetChanged();
        }
    };


    @Override
    public void onStart() {
        super.onStart();
        Session.getInstance(getActivity()).getBillingManager().onStart(getActivity(), billingListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        Session.getInstance(getActivity()).getBillingManager().onStop(getActivity());
    }

    /*
            private void updateCredits()
            {
            }
            */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_credit,container,false);
        lvPriceList = (ListView)result.findViewById(R.id.lvPricelist);
        lvPriceList.setAdapter(priceAdapter);
        lvPriceList.setOnItemClickListener(onItemClickListener);
        holderProgress = result.findViewById(R.id.holderProgress);
        billingProgress = (ProgressBar) holderProgress.findViewById(R.id.creditProgress);
        tvBillingStatus = (TextView) holderProgress.findViewById(R.id.creditStatus);

        holderButtons = holderProgress.findViewById(R.id.holderButtons);
        btCreditRetry = holderProgress.findViewById(R.id.btCreditRetry);
        btCreditRefund = holderProgress.findViewById(R.id.btCreditRefund);
        btCreditRetry.setOnClickListener(onClickListener);
        btCreditRefund.setOnClickListener(onClickListener);
//        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, new IntentFilter(ACTION_CREDIT_UPDATE));
//        updateCredits();
        return result;
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btCreditRetry)
            {
                billingListener.onStatus(BillingManager.BillingStatus.PaymentProcess);
                Session.getInstance(getActivity()).getBillingManager().processPendingCredits();
            } else
            if (v == btCreditRefund) {
                ArrayList<String> pending = Session.getInstance(getActivity()).getBillingManager().getPendingOrders();
                if (pending == null || pending.size() == 0)
                    return;
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                i.putExtra(Intent.EXTRA_EMAIL  , new String[]{getResources().getString(R.string.refund_email)});
                i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.refund_email_title));
                StringBuilder builder = new StringBuilder();
                builder.append(getString(R.string.refund_email_message));
                for (int c = 0, l = pending.size(); c < l; c++) {
                    builder.append("\n");
                    builder.append(pending.get(c));
                }
                String body = getString(R.string.refund_email_message);
                i.putExtra(Intent.EXTRA_TEXT   , builder.toString());
                try {
                    startActivity(Intent.createChooser(i, getString(R.string.refund_email_dialog_title)));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(getActivity(), "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            CreditItem ci = priceList.get(position);
            Session.getInstance(getActivity()).getBillingManager().buyCredits(getActivity(),ci.productId);
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        lvPriceList = null;
        holderButtons = null;
        holderProgress = null;
        tvBillingStatus = null;
        billingProgress = null;
        btCreditRetry = null;
        btCreditRefund = null;
    }

    public class PriceAdapter extends ArrayAdapter<CreditItem> {

        private LayoutInflater inflater;
        private Session session;

        public PriceAdapter() {
            super(getActivity(), 0, priceList);
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
