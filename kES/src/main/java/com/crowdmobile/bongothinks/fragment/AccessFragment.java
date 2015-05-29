package com.crowdmobile.bongothinks.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.crowdmobile.bongothinks.AccountActivity;
import com.crowdmobile.bongothinks.R;
import com.crowdmobile.bongothinks.util.PreferenceUtils;

/**
 * Created by gadza on 2015.03.27..
 */
public class AccessFragment extends Fragment {

    public static class AccessViewHolder {
        public View root;
        public TextView tvTitle;
        public TextView tvMessage;
        public TextView btAccess;

        public void setVisibility(int visibility)
        {
            root.setVisibility(visibility);
        }
    }

    public static AccessViewHolder getViews(View root)
    {
        AccessViewHolder result = new AccessViewHolder();
        result.root = root;
        result.tvTitle = (TextView)root.findViewById(R.id.tvAccessTitle);
        result.tvMessage = (TextView)root.findViewById(R.id.tvAccessMessage);
        result.btAccess = (TextView)root.findViewById(R.id.btAccessButton);
        return result;
    }

    AccessViewHolder holder;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_access, container, false);
        holder = getViews(result);
        holder.btAccess.setOnClickListener(onClickListener);
        return result;
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == holder.btAccess)
            {
                PreferenceUtils.setSkipLogin(getActivity(),false);
                AccountActivity.open(getActivity());
            }
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        holder = null;
    }
}
