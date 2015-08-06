package com.kes;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

/**
 * Created by gadza on 2015.05.27..
 */
public class KesConfigOptions {

    public String api_id = null;
    public boolean staging = false;

    public static KesConfigOptions loadDefaultOptions(Context ctx) {
        KesConfigOptions options = new KesConfigOptions();
        options.loadFromProperties(ctx,"kes.cfg");
        return options;
    }

    public void loadFromProperties(Context ctx, String propertiesFile) {
        Resources resources = ctx.getResources();
        AssetManager assetManager = resources.getAssets();

        try {
            if(!Arrays.asList(assetManager.list("")).contains(propertiesFile)) {
                return;
            }
        } catch (IOException var14) {
            return;
        }

        Properties properties = new Properties();

        try {
            InputStream ioe = assetManager.open(propertiesFile);
            properties.load(ioe);
        } catch (IOException var13) {
        }

    }


}
