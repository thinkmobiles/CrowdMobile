package com.kes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.kes.model.CommentResponse;
import com.kes.model.PhotoComment;
import com.kes.net.DataFetcher;
import com.kes.net.ModelFactory;

import java.io.IOException;

class TaskCheckUnread extends NetworkExecutable<FeedManager.FeedWrapper> {
	public static final String ACTION = TaskCheckUnread.class.getName();
    public static final String TAG_TOKEN = "token";

	static void loadFeed(Context context, String token, Bundle extras)
	{
		Intent intent = new Intent(ACTION);
        intent.replaceExtras(extras);
        intent.putExtra(TAG_TOKEN,token);
		NetworkService.execute(context, intent);
	}

    @Override
    public FeedManager.FeedWrapper getResultWrapper() {
        return new FeedManager.FeedWrapper();
    };


    @Override
    public void run(Context context, Session session, FeedManager.FeedWrapper wrapper) {
        session.getFeedManager().updateUnread(context, wrapper);
    }

    @Override
    public void onExecute(Context context, Intent intent, FeedManager.FeedWrapper wrapper) throws DataFetcher.KESNetworkException, IOException, InterruptedException {
        wrapper.extras = intent.getExtras();

        String token = wrapper.extras.getString(TAG_TOKEN);
        wrapper.feedType = FeedManager.FeedType.My;
        String filter = "my";

        ModelFactory.PhotoCommentWrapper photoCommentWrapper =
                com.kes.net.NetworkAPI.getFeed(token,true, null,null, null, filter, wrapper.tags);

        wrapper.comments = photoCommentWrapper.photo_comments;

        if (wrapper.comments == null && wrapper.comments.length == 0)
            return;
        int maxID = Integer.MIN_VALUE;
        for (int i = 0; i < wrapper.comments.length; i++)
        {
            PhotoComment pc = wrapper.comments[i];
            if (pc.responses != null && pc.responses.length > 0) {
                for (int j = 0; j < pc.responses.length; j++) {
                    CommentResponse cr = pc.responses[j];
                        if (cr.id >= maxID)
                            maxID = cr.id;
                }
            }
        }
        wrapper.max_id = maxID;
	}
	
}
