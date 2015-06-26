package com.kes.model;

import android.text.TextUtils;

public class User {
    public int id;
    public int balance;
    public int unread_count;
    public String login_type;
    public String auth_token;
    public String first_name;
    public String last_name;
    public boolean show_last_name;
    public boolean show_profile_photo;
    public String profile_photo_url;
    public String UA_ChannelID;
    public boolean upToDate;

	public boolean isRegistered()
	{
		return auth_token != null && auth_token.length() > 0;
	}
    public String getFullName()
    {
        String retval = "";
        if (!TextUtils.isEmpty(first_name))
            retval = first_name;
        if (!TextUtils.isEmpty(last_name))
        {
            if (!TextUtils.isEmpty(retval))
                retval += " ";
            retval += last_name;
        }
        return retval;
    }

}
