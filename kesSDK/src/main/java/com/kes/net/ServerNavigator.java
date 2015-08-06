package com.kes.net;


public class ServerNavigator {

    private static final String BASE_URL_STAGING = "http://kes-middle-staging.elasticbeanstalk.com/api/kes/v1.1/";
    private static final String BASE_URL_PROD = "https://middletier.globalaqa.com/api/kes/v1.1/";

    public static void setStaging()
    {
        BASE_URL = BASE_URL_STAGING;
    }

    private static String BASE_URL = BASE_URL_PROD;

	public static String getMe(String token) {
		return BASE_URL + "account/" + token + "/";
	}

    public static String updateAccount() {
        return BASE_URL + "account";
    }

    public static String registerMe() {
        return BASE_URL + "login";
    }

    public static String getFeed() {
        return BASE_URL + "photo_comments/";
    }

    public static String postComment() {
        return BASE_URL + "photo_comment_requests/";
    }

    public static String push_token() {
        return BASE_URL + "push_token/";
    }

    public static String getSuggestions() {
        return BASE_URL + "suggested_questions";
    }

    public static String report(int id) {
        return BASE_URL + "photo_comments/" + Integer.toString(id) + "/report";
    }

    public static String markAsRead(int photocommentid, int responseid) {
        return BASE_URL + "photo_comments/" + Integer.toString(photocommentid) + "/responses/" + Integer.toString(responseid) + "/mark_as_read";
    }

    public static String like(int photocommentid, int responseid) {
        return BASE_URL + "photo_comments/" + Integer.toString(photocommentid) + "/responses/" + Integer.toString(responseid) + "/like";
    }


    public static String delete(int photocommentid, int responseid) {
        return BASE_URL + "photo_comments/" + Integer.toString(photocommentid) + "/responses/" + Integer.toString(responseid);
    }

    public static String markAsPrivate(int id) {
        return BASE_URL + "photo_comments/" + Integer.toString(id) + "/mark_as_private";
    }

    public static String addCredit() {
        return BASE_URL + "credit";
    }

}
