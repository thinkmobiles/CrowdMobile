package com.crowdmobile.kesapp;

import android.app.Application;
import android.support.v4.app.NotificationCompat;

import com.crowdmobile.kesapp.fragment.PrefsFragment;
import com.crowdmobile.kesapp.util.HockeyUtil;
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
        options.inProduction = !BuildConfig.DEBUG; //determines which app key to use
//        String hash = Settings.getApplicationSignature(this);
//        Log.w("HASH", hash);    //2BYxpeK18kZzvsdogYqHJiLCK2M
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
        kesConfigOptions.staging = AppCfg.staging;
        kesConfigOptions.api_id = AppCfg.API_ID;

        KES.initialize(this, kesConfigOptions);
        KES.shared().getAccountManager().setUAChannelID(UAirship.shared().getPushManager().getChannelId());
        PrefsFragment.setLocale(this, false);
    }
}
