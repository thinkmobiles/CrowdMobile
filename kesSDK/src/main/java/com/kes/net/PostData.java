package com.kes.net;

import android.text.TextUtils;

/**
 * Created by gadza on 2015.03.18..
 */
public class PostData {

    protected static class PhotoCommentRequest {
        public String message;
        public String photo_data;
        public String tag_list;
        public boolean is_private;

        public void setTags(String tags[]) {
            if (tags == null)
                tag_list = null;
            else
                tag_list = TextUtils.join(" ", tags);
        }
    }

    protected static class AccountSettingsRequest {

        public boolean show_profile_photo;
        public boolean show_last_name;

    }

    protected static class LoginRequest {
        protected String login_type;
        protected String access_token;
        protected String access_token_secret;
        protected String uid;
        protected String device_push_token;
        protected String device;
        protected String auth_token;
    };

    protected static class UpdatePushTokenRequest {
        /*
        protected String platform;
        protected String udid;
        protected String ua_token;
        */
        protected String push_token;
        protected String auth_token;
    }

    private static class Credit {
        protected String receipt_data;
    }

    protected static class AddCreditRequest {
        protected Credit credit = new Credit();
        protected String auth_token;
        protected String device;

        protected AddCreditRequest(String auth_token,String receipt)
        {
            this.credit.receipt_data = receipt;
            this.auth_token = auth_token;
            this.device = "android";
        }
    }


}
