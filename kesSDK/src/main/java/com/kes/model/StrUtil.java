package com.kes.model;

/**
 * Created by gadza on 2015.05.13..
 */
public class StrUtil {

    public static boolean strEqual(String str1,String str2)
    {
        if (str1 == null && str2 == null)
            return true;
        if (str1 == null && str2 != null)
            return false;
        if (!str1.equals(str2))
            return false;
        return true;
    }

}
