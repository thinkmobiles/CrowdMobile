//TODO: FacebookReg.onActivityResult MUST be called whichever activity it is used in
package com.crowdmobile.kesapp.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.facebook.FacebookException;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;

import java.util.Arrays;

public class FacebookLogin {

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
					.setPermissions(Arrays.asList("basic_info", "user_about_me", "user_birthday"))
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
