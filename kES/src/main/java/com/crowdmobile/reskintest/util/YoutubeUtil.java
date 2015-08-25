package com.crowdmobile.reskintest.util;

import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.crowdmobile.reskintest.MainActivity;
import com.crowdmobile.reskintest.fragment.SocialFragment;
import com.crowdmobile.reskintest.model.PostOwner;
import com.crowdmobile.reskintest.model.SocialPost;
import com.crowdmobile.reskintest.model.YoutubeResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by samson on 22.08.15.
 */
public class YoutubeUtil {

    private static final String TAG = YoutubeUtil.class.getSimpleName();
    private static final String CHANNELID = "UCEeYPJ1GSYWf0RXS8nARHjg";
    private ArrayList<SocialPost> socialPosts;
    private String nextPageToken;
    private AsynkYoutubeFeed youtubeTask;
    private MainActivity activity;

    public YoutubeUtil(MainActivity activity) {
        this.activity = activity;
    }

    public void initYoutube(){
        if(PreferenceUtils.getYoutubeToken(activity) == null) {
            android.accounts.AccountManager.get(activity).getAuthTokenByFeatures(
                    "com.google",
                    "oauth2:https://gdata.youtube.com",
                    null,
                    activity,
                    null,
                    null,
                    new AccountManagerCallback<Bundle>() {
                        @Override
                        public void run(AccountManagerFuture<Bundle> future) {
                            try {
                                PreferenceUtils.setYoutubeToken(activity.getApplicationContext(), future.getResult().getString(android.accounts.AccountManager.KEY_AUTHTOKEN));
                            } catch (Exception e) {
                                Log.e(TAG, e.getClass().getSimpleName() + ": " + e.getMessage());
                            }
                        }
                    },
                    null
            );
        }
    }

    public void executeGetPosts(SocialFragment fragment){
        socialPosts = new ArrayList<>();
        youtubeTask = new AsynkYoutubeFeed();
        youtubeTask.execute(
                "https://www.googleapis.com/youtube/v3/channels?" +
                        "part=snippet" +
                        "&id=" + CHANNELID +
                        "&fields=items%2Fsnippet" +
                        "&access_token=" + PreferenceUtils.getYoutubeToken(activity),
                "https://www.googleapis.com/youtube/v3/search?" +
                        "part=snippet" +
                        "&maxResults=20" +
                        "&channelId=" + CHANNELID +
                        "&order=date" +
                        "&fields=items(id%2Csnippet)%2CnextPageToken" +
                        "&access_token=" + PreferenceUtils.getYoutubeToken(activity)
        );
        try {
            socialPosts.addAll(youtubeTask.get());
            fragment.setCallbackData(socialPosts);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void getNextPosts(SocialFragment fragment){
        nextPageToken = youtubeTask.getPageToken();
        youtubeTask = new AsynkYoutubeFeed();
        if(nextPageToken != null) {
            youtubeTask.execute(
                    "https://www.googleapis.com/youtube/v3/channels?" +
                            "part=snippet" +
                            "&id=" + CHANNELID +
                            "&fields=items%2Fsnippet" +
                            "&access_token=" + PreferenceUtils.getYoutubeToken(activity),
                    "https://www.googleapis.com/youtube/v3/search?" +
                            "part=snippet" +
                            "&maxResults=20" +
                            "&channelId=" + CHANNELID +
                            "&order=date" +
                            "&pageToken=" + nextPageToken +
                            "&fields=items(id%2Csnippet)%2CnextPageToken" +
                            "&access_token=" + PreferenceUtils.getYoutubeToken(activity)
            );
            try {
                socialPosts.addAll(youtubeTask.get());
                fragment.setCallbackData(socialPosts);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public class AsynkYoutubeFeed extends AsyncTask<String, Void, ArrayList<SocialPost>> {
        private String pageToken;

        public String getPageToken() {
            return pageToken;
        }

        @Override
        protected ArrayList<SocialPost> doInBackground(String... params) {
            ArrayList<SocialPost> socialPosts = new ArrayList<>();
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            try {
                HttpGet httpGet = new HttpGet(params[0]);
                response = httpclient.execute(httpGet);

                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                Gson gson = new GsonBuilder().create();
                YoutubeResponse channelResponse = gson.fromJson(reader, YoutubeResponse.class);
                reader.close();

                httpGet.setURI(URI.create(params[1]));
                response = httpclient.execute(httpGet);

                reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                YoutubeResponse feedResponse = gson.fromJson(reader, YoutubeResponse.class);
                reader.close();

                pageToken = feedResponse.getNextPageToken();
                List<YoutubeResponse.Items> list = feedResponse.getItems();
                for(YoutubeResponse.Items item : list){
                    PostOwner postOwner = new PostOwner(
                            null,
                            channelResponse.getItems().get(0).getSnippet().getTitle(),
                            channelResponse.getItems().get(0).getSnippet().getThumbnails().getHigh().getUrl()
                    );
                    SocialPost socialPost = new SocialPost(
                            null,
                            "",
                            item.getSnippet().getThumbnails().getHigh().getUrl(),
                            item.getSnippet().getPublishedAt(),
                            postOwner
                    );
                    socialPosts.add(socialPost);
                }


            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "get YoutubeResponse error");
            }

            return socialPosts;
        }

        @Override
        protected void onPostExecute(ArrayList<SocialPost> socialPosts) {
            super.onPostExecute(socialPosts);
        }
    }
}
