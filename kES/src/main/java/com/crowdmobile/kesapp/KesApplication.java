package com.crowdmobile.kesapp;

import android.app.Application;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.crowdmobile.kesapp.util.HockeyUtil;
import com.facebook.Settings;
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

    @Override
    public void onCreate() {
        super.onCreate();
        HockeyUtil.init(this);
        AirshipConfigOptions options = AirshipConfigOptions.loadDefaultOptions(this);
        options.inProduction = false; //determines which app key to use
        String hash = Settings.getApplicationSignature(this);
        Log.d("HASH",hash);
        // Take off initializes the services
        UAirship.takeOff(this, options, new UAirship.OnReadyCallback() {
            @Override
            public void onAirshipReady(UAirship airship) {

                DefaultNotificationFactory defaultNotificationFactory = new DefaultNotificationFactory(getApplicationContext());
                defaultNotificationFactory.setSmallIconId(R.mipmap.ic_launcher);
                defaultNotificationFactory.setColor(NotificationCompat.COLOR_DEFAULT);
                defaultNotificationFactory.setTitleId(R.string.notification_title);
                airship.getPushManager().setNotificationFactory(defaultNotificationFactory);
                airship.getPushManager().setPushEnabled(true);
            }
        });

        KesConfigOptions kesConfigOptions = new KesConfigOptions();
        kesConfigOptions.serverURL = AppCfg.BASE_URL;

        KES.initialize(this, kesConfigOptions);
        KES.shared().getAccountManager().setUAChannelID(UAirship.shared().getPushManager().getChannelId());
    }
}
