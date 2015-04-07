package com.crowdmobile.kes.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.crowdmobile.kes.AccountActivity;
import com.crowdmobile.kes.R;
import com.crowdmobile.kes.util.PreferenceUtils;

/**
 * Created by gadza on 2015.03.27..
 */
public class NotRegisteredFragment extends Fragment {

    View btGetStarted;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_access, container, false);
        btGetStarted = result.findViewById(R.id.btGetStarted);
        btGetStarted.setOnClickListener(onClickListener);
        return result;
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btGetStarted)
            {
                PreferenceUtils.setSkipLogin(getActivity(),false);
                AccountActivity.open(getActivity());
            }
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        btGetStarted = null;
    }
}
