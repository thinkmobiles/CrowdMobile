package com.crowdmobile.kesapp;

/*
* BongoThinks configuration
* */
public class AppCfg {

    public static boolean isStaging()
    {
        return BASE_URL != BASE_URL_PROD;
    }
    private static final String BASE_URL_STAGING = "http://kes-middletier-staging.elasticbeanstalk.com/api/bongothinks/v1.1/";
    private static final String BASE_URL_LOCAL = "http://middletier.globalaqa.com:8080/api/bongothinks/v1.1/";
    private static final String BASE_URL_PROD = "https://middletier.globalaqa.com/api/bongothinks/v1.1/";

    private static final String BASE_URL_V2 = "http://kes-middle-staging.elasticbeanstalk.com/api/bongothinks/v1.1/";


    public static final String BASE_URL = BASE_URL_V2;

    public static final String ApiKey = "AIzaSyA-R5ilcQaQ_AQsySEI0cO-1l3yIIUdncw";

    public static final String SIGNATURE_PUBLIC = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnMT+S1eO2lrvFLoQ2e2PBQ0zX39Hu10Hj+Cte48sYzUdiDVE1f+haMMg1MoOqQoPSYegYt7E/BwOWeYcgOwzDI4rUqlzRT3pFHMsgLcdYFnR1n8Yxy5e7YfsdB2Mkx8co8sFJTfQU8UT00bMDa8yesReoihlWkxi8NHnE/2A4aPwW6za8aHSlHQyHQnr22huzigXJsY5/wK77d+MxDaCDF/49P8wKD8VFa60g9E3NthMEqfw1LhSz/tCSDncoyffZGPrNTt8Apr5emHKnGAaNtz4eN9wKokVj+hhK3hbRWA2PbTGc2mqFfqxv1qD7niF/3yHfGokb3iCk2KZWi6RowIDAQAB";

    public static final String HOCKEYAPP_ID = "7f03f42aa9fceb200d7f931d3d2f49f8";

    public static final String TwitterKey = "aM5iuM7nhCyyMTmNtRKHbaxbD";
    public static final String TwitterSecret = "OBjkkelLjBuORQQYSKa9rQqgYCryoT8orSeYKec4SDSHGtDS8q";

}

