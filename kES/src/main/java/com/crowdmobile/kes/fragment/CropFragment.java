package com.crowdmobile.kes.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.crowdmobile.kes.R;
import com.crowdmobile.kes.widget.CropView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

/**
 * Created by gadza on 2015.03.25..
 */
public class CropFragment extends Fragment {


    public interface CropFragmentResult {
        public void onCroppedImage(Bitmap picture);
        public void onCropCanceled();
        public ImageView getTransfer();
    }

    ViewGroup holder;
    CropFragmentResult callback;
    CropView cropView;
    View btOK,btCancel;
    String filePath;
    BtnClick btnClick;
    View progress;
    boolean animated = false;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        callback = (CropFragmentResult)activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null;
    }

    public final static String TAG_FILEPATH = "filepath";
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        holder = (ViewGroup)inflater.inflate(R.layout.fragment_crop, container, false);
        btnClick = new BtnClick();
        cropView = (CropView)holder.findViewById(R.id.cropImage);
        progress = holder.findViewById(R.id.progress);
        //cropView.setCropViewListener(listener);
        btOK = holder.findViewById(R.id.btOK);
        btCancel = holder.findViewById(R.id.btCancel);
        btOK.setOnClickListener(btnClick);
        btCancel.setOnClickListener(btnClick);
        Bundle extras = getArguments();
        filePath = extras.getString(TAG_FILEPATH);

        Picasso.with(getActivity()).invalidate("file://" + filePath);
        Picasso.with(getActivity()).load("file://" + filePath).transform(transformation).into(cropView, picassoCallback);
        return holder;
    }

    Transformation transformation = new Transformation() {

        @Override public Bitmap transform(Bitmap source) {
            Display display = getActivity().getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int targetWidth = size.x;
            if (targetWidth > source.getWidth())
                targetWidth = source.getWidth();

            double aspectRatio = (double) source.getHeight() / (double) source.getWidth();
            int targetHeight = (int) (targetWidth * aspectRatio);
            Bitmap result = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, false);
            if (result != source) {
                // Same bitmap is returned if sizes are the same
                source.recycle();
            }
            return result;
        }

        @Override public String key() {
            return "transformation" + " desiredWidth";
        }
    };

    Callback picassoCallback = new Callback() {
        @Override
        public void onSuccess() {
            holder.removeView(progress);
            progress = null;
        }

        @Override
        public void onError() {
            holder.removeView(progress);
            progress = null;
        }
    };

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    /*
    CropView.CropViewListener listener = new CropView.CropViewListener() {
        @Override
        public void onLayout(int l, int t, int r, int b) {
            if (!animated)
            {
                final ImageView transfer = callback.getTransfer();
                int w = r - l;
                int h = b - t;
                int x = (w - transfer.getWidth()) / 2;
                int y = (h - transfer.getHeight()) / 2;
                animated = true;
                transfer.animate()
                        .translationXBy(x)
                        .translationYBy(y)
                        .setDuration(400)
                        .start();
            }
        }
    };
    */

    @Override
    public void onResume() {
        super.onResume();
    }

    class BtnClick implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (v == btCancel)
                callback.onCropCanceled();
            else if (v == btOK) {
                callback.onCroppedImage(cropView.getCroppedImage());
            }
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        holder = null;
        progress = null;
        cropView = null;
        btOK = null;
        btCancel = null;
        btnClick = null;
    }
}
