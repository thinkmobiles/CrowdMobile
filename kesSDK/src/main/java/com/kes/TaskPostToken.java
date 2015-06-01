package com.kes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.kes.net.DataFetcher;

import java.io.IOException;

class TaskPostToken extends NetworkExecutable<AccountManager.UserWrapper> {
	public static final String ACTION = TaskPostToken.class.getName();
    public static final String TAG_TOKEN = "token";
    public static final String TAG_PUSHTOKEN = "pushtoken";

	static void updatePushToken(Context context, String authToken, String pushToken)
	{
		Intent intent = new Intent(ACTION);
        intent.putExtra(TAG_TOKEN,authToken);
        intent.putExtra(TAG_PUSHTOKEN,pushToken);
		NetworkService.execute(context, intent);
	}

    @Override
    protected AccountManager.UserWrapper getResultWrapper() {
        return new AccountManager.UserWrapper();
    }

    protected void run(Context context, KES session, AccountManager.UserWrapper wrapper) {
        session.getAccountManager().updateUser(wrapper);
    }

    @Override
    protected void onExecute(Context context, Intent intent, AccountManager.UserWrapper wrapper) throws DataFetcher.KESNetworkException, IOException, InterruptedException {
        Bundle extras = intent.getExtras();
        String token = extras.getString(TAG_TOKEN);
        String push_token = extras.getString(TAG_PUSHTOKEN);
        wrapper.user = NetworkAPI.updatePushToken(token, push_token);
        if (wrapper.user == null)
            return;
	}
	
}
