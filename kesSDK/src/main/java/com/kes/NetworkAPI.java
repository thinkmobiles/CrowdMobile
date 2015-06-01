package com.kes;


import com.kes.model.CreditResponse;
import com.kes.model.PhotoComment;
import com.kes.model.User;
import com.kes.net.DataFetcher;
import com.kes.net.DataFetcher.RequestType;
import com.kes.net.ModelFactory;
import com.kes.net.NetUtils;

import java.util.HashMap;
import java.util.Map;

public class NetworkAPI {

	protected static User getAccount(String token) throws DataFetcher.KESNetworkException, InterruptedException
	{
        if (token == null || token.length() == 0)
            throw new IllegalStateException("token can't be null");
		String url = com.kes.net.ServerNavigator.getMe(token);
		String result = DataFetcher.requestAction(url, RequestType.GET, null,null, null);
		return ModelFactory.getAccount(result);
	}

    protected static User updateAccount(String token,boolean show_profile_photo,boolean show_last_name) throws DataFetcher.KESNetworkException, InterruptedException
    {
        String url = com.kes.net.ServerNavigator.updateAccount();
        String putData = ModelFactory.getAccountUpdateJson(token, show_profile_photo, show_last_name);
        String result = DataFetcher.requestAction(url, RequestType.PUT, null,null, putData);
        return ModelFactory.getAccount(result);
    }


    protected static User registerMe(ModelFactory.LoginType login_type,
                                  String access_token,
                                  String access_token_secret,
                                  String uid,
                                  String device_id
                                  )
            throws DataFetcher.KESNetworkException, InterruptedException {
            String url = com.kes.net.ServerNavigator.registerMe();
            String params = ModelFactory.getLoginWrapper(
                    login_type,
                    access_token,
                    access_token_secret,
                    uid,
                    device_id);

        String result = DataFetcher.requestAction(url, RequestType.POST, null, null, params);
        return ModelFactory.getAccount(result);
    }


	protected static User updatePushToken(String auth_token, String ua_token)
			throws DataFetcher.KESNetworkException, InterruptedException {
		String url = com.kes.net.ServerNavigator.push_token();
		String json = ModelFactory.getUpdatePushTokenJson(auth_token, ua_token);
		String result = DataFetcher.requestAction(url, RequestType.PUT, null, null, json);
        return ModelFactory.getAccount(result);
	}

    public static CreditResponse addCredit(String auth_token, String ua_token)
            throws DataFetcher.KESNetworkException, InterruptedException {
        String url = com.kes.net.ServerNavigator.addCredit();
        String json = ModelFactory.getAddCreditJson(auth_token, ua_token);
//        json = "imacpcbldbhfkcmccgkdlkfd.AO-J1OzM9rRRxlxNEhst8KWd9t93qnWhtjK_GGLhk_oUt8FUraBaHfwC7Dhl7fJtrIuDPv72OklvHlvJ6aW_nDfvykll_62e6ftxAMu7I_ZEqwwMQz7T6zI\\";
        String result = DataFetcher.requestAction(url, RequestType.POST, null, null, json);
        return ModelFactory.getCreditResponse(result);
    }

//    static boolean toggle = false;

    protected static ModelFactory.PhotoCommentWrapper getFeed(
            String auth_token,
            boolean newOnly,
            Integer max_id,
            Integer since_id,
            Integer page_size,
            String filter,
            String tags) throws DataFetcher.KESNetworkException, InterruptedException
    {
        String url = com.kes.net.ServerNavigator.getFeed();
        Map<String, String> getParams = new HashMap<String, String>();

        if (auth_token != null)
            getParams = NetUtils.buildParameters(getParams, "auth_token", auth_token);

        getParams = NetUtils.buildParameters(getParams,"filter", filter);
        if (newOnly)
            getParams = NetUtils.buildParameters(getParams,"new_only", "true");
        if (max_id != null)
            getParams = NetUtils.buildParameters(getParams,"max_id", max_id.toString());
        if (since_id != null)
            getParams = NetUtils.buildParameters(getParams,"since_id", since_id.toString());
        if (page_size != null)
            getParams = NetUtils.buildParameters(getParams,"page_size", page_size.toString());
        if (tags != null)
            getParams = NetUtils.buildParameters(getParams,"tags" + tags);
//        toggle = !toggle;
//        if (!toggle)
//            throw new DataFetcher.KESNetworkException();
        String result = DataFetcher.requestAction(url, RequestType.GET, getParams,null, null);
        return ModelFactory.getFeed(result);
    }

    protected static PhotoComment postQuestion(String token, String message, String photo_data, String tag_list[], boolean is_private) throws DataFetcher.KESNetworkException, InterruptedException
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

    protected static void report(String token, int id) throws DataFetcher.KESNetworkException, InterruptedException
    {
        String url = com.kes.net.ServerNavigator.report(id);
        String postData = ModelFactory.getTokenJson(token);
        DataFetcher.requestAction(url, RequestType.PUT, null,null, postData);
    }

    protected static PhotoComment markAsRead(String token, int photocommentid, int responseid) throws DataFetcher.KESNetworkException, InterruptedException
    {
        String url = com.kes.net.ServerNavigator.markAsRead(photocommentid, responseid);
        String postData = ModelFactory.getTokenJson(token);
        String result = DataFetcher.requestAction(url, RequestType.PUT, null,null, postData);
        return ModelFactory.getPhotoComment(result);
    }

    protected static void like(String token, int photocommentid, int responseid) throws DataFetcher.KESNetworkException, InterruptedException
    {
        String url = com.kes.net.ServerNavigator.like(photocommentid, responseid);
        String postData = ModelFactory.getTokenJson(token);
        DataFetcher.requestAction(url, RequestType.PUT, null,null, postData);
    }

    protected static void delete(String token, int photocommentid, int responseid) throws DataFetcher.KESNetworkException, InterruptedException
    {
        String url = com.kes.net.ServerNavigator.delete(photocommentid, responseid);
        String postData = ModelFactory.getTokenJson(token);
        DataFetcher.requestAction(url, RequestType.DELETE, null,null, postData);
    }

    protected static PhotoComment markAsPrivate(String token, int id) throws DataFetcher.KESNetworkException, InterruptedException
    {
        String url = com.kes.net.ServerNavigator.markAsPrivate(id);
        String postData = ModelFactory.getTokenJson(token);
        String result = DataFetcher.requestAction(url, RequestType.PUT, null,null, postData);
        return ModelFactory.getPhotoComment(result);
    }

}
