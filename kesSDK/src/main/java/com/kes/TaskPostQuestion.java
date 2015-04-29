package com.kes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.kes.net.DataFetcher;

import java.io.IOException;

class TaskPostQuestion extends NetworkExecutable<FeedManager.PhotoCommentResponseHolder> {
	public static final String ACTION = TaskPostQuestion.class.getName();
    public static final String TAG_TOKEN = "token";
    public static final String TAG_INTERNALID = "internalid";
    public static final String TAG_MESSAGE = "message";
    public static final String TAG_FILEPATH = "filepath";
    public static final String TAG_TAGS = "tags";
    public static final String TAG_PRIVATE = "private";

	static void postQuestion(Context context, String token, int internalID, String message, String filepath, String tag_list[], boolean is_private)
	{
		Intent intent = new Intent(ACTION);
        intent.putExtra(TAG_TOKEN,token);
        intent.putExtra(TAG_INTERNALID,internalID);
        intent.putExtra(TAG_PRIVATE,is_private);
        intent.putExtra(TAG_MESSAGE,message);
        intent.putExtra(TAG_FILEPATH,filepath);
        intent.putExtra(TAG_TAGS,tag_list);
		NetworkService.execute(context, intent);
	}

    @Override
    protected FeedManager.PhotoCommentResponseHolder getResultWrapper() {
        return new FeedManager.PhotoCommentResponseHolder();
    }

    protected void run(Context context, Session session, FeedManager.PhotoCommentResponseHolder wrapper) {
        session.getFeedManager().updatePostedQuestion(wrapper);
    }

    static int responseid = 599;

    @Override
    protected void onExecute(Context context, Intent intent, FeedManager.PhotoCommentResponseHolder wrapper) throws DataFetcher.KESNetworkException, IOException, InterruptedException {
        Bundle extras = intent.getExtras();
        String token = extras.getString(TAG_TOKEN);
        int internalID = extras.getInt(TAG_INTERNALID);
        String message = extras.getString(TAG_MESSAGE);
        String filePath = extras.getString(TAG_FILEPATH);
        String[] tags = extras.getStringArray(TAG_TAGS);
        boolean is_private = extras.getBoolean(TAG_PRIVATE);
        String photo_data = null;
        if (filePath != null)
            photo_data = Utils.fileToBase64(filePath);
//        Thread.sleep(3000);
//        if (true) throw new IOException("Test IO exception");
        wrapper.response = com.kes.net.NetworkAPI.postQuestion(token,message,photo_data,tags,is_private);
        wrapper.internalid = internalID;
        //throw new DataFetcher.KESNetworkException("Cool");
        /*
        wrapper.response = new PhotoComment();
        wrapper.response.id = responseid++;
        wrapper.response.message = message;
        if (filePath != null)
            wrapper.response.photo_url = "file://" + filePath;
        */
	}
	
}
