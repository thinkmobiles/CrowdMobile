package com.kes.model;

/**
 * Created by gadza on 2015.03.05..
 */
public class CommentResponse {
    public int id;
    public String comment;
    public boolean read;
    public String photo_url;
    public int likes_count;
    public boolean liked;
    public long created_at;

    public boolean equals(CommentResponse other)
    {
        if (id != other.id ||
                !StrUtil.strEqual(comment,other.comment) ||
                read != other.read ||
                liked != other.liked ||
                !StrUtil.strEqual(photo_url,other.photo_url) ||
                likes_count != other.likes_count ||
                created_at != other.created_at)
            return false;
        return true;
    }
}
