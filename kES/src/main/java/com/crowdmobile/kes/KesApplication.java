package com.crowdmobile.kes;

import android.app.Application;
import android.support.v4.app.NotificationCompat;

import com.kes.Session;
import com.urbanairship.AirshipConfigOptions;
import com.urbanairship.UAirship;
import com.urbanairship.push.notifications.DefaultNotificationFactory;

//import com.urbanairship.UAirship;

/**
 * Created by gadza on 2015.03.23..
 */
public class KesApplication extends Application {

    public static final String HOCKEYAPP_ID = "7f03f42aa9fceb200d7f931d3d2f49f8";
    public static final boolean enableHockey = false;
    private static String channelID = null;

    public static String getChannelID()
    {
        return UAirship.shared().getPushManager().getChannelId();
        /*
        if (channelID == null)
            channelID = UAirship.shared().getPushManager().getChannelId();
        return channelID;
        */
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AirshipConfigOptions options = AirshipConfigOptions.loadDefaultOptions(this);
        options.inProduction = false; //determines which app key to use

        // Take off initializes the services
        UAirship.takeOff(this, options, new UAirship.OnReadyCallback() {
            @Override
            public void onAirshipReady(UAirship airship) {

                DefaultNotificationFactory defaultNotificationFactory = new DefaultNotificationFactory(getApplicationContext());
                defaultNotificationFactory.setSmallIconId(R.drawable.ic_launcher);
                defaultNotificationFactory.setColor(NotificationCompat.COLOR_DEFAULT);
                defaultNotificationFactory.setTitleId(R.string.notification_title);
                airship.getPushManager().setNotificationFactory(defaultNotificationFactory);
                airship.getPushManager().setPushEnabled(true);
            }
        });
        Session.getInstance(this).getAccountManager().setUAChannelID(getChannelID());
    }
}
