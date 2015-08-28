package com.crowdmobile.reskintest.util;

import android.util.Log;

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

    public static Date getDateFormatYoutube(String _date) {
        Date date = null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        try {
            date = format.parse(_date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static String parseDuration(String duration){
        String result = "";
        String time = duration.substring(2);
        int indexH = time.lastIndexOf("H");
        int indexM = time.lastIndexOf("M");
        int indexS = time.lastIndexOf("S");
        if(indexH != -1){
            result = result + time.substring(0, indexH) + ":";
            if(indexM != -1){
                result = result +
                        ((indexM - indexH > 2) ? time.substring(indexH + 1, indexM) : "0" + time.substring(indexH + 1, indexM))
                        + ":";
                if(indexS != -1){
                    result = result +
                            (indexS - indexM > 2 ? time.substring(indexM + 1, indexS) : "0" + time.substring(indexM + 1, indexS));
                } else {
                    result += "00";
                }
            } else {
                if(indexS != -1){
                    result = result +
                            "00:" +
                            (indexS - indexH > 2 ? time.substring(indexH + 1, indexS) : "0" + time.substring(indexH + 1, indexS));
                } else {
                    result += "00:00";
                }
            }
        } else {
            if(indexM != -1){
                result = result + time.substring(0, indexM) + ":";
                if(indexS != -1){
                    result = result +
                            (indexS - indexM > 2 ? time.substring(indexM + 1, indexS) : "0" + time.substring(indexM + 1, indexS));
                } else {
                    result += "00";
                }
            } else {
                if(indexS != -1)
                    result = result + "0:" + time.substring(0, indexS);
                else
                    result += "00:00";
            }
        }
        return result;
    }


    public static String dateParce(Date _date) {
        SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm");
        return format.format(_date);
    }
}
