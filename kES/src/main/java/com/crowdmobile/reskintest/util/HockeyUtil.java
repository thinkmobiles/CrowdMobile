package com.crowdmobile.reskintest.util;

import android.app.Activity;
import android.content.Context;

import com.crowdmobile.reskintest.AppCfg;
import com.crowdmobile.reskintest.BuildConfig;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.CrashManagerListener;
import net.hockeyapp.android.UpdateManager;

/**
 * Created by gadza on 2015.06.02..
 */
public class HockeyUtil {

    public static boolean enableHockey = false;
    private static String APP_ID = null;

    public static void init(Context context)
    {
        enableHockey = !BuildConfig.DEBUG;
        if (enableHockey) {
            APP_ID = AppCfg.HOCKEYAPP_ID;
            CrashManager.initialize(context, APP_ID, new CrashManagerListener() {
                @Override
                public boolean shouldAutoUploadCrashes() {
                    return true;
                }
            });
        }
    }

    public static void onMainActivityCreate(Activity activity)
    {
        if (enableHockey) {
            if (APP_ID == null)
                throw new IllegalStateException("App id has not been initialized");
            UpdateManager.register(activity, APP_ID);
            CrashManager.execute(activity, new CrashManagerListener() {
                @Override
                public boolean shouldAutoUploadCrashes() {
                    return true;
                }
            });
        }
    }

    public static void onMainActivityPause()
    {
        if (enableHockey)
            UpdateManager.unregister();
    }

}
