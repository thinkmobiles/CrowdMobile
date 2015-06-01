package com.kes;

//TODO:there are functions which are needed to be repeated in case of fail.
//Like registration of UA

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings.Secure;

import com.kes.net.DataFetcher;
import com.kes.net.ModelFactory;

import java.io.IOException;

class TaskLogin extends NetworkExecutable<AccountManager.UserWrapper> {
    public static final String TAG = TaskLogin.class.getSimpleName();
    public static final String ACTION = TaskLogin.class.getName();
    private static final String CMD_LOGIN = "com.crowdmobile.kes.net.login";

    protected static final String TAG_LOGIN_TYPE = "login_type";
    protected static final String TAG_ACCESS_TOKEN = "access_token";
    protected static final String TAG_ACCESS_TOKEN_SECRET = "access_token_secret";
    protected static final String TAG_UID = "uid";

    public static void login(
            Context context,
            ModelFactory.LoginType loginType,
            String access_token,
            String access_token_secret,
            String uid) {
        Intent intent = new Intent(ACTION);
        intent.putExtra(TAG_CMD, CMD_LOGIN);
        intent.putExtra(TAG_LOGIN_TYPE, loginType.ordinal());
        intent.putExtra(TAG_ACCESS_TOKEN, access_token);
        intent.putExtra(TAG_ACCESS_TOKEN_SECRET, access_token_secret);
        intent.putExtra(TAG_UID, uid);
        NetworkService.execute(context, intent);
    }

    @Override
    public void onExecute(Context context, Intent intent, AccountManager.UserWrapper wrapper) throws DataFetcher.KESNetworkException, IOException, InterruptedException {
        Bundle extras = intent.getExtras();
        String token = null;
        com.kes.net.DataFetcher.KESNetworkException exc = null;

        /*
        user.id = 1;
        user.firstName = "John";
        user.lastName = "Droid";
        user.auth_token = "auth_token";
        user.syncStatus = User.SyncStatus.UpToDate;
        */
        wrapper.user = NetworkAPI.registerMe(
                ModelFactory.LoginType.values()[extras.getInt(TAG_LOGIN_TYPE)],
                extras.getString(TAG_ACCESS_TOKEN),
                extras.getString(TAG_ACCESS_TOKEN_SECRET),
                extras.getString(TAG_UID),
                Secure.getString(context.getContentResolver(), Secure.ANDROID_ID));
        if (wrapper.user == null)
            throw new DataFetcher.KESNetworkException(DataFetcher.KESNetworkException.CODE_Invalid_authentication_code,"invalid token");
    }

    @Override
    public AccountManager.UserWrapper getResultWrapper() {
        return new AccountManager.UserWrapper();
    }


    public void run(Context context, KES session, AccountManager.UserWrapper wrapper) {
        session.getAccountManager().postLoginResult(wrapper);
    }

	
}
