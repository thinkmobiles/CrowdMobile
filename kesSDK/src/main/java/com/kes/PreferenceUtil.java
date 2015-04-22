package com.kes;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.kes.model.User;
/**
 * Created by gadza on 2015.03.02..
 */
class PreferenceUtil {

    public static void setEmail(Context context, String value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(context.getString(R.string.key_user_email),value)
                .commit();
    }

    public static String getEmail(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.key_user_email), "");
    }

    public static void setBalance(Context context, int value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putInt(context.getString(R.string.key_user_balance),value)
                .commit();
    }

    public static void setUser(Context context, User user) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

        if (user == null)
        {
            editor
                    .remove(context.getString(R.string.key_user_id))
                    .remove(context.getString(R.string.key_user_authtoken))
                    .remove(context.getString(R.string.key_user_firstname))
                    .remove(context.getString(R.string.key_user_lastname))
                    .remove(context.getString(R.string.key_user_show_last_name))
                    .remove(context.getString(R.string.key_user_show_profile_photo))
                    .remove(context.getString(R.string.key_user_profile_photo_url))
                    .remove(context.getString(R.string.key_user_balance))
                    .remove(context.getString(R.string.key_user_unread_count))
                    .remove(context.getString(R.string.key_user_login_type));
        } else
        {
            editor
                    .putInt(context.getString(R.string.key_user_id), user.id)
                    .putString(context.getString(R.string.key_user_authtoken),user.auth_token)
                    .putString(context.getString(R.string.key_user_firstname), user.first_name)
                    .putString(context.getString(R.string.key_user_lastname), user.last_name)
                    .putBoolean(context.getString(R.string.key_user_show_last_name), user.show_last_name)
                    .putBoolean(context.getString(R.string.key_user_show_profile_photo), user.show_profile_photo)
                    .putString(context.getString(R.string.key_user_profile_photo_url), user.profile_photo_url)
                    .putInt(context.getString(R.string.key_user_balance), user.balance)
                    .putInt(context.getString(R.string.key_user_unread_count), user.unread_count)
                    .putString(context.getString(R.string.key_user_login_type), user.login_type);
        }
        editor.commit();
    }

    /*
    public static String getAuthToken(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.key_user_authtoken), null);
    }

    public static void setAuthToken(Context context, String value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(context.getString(R.string.key_user_authtoken),value)
                .commit();
    }
    */

    public static User getUser(Context context) {
        User result = new User();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        result.id = sp.getInt(context.getString(R.string.key_user_id), -1);
        result.first_name = sp.getString(context.getString(R.string.key_user_firstname), null);
        result.last_name = sp.getString(context.getString(R.string.key_user_lastname), null);
        result.show_last_name = sp.getBoolean(context.getString(R.string.key_user_show_last_name), false);
        result.show_profile_photo = sp.getBoolean(context.getString(R.string.key_user_show_profile_photo), false);
        result.profile_photo_url = sp.getString(context.getString(R.string.key_user_profile_photo_url), null);
        result.balance = sp.getInt(context.getString(R.string.key_user_balance), 0);
        result.unread_count = sp.getInt(context.getString(R.string.key_user_unread_count), 0);
        result.login_type = sp.getString(context.getString(R.string.key_user_login_type), null);
        result.auth_token = sp.getString(context.getString(R.string.key_user_authtoken), null);
        result.upToDate = false;
        return result;
    }

    public static void clearUser(Context context)
    {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .remove(context.getString(R.string.key_user_id))
                .remove(context.getString(R.string.key_user_firstname))
                .remove(context.getString(R.string.key_user_lastname))
                .remove(context.getString(R.string.key_user_authtoken))
                .commit();
    }


}
