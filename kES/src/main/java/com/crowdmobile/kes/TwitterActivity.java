package com.crowdmobile.kes;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by gadza on 2015.04.23..
 */
public class TwitterActivity extends Activity {

    public static int RESULT_ERROR = 1000;

    private WebView webView;
    String callbackUrl, authUrl;
    public static final String CALLBACK_URL = "callback_url";
    public static final String AUTH_URL = "extra_url";
    private View progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_twitter);
        authUrl = getIntent().getStringExtra(AUTH_URL);
        callbackUrl = getIntent().getStringExtra(CALLBACK_URL);

        if (null == authUrl || null == callbackUrl) {
            finish();
            return;
        }
        progress = findViewById(R.id.progress);
        webView = (WebView) findViewById(R.id.webView);
        webView.setWebViewClient(wvc);
        webView.loadUrl(authUrl);
    }

    WebViewClient wvc = new WebViewClient() {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progress.setVisibility(View.GONE);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            setResult(RESULT_ERROR);
            finish();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith(callbackUrl)) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(CALLBACK_URL, url);
                setResult(RESULT_OK, resultIntent);
                finish();
                return true;
            }
            return super.shouldOverrideUrlLoading(view, url);
        }
    };

}

