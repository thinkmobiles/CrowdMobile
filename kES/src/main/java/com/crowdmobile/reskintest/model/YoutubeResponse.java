package com.crowdmobile.reskintest.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by samson on 22.08.15.
 */
public class YoutubeResponse {

    private String nextPageToken;
    private List<Items> items;

    public String getNextPageToken() {
        return nextPageToken;
    }

    public void setNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
    }

    public List<Items> getItems() {
        return items;
    }

    public void setItems(List<Items> items) {
        this.items = items;
    }

    public class Items{
        private Id id;
        private Snippet snippet;

        public Snippet getSnippet() {
            return snippet;
        }

        public void setSnippet(Snippet snippet) {
            this.snippet = snippet;
        }
    }

    public class Id{
        private String videoId;
    }

    public class Snippet{

        private String publishedAt;
        private String title;
        private Thumbnail thumbnails;

        public String getPublishedAt() {
            return publishedAt;
        }

        public void setPublishedAt(String publishedAt) {
            this.publishedAt = publishedAt;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Thumbnail getThumbnails() {
            return thumbnails;
        }

        public void setThumbnails(Thumbnail thumbnails) {
            this.thumbnails = thumbnails;
        }
    }

    public class Thumbnail{

        private Quality medium;
        private Quality high;
        @SerializedName("default")
        private Quality def;

        public Quality getMedium() {
            return medium;
        }

        public void setMedium(Quality medium) {
            this.medium = medium;
        }

        public Quality getHigh() {
            return high;
        }

        public void setHigh(Quality high) {
            this.high = high;
        }

        public Quality getDefault() {
            return def;
        }

        public void setDefault(Quality def) {
            this.def = def;
        }
    }

    public class Quality{
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}






