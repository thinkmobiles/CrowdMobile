package com.kes;

import android.content.Context;
import android.os.Bundle;

/**
 * Created by gadza on 2015.04.28..
 */
public abstract class BaseNotificationCreator {

    protected static final String TAG_NOTIFICATION_CREATOR = "notification_creator";

    public abstract void createNotification(Context context, Bundle extras);

}
