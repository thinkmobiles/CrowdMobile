package com.crowdmobile.reskintest.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by john on 24.08.15.
 */
public abstract class DateParser {
    public static Date getDateFacebook(String _date) {
        Date date = null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        try {
            date = format.parse(_date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static Date getDateFormatTwitter(String _date) {
        Date date = null;
        SimpleDateFormat format = new SimpleDateFormat("eee MMM dd HH:mm:ss ZZZZ yyyy");
        try {
            date = format.parse(_date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }


    public static String dateParce(Date _date) {
        SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm");
        String formatedDate = format.format(_date);
        return formatedDate;
    }
}
