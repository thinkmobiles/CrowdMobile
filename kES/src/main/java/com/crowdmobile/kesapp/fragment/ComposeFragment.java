package com.crowdmobile.kesapp.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crowdmobile.kesapp.MainActivityInterface;
import com.crowdmobile.kesapp.PictureActivity;
import com.crowdmobile.kesapp.R;
import com.crowdmobile.kesapp.SuggestionDialog;
import com.crowdmobile.kesapp.util.PreferenceUtils;
import com.crowdmobile.kesapp.widget.NavigationBar;
import com.kes.FeedManager;
import com.kes.KES;
import com.kes.model.PhotoComment;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

/**
 * Created by gadza on 2015.03.11..
 */
public class ComposeFragment extends Fragment {
    static final int REQUEST_IMAGE_CAPTURE = 0x1234;
    private final static int maxlines = 4;

    boolean hasImage = false;
    EditText edMessage;
    ImageView imgPrivate;
    ImageView imgPreview;
    ImageView imgPost;
    View holderSuggestion;
    View holderPostVisibility;
    TextView tvSuggestion;
    TextView tvVisibility;

//    View holderImage;
    View previewClose;
    View tvHint;
    TransitionDrawable transitionPost;
    int postEnabled = -1;
    boolean afterResume = false;
    boolean isPrivate;
    Handler mHandler;
    CharsetEncoder latinEncoder;
    SuggestionDialog suggestionDialog;
    MainActivityInterface mainActivityInterface;
    LayoutInflater inflater;
    String[] suggestions;

    @Override
    public void onResume() {
        super.onResume();
        afterResume = true;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mainActivityInterface = (MainActivityInterface)activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mainActivityInterface = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        latinEncoder = Charset.forName("ISO-8859-1").newEncoder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler = null;
        latinEncoder = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.inflater = inflater;

        setHasOptionsMenu(true);
        afterResume = false;
        View result = inflater.inflate(R.layout.fragment_compose,container,false);
        tvHint = result.findViewById(R.id.tvHint);
//        holderImage = result.findViewById(R.id.holderImage);
        imgPost = (ImageView)result.findViewById(R.id.imgPost);
        imgPost.setOnClickListener(onClickListener);
        transitionPost = (TransitionDrawable)imgPost.getDrawable();
        edMessage = (EditText)result.findViewById(R.id.edMessage);
        edMessage.addTextChangedListener(tv);
        edMessage.setOnKeyListener(kl);
        imgPrivate = (ImageView)result.findViewById(R.id.imgPrivate);
        imgPreview = (ImageView)result.findViewById(R.id.imgPreview);
        imgPreview.setOnClickListener(onClickListener);
        previewClose = result.findViewById(R.id.ivPreviewClose);
        previewClose.setOnClickListener(onClickListener);
        holderSuggestion = result.findViewById(R.id.holderPostSuggestion);
        holderPostVisibility = result.findViewById(R.id.holderPostVisibility);
        holderSuggestion.setOnClickListener(onClickListener);
        holderSuggestion.setVisibility(suggestions != null ? View.VISIBLE : View.GONE);
        holderPostVisibility.setOnClickListener(onClickListener);
        tvSuggestion = (TextView)result.findViewById(R.id.tvSuggestion);
        tvSuggestion.setPaintFlags(tvSuggestion.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvVisibility = (TextView)result.findViewById(R.id.tvVisibility);
        tvVisibility.setPaintFlags(tvVisibility.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        edMessage.setText(PreferenceUtils.getComposeText(getActivity()));
        InputFilter filter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end,
                                       Spanned dest, int dstart, int dend) {
                    if (latinEncoder.canEncode(source))
                        return null;
                    else {
                        Toast.makeText(getActivity(),R.string.compose_invalidchar,Toast.LENGTH_SHORT).show();
                        return "";
                    }
            }
        };
        edMessage.setFilters(new InputFilter[] { filter });

        isPrivate = PreferenceUtils.getComposePrivate(getActivity());
        updatePrivate();
        loadPic();
        KES.shared().getFeedManager().registerOnChangeListener(onFeedChange);
        KES.shared().getFeedManager().getSuggestedQuestions();
        return result;
    }


    private void updatePrivate()
    {
        if (isPrivate) {
            imgPrivate.setImageResource(R.drawable.ic_private);
            tvVisibility.setText(R.string.compose_private);
        }
        else {
            imgPrivate.setImageResource(R.drawable.ic_public);
            tvVisibility.setText(R.string.compose_public);
        }
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
        PreferenceUtils.setComposePrivate(getActivity(), isPrivate);
        //PreferenceUtils.setComposePicture(getActivity(), mCurrentPhoto != null ? mCurrentPhoto.getAbsolutePath() : null);
    }

