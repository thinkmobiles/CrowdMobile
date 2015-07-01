package com.crowdmobile.kesapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.crowdmobile.kesapp.R;

public class PreferenceUtils {

    public static void setSkipLogin(Context context, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(context.getString(R.string.key_skip_login),value)
                .commit();
    }

    public static boolean getSkipLogin(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.key_skip_login), false);
    }

    public static void setActiveFragment(Context context, int value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putInt(context.getString(R.string.key_active_fragment),value)
                .commit();
    }

    public static int getActiveFragment(Context context, int defValue) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(context.getString(R.string.key_active_fragment), defValue);
    }

    public static void setComposeText(Context context, String value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(context.getString(R.string.key_compose_text),value)
                .commit();
    }

    public static String getComposeText(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.key_compose_text), "");
    }

    public static void setComposePrivate(Context context, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(context.getString(R.string.key_compose_private),value)
                .commit();
    }

    public static boolean getComposePrivate(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.key_compose_private), false);
    }

    public static void setCameraPicture(Context context, String value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        if (value == null)
            editor.remove(context.getString(R.string.key_camera_picture));
        else
            editor.putString(context.getString(R.string.key_camera_picture),value);
        editor.commit();
    }

    public static String getCameraPicture(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.key_camera_picture), null);
    }

    public static void setComposedPicture(Context context, String value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        if (value == null)
            editor.remove(context.getString(R.string.key_composed_picture));
        else
            editor.putString(context.getString(R.string.key_composed_picture),value);
        editor.commit();
    }

    public static String getComposedPicture(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.key_composed_picture), null);
    }

    public static void setSkipNoPic(Context context, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(context.getString(R.string.key_skip_nopic),value)
                .commit();
    }

    public static boolean getSkipNoPic(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.key_skip_nopic), false);
    }


}
