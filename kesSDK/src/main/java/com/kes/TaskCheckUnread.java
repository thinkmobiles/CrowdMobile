package com.kes;

import android.content.Context;
import android.content.Intent;

import com.kes.net.DataFetcher;
import com.kes.net.ModelFactory;

import java.io.IOException;

class TaskCheckUnread extends NetworkExecutable<FeedManager.FeedWrapper> {
	public static final String ACTION = TaskCheckUnread.class.getName();
    protected static final String TAG_NOTIFICATION_CREATOR = "notification_creator";
    public static final String TAG_TOKEN = "token";
    public static final String TAG_MAXID = "token";

	static void loadFeed(Context context, String token, Integer maxID)
	{
		Intent intent = new Intent(ACTION);
        intent.putExtra(TAG_TOKEN, token);
        if (maxID != null)
            intent.putExtra(TAG_MAXID,maxID);
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

        wrapper.unreadOnly = true;
        wrapper.max_id = null;
        if (wrapper.extras.containsKey(TAG_MAXID))
            wrapper.max_id = wrapper.extras.getInt(TAG_MAXID);

        String token = wrapper.extras.getString(TAG_TOKEN);
        wrapper.feedType = FeedManager.FeedType.My;
        String filter = "my";

        wrapper.user = com.kes.net.NetworkAPI.getAccount(token);

        if (wrapper.user.unread_count == 0)
            return;

        ModelFactory.PhotoCommentWrapper photoCommentWrapper =
                com.kes.net.NetworkAPI.getFeed(token,true, null,null, wrapper.user.unread_count, filter, wrapper.tags);

        if (photoCommentWrapper.photo_comments == null && photoCommentWrapper.photo_comments.length == 0)
            return;

        if (wrapper.max_id == null || wrapper.photoComments[0].id > wrapper.max_id) {
            wrapper.unreadOnly = false;
            photoCommentWrapper =
                    com.kes.net.NetworkAPI.getFeed(token, false, null, null, null, filter, wrapper.tags);
        }

        wrapper.photoComments = photoCommentWrapper.photo_comments;
	}
	
}
