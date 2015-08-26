package com.crowdmobile.reskintest.model;

/**
 * Created by john on 19.08.15.
 */
public class PostOwner {

    private String id, name, avatar;

    public PostOwner() {
    }

    public PostOwner(String id, String name,String avatar) {
        this.id = id;
        this.name = name;
        this.avatar = avatar;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIcon() {
        return avatar;
    }

    public void setIcon(String icon) {
        this.avatar = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "PostOwner{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", avatar='" + avatar + '\'' +
                '}';
    }
}
