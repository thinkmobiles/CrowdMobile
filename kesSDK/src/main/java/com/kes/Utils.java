package com.kes;

import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by gadza on 2015.03.06..
 */
class Utils {

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

    public static boolean strHasValue(String str)
    {
        return str != null && str.length() > 0;
    }

    public static String fileToBase64(String path) throws IOException {
        InputStream inputStream = new FileInputStream(path);
        byte[] bytes;
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            bytes = output.toByteArray();
            return Base64.encodeToString(bytes, Base64.DEFAULT);

        } finally {
            inputStream.close();
        }
    }
}
