package com.crowdmobile.kesapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import com.crowdmobile.kesapp.fragment.CropFragment;
import com.crowdmobile.kesapp.fragment.GalleryFragment;
import com.crowdmobile.kesapp.util.PreferenceUtils;
import com.urbanairship.analytics.Analytics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by gadza on 2015.03.20..
 */
public class PictureActivity extends ActionBarActivity implements GalleryFragment.GalleryFragmentListener,CropFragment.CropFragmentResult {
    static final int REQUEST_IMAGE_CAPTURE = 0x5678;
    private Toolbar toolbar;
    private ImageView transfer = null;
    private String cropPath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        //transfer = (ImageView)findViewById(R.id.imgTransfer);
        if (savedInstanceState != null)
            cropPath = savedInstanceState.getString(CropFragment.TAG_FILEPATH, null);
        if (cropPath == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.fragmentHolder, new GalleryFragment()).commit();
        } else
            crop(cropPath);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Analytics.activityStarted(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Analytics.activityStopped(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onCroppedImage(Bitmap picture) {
        cropPath = null;
        File tmpFile = null;
        FileOutputStream out = null;
        try {
            tmpFile = createComposedFile();
            out = new FileOutputStream(tmpFile);
            picture.compress(Bitmap.CompressFormat.JPEG, 75, out); // bmp is your Bitmap instance
            setResult(RESULT_OK);
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_storagewrite, Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            if (tmpFile != null && tmpFile.exists())
                tmpFile.delete();
            PreferenceUtils.setComposedPicture(this, null);
        }
        finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignored) {}
        }
        onBackPressed();
    }

    @Override
    public void onCropCanceled() {
        cropPath = null;
        getFragmentManager().beginTransaction()
            .setCustomAnimations(R.anim.oa_fade_in, R.anim.oa_fade_out)
            .replace(R.id.fragmentHolder,new GalleryFragment()).commit();
    }

    @Override
    public ImageView getTransfer() {
        return transfer;
    }


    private File createComposedFile() throws IOException {
        // Create an image file name
        File tmpFile = null;
        String tmp = PreferenceUtils.getComposedPicture(this);
        if (tmp != null) {
            tmpFile = new File(tmp);
            if (tmpFile.exists())
                return tmpFile;
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        tmpFile = File.createTempFile(
                "composed",  /* prefix */
                ".jpg",         /* suffix */
                getExternalCacheDir()      /* directory */
        );
        PreferenceUtils.setComposedPicture(this,tmpFile.getAbsolutePath());
        return tmpFile;
    }

    private File createCameraFile() throws IOException {
        // Create an image file name
        File tmpFile = null;
        String tmp = PreferenceUtils.getCameraPicture(this);
        if (tmp != null) {
            tmpFile = new File(tmp);
            if (tmpFile.exists())
                return tmpFile;
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        tmpFile = File.createTempFile(
                "cameratemp",  /* prefix */
                ".jpg",         /* suffix */
                getExternalCacheDir()      /* directory */
        );
        if (tmpFile == null || !tmpFile.exists())
            throw new IOException();
        PreferenceUtils.setCameraPicture(this,tmpFile.getAbsolutePath());
        return tmpFile;
    }

    @Override
    public void onPictureSelected(int location[], Drawable d, String path) {
        if (path == null)
        {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            try {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(createCameraFile()));
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            } catch (IOException ex) {
                Toast.makeText(this, R.string.error_storagewrite, Toast.LENGTH_SHORT).show();
            }
            return;
        }
        /*
        transfer.setImageDrawable(d);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(location[2],location[2]);
        lp.leftMargin = location[0];
        lp.topMargin = location[1];
        transfer.setLayoutParams(lp);
        transfer.setTranslationX(0);
        transfer.setTranslationY(0);
        */
        crop(path);
    }

    private void crop(String path)
    {
        cropPath = path;
        CropFragment cropFragment = new CropFragment();
        Bundle data = new Bundle();
        data.putString(CropFragment.TAG_FILEPATH, path);
        cropFragment.setArguments(data);
        getFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.oa_fade_in, R.anim.oa_fade_out)
                .replace(R.id.fragmentHolder, cropFragment)
                .addToBackStack("crop")
                .commit();
    }

    public class ImageFeed {
        public String path;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode != Activity.RESULT_OK)
                return;
            crop(PreferenceUtils.getCameraPicture(this));
        }
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            super.onBackPressed();
            overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (cropPath != null) outState.putString(CropFragment.TAG_FILEPATH, cropPath);
    }
}
