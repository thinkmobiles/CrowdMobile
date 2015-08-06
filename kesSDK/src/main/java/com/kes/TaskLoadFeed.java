package com.kes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.kes.net.DataFetcher;
import com.kes.net.ModelFactory;

import java.io.IOException;

/*
Server kilepes es visszatoltes utan adja a teljes feedet mark as read hivas utan
        package com.kes;
*/


class TaskLoadFeed extends com.kes.NetworkExecutable<FeedManager.FeedWrapper> {
	public static final String ACTION = TaskLoadFeed.class.getName();
    public static final String TAG_TOKEN = "token";
    public static final String TAG_FEEDTYPE = "feedtype";
    public static final String TAG_MAXID = "maxid";
    public static final String TAG_SINCEID = "sinceid";
    public static final String TAG_PAGESIZE = "pagesize";
    public static final String TAG_TAGS = "tags";
    public static final String TAG_APPENDED = "appended";

	static void loadFeed(Context context, String token, FeedManager.FeedWrapper wrapper)
	{
		Intent intent = new Intent(ACTION);
        intent.putExtra(TAG_TOKEN,token);
        intent.putExtra(TAG_FEEDTYPE, wrapper.feedType.ordinal());
        if (wrapper.max_id != null)
            intent.putExtra(TAG_MAXID,wrapper.max_id);
        if (wrapper.since_id != null)
            intent.putExtra(TAG_SINCEID,wrapper.since_id);
        intent.putExtra(TAG_PAGESIZE,wrapper.page_size);
        intent.putExtra(TAG_TAGS,wrapper.tags);
        intent.putExtra(TAG_APPENDED,wrapper.appended);
		NetworkService.execute(context, intent);
	}

    @Override
    public FeedManager.FeedWrapper getResultWrapper() {
        return new FeedManager.FeedWrapper();
    };


    @Override
    public void run(Context context, KES session, FeedManager.FeedWrapper wrapper) {
        session.getFeedManager().updateData(wrapper);
    }

//    static boolean first = true;
//    static boolean changed = false;

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
        wrapper.page_size = extras.getInt(TAG_PAGESIZE);
        //wrapper.page_size = 3;
        wrapper.appended = extras.getBoolean(TAG_APPENDED);
        wrapper.tags = extras.getString(TAG_TAGS);

        /*
        if (first && wrapper.max_id == null && wrapper.feedType == FeedManager.FeedType.Public)
        {
            first = false;
            changed = true;
            wrapper.max_id = 28191;
        }
        */
        ModelFactory.PhotoCommentWrapper photoCommentWrapper =
                NetworkAPI.getFeed(token, false, wrapper.max_id, wrapper.since_id, wrapper.page_size, filter, wrapper.tags);

        /*
        if (wrapper.feedType == FeedManager.FeedType.Public && changed) {
            changed = false;
            wrapper.max_id = null;
        }
        */

        //Thread.sleep(1000);
        //TODO:remove,debug stuff, removes answers
        /*
        if (wrapper.feedType == FeedManager.FeedType.My)
        {
            if (photoCommentWrapper.photo_comments != null && photoCommentWrapper.photo_comments.length > 0)
            {
                for (int i = 0; i < photoCommentWrapper.photo_comments.length; i++)
                    photoCommentWrapper.photo_comments[i].responses = null;
            }
        }

        */

        //TODO:---------------------



     //   Thread.sleep(2000);

        wrapper.photoComments = photoCommentWrapper.photo_comments;
        /*
        if (wrapper.feedType == FeedManager.FeedType.Public && wrapper.max_id == null)
        {
            if (counter++ == 1)
            {
                int size = photoCommentWrapper.photo_comments.length - 1;
                wrapper.photoComments = new PhotoComment[size];
                wrapper.photoComments[0] = photoCommentWrapper.photo_comments[0];
                for (int i = 1; i < size; i++)
                    wrapper.photoComments[i] = photoCommentWrapper.photo_comments[i + 1];
            }
        }
        */
	}
	
}
