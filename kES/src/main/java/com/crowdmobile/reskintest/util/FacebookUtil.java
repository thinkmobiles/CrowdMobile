//TODO: FacebookReg.onActivityResult MUST be called whichever activity it is used in
package com.crowdmobile.reskintest.util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.crowdmobile.reskintest.MainActivity;
import com.crowdmobile.reskintest.R;
import com.crowdmobile.reskintest.fragment.SocialFragment;
import com.crowdmobile.reskintest.model.PostOwner;
import com.crowdmobile.reskintest.model.SocialPost;
import com.facebook.FacebookException;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Request.Callback;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.android.Facebook;
import com.facebook.model.GraphUser;
import com.google.gson.Gson;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class FacebookUtil {

	public static String KARDASHIAN_ID ="114696805612";
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
		void onStatuses(Response response);
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

	public FacebookUtil(Activity activity, FacebookCallback callback)
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
		session = Session.getActiveSession();
		if (session != null){
			Request request = Request.newMeRequest(session,new GraphUserCallback());
			request.executeAsync();
		}else {

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
	}

	public void executeGetPosts(){
		session = Session.getActiveSession();
		if(session!=null){
			makeGetPostRequest(session);
		}else{
			try {
				session = new Session(mActivity);
				Session.getActiveSession().setActiveSession(session);
				session.openForRead( new Session.OpenRequest(mActivity)
						.setPermissions("basic_info", "read_stream")
						.setCallback(new FBStatusForGetPosts()));
			} catch (FacebookException | NullPointerException e) {
				closeSession();
				callback.onFail(Fail.SessionOpen);
			}
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


			if (state == SessionState.OPENED)
			{
				Request request = Request.newMeRequest(session,new GraphUserCallback());
				request.executeAsync();
				return;

			}
			if (exception != null || state.isClosed()) {
				closeSession();
                if (!userInfoCalled)
					callback.onFail(Fail.Login);
					return;
			}
		}

	};

	class FBStatusForGetPosts implements Session.StatusCallback{

		@Override
		public void call(Session session, SessionState state, Exception exception) {
			if(state == SessionState.OPENED)
			{
				makeGetPostRequest(session);
			}
			if (exception != null || state.isClosed()) {
				closeSession();
				if (!userInfoCalled)
					callback.onFail(Fail.Login);
				return;
			}
		}
	}

	class GetPostRequest implements Callback{

		@Override
		public void onCompleted(Response response) {

				callback.onStatuses(response);
//				closeSession();
		}
	}

	public void makeGetPostRequest(Session session){
		Request request = new Request(
				session,
				"/"+ KARDASHIAN_ID +"/feed",
				null,
				HttpMethod.GET,
				new GetPostRequest());


		Bundle parameters = new Bundle();
		parameters.putString("fields", "id,message,full_picture, created_time,from");
		request.setParameters(parameters);
		request.executeAsync();
	}



	public ArrayList<SocialPost> getListPosts(Response graphResponse) throws JSONException {
		socialPosts = new ArrayList<>();

		JSONArray jsonArray = graphResponse.getGraphObject().getInnerJSONObject().getJSONArray("data");
		Gson gson = new Gson();

		for (int i = 0; i < jsonArray.length(); i++) {
			String json = String.valueOf(jsonArray.getJSONObject(i));
			SocialPost post = gson.fromJson(json, SocialPost.class);

			String date = DateParser.dateParce(DateParser.getDateFacebook(post.getCreate_date()));

			String ownerJson = String.valueOf(jsonArray.getJSONObject(i).getJSONObject("from"));
			PostOwner postOwner = gson.fromJson(ownerJson,PostOwner.class);
			postOwner.setIcon(mActivity.getResources().getString(R.string.base_url_facebook_avatar)
					+ postOwner.getId() + mActivity.getResources().getString(R.string.second_url_facebook_avatar));
			post.setPostOwner(postOwner);
			post.setCreate_date(date);
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

//			closeSession();
		}
	};
	
	
	

	
}
