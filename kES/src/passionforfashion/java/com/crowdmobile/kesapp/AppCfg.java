//Passion 4 Fashion
package com.crowdmobile.kesapp;

public class AppCfg {

    public static boolean isStaging()
    {
        return BASE_URL != BASE_URL_PROD;
    }

    //    private static final String BASE_URL_STAGING = "http://kes-middletier-staging.elasticbeanstalk.com/api/bongothinks/v1/";
    public static final String ApiKey = "AIzaSyAXzZDflSBsnD7xmW3CvHhFuu-sCtvrJgc";
    private static final String BASE_URL_PROD = "https://middletier.globalaqa.com/api/passionforfashion/v1.1";

    public static final String BASE_URL = BASE_URL_PROD;

    public static final String SIGNATURE_PUBLIC = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4NqSQZdiYdW7Fyn+78qpr0z+iYyRYY0Nq26rkexrKOckBPq+UiNAKM2Xz/2QmtrkmZCPQdsU+2LDEHGE8v+un22TYu8zf7+UlwhzMYQXzT5JWML1mrWJzuue+t0KQ4MpEI14KWOGpuM/jVqZFa4I9i0xZw//RBY5KINjVjy6N1mE5mrUsuoPfcxkGe8SgZzr10BPsNbCZFp/mRoUAgeohh3HUS72ApaG51Y7rDzuWndtiLpwzJf2zwiWWvFVPPJo52f12Kd0oTrVDZckMY8UGxHZ+wuk7TNbAUjhXawCfLl+cM/aHqt/ezn4WXBexNtrk9t8mDTuHk4JCzJXakgA9wIDAQAB";

    public static final String HOCKEYAPP_ID = "e048a86968445dee0907a4872f3280de";

    public static final String TwitterKey = "9jkRqbM5rltjann2glw74sFvf";
    public static final String TwitterSecret = "CRoW3wADj1DnP9T1OlxYnHLdl6AxonAD4ci97LD57LwknAYnnY";
}

