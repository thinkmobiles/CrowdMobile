package com.kes;

import android.content.Context;
import android.content.Intent;

import com.kes.net.DataFetcher;

import java.io.IOException;

class TaskLoadUser extends  NetworkExecutable<AccountManager.UserWrapper> {
    public static final String ACTION = TaskLoadUser.class.getName();
    public static final String TAG_TOKEN = "token";

    static void loadUser(Context context, String token) {
        Intent intent = new Intent(ACTION);
        intent.putExtra(TAG_TOKEN, token);
        NetworkService.execute(context, intent);
    }

    @Override
    public AccountManager.UserWrapper getResultWrapper() {
        return new AccountManager.UserWrapper();
    }

    @Override
    public void run(Context context, Session session, AccountManager.UserWrapper wrapper) {
        session.getAccountManager().updateUser(wrapper);
    }

    @Override
    public void onExecute(Context context, Intent intent, AccountManager.UserWrapper wrapper) throws DataFetcher.KESNetworkException, IOException, InterruptedException {
        String token = intent.getStringExtra(TAG_TOKEN);
        wrapper.user = com.kes.net.NetworkAPI.getAccount(token);
    }
}
