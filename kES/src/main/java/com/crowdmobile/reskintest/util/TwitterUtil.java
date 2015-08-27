package com.crowdmobile.reskintest.util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RelativeLayout;

import com.crowdmobile.reskintest.AppCfg;
import com.crowdmobile.reskintest.MainActivity;
import com.crowdmobile.reskintest.R;
import com.crowdmobile.reskintest.TwitterActivity;
import com.crowdmobile.reskintest.fragment.SocialFragment;
import com.crowdmobile.reskintest.model.PostOwner;
import com.crowdmobile.reskintest.model.SocialPost;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.OAuth2Token;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Created on 9/3/14.
 */
public class TwitterUtil {
    public static long KARDASHIAN_ID = 25365536;
    private static final String TAG = "TwitterManager";
    private final int REQUESTCODE = 65535;
    private int paging = 1;

    public interface TwitterLoginCallback {

        void onSuccess(String token, String secret, long uid);

        void onFailed(String message);

        void onCanceled();
    }

    private static final String TOKEN_KEY = "TOKEN_KEY";
    private static final String UID_KEY = "UID_KEY";

    private static final String TWITTER_OAUTH_VERIFIER = "oauth_verifier";
    private static final String TWITTER_OAUTH_TOKEN = "oauth_token";
    private static final String TWITTER_OAUTH_DENIED = "denied";

    private final String callbackUrl;

    public Twitter getTwitter() {
        return twitter;
    }

    private Twitter twitter;
    private Context context;

    private AsyncTask oAuthTask;
    private AsyncTask tokenTask;
    private static TwitterUtil sInstance;
    private SocialFragment socialFragment;

    public void clearPaging() {
        paging = 1;
    }

    public static TwitterUtil getInstance(Context context) {
        if (sInstance == null)
            sInstance = new TwitterUtil(context);
        return sInstance;
    }

    public LoginManager getLoginManager(TwitterLoginCallback callback) {
        return new LoginManager(callback);
    }

    public void logout() {
        twitter.setOAuthAccessToken(null);
    }

    public class LoginManager {
        private TwitterLoginCallback callback;
        private RelativeLayout.LayoutParams params;
        private String token = null;
        private String secret = null;

        public LoginManager(TwitterLoginCallback callback) {
            this.callback = callback;
            params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        }

        public void handleLoginResponse(String url) {
            Uri uri = Uri.parse(url);

            if (uri != null && ((Object) uri).toString().startsWith(callbackUrl)) {
                String denied = uri.getQueryParameter(TWITTER_OAUTH_DENIED);
                String t = uri.getQueryParameter(TWITTER_OAUTH_TOKEN);
                String verifier = uri.getQueryParameter(TWITTER_OAUTH_VERIFIER);
                if (denied != null) {
                    callback.onCanceled();
                    return;
                }
                if (verifier == null) {
                    return;
                }
                tokenTask = new AsyncTask<String, Void, AccessToken>() {

                    @Override
                    protected AccessToken doInBackground(String... params) {
                        AccessToken accessToken = null;
                        try {
                            accessToken = twitter.getOAuthAccessToken(params[0]);
                        } catch (TwitterException e) {
                            e.printStackTrace();
                        }
                        return accessToken;
                    }

                    @Override
                    protected void onPostExecute(AccessToken accessToken) {
                        if (callback != null && !isCancelled()) {
                            if (accessToken != null) {
                                twitter.setOAuthAccessToken(accessToken);
                                callback.onSuccess(accessToken.getToken(), accessToken.getTokenSecret(), accessToken.getUserId());
                            } else {
                                callback.onFailed("Login to twitter failed");
                            }
                        }
                    }
                }.execute(verifier);
            }
        }

        public void login(final Activity activity) {
            if (oAuthTask != null)
                return;
            twitter.setOAuthAccessToken(null);
            oAuthTask = new AsyncTask<Void, Void, RequestToken>() {
                @Override
                protected RequestToken doInBackground(Void... params) {
                    RequestToken result = null;
                    try {
                        result = twitter.getOAuthRequestToken(callbackUrl);
                    } catch (TwitterException e) {
                        e.printStackTrace();
                    }
                    return result;
                }

                @Override
                protected void onPostExecute(RequestToken requestToken) {
                    oAuthTask = null;
                    if (isCancelled())
                        callback.onCanceled();
                    if (requestToken == null)
                        callback.onFailed("Can't connect to twitter");
                    else {
                        Intent intent = new Intent(activity, TwitterActivity.class);
                        intent.putExtra(TwitterActivity.AUTH_URL, requestToken.getAuthenticationURL());
                        intent.putExtra(TwitterActivity.CALLBACK_URL, callbackUrl);
                        activity.startActivityForResult(intent, REQUESTCODE);
                    }
                }
            }.execute();
        }

