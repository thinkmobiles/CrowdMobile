package com.crowdmobile.kesapp;

import android.app.Application;
import android.support.v4.app.NotificationCompat;

import com.kes.KES;
import com.kes.KesConfigOptions;
import com.urbanairship.AirshipConfigOptions;
import com.urbanairship.UAirship;
import com.urbanairship.push.notifications.DefaultNotificationFactory;

//import com.urbanairship.UAirship;

/**
 * Created by gadza on 2015.03.23..
 */
public class KesApplication extends Application {

    public static final String HOCKEYAPP_ID = "7f03f42aa9fceb200d7f931d3d2f49f8";
    public static final boolean enableHockey = true;

    private static final String BASE_URL_STAGING = "http://kes-middletier-staging.elasticbeanstalk.com/api/bongothinks/v1/";
    private static final String BASE_URL_PROD = "http://kes-middletier-prod.elasticbeanstalk.com/api/bongothinks/v1.1/";


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

        KesConfigOptions kesConfigOptions = new KesConfigOptions();
        kesConfigOptions.serverURL = BASE_URL_STAGING;

        KES.initialize(this, kesConfigOptions);
        KES.shared().getAccountManager().setUAChannelID(UAirship.shared().getPushManager().getChannelId());
    }
}
