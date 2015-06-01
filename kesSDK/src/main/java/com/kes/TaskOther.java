package com.kes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.kes.net.DataFetcher;

import java.io.IOException;

class TaskOther extends NetworkExecutable<ResultWrapper> {
	public static final String ACTION = TaskOther.class.getName();
    private static final String TAG_ACTIONTYPE = "action_type";

	static void execute(Context context, String token, ResultWrapper.ActionType actionType, int questionid, int commentid)
	{
		Intent intent = new Intent(ACTION);
        intent.putExtra(TAG_TOKEN,token);
        intent.putExtra(TAG_ACTIONTYPE, actionType.ordinal());
        intent.putExtra(TAG_QUESTION_ID,questionid);
        intent.putExtra(TAG_COMMENT_ID,commentid);
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
        wrapper.actionType = ResultWrapper.ActionType.values()[extras.getInt(TAG_ACTIONTYPE)];
        wrapper.questionID = extras.getInt(TAG_QUESTION_ID);
        wrapper.commentID = extras.getInt(TAG_COMMENT_ID);
        if (wrapper.actionType == ResultWrapper.ActionType.Report)
            NetworkAPI.report(token, wrapper.questionID);
        else if (wrapper.actionType == ResultWrapper.ActionType.MarkAsRead) {
            wrapper.photoComment = NetworkAPI.markAsRead(token, wrapper.questionID, wrapper.commentID);
            wrapper.user = NetworkAPI.getAccount(token);
        }
        else if (wrapper.actionType == ResultWrapper.ActionType.MarkAsPrivate)
            wrapper.photoComment = NetworkAPI.markAsPrivate(token, wrapper.questionID);
	}
	
}
