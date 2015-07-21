package com.kes.model;

import com.kes.FeedManager;
import com.kes.KES;

/**
 * Created by gadza on 2015.03.05..
 */
public class PhotoComment {

    public static enum PostStatus {Posted, Pending, Error};

    protected int id;
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

    public int getID()
    {
        return id;
    }

    public void setID(int id)
    {
        this.id = id;
    }

    public int getID(FeedManager.FeedType feedType) throws NullPointerException
    {
        if (feedType == FeedManager.FeedType.My)
            return id;
        else {
            if (responses == null || responses.length == 0)
                throw new NullPointerException();
            return responses[0].id;
        }
    }

    public Object getTag()
    {
        return tag;
    }

    //Temporary variables
    public boolean reported = false;
    public boolean liked;

    private void setAsRead(FeedManager feedManager,boolean value)
    {
        if (responses == null || responses.length == 0)
            return;
        for (int i = 0; i < responses.length; i++)
            if (responses[i].read != value) {
                responses[i].read = value;
                if (feedManager != null && value)
                    feedManager.markAsRead(id, responses[i].id);
            }
    }

    public void setAsRead(boolean value)
    {
        setAsRead(null,value);
    }

    public void markAsRead()
    {
        setAsRead(KES.shared().getFeedManager(), true);
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

    public boolean equals(PhotoComment other)
    {
        if (id != other.id ||
                !StrUtil.strEqual(this.message, other.message) ||
                !StrUtil.strEqual(first_name, other.first_name) ||
                !StrUtil.strEqual(last_name, other.last_name) ||
                !StrUtil.strEqual(profile_photo_url, other.profile_photo_url) ||
                !StrUtil.strEqual(photo_url, other.photo_url) ||
                !StrUtil.strEqual(thumbnail_url, other.thumbnail_url) ||
                created_at != other.created_at ||
                !StrUtil.strEqual(share_url, other.share_url) ||
                is_private != other.is_private ||
                status != other.status)
            return false;
        int rlen = 0;
        int slen = 0;
        if (responses != null)
            rlen = responses.length;
        if (other.responses != null)
            slen = other.responses.length;
        if (rlen != slen)
            return false;
        for (int i = 0; i < rlen; i++)
            if (!responses[i].equals(other.responses[i]))
                return false;
        return true;
    }


}
