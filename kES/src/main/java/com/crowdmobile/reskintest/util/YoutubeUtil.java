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
    private static final String API_KEY = "AIzaSyDxcOyknQ5yWoK60YODjRdVkD_t5sSLQJs";
    private static final String CHANNELID = "UCEeYPJ1GSYWf0RXS8nARHjg";
    private String nextPageToken;
    private AsynkYoutubeFeed youtubeTask;
    private MainActivity activity;
    private SocialFragment fragment;

    public YoutubeUtil(MainActivity activity) {
        this.activity = activity;
    }

    public void executeGetPosts(SocialFragment fragment){
        this.fragment = fragment;
        youtubeTask = new AsynkYoutubeFeed();
        youtubeTask.execute(
                "https://www.googleapis.com/youtube/v3/channels?" +
                        "part=snippet" +
                        "&id=" + CHANNELID +
                        "&fields=items%2Fsnippet" +
                        "&key=" + API_KEY,
                "https://www.googleapis.com/youtube/v3/search?" +
                        "part=snippet" +
                        "&maxResults=20" +
                        "&channelId=" + CHANNELID +
                        "&order=date" +
                        "&fields=items(id%2Csnippet)%2CnextPageToken" +
                        "&key=" + API_KEY
        );
        try {
            fragment.clearFeed();
            fragment.updateFeedYoutube(youtubeTask.get());
//            fragment.setCallbackData(youtubeTask.get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void getNextPosts(SocialFragment fragment){
        this.fragment = fragment;
        nextPageToken = youtubeTask.getPageToken();
        youtubeTask = new AsynkYoutubeFeed();
        if(nextPageToken != null) {
            youtubeTask.execute(
                    "https://www.googleapis.com/youtube/v3/channels?" +
                            "part=snippet" +
                            "&id=" + CHANNELID +
                            "&fields=items%2Fsnippet" +
                            "&key=" + API_KEY,
                    "https://www.googleapis.com/youtube/v3/search?" +
                            "part=snippet" +
                            "&maxResults=20" +
                            "&channelId=" + CHANNELID +
                            "&order=date" +
                            "&pageToken=" + nextPageToken +
                            "&fields=items(id%2Csnippet)%2CnextPageToken" +
                            "&key=" + API_KEY
            );
            try {
                fragment.updateFeedYoutube(youtubeTask.get());
//                fragment.setCallbackData(youtubeTask.get());
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
            Gson gson = new GsonBuilder().create();
            HttpResponse response;
            try {
                HttpGet httpGet = new HttpGet(params[0]);
                response = httpclient.execute(httpGet);

                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                YoutubeResponse channelResponse = gson.fromJson(reader, YoutubeResponse.class);
                reader.close();

                httpGet.setURI(URI.create(params[1]));
                response = httpclient.execute(httpGet);

                reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                YoutubeResponse feedResponse = gson.fromJson(reader, YoutubeResponse.class);
                reader.close();

                pageToken = feedResponse.getNextPageToken();
                List<YoutubeResponse.Items> list = feedResponse.getItems();
                YoutubeResponse.Snippet channelSnippet = channelResponse.getItems().get(0).getSnippet();
                for(YoutubeResponse.Items item : list){
                    PostOwner postOwner = new PostOwner(
                            channelSnippet.getThumbnails().getDefault().getUrl(),
                            channelSnippet.getTitle(),
                            channelSnippet.getThumbnails().getHigh().getUrl()
                    );
                    SocialPost socialPost = new SocialPost(
                            null,
                            item.getSnippet().getTitle(),
                            item.getSnippet().getThumbnails().getHigh().getUrl(),
                            DateParser.dateParce(DateParser.getDateFormatYoutube(item.getSnippet().getPublishedAt())),
                            postOwner
                    );
                    socialPosts.add(socialPost);
                }


            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "getYoutubeResponse error");
            }

            return socialPosts;
        }

        @Override
        protected void onPostExecute(ArrayList<SocialPost> socialPosts) {
            super.onPostExecute(socialPosts);
            fragment.cancelRefresh();
        }
    }
}