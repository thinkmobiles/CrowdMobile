package com.kes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.kes.net.DataFetcher;
import com.kes.net.ModelFactory;

import java.io.IOException;

class TaskSuggestion extends NetworkExecutable<FeedManager.SuggestionsHolder> {
	public static final String ACTION = TaskSuggestion.class.getName();
    public static final String TAG_TOKEN = "token";

	static void execute(Context context, String token)
	{
		Intent intent = new Intent(ACTION);
        intent.putExtra(TAG_TOKEN,token);
		NetworkService.execute(context, intent);
	}

    @Override
    protected FeedManager.SuggestionsHolder getResultWrapper() {
        return new FeedManager.SuggestionsHolder();
    }

    protected void run(Context context, KES session, FeedManager.SuggestionsHolder wrapper) {
        session.getFeedManager().updateSuggestions(wrapper,true);
    }

    @Override
    protected void onExecute(Context context, Intent intent, FeedManager.SuggestionsHolder wrapper) throws DataFetcher.KESNetworkException, IOException, InterruptedException {
        Bundle extras = intent.getExtras();
        String token = extras.getString(TAG_TOKEN);
        ModelFactory.SuggestionsWrapper result = NetworkAPI.getSuggestions(token);
        if (result.suggested_questions != null && result.suggested_questions.length == 0)
            result.suggested_questions = null;
        wrapper.response = result.suggested_questions;
	}
	
}
