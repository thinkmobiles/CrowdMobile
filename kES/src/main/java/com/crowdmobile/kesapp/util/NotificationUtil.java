package com.crowdmobile.kesapp.util;

import android.app.Notification;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.crowdmobile.kesapp.R;
import com.urbanairship.push.PushMessage;
import com.urbanairship.push.notifications.NotificationFactory;
import com.urbanairship.util.NotificationIDGenerator;

/**
 * Created by gadza on 2015.04.27..
 */
public class NotificationUtil extends NotificationFactory {

    public NotificationUtil(Context context)
    {
        super(context);
    }

    @Override
    public Notification createNotification(PushMessage message, int notificationId) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext())
                .setContentTitle("Bongo thinks")
                .setContentText(message.getAlert())
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher);


        // To support interactive notification buttons extend the NotificationCompat.Builder
        builder.extend(createNotificationActionsExtender(message, notificationId));

        return builder.build();
    }

    @Override
    public int getNextId(PushMessage pushMessage) {
        return NotificationIDGenerator.nextID();
    }

    /*
    public static void createNotification(Context context)
    {

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
    */

}
