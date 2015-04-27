package com.crowdmobile.kes.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;

import com.crowdmobile.kes.MainActivity;
import com.crowdmobile.kes.R;

/**
 * Created by gadza on 2015.04.27..
 */
public class NotificationUtil {

    public static void createNotification(Context context)
    {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(MainActivity.TAG_MYPOSTS,true);
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

// build notification
// the addAction re-use the same intent to keep the example short
        Resources r = context.getResources();
        Notification.Builder b  = new Notification.Builder(context)
                .setContentTitle(r.getString(R.string.notification_title))
                .setContentText(r.getString(R.string.notification_text))
                .setSmallIcon(R.drawable.ic_settings_logo)
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .setContentIntent(pIntent);
        Notification n = null;
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT < 16)
            nm.notify(0, b.getNotification());
        else
            nm.notify(0, b.build());
    }

}
