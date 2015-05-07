package com.kes;

import android.content.Context;
import android.content.Intent;

import com.google.gson.JsonSyntaxException;
import com.kes.net.DataFetcher;

import java.io.IOException;

/**
 * Created by gadza on 2015.03.13..
 */
abstract class NetworkExecutable<T extends ResultWrapper> {
    public static final String TAG_TOKEN = "token";
    public static final String TAG_QUESTION_ID = "questionid";
    public static final String TAG_COMMENT_ID = "commentid";
    protected static final String TAG_CMD = "command";
    private T mWrapper;
    protected abstract T getResultWrapper();

    protected void serviceExecuteOnThread(Context context, Intent intent) throws InterruptedException
    {
        mWrapper = getResultWrapper();
        if (mWrapper == null)
            throw new IllegalStateException("wrapper cannot be null");
        try {
            onExecute(context, intent, mWrapper);
        } catch (DataFetcher.KESNetworkException | JsonSyntaxException | IOException e)
        {
            mWrapper.exception = e;
        }
    }

    protected void serviceExecuteOnUI(Context context,Session session)
    {
        if (mWrapper != null &&
                mWrapper.exception != null &&
                mWrapper.exception instanceof DataFetcher.KESNetworkException &&
                !mWrapper.suppressError)
            session.networkError((DataFetcher.KESNetworkException)mWrapper.exception);
        //Log.d("CLASS", this.getClass().getSimpleName());
        run(context, session, mWrapper);
    }

    protected abstract void onExecute(Context context, Intent intent, T wrapper) throws DataFetcher.KESNetworkException, IOException, InterruptedException;
    protected abstract void run(Context context,Session session, T holder);
}

