package com.kes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.kes.net.DataFetcher;

import java.io.IOException;

class TaskLike extends NetworkExecutable<ResultWrapper> {
	public static final String ACTION = TaskLike.class.getName();
    public static final String TAG_TOKEN = "token";

	static void likeQuestion(Context context, String token, int questionID, int commentID)
	{
		Intent intent = new Intent(ACTION);
        intent.putExtra(TAG_TOKEN,token);
        intent.putExtra(TAG_QUESTION_ID, questionID);
        intent.putExtra(TAG_COMMENT_ID, commentID);
		NetworkService.execute(context, intent);
	}

    @Override
    protected ResultWrapper getResultWrapper() {
        return new ResultWrapper();
    }

    protected void run(Context context, KES session, ResultWrapper wrapper) {
        session.getFeedManager().updateAction(wrapper);
    }

    @Override
    protected void onExecute(Context context, Intent intent, ResultWrapper wrapper) throws DataFetcher.KESNetworkException, IOException, InterruptedException {
        Bundle extras = intent.getExtras();
        String token = extras.getString(TAG_TOKEN);
        wrapper.questionID = extras.getInt(TAG_QUESTION_ID);
        wrapper.commentID = extras.getInt(TAG_COMMENT_ID);
        NetworkAPI.like(token, wrapper.questionID, wrapper.commentID);
	}
	
}
