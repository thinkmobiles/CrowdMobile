package com.kes.net;


import com.kes.model.PhotoComment;
import com.kes.model.User;
import com.kes.net.DataFetcher.RequestType;

import java.util.HashMap;
import java.util.Map;

public class NetworkAPI {

	public static User getAccount(String token) throws DataFetcher.KESNetworkException, InterruptedException
	{
		String url = com.kes.net.ServerNavigator.getMe(token);
		String result = DataFetcher.requestAction(url, RequestType.GET, null,null, null);
		return ModelFactory.getAccount(result);
	}

    public static User updateAccount(String token,boolean show_profile_photo,boolean show_last_name) throws DataFetcher.KESNetworkException, InterruptedException
    {
        String url = com.kes.net.ServerNavigator.updateAccount();
        String putData = ModelFactory.getAccountUpdateJson(token, show_profile_photo, show_last_name);
        String result = DataFetcher.requestAction(url, RequestType.PUT, null,null, putData);
        return ModelFactory.getAccount(result);
    }


    public static User registerMe(ModelFactory.LoginType login_type,
                                  String access_token,
                                  String access_token_secret,
                                  String uid,
                                  String device_push_token,
                                  String old_auth_token)
            throws DataFetcher.KESNetworkException, InterruptedException {
            String url = com.kes.net.ServerNavigator.registerMe();
            String params = ModelFactory.getLoginWrapper(
                    login_type,
                    access_token,
                    access_token_secret,
                    uid,
                    device_push_token,
                    old_auth_token);

        String result = DataFetcher.requestAction(url, RequestType.POST, null, null, params);
        return ModelFactory.getAccount(result);
    }

	public static User updatePushToken(String auth_token, String ua_token)
			throws DataFetcher.KESNetworkException, InterruptedException {
		String url = com.kes.net.ServerNavigator.registerDevice();
		String json = ModelFactory.getUpdatePushTokenJson(auth_token, ua_token);
		String result = DataFetcher.requestAction(url, RequestType.PUT, null, null, json);
        return ModelFactory.getAccount(result);
	}

    public static ModelFactory.PhotoCommentWrapper getFeed(
            String auth_token,
            Integer max_id,
            Integer since_id,
            Integer page_size,
            String filter,
            String tags) throws DataFetcher.KESNetworkException, InterruptedException
    {
        String url = com.kes.net.ServerNavigator.getFeed();
        Map<String, String> getParams = new HashMap<String, String>();

        if (auth_token != null)
            getParams = NetUtils.buildParameters(getParams,"auth_token", auth_token);

        getParams = NetUtils.buildParameters(getParams,"filter", filter);
        if (max_id != null)
            getParams = NetUtils.buildParameters(getParams,"max_id", max_id.toString());
        if (since_id != null)
            getParams = NetUtils.buildParameters(getParams,"since_id", since_id.toString());
        if (page_size != null)
            getParams = NetUtils.buildParameters(getParams,"page_size", page_size.toString());
        if (tags != null)
            getParams = NetUtils.buildParameters(getParams,"tags" + tags);

        String result = DataFetcher.requestAction(url, RequestType.GET, getParams,null, null);
        return ModelFactory.getFeed(result);
    }

    public static PhotoComment postQuestion(String token, String message, String photo_data, String tag_list[], boolean is_private) throws DataFetcher.KESNetworkException, InterruptedException
    {
        String url = com.kes.net.ServerNavigator.postComment();
        /*
        Map<String, String> getParams = new HashMap<String, String>();
        if (token != null)
            getParams = NetUtils.buildParameters(getParams,"auth_token", token);
        getParams = NetUtils.buildParameters(getParams,"filter", queryType);
        getParams = NetUtils.buildParameters(getParams,"page", Integer.toString(page));
        if (tags != null) getParams = NetUtils.buildParameters(getParams,"tags" + tags);
        */
        String postData = ModelFactory.getPhotoCommentJson(token,message,photo_data,tag_list,is_private);
        String result = DataFetcher.requestAction(url, RequestType.POST, null,null, postData);
        return ModelFactory.getPhotoComment(result);
    }

    public static void report(String token, int id) throws DataFetcher.KESNetworkException, InterruptedException
    {
        String url = com.kes.net.ServerNavigator.report(id);
        String postData = ModelFactory.getTokenJson(token);
        DataFetcher.requestAction(url, RequestType.PUT, null,null, postData);
    }

    public static void markAsRead(String token, int photocommentid, int responseid) throws DataFetcher.KESNetworkException, InterruptedException
    {
        String url = com.kes.net.ServerNavigator.markAsRead(photocommentid, responseid);
        String postData = ModelFactory.getTokenJson(token);
        DataFetcher.requestAction(url, RequestType.PUT, null,null, postData);
    }

    public static void like(String token, int photocommentid, int responseid) throws DataFetcher.KESNetworkException, InterruptedException
    {
        String url = com.kes.net.ServerNavigator.like(photocommentid, responseid);
        String postData = ModelFactory.getTokenJson(token);
        DataFetcher.requestAction(url, RequestType.PUT, null,null, postData);
    }

    public static void delete(String token, int photocommentid, int responseid) throws DataFetcher.KESNetworkException, InterruptedException
    {
        String url = com.kes.net.ServerNavigator.delete(photocommentid, responseid);
        String postData = ModelFactory.getTokenJson(token);
        DataFetcher.requestAction(url, RequestType.DELETE, null,null, postData);
    }

    public static void markAsPrivate(String token, int id) throws DataFetcher.KESNetworkException, InterruptedException
    {
        String url = com.kes.net.ServerNavigator.markAsPrivate(id);
        String postData = ModelFactory.getTokenJson(token);
        DataFetcher.requestAction(url, RequestType.PUT, null,null, postData);
    }

}
