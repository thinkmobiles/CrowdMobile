package com.crowdmobile.reskintest.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.crowdmobile.reskintest.R;
import com.crowdmobile.reskintest.util.Graphic;
import com.crowdmobile.reskintest.widget.CropView;

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
    Point displaySize;
    boolean animated = false;
    LoadImage loadImage;

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

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        displaySize = new Point();
        display.getSize(displaySize);
        loadImage = new LoadImage();
        loadImage.execute(filePath);
        /*
        Picasso.with(getActivity()).invalidate("file://" + filePath);
        Picasso.with(getActivity()).load("file://" + filePath).fit().transform(transformation).into(cropView, picassoCallback);
        */
        return holder;
    }

    class LoadImage extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String[] params) {
            return Graphic.decodeBitmap(params[0], displaySize.x, displaySize.y, true);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            holder.removeView(progress);
            progress = null;
            if (result == null)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder
                        .setTitle(R.string.crop_title)
                        .setMessage(R.string.error_picture)
                        .setNegativeButton(android.R.string.cancel, new Dialog.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                callback.onCropCanceled();
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                callback.onCropCanceled();
                            }
                        })
                        .show();
            }
            cropView.setBitmap(result);
        }
    };


    /*
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
    */

    /*
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
    */

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
        if (loadImage != null) {
            loadImage.cancel(true);
            loadImage = null;
        }
        holder = null;
        progress = null;
        cropView = null;
        btOK = null;
        btCancel = null;
        btnClick = null;
    }
}
