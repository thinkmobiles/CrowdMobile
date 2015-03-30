package com.crowdmobile.kes;

import android.app.Application;

/**
 * Created by gadza on 2015.03.23..
 */
public class KesApplication extends Application {

    public static final String HOCKEYAPP_ID = "7f03f42aa9fceb200d7f931d3d2f49f8";
    public static final boolean enableHockey = false;


    @Override
    public void onCreate() {
        super.onCreate();
        /*
        TwitterAuthConfig authConfig =
                new TwitterAuthConfig("Koup72sxM7oQPTRFxISrpxxyN",
                        "37nzsQNfumqtlXoYZHoJ6qU5563GVF9rIz07dWX0lsFFBwsZOo");
        Fabric.with(this, new Twitter(authConfig));
        */
    }
}
