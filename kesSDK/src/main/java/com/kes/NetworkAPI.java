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

        /*
        result = "{\"photo_comments\":[{\"id\":450,\"message\":\"Do you want a kiss, Bongo hehehe? How does my makeup look?\",\"responses\":[{\"id\":450,\"comment\":\"Bongo thinks this brunette beauty looks as vintage as Dita von TEASE! Mess with this sassy lady and you will get burned. James McAndrews and Tony Harper know that behind those smoky eyes is one hell of a fire! \",\"read\":false,\"photo_url\":\"\",\"likes_count\":0,\"created_at\":1433431015000}],\"first_name\":\"Maxim\",\"last_name\":\"Cobalcescu\",\"profile_photo_url\":\"https://fbcdn-profile-a.akamaihd.net/hprofile-ak-xta1/v/t1.0-1/p200x200/11111131_105050616495718_3187236089417650467_n.jpg?oh=7e1b585c2bf3f7e6f1fd6c7390c63acd&oe=56349F6C&__gda__=1442790466_085d224af9d4f26f9cc22efe9e8f2282\",\"photo_url\":\"file:/storage/sdcard0/Download/pic1.jpg\",\"thumbnail_url\":\"http://d1fey7b856aaw8.cloudfront.net/photos/000/000/450/original/thumbnail/photo.jpg\",\"created_at\":1433430672000,\"share_url\":\"http://passion4fashionapp.com/passionforfashion_share.php?id=20150604151112450\",\"is_private\":false},{\"id\":440,\"message\":\"Should I become a chef now? LOL! \",\"responses\":[{\"id\":440,\"comment\":\"Bongo thinks silly Sarah rocks that paper sack like Mario Batali with street cred. Greg Taylor and Sarah Whittaker think she could host a hip-hop cooking show called RAPatouille!\",\"read\":false,\"photo_url\":\"\",\"likes_count\":0,\"created_at\":1433224288000}],\"first_name\":\"\",\"last_name\":\"\",\"profile_photo_url\":\"\",\"photo_url\":\"file:/storage/sdcard0/Download/pic2.jpg\",\"thumbnail_url\":\"\",\"created_at\":1433223631000,\"share_url\":\"http://passion4fashionapp.com/passionforfashion_share.php?id=20150602054031440\",\"is_private\":false},{\"id\":431,\"message\":\"Bongo, should I shave my moustache?\",\"responses\":[{\"id\":431,\"comment\":\"Bongo thinks Damian has a ‘stache so good, he wishes he could hide it under his mattress mobster style. Adriana Sullivan and Ben Crosby think this Borat selfie totally Makes Benefit Glorious Nation of Kazakhstan.\",\"read\":false,\"photo_url\":\"https://photos-2.dropbox.com/t/2/AABJ7kNzplyQDtC2C8WYmeUP_SgsFj0kSvTbGIe0CdbiuA/12/7090937/png/32x32/1/_/1/2/pic3.png/CPnlsAMgASACIAMgBCAFIAYgBygBKAI/Gn1onDOHbMWR_tOeviIpBmdzurmbFJ2iqm0Oni1dJwY?size=1600x1200&size_mode=2\",\"likes_count\":0,\"created_at\":1432468160000}],\"first_name\":\"\",\"last_name\":\"\",\"profile_photo_url\":\"\",\"photo_url\":\"file:/storage/sdcard0/Download/pic3.jpg\",\"thumbnail_url\":\"http://d1fey7b856aaw8.cloudfront.net/photos/000/000/431/original/thumbnail/photo.jpg\",\"created_at\":1432467973000,\"share_url\":\"http://passion4fashionapp.com/passionforfashion_share.php?id=20150524114613431\",\"is_private\":false}]}";
        */
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

    protected static PhotoComment markAsPrivate(String token, int id, boolean isPrivate) throws DataFetcher.KESNetworkException, InterruptedException
    {
        String url = com.kes.net.ServerNavigator.markAsPrivate(id);
        String postData = ModelFactory.getPrivateJson(token,isPrivate);
        String result = DataFetcher.requestAction(url, RequestType.PUT, null,null, postData);
        return ModelFactory.getPhotoComment(result);
    }

}
