package com.kes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.kes.model.PhotoComment;
import com.kes.net.DataFetcher;
import com.kes.net.ModelFactory;

import java.io.IOException;
import java.util.ArrayList;

class TaskCheckUnread extends NetworkExecutable<FeedManager.FeedWrapper> {
	public static final String ACTION = TaskCheckUnread.class.getName();
    public static final String TAG_TOKEN = "token";

	static void loadFeed(Context context, String token)
	{
		Intent intent = new Intent(ACTION);
        intent.putExtra(TAG_TOKEN,token);
		NetworkService.execute(context, intent);
	}

    @Override
    public FeedManager.FeedWrapper getResultWrapper() {
        return new FeedManager.FeedWrapper();
    };


    @Override
    public void run(Context context, Session session, FeedManager.FeedWrapper wrapper) {
        session.getFeedManager().updateUnread(wrapper);
    }

    @Override
    public void onExecute(Context context, Intent intent, FeedManager.FeedWrapper wrapper) throws DataFetcher.KESNetworkException, IOException, InterruptedException {
        Bundle extras = intent.getExtras();
        String token = extras.getString(TAG_TOKEN);
        wrapper.feedType = FeedManager.FeedType.My;
        String filter = "my";

        ModelFactory.PhotoCommentWrapper photoCommentWrapper =
                com.kes.net.NetworkAPI.getFeed(token,wrapper.max_id,wrapper.since_id, null, filter, wrapper.tags);
        if (photoCommentWrapper.photo_comments != null && photoCommentWrapper.photo_comments.length > 0) {
            PhotoComment pcs[] = photoCommentWrapper.photo_comments;
            photoCommentWrapper.photo_comments = null;
            ArrayList<PhotoComment> photoComments = new ArrayList<PhotoComment>();
            for (int i = 0; i < pcs.length; i++)
            {
                PhotoComment pc = pcs[i];
                if (pc.responses != null && pc.responses.length > 0)
                    for (int j = 0; j < pc.responses.length; j++)
                        if (!pc.responses[j].read)
                        {
                            photoComments.add(pc);
                            break;
                        }
            }
            if (photoComments.size() > 0)
            {
                photoCommentWrapper.photo_comments = new PhotoComment[photoComments.size()];
                photoComments.toArray(photoCommentWrapper.photo_comments);
            }
        }
        wrapper.comments = photoCommentWrapper.photo_comments;
	}
	
}
