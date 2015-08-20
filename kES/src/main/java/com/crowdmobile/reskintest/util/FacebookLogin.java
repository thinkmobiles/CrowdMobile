//TODO: FacebookReg.onActivityResult MUST be called whichever activity it is used in
package com.crowdmobile.reskintest.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.crowdmobile.reskintest.MainActivity;
import com.crowdmobile.reskintest.R;
import com.crowdmobile.reskintest.model.PostOwner;
import com.crowdmobile.reskintest.model.SocialPost;
import com.facebook.FacebookException;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
import com.google.gson.Gson;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class FacebookLogin {

	public static String KARDASJAN_ID ="114696805612";
	public ArrayList<SocialPost> socialPosts;

	public static class UserInfo {
		public String token;
		public String uid;
		public String firstName;
		public String lastName;
		public String gender;
	}

    public enum Fail {SessionOpen, Login};

	public interface FacebookCallback {
		public void onFail(Fail fail);
		public void onUserInfo(UserInfo userInfo);
	};
	
//	public final static String ACTION_FACEBOOKREG_FAIL = "trndy.facebookregfail";
//	public final static String TAG_ERRORMSG = "errormsg";
	
//	private static WeakReference<FacebookCallback>wFacebookCallback;

    private Activity mActivity;
	private FacebookCallback callback;
	private Session session;
	private boolean userInfoCalled;

	public void logout(Context context)
	{
		try {
            if (session == null)
			    session = new Session(context);
			session.closeAndClearTokenInformation();
		} catch (FacebookException | NullPointerException e) {
			e.printStackTrace();
		}
        finally {
            session = null;
        }
	}
	
	public FacebookLogin(Activity activity, FacebookCallback callback)
	{
        mActivity = activity;
		if (callback == null)
            throw new IllegalStateException("callback can't be null");
        this.callback = callback;
	}

	private void closeSession()
	{
		if (session != null)
		{
			session.close();
			session = null;
		}
		Session.setActiveSession(null);
	}
	
	public void release()
	{
        callback = null;
		closeSession();
	}
	
	public void execute()
	{
		if (session != null)
			throw new IllegalStateException();
		//if (!isFBInstalled(callback.getActivity()))
		//	return;
		userInfoCalled = false;
		try {
			session = new Session(mActivity);
			Session.setActiveSession(session);
			session.openForRead(new Session.OpenRequest(mActivity)
					.setPermissions(Arrays.asList("basic_info", "user_about_me", "user_birthday", "read_stream"))
							.setCallback(new FBStatusCallback()));
		} catch (FacebookException | NullPointerException e) {
			closeSession();
			callback.onFail(Fail.SessionOpen);
		}
	}

    /*
	public static boolean isFBInstalled(Context context)
	{
		try{
		    ApplicationInfo info = context.getPackageManager().
		            getApplicationInfo("com.facebook.katana", 0 );
		    return true;
		} catch( PackageManager.NameNotFoundException e ){
			return false;
		}
	}
	*/

	public boolean onActivityResult(Activity activity,int requestCode, int resultCode, Intent data) {
		Session session = Session.getActiveSession();
		if (session != null)
			return session.onActivityResult(activity, requestCode,
				resultCode, data);
		return false;
	}
	
	class FBStatusCallback implements Session.StatusCallback {
		
		@Override
		public void call(Session session, SessionState state, Exception exception) {

			Request request;
			if (state == SessionState.OPENED)
			{
				request = new Request(
						Session.getActiveSession(),
						"/"+ KARDASJAN_ID +"/feed",
						null,
						HttpMethod.GET,
						new Request.Callback() {
							@Override
							public void onCompleted(Response response) {
								try {
									if(mActivity != null&& mActivity instanceof MainActivity) {
										MainActivity mainActivity = (MainActivity) mActivity;
										mainActivity.setSocialData(getListPosts(response));
									}

								} catch (JSONException e) {
									e.printStackTrace();
								}
							}
						}
				);
				Bundle parameters = new Bundle();
				parameters.putString("fields", "id,message,picture, created_time,from");
				request.setParameters(parameters);
				request.executeAsync();

			}
			if (exception != null || state.isClosed()) {
				closeSession();
                if (!userInfoCalled)
					callback.onFail(Fail.Login);
					return;
			}
		}

	};

	private ArrayList<SocialPost> getListPosts(Response graphResponse) throws JSONException {
		socialPosts = new ArrayList<>();

		JSONArray jsonArray = graphResponse.getGraphObject().getInnerJSONObject().getJSONArray("data");
		Gson gson = new Gson();

		for (int i = 0; i < jsonArray.length(); i++) {
			String json = String.valueOf(jsonArray.getJSONObject(i));
			SocialPost post = gson.fromJson(json, SocialPost.class);

			String ownerJson = String.valueOf(jsonArray.getJSONObject(i).getJSONObject("from"));
			PostOwner postOwner = gson.fromJson(ownerJson,PostOwner.class);
			postOwner.setIcon(mActivity.getResources().getString(R.string.base_url_facebook_avatar)
					+ postOwner.getId() + mActivity.getResources().getString(R.string.second_url_facebook_avatar));
			post.setPostOwner(postOwner);
			Log.e("ZZZZ ", post.toString());
			socialPosts.add(post);
		}


//		createView();
//		initListeners();
//		groupAdapter = new CustomGroupAdapter(CustomFriendsList.this, groupesData);
//		friendsList.setAdapter(groupAdapter);
//		views.add(view);
//		pagerAdapter.notifyDataSetChanged();

		return socialPosts;
	}

	class GraphUserCallback implements Request.GraphUserCallback {

		@Override
		public void onCompleted(GraphUser user, Response response) {
			UserInfo result = null;
			Session session = response.getRequest().getSession();
			String token = session.getAccessToken();
			if (token != null && token.length() > 0 && user != null)
			{
				result = new UserInfo();
                result.uid = user.getId();
                result.firstName = user.getFirstName();
                result.lastName = user.getLastName();
                result.token = token;
                try {
                    result.gender = user.asMap().get("gender").toString();
                } catch (NullPointerException ignored) {}
				callback.onUserInfo(result);
				userInfoCalled = true;
			};

/*
					String gender = ((String) user.asMap().get("gender"));
					String birthday = null;
					SimpleDateFormat parser = new SimpleDateFormat("MM/dd/yyyy");
					try {
						java.util.Date dt = parser.parse(user.getBirthday());
						SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
						birthday = format.format(dt);
					} catch (ParseException e) {
					}
*/					
					/*
					RegisterUser.register(activity, firstName,
							lastName, "facebook", uid, token);
							*/

			closeSession();
		}
	};
	
	
	

	
}
