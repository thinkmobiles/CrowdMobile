package com.crowdmobile.reskintest.util;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;

import com.crowdmobile.reskintest.AppCfg;
import com.crowdmobile.reskintest.MainActivity;
import com.crowdmobile.reskintest.fragment.SocialFragment;
import com.crowdmobile.reskintest.model.PostOwner;
import com.crowdmobile.reskintest.model.SocialPost;
import com.crowdmobile.reskintest.model.YoutubeResponse;
import com.facebook.Response;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by samson on 22.08.15.
 */
public class YoutubeUtil {

    public static final String API_KEY = "AIzaSyDxcOyknQ5yWoK60YODjRdVkD_t5sSLQJs";
    private static final String TAG = YoutubeUtil.class.getSimpleName();
    private static final String CHANNELID = "UC1UHSsR2ZL52UExsJAQaGzg";
    private static final String HTTPS_GET_CHANNEL = "https://www.googleapis.com/youtube/v3/channels?" +
            "part=snippet" +
            "&id=" + CHANNELID +
            "&fields=items%2Fsnippet" +
            "&key=" + API_KEY;

    private AsynkYoutubeFeed youtubeTask;
    private MainActivity activity;
    private SocialFragment fragment;

    public void executeGetPosts(SocialFragment fragment){
        this.fragment = fragment;
        fragment.clearFeed();
        youtubeTask = new AsynkYoutubeFeed();
        youtubeTask.execute(HTTPS_GET_CHANNEL,
                "https://www.googleapis.com/youtube/v3/search?" +
                        "part=snippet" +
                        "&maxResults=10" +
                        "&channelId=" + CHANNELID +
                        "&order=date" +
                        "&fields=items(id%2Csnippet)%2CnextPageToken" +
                        "&key=" + API_KEY
        );
    }

    public void getNextPosts(SocialFragment fragment){
        this.fragment = fragment;
        String nextPageToken = youtubeTask.getPageToken();
        youtubeTask = new AsynkYoutubeFeed();
        if(nextPageToken != null) {
            youtubeTask.execute(HTTPS_GET_CHANNEL,
                    "https://www.googleapis.com/youtube/v3/search?" +
                            "part=snippet" +
                            "&maxResults=10" +
                            "&channelId=" + CHANNELID +
                            "&order=date" +
                            "&pageToken=" + nextPageToken +
                            "&fields=items(id%2Csnippet)%2CnextPageToken" +
                            "&key=" + API_KEY
            );
        } else {
            fragment.updateFeedYoutube(new ArrayList<SocialPost>());
        }
    }

    public class AsynkYoutubeFeed extends AsyncTask<String, Void, ArrayList<SocialPost>> {
        private String pageToken;
        private HttpClient httpclient = new DefaultHttpClient();
        private Gson gson = new GsonBuilder().create();
        private HttpGet httpGet = new HttpGet();
        private HttpResponse response;

        public String getPageToken() {
            return pageToken;
        }

        @Override
        protected ArrayList<SocialPost> doInBackground(String... params) {
            ArrayList<SocialPost> socialPosts = new ArrayList<>();
            try {

                YoutubeResponse channelResponse = executeRequest(URI.create(params[0]));
                YoutubeResponse feedResponse = executeRequest(URI.create(params[1]));

                pageToken = feedResponse.getNextPageToken();
                List<YoutubeResponse.Items> list = feedResponse.getItems();
                YoutubeResponse.Snippet channelSnippet = channelResponse.getItems().get(0).getSnippet();
                if(list != null) {
                    for (YoutubeResponse.Items item : list) {
                        PostOwner postOwner = new PostOwner(
                                channelSnippet.getThumbnails().getDefault().getUrl(),
                                channelSnippet.getTitle(),
                                channelSnippet.getThumbnails().getHigh().getUrl()
                        );

                        YoutubeResponse videoResponse = executeRequest(URI.create(
                                "https://www.googleapis.com/youtube/v3/videos?" +
                                        "part=contentDetails" +
                                        "&id=" + item.getId().getVideoId() +
                                        "&fields=items%2FcontentDetails" +
                                        "&key=" + YoutubeUtil.API_KEY
                        ));

                        SocialPost socialPost = new SocialPost(
                                item.getId().getVideoId(),
                                item.getSnippet().getTitle(),
                                item.getSnippet().getThumbnails().getHigh().getUrl(),
                                DateParser.dateParce(DateParser.getDateFormatYoutube(item.getSnippet().getPublishedAt())),
                                "",
                                postOwner
                        );
                        if (!videoResponse.getItems().isEmpty()) {
                            socialPost.setDuration(videoResponse.getItems().get(0).getContentDetails().getDuration());
                        }

                        if (item.getId() != null && item.getId().getVideoId() != null) {
                            socialPosts.add(socialPost);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "getYoutubeResponse error");
            }
            return socialPosts;
        }

        private YoutubeResponse executeRequest(URI uri) throws IOException {

            httpGet.setURI(uri);
            response = httpclient.execute(httpGet);

            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            YoutubeResponse response = gson.fromJson(reader, YoutubeResponse.class);
            reader.close();
            return response;
        }

        @Override
        protected void onPostExecute(ArrayList<SocialPost> socialPosts) {
            super.onPostExecute(socialPosts);
            fragment.updateFeedYoutube(socialPosts);
            fragment.cancelRefresh();
        }
    }
}
