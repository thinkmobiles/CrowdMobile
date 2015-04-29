package com.kes;

import android.content.Context;
import android.os.Bundle;

/**
 * Created by gadza on 2015.04.28..
 */
public class PushHandler {

    /*
    intent class have to be instance of notification creator
    */
    public static void handlePush(Context context, Class<? extends BaseNotificationCreator> cls, Bundle extras)
    {
        if (extras == null)
            extras = new Bundle();
        extras.putString(BaseNotificationCreator.TAG_NOTIFICATION_CREATOR, cls.getName());
        TaskCheckUnread.loadFeed(context, PreferenceUtil.getAuthToken(context), extras);
    }

}
