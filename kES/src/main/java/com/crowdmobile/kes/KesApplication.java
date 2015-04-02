package com.crowdmobile.kes;

import android.app.Application;

//import com.urbanairship.UAirship;

/**
 * Created by gadza on 2015.03.23..
 */
public class KesApplication extends Application {

    public static final String HOCKEYAPP_ID = "7f03f42aa9fceb200d7f931d3d2f49f8";
    public static final boolean enableHockey = false;
    private static String channelID = null;

    /*
    public static String getChannelID()
    {
        if (channelID == null)
            channelID = UAirship.shared().getPushManager().getChannelId();
        return channelID;
    }
    */

    @Override
    public void onCreate() {
        super.onCreate();
        /*
        AirshipConfigOptions options = AirshipConfigOptions.loadDefaultOptions(this);
        options.inProduction = false; //determines which app key to use

        // Take off initializes the services
        UAirship.takeOff(this, options, new UAirship.OnReadyCallback() {
            @Override
            public void onAirshipReady(UAirship airship) {
                airship.getPushManager().setPushEnabled(true);
                airship.getPushManager().getChannelId();
            }
        });
        getChannelID();
        Session.getInstance(this).getAccountManager().setUAChannelID(getChannelID());
        */
    }
}
