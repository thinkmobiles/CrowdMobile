package com.crowdmobile.reskintest.model;

/**
 * Created by john on 19.08.15.
 */
public class SocialPost {

    private String id, message, full_picture, created_time;
    private PostOwner postOwner;

    public SocialPost() {
        this.postOwner = new PostOwner();
        this.id = "-1";
    }

    public SocialPost(String id, String description, String image, String create_date, PostOwner postOwner) {
        this.id = id;
        this.message = description;
        this.full_picture = image;
        this.created_time = create_date;
        this.postOwner = postOwner;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return message;
    }

    public void setDescription(String description) {
        this.message = description;
    }

    public String getImage() {
        return full_picture;
    }

    public void setImage(String image) {
        this.full_picture = image;
    }

    public PostOwner getPostOwner() {
        return postOwner;
    }

    public void setPostOwner(PostOwner postOwner) {
        this.postOwner = postOwner;
    }

    public String getCreate_date() {
        return created_time;
    }

    public void setCreate_date(String create_date) {
        this.created_time = create_date;
    }

    @Override
    public String toString() {
        return "SocialPost{" +
                "id='" + id + '\'' +
                ", message='" + message + '\'' +
                ", picture='" + full_picture + '\'' +
                ", created_time='" + created_time + '\'' +
                ", postOwner=" + postOwner +
                '}';
    }
}