    private void showSuggestions()
    {
        if (suggestions == null)
            return;

        if (suggestionDialog == null)
            suggestionDialog = new SuggestionDialog(getActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        /*
        final ArrayList<String> suggestions = new ArrayList<String>();
        suggestions.add("Here we can place some predefined questions from Bongo for users to choose from.This will make it easier to ask Bongo a question");
        suggestions.add("Another question to ask Bongo. Predefined like the previous one and the next.");
        suggestions.add("Bongo knows a lot.And when people select a question here they save themselved of the hassle of typing a question themselves.");
        suggestions.add("Bongo knows a lot.And when people select a question here they save themselved of the hassle of typing a question themselves.");
        */
        suggestionDialog.setItems(suggestions);
        suggestionDialog.setOnItemSelectedListener(new SuggestionDialog.ItemSelectedListener() {
            @Override
            public void onItemSelected(String item) {
                edMessage.setText(item);
                if (!TextUtils.isEmpty(item))
                    edMessage.setSelection(item.length());
                suggestionDialog.hide();

            }
        });
        suggestionDialog.show();
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == holderPostVisibility)
            {
                isPrivate = !isPrivate;
                updatePrivate();
            }
            else if (v == holderSuggestion)
            {
                showSuggestions();
            }
            else if (v == imgPost)
            {
                preparePost();
            }
            else if (v == imgPreview)
                takePicture();
            else if (v == previewClose)
                previewClose(true);
        }
    };

    private void previewClose(boolean deleteFile)
    {
        String filePath = PreferenceUtils.getComposedPicture(getActivity());
        if (filePath != null) {
            PreferenceUtils.setComposedPicture(getActivity(), null);
            if (deleteFile)
                new File(filePath).delete();
            clearPicture();
        }
        hasImage = false;
        checkPostEnabled();
    }

    private void clearPicture()
    {
        imgPreview.setImageResource(R.drawable.ic_takephoto);
        previewClose.setVisibility(View.GONE);
    }

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
            clearPicture();
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
        previewClose.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        edMessage.removeTextChangedListener(tv);
        edMessage = null;
        imgPreview = null;
        imgPost = null;
        imgPrivate = null;
        imgPreview = null;
        holderPostVisibility = null;
        tvVisibility = null;
        holderSuggestion = null;
        tvSuggestion = null;
        previewClose = null;
        tvHint = null;
        KES.shared().getFeedManager().unRegisterOnChangeListener(onFeedChange);
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
                //previewClose(true);
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

    private void preparePost()
    {
        final String question = edMessage.getText().toString();
        final String picturePath = PreferenceUtils.getComposedPicture(getActivity());
        if (question == null || question.length() == 0 && picturePath == null)
            return;
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(edMessage.getWindowToken(), 0);

        if (TextUtils.isEmpty(picturePath)) {
            pictureWarning();
            return;
        }
        postQuestion();
    }

    private void postQuestion()
    {
        final String question = edMessage.getText().toString();
        final String picturePath = PreferenceUtils.getComposedPicture(getActivity());

        if (KES.shared().getAccountManager().getUser().balance < 1) {
            mainActivityInterface.showNoCreditDialog();
            return;
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                edMessage.setText("");
                previewClose(false);
                NavigationBar nb = ((NavigationBar.NavigationCallback) getActivity()).getNavigationBar();
                nb.navigateTo(NavigationBar.Attached.MyFeed);
                KES.shared().getFeedManager().postQuestion(isPrivate, question, picturePath);
            }
        },250);
    }

    void pictureWarning()
    {
        //PreferenceUtils.setSkipNoPic(getActivity(),false);
        if (PreferenceUtils.getSkipNoPic(getActivity()))
        {
            postQuestion();
            return;
        }
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        View dialogView = inflater.inflate(R.layout.dialog_nopicture,null);

        CheckBox cb = (CheckBox)dialogView.findViewById(R.id.cbDontShowAgain);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
               if (isChecked)
                   PreferenceUtils.setSkipNoPic(buttonView.getContext(),true);
            }
        });

        alertDialogBuilder.setTitle(R.string.compose_nopic_title);
        alertDialogBuilder.setMessage(R.string.compose_nopic_message);

        // set dialog message
        alertDialogBuilder
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        postQuestion();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    FeedManager.OnChangeListener onFeedChange = new FeedManager.OnChangeListener() {
        @Override
        public void onPageLoaded(FeedManager.FeedWrapper wrapper) {

        }

        @Override
        public void onSuggestedQuestions(String[] questions, Exception error) {
            suggestions = questions;
            holderSuggestion.setVisibility(suggestions != null ? View.VISIBLE : View.GONE);
        }

        @Override
        public void onMarkAsReadResult(PhotoComment photoComment, Exception error) {

        }

        @Override
        public void onLikeResult(int questionID, int commentID, Exception error) {

        }

        @Override
        public void onReportResult(int questionID, Exception error) {

        }

        @Override
        public void onDeleteResult(int questionID, int commentID, Exception error) {

        }

        @Override
        public void onMarkAsPrivateResult(PhotoComment photoComment, Exception error) {

        }

        @Override
        public void onPosting(PhotoComment photoComment) {

        }

        @Override
        public void onPostResult(PhotoComment photoComment, Exception error) {

        }

        @Override
        public void onInsufficientCredit() {

        }
    };

}
