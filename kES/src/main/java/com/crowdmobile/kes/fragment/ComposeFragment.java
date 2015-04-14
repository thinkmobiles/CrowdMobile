package com.crowdmobile.kes.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.crowdmobile.kes.PictureActivity;
import com.crowdmobile.kes.R;
import com.crowdmobile.kes.util.PreferenceUtils;
import com.crowdmobile.kes.widget.NavigationBar;
import com.kes.Session;

import java.io.File;

/**
 * Created by gadza on 2015.03.11..
 */
public class ComposeFragment extends Fragment {
    static final int REQUEST_IMAGE_CAPTURE = 0x1234;
    private final static int maxlines = 4;

    boolean hasImage = false;
    EditText edMessage;
    View imgCamera;
    ImageView imgPreview;
    ImageView imgPost;
    View holderImage;
    View previewClose;
    View tvHint;
    TransitionDrawable transitionPost;
    int postEnabled = -1;
    boolean afterResume = false;

    @Override
    public void onResume() {
        super.onResume();
        afterResume = true;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        afterResume = false;
        View result = inflater.inflate(R.layout.fragment_compose,container,false);
        tvHint = result.findViewById(R.id.tvHint);
        holderImage = result.findViewById(R.id.holderImage);
        imgCamera = result.findViewById(R.id.imgCamera);
        imgCamera.setOnClickListener(onClickListener);
        imgPost = (ImageView)result.findViewById(R.id.imgPost);
        imgPost.setOnClickListener(onClickListener);
        transitionPost = (TransitionDrawable)imgPost.getDrawable();
        edMessage = (EditText)result.findViewById(R.id.edMessage);
        edMessage.addTextChangedListener(tv);
        edMessage.setOnKeyListener(kl);
        imgPreview = (ImageView)result.findViewById(R.id.imgPreview);
        previewClose = result.findViewById(R.id.ivPreviewClose);
        previewClose.setOnClickListener(onClickListener);
        edMessage.setText(PreferenceUtils.getComposeText(getActivity()));

        loadPic();
        return result;
    }

    /*
    public void hideInput()
    {
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edMessage.getWindowToken(), 0);
    }
    */

    @Override
    public void onPause() {
        super.onPause();
        PreferenceUtils.setComposeText(getActivity(), edMessage.getText().toString());
        //PreferenceUtils.setComposePicture(getActivity(), mCurrentPhoto != null ? mCurrentPhoto.getAbsolutePath() : null);
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == imgPost)
            {
                postQuestion();
            }
            else if (v == imgCamera)
                takePicture();
            else if (v == previewClose)
            {
                String filePath = PreferenceUtils.getComposedPicture(getActivity());
                if (filePath != null) {
                    PreferenceUtils.setComposedPicture(getActivity(), null);
                    new File(filePath).delete();
                    holderImage.setVisibility(View.GONE);
                }
                hasImage = false;
                checkPostEnabled();
            }
        }
    };


    private void takePicture()
    {
        // Save a file: path for use with ACTION_VIEW intents

        Intent intent = new Intent(getActivity(), PictureActivity.class);
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        getActivity().overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
        /*
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            try {
                createImageFile();
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mCurrentPhoto));
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            } catch (IOException ex) {
                Toast.makeText(getActivity(),R.string.error_storagewrite,Toast.LENGTH_SHORT).show();
            }
        }
        */
    }

    private void loadPic() {
        String photoPath = PreferenceUtils.getComposedPicture(getActivity());
        if (photoPath == null) {
            hasImage = false;
            checkPostEnabled();
            holderImage.setVisibility(View.GONE);
            return;
        }

        // Get the dimensions of the View
        int targetW = 1024;
        int targetH = 1024;
        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        Bitmap bitmap = BitmapFactory.decodeFile(photoPath, bmOptions);
        if (bitmap == null)
        {
            Toast.makeText(getActivity(),"Failed to load photo",Toast.LENGTH_SHORT).show();
        }
        imgPreview.setImageBitmap(bitmap);
        hasImage = true;
        checkPostEnabled();
        holderImage.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        holderImage = null;
        edMessage.removeTextChangedListener(tv);
        edMessage = null;
        imgCamera = null;
        imgPost = null;
        imgPreview = null;
        previewClose = null;
        tvHint = null;
        super.onDestroyView();
    }

    TextWatcher tv = new TextWatcher() {
        String text;

        public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        }

        public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            text = new String(arg0.toString());
        }

        public void afterTextChanged(Editable editable) {
            if (edMessage == null)
                return;
            int lineCount = edMessage.getLineCount();
            if (lineCount > maxlines) {
                edMessage.setText(text);
            }
            checkPostEnabled();
        }

        ;
    };

    private void checkPostEnabled()
    {
        int time = 0;
        if (afterResume)
            time = 300;

        boolean enabled = false;
        String s = edMessage.getText().toString();
        if (s != null && s.length() > 0) {
            enabled = true;
            tvHint.setVisibility(View.GONE);
        } else
            tvHint.setVisibility(View.VISIBLE);

        enabled |= hasImage;
        if (enabled && postEnabled != 1)
        {
            postEnabled = 1;
            transitionPost.setLevel(1);
            transitionPost.startTransition(time);
            return;
        }
        if (!enabled && postEnabled != -1)
        {
            postEnabled = -1;
            transitionPost.reverseTransition(time);
            return;
        }
    }

    View.OnKeyListener kl = new View.OnKeyListener() {

        public boolean onKey(View v, int keyCode, KeyEvent event) {

// if enter is pressed start calculating
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN){
                int editTextLineCount = ((EditText)v).getLineCount();
                if (editTextLineCount >= maxlines)
                    return true;
            }

            return false;
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode != Activity.RESULT_OK)
            {
                Toast.makeText(getActivity(),"Canceled",Toast.LENGTH_SHORT).show();
                imgPreview.setImageBitmap(null);
                return;
            }
            /*
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            imgPreview.setImageBitmap(imageBitmap);
            */
            loadPic();
        }
    }

    private void postQuestion()
    {
        String question = edMessage.getText().toString();
        String picturePath = PreferenceUtils.getComposedPicture(getActivity());
        if (question == null || question.length() == 0 && picturePath == null)
            return;

        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edMessage.getWindowToken(), 0);
        Session.getInstance(getActivity()).getFeedManager().postQuestion(question, picturePath);
        edMessage.setText("");
        NavigationBar nb = ((NavigationBar.NavigationCallback)getActivity()).getNavigationBar();
        nb.invalidateMyFeed();
        nb.navigateTo(NavigationBar.Attached.MyFeed);
    }

}
