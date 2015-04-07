package com.crowdmobile.kes;

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

import com.crowdmobile.kes.fragment.CropFragment;
import com.crowdmobile.kes.fragment.GalleryFragment;
import com.crowdmobile.kes.util.PreferenceUtils;

import net.hockeyapp.android.CrashManager;

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
    private File mCameraPicture;
    private File mComposedPicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        }
        super.onCreate(savedInstanceState);
        if (KesApplication.enableHockey) {
            CrashManager.register(this, KesApplication.HOCKEYAPP_ID);
        }
        setContentView(R.layout.activity_picture);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        //transfer = (ImageView)findViewById(R.id.imgTransfer);
        getFragmentManager().beginTransaction()
            .replace(R.id.fragmentHolder,new GalleryFragment()).commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCroppedImage(Bitmap picture) {
        FileOutputStream out = null;
        boolean success = false;
        try {
            createComposedFile();
            out = new FileOutputStream(mComposedPicture);
            picture.compress(Bitmap.CompressFormat.JPEG, 75, out); // bmp is your Bitmap instance
            success = true;
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_storagewrite, Toast.LENGTH_SHORT).show();
        }
        finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignored) {}
        }
        if (!success)
        {
            mCameraPicture.delete();
            PreferenceUtils.setComposedPicture(this,null);
        }
        setResult(success ? RESULT_OK : RESULT_CANCELED);
        onBackPressed();
    }

    @Override
    public void onCropCanceled() {
        getFragmentManager().beginTransaction()
            .setCustomAnimations(R.anim.oa_fade_in, R.anim.oa_fade_out)
            .replace(R.id.fragmentHolder,new GalleryFragment()).commit();
    }

    @Override
    public ImageView getTransfer() {
        return transfer;
    }


    private void createComposedFile() throws IOException {
        // Create an image file name
        if (mComposedPicture != null && mComposedPicture.exists())
            return;
        String tmp = PreferenceUtils.getComposedPicture(this);
        if (tmp != null) {
            mComposedPicture = new File(tmp);
            if (mComposedPicture.exists())
                return;
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        mComposedPicture = File.createTempFile(
                "composed",  /* prefix */
                ".jpg",         /* suffix */
                getExternalCacheDir()      /* directory */
        );
        PreferenceUtils.setComposedPicture(this,mComposedPicture.getAbsolutePath());
    }

    private void createCameraFile() throws IOException {
        // Create an image file name
        if (mCameraPicture != null && mCameraPicture.exists())
            return;
        String tmp = PreferenceUtils.getCameraPicture(this);
        if (tmp != null) {
            mCameraPicture = new File(tmp);
            if (mCameraPicture.exists())
                return;
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        mCameraPicture = File.createTempFile(
                "cameratemp",  /* prefix */
                ".jpg",         /* suffix */
                getExternalCacheDir()      /* directory */
        );
        PreferenceUtils.setCameraPicture(this,mCameraPicture.getAbsolutePath());
    }

    @Override
    public void onPictureSelected(int location[], Drawable d, String path) {
        if (path == null)
        {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            try {
                createCameraFile();
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mCameraPicture));
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
            crop(mCameraPicture.getAbsolutePath());
        }
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            super.onBackPressed();
            overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
        }
    }
}
