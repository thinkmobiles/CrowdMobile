package com.kes.net;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.kes.model.CreditResponse;
import com.kes.model.PhotoComment;
import com.kes.model.User;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ModelFactory {

	private static final Gson gson = createCustomGsonWithISO8601DateDeserializer();

	public static Gson createCustomGsonWithISO8601DateDeserializer() {
		Gson gson = new GsonBuilder()
        .registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
			@Override
			public Date deserialize(JsonElement json, Type arg1,
					JsonDeserializationContext arg2) throws JsonParseException {
				String dateTime = json.getAsString();
				try {
					return toDate(dateTime);
				} catch (ParseException e) {
					return new Date();
				}
			}
		})
        .create();
		return gson;
	}

	/** Transform ISO 8601 string to Date. */
	public static Date toDate(final String iso8601string)
			throws ParseException {
		String s = iso8601string.replace("Z", "+00:00");
		try {
			s = s.substring(0, 22) + s.substring(23);
		} catch (IndexOutOfBoundsException e) {
			throw new ParseException("Invalid length", 0);
		}
		Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(s);
		return date;
	}

    public static class ServerError {
        public String message;
        public int code;
    }

    static class ServerErrorWrapper {
        ServerError error;
    }

    public static ServerErrorWrapper getServerError(String json) {
        ServerErrorWrapper retval = gson.fromJson(json, ServerErrorWrapper.class);
        return retval;
    }

	public static User getAccount(String json) {
		return gson.fromJson(json, User.class);
	}

    public static CreditResponse getCreditResponse(String json) {
        return gson.fromJson(json, CreditResponse.class);
    }

    //--------------------------------------------------------------------------------------------------
    public static class PhotoCommentWrapper {
        public PhotoComment[] photo_comments;
    }

    //--------------------------------------------------------------------------------------------------
    public static PhotoComment getPhotoComment(String json) {
        return gson.fromJson(json, PhotoComment.class);
    }

    //--------------------------------------------------------------------------------------------------
    public static PhotoCommentWrapper getFeed(String json) throws JsonSyntaxException {
        return gson.fromJson(json, PhotoCommentWrapper.class);
    }

    //--------------------------------------------------------------------------------------------------
    public enum LoginType {Facebook, Twitter};

	public static String getLoginWrapper(
			LoginType loginType,
			String access_token,
			String access_token_secret,
			String uid,
			String device_push_token,
			String auth_token)
	{
		PostData.LoginRequest result = new PostData.LoginRequest();
		result.login_type = loginType.name();
		result.access_token = access_token;
		result.access_token_secret = access_token_secret;
		result.uid = uid;
		result.device_push_token = device_push_token;
		result.device = "android";
		result.auth_token = auth_token;
		return gson.toJson(result);
	}


    //--------------------------------------------------------------------------------------------------
	public static String getUpdatePushTokenJson(String auth_token, String ua_token) {
		PostData.UpdatePushTokenRequest result = new PostData.UpdatePushTokenRequest();
		result.auth_token = auth_token;
        result.push_token = ua_token;
		return gson.toJson(result);
	}

    //--------------------------------------------------------------------------------------------------
    public static String getAddCreditJson(String auth_token, String receipt) {
        PostData.AddCreditRequest result = new PostData.AddCreditRequest(auth_token,receipt);
        return gson.toJson(result);
    }

    //--------------------------------------------------------------------------------------------------
    static class PhotoCommentRequestWrapper {
        PostData.PhotoCommentRequest photo_comment_request;
        String auth_token;
    }


    public static String getPhotoCommentJson(
            String auth_token,
            String message,
            String photo_data,
            String tag_list[],
            boolean is_private)
    {
        PhotoCommentRequestWrapper result = new PhotoCommentRequestWrapper();
        result.auth_token = auth_token;
        result.photo_comment_request = new PostData.PhotoCommentRequest();
        result.photo_comment_request.message = message;
        result.photo_comment_request.photo_data = photo_data;
        result.photo_comment_request.setTags(tag_list);
        result.photo_comment_request.is_private = is_private;
        return gson.toJson(result);
    }
    //--------------------------------------------------------------------------------------------------
    static class PushTokenRequestWrapper {
        String push_token;
        String auth_token;
    }


    public static String getPushTokenJson(
            String auth_token,
            String push_token)
    {
        PushTokenRequestWrapper result = new PushTokenRequestWrapper();
        result.auth_token = auth_token;
        result.push_token = push_token;
        return gson.toJson(result);
    }

    //--------------------------------------------------------------------------------------------------
    static class UpdateAccountWrapper {
        PostData.AccountSettingsRequest account_settings;
        String auth_token;
    }

    public static String getAccountUpdateJson(
            String auth_token,
            boolean show_profile_photo,
            boolean show_last_name
            )
    {
        UpdateAccountWrapper result = new UpdateAccountWrapper();
        result.auth_token = auth_token;
        result.account_settings = new PostData.AccountSettingsRequest();
        result.account_settings.show_last_name = show_last_name;
        result.account_settings.show_profile_photo = show_profile_photo;
        return gson.toJson(result);
    }

//--------------------------------------------------------------------------------------------------
    static class TokenWrapper {
        String auth_token;
    }

    public static String getTokenJson(String token) {
        TokenWrapper result = new TokenWrapper();
        result.auth_token = token;
        return gson.toJson(result);
    }

}
