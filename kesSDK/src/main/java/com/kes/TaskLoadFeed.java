package com.kes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.kes.model.PhotoComment;
import com.kes.net.DataFetcher;
import com.kes.net.ModelFactory;

import java.io.IOException;

class TaskLoadFeed extends NetworkExecutable<FeedManager.FeedWrapper> {
	public static final String ACTION = TaskLoadFeed.class.getName();
    public static final String TAG_TOKEN = "token";
    public static final String TAG_FEEDTYPE = "feedtype";
    public static final String TAG_MAXID = "maxid";
    public static final String TAG_SINCEID = "sinceid";
    public static final String TAG_TAGS = "tags";

	static void loadFeed(Context context, String token, FeedManager.FeedWrapper wrapper)
	{
		Intent intent = new Intent(ACTION);
        intent.putExtra(TAG_TOKEN,token);
        intent.putExtra(TAG_FEEDTYPE, wrapper.feedType.ordinal());
        if (wrapper.max_id != null)
            intent.putExtra(TAG_MAXID,wrapper.max_id);
        if (wrapper.since_id != null)
            intent.putExtra(TAG_SINCEID,wrapper.since_id);
        intent.putExtra(TAG_TAGS,wrapper.tags);
		NetworkService.execute(context, intent);
	}

    @Override
    public FeedManager.FeedWrapper getResultWrapper() {
        return new FeedManager.FeedWrapper();
    };


    @Override
    public void run(Context context, Session session, FeedManager.FeedWrapper wrapper) {
        session.getFeedManager().updateData(wrapper);
    }

    @Override
    public void onExecute(Context context, Intent intent, FeedManager.FeedWrapper wrapper) throws DataFetcher.KESNetworkException, IOException, InterruptedException {
        Bundle extras = intent.getExtras();
        String token = extras.getString(TAG_TOKEN);
        wrapper.feedType = FeedManager.FeedType.values()[extras.getInt(TAG_FEEDTYPE)];
        String filter = null;
        switch (wrapper.feedType)
        {
            case Public:
                filter = "feed";
                break;
            case My:
                filter = "my";
                break;
            default:
                break;
        }
        if (extras.containsKey(TAG_MAXID))
            wrapper.max_id = Integer.valueOf(extras.getInt(TAG_MAXID));
        if (extras.containsKey(TAG_SINCEID))
            wrapper.since_id = Integer.valueOf(extras.getInt(TAG_SINCEID));
        wrapper.tags = extras.getString(TAG_TAGS);

        ModelFactory.PhotoCommentWrapper photoCommentWrapper =
                com.kes.net.NetworkAPI.getFeed(token,wrapper.max_id,wrapper.since_id, null, filter, wrapper.tags);
        //Thread.sleep(2000);
        if (wrapper.max_id == null || wrapper.since_id == null && photoCommentWrapper.photo_comments != null)
        {
            int max = Integer.MIN_VALUE;
            int min = Integer.MAX_VALUE;
            for (int i = 0 ; i < photoCommentWrapper.photo_comments.length; i++)
            {
                PhotoComment p = photoCommentWrapper.photo_comments[i];
                if (p.id > max) max = p.id;
                if (p.id < min) min = p.id;
            }
            /*
            if (wrapper.max_id == null)
                wrapper.max_id = max;
            if (wrapper.since_id == null)
                wrapper.since_id = min;
            */
        }
        wrapper.comments = photoCommentWrapper.photo_comments;
	}
	
}
