package com.kes.model;

import com.kes.FeedManager;

/**
 * Created by gadza on 2015.03.05..
 */
public class PhotoComment {

    public static enum PostStatus {Posted, Pending, Error};

    public int id;
    public String message;
    public CommentResponse[] responses;
    public String first_name;
    public String last_name;
    public String profile_photo_url;
    public String photo_url;
    public String thumbnail_url;
    public long created_at;
    public String share_url;
    public boolean is_private;

    public PostStatus status = PostStatus.Posted;
    //public boolean flag_first = false;
    private Object tag;

    public void setTag(Object tag)
    {
        this.tag = tag;
    }

    public Object getTag()
    {
        return tag;
    }

    //Temporary variables
    public boolean reported = false;

    public void markAsRead(FeedManager feedManager)
    {
        if (responses == null || responses.length == 0)
            return;
        for (int i = 0; i < responses.length; i++)
            if (!responses[i].read) {
                responses[i].read = true;
                feedManager.markAsRead(id, responses[i].id);
            }
    }

    public boolean isUnread()
    {
        if (responses == null || responses.length == 0)
            return false;
        for (int j = 0; j < responses.length; j++) {
//            responses[j].read = false;   //todo:remove,debug
            if (!responses[j].read)
                return true;
        }
        return false;
    }

    public void copyFrom(PhotoComment src)
    {
        this.id = src.id;
        this.message = src.message;
        this.responses = src.responses;
        this.first_name = src.first_name;
        this.last_name = src.last_name;
        this.profile_photo_url = src.profile_photo_url;
        this.photo_url = src.photo_url;
        this.thumbnail_url = src.thumbnail_url;
        this.created_at = src.created_at;
        this.share_url = src.share_url;
        this.is_private = src.is_private;
        this.status = src.status;
        this.tag = src.tag;
    }
}
