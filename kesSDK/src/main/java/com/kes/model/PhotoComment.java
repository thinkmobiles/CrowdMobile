package com.kes.model;

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
    }
}