        public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == REQUESTCODE) {
                if (resultCode == TwitterActivity.RESULT_ERROR) {
                    callback.onFailed("Can't login");
                    return true;
                }
                if (resultCode == Activity.RESULT_CANCELED) {
                    callback.onCanceled();
                    return true;
                }
                if (resultCode == Activity.RESULT_OK) {
                    String url = data.getStringExtra(TwitterActivity.CALLBACK_URL);
                    handleLoginResponse(url);
                    return true;
                }
            }
            return false;
        }

    }

    private TwitterUtil(Context context) {
        this.context = context;
        callbackUrl = context.getString(R.string.callback_url_scheme) + "://" +
                context.getString(R.string.callback_url_host) + "?force_login=true";
        Configuration configuration = new ConfigurationBuilder()
                .setOAuthConsumerKey(AppCfg.TwitterKey)
                .setOAuthConsumerSecret(AppCfg.TwitterSecret).build();

        twitter = new TwitterFactory(configuration).getInstance();
    }


    public void release() {
        dropTasks();
        context = null;
    }

    public boolean isAuthenticated() {
        try {
            return twitter.getOAuthAccessToken() != null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void clearAccessToken() {
        twitter.setOAuthAccessToken(null);
    }


    public void share(String text, String photoUrl) {
        new AsyncTask<String, Void, Status>() {
            @Override
            protected twitter4j.Status doInBackground(String... params) {
                InputStream imageInput = getStreamFor(params[1]);

                try {
                    StatusUpdate su = new StatusUpdate(params[0]);
                    if (imageInput != null) {
                        su.setMedia("image", imageInput);
                    } else {
                        Log.d(TAG, "input stream is NULL");
                    }
                    return twitter.updateStatus(su);
                } catch (TwitterException e) {
                    e.printStackTrace();
                } finally {
                    if (imageInput != null) {
                        try {
                            imageInput.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(twitter4j.Status status) {
                Log.d(TAG, "share -> " + (status != null ? status.getText() : "error"));
            }
        }.execute(text, photoUrl);
    }

    private InputStream getStreamFor(String url) {
        InputStream input = null;
        /*
        if(url != null) {
            try {
                RestClient restClient = new RestClient(context);
                ApiResponse resp = restClient.doGet(url);
                input = resp.body.getContent();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        */
        return input;
    }

    private void dropTasks() {
        if (oAuthTask != null) {
            oAuthTask.cancel(true);
            oAuthTask = null;
        }
        if (tokenTask != null) {
            tokenTask.cancel(true);
            tokenTask = null;
        }
    }

    public Twitter getTwitterWithoutAuth(){
        Twitter twitter;
        Configuration configuration = new ConfigurationBuilder()
                .setOAuthConsumerKey(AppCfg.TwitterKey)
                .setOAuthConsumerSecret(AppCfg.TwitterSecret)
                .setOAuthAccessToken(AppCfg.TwitterAccessToken)
                .setOAuthAccessTokenSecret(AppCfg.TwitterAccessTokenSecret)
                .setApplicationOnlyAuthEnabled(true)
                .build();
        twitter = new TwitterFactory(configuration).getInstance();
        try {
            OAuth2Token token = twitter.getOAuth2Token();
        } catch (TwitterException e) {
            e.printStackTrace();
        }
        return twitter;
    }

    public ArrayList<SocialPost> getTwitterStatuses() {

        ArrayList<SocialPost> socialPosts = new ArrayList<>();

        try {
            List<Status> statuses;
            Paging paging = new Paging(this.paging, 10);

            statuses = getTwitterWithoutAuth().getUserTimeline(KARDASHIAN_ID, paging);

            for (Status status : statuses) {
                String image_data = null;

                if (status.getMediaEntities() != null && status.getMediaEntities().length != 0)
                    if (status.getMediaEntities()[0].getType().equals("photo"))
                        image_data = status.getMediaEntities()[0].getMediaURLHttps();

                String date = DateParser.dateParce(status.getCreatedAt());
                String str = status.getText();
                String desc = "";

                if (str.lastIndexOf("http") == 0)
                    desc = str;
                else
                    desc = str.substring(0, ((str.lastIndexOf("http") != -1) ? str.lastIndexOf("http") : str.length() - 1));

                PostOwner postOwner = new PostOwner(String.valueOf(status.getUser().getId()), status.getUser().getScreenName(), status.getUser().getOriginalProfileImageURLHttps());
                SocialPost socialPost = new SocialPost(String.valueOf(status.getId()), desc, image_data, date, postOwner);
                socialPosts.add(socialPost);
            }
        } catch (TwitterException te) {
            te.printStackTrace();
            System.out.println("Failed to get timeline: " + te.getMessage());
        }

        return socialPosts;
    }

    public void executeGetPosts(SocialFragment socialFragment, int paging){
        this.socialFragment = socialFragment;
        this.paging = paging;
        new AsyncTwitterPosts().execute();
    }

    private class AsyncTwitterPosts extends AsyncTask<Void, Void, ArrayList<SocialPost>> {

        @Override
        protected void onPostExecute(ArrayList<SocialPost> result) {

//            socialFragment.setCallbackData(result);
            socialFragment.clearFeed();
            socialFragment.updateFeedTwitter(result);
//            progressDialog.dismiss();
//            dialog.dismiss();
            socialFragment.cancelRefresh();
        }

        @Override
        protected ArrayList<SocialPost> doInBackground(Void... params) {
            return  getTwitterStatuses();
        }

    }


}
