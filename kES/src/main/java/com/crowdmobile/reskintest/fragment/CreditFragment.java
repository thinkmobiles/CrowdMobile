package com.crowdmobile.reskintest.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crowdmobile.reskintest.MainActivity;
import com.crowdmobile.reskintest.R;
import com.crowdmobile.reskintest.list.PriceItem;
import com.crowdmobile.reskintest.widget.NavigationBar;
import com.kes.BillingManager;
import com.kes.KES;
import com.kes.billing.CreditItem;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by gadza on 2015.03.11..
 */
public class CreditFragment extends Fragment {

    ListView lvPriceList;
    ArrayList<CreditItem> priceList = new ArrayList<CreditItem>();
    private PriceAdapter priceAdapter;
    View holderProgress;
    ProgressBar billingProgress;
    TextView tvBillingStatus;
    View holderButtons, btCreditRetry,btCreditRefund;
    String currencyFormat;
    ImageView ivShopFooter;
    View holderPurchased;
    TextView tvPurchased;
    Animation purchasedAnimation;
    AnimationDrawable footerAnimation;

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
        public void onPurchased(int quantity) {
            tvPurchased.setText(Integer.toString(quantity));
            holderPurchased.setVisibility(View.VISIBLE);
            holderPurchased.startAnimation(purchasedAnimation);
        }

        @Override
        public void onStatus(BillingManager.BillingStatus status) {
            if (status == BillingManager.BillingStatus.PaymentFail ||
                status == BillingManager.BillingStatus.InventoryFail ||
                status == BillingManager.BillingStatus.InitFailed ||
                status == BillingManager.BillingStatus.Idle)
                billingProgress.setVisibility(View.GONE);
            else
                billingProgress.setVisibility(View.VISIBLE);


            if (status == BillingManager.BillingStatus.PaymentFail)
                holderButtons.setVisibility(View.VISIBLE);
            else
                holderButtons.setVisibility(View.GONE);

            if (status == BillingManager.BillingStatus.Idle)
                holderProgress.setVisibility(View.GONE);
            else
            {
                if (status == BillingManager.BillingStatus.Init)
                tvBillingStatus.setText(R.string.credit_status_init);
                else if (status == BillingManager.BillingStatus.InitFailed)
                    tvBillingStatus.setText(R.string.credit_status_initfail);
                else if (status == BillingManager.BillingStatus.InventoryFail)
                    tvBillingStatus.setText(R.string.credit_status_inventoryfail);
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
            if (priceList.size() > 0)
                return;
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
            KES.shared().getBillingManager().onStart(getActivity(), billingListener);
        if (footerAnimation != null) {
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(navigationChange, new IntentFilter(NavigationBar.ACTION_CHANGE));
            navigationChange.onReceive(null, null);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        KES.shared().getBillingManager().onStop(getActivity());
        if (footerAnimation != null)
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(navigationChange);
    }

    BroadcastReceiver navigationChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Don't load UI thread at all when scrolling
            if (((MainActivity) getActivity()).getNavigationBar().getAttached() == NavigationBar.Attached.Checkout)
                footerAnimation.start();
            else
                footerAnimation.stop();
        }
    };

    /*
            private void updateCredits()
            {
            }
            */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_credit,container,false);
        holderPurchased = result.findViewById(R.id.holderPurchased);
        tvPurchased = (TextView)holderPurchased.findViewById(R.id.tvPurchased);
        purchasedAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.purchased);
//        holderPurchased.startAnimation(purchasedAnimation);   //test

        ivShopFooter = (ImageView)result.findViewById(R.id.ivShopFooter);
        Drawable d = ivShopFooter.getDrawable();
        if (d != null && d instanceof  AnimationDrawable) {
            footerAnimation = (AnimationDrawable) d;
            footerAnimation.stop();
        }
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
        billingListener.onStatus(BillingManager.BillingStatus.Idle);
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
                KES.shared().getBillingManager().processPendingCredits();
            } else
            if (v == btCreditRefund) {
                ArrayList<String> pending = KES.shared().getBillingManager().getPendingOrders();
                if (pending == null || pending.size() == 0)
                    return;
                Intent i = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:"));
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
            KES.shared().getBillingManager().buyCredits(getActivity(),ci.productId);
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        purchasedAnimation.cancel();
        holderPurchased = null;
        purchasedAnimation = null;
        tvPurchased = null;
        lvPriceList = null;
        ivShopFooter = null;
        holderButtons = null;
        holderProgress = null;
        tvBillingStatus = null;
        billingProgress = null;
        btCreditRetry = null;
        btCreditRefund = null;
    }

    public class PriceAdapter extends ArrayAdapter<CreditItem> {

        private LayoutInflater inflater;
        private KES session;
        private int bestOffer = -1;

        public PriceAdapter() {
            super(getActivity(), 0, priceList);
            inflater = getActivity().getLayoutInflater();
            calcMinPrice();
        }


        private void calcMinPrice()
        {
            double minPrice = Double.MAX_VALUE;
            for (int i = 0; i < priceList.size(); i++)
            {
                CreditItem ci = priceList.get(i);
                double priceOne = ci.price / ci.quantity;
                if (priceOne < minPrice)
                {
                    minPrice = priceOne;
                    bestOffer = i;
                }
            }
        }

        @Override
        public void notifyDataSetChanged() {
            calcMinPrice();
            super.notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = PriceItem.createView(inflater, parent);
            PriceItem.updateView(convertView,getItem(position),currencyFormat, position == bestOffer);
            return convertView;
        }

    }

}
