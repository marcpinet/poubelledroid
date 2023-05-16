package com.polytech.poubelledroid.socialnetflow;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Tweet {
    private final String content;
    private final String username;
    private final String profileImageUrl;
    private final LocalDateTime date;
    private final List<String> mediaUrls;
    private final int retweetCount;
    private final int favoriteCount;

    public Tweet(
            String content,
            String username,
            String profileImageUrl,
            String dateAsString,
            List<String> mediasURL,
            int retweetCount,
            int favoriteCount) {
        this.content = content;
        this.username = username;
        this.profileImageUrl = profileImageUrl;
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        this.date = LocalDateTime.parse(dateAsString, formatter);
        this.mediaUrls = new ArrayList<>();
        this.mediaUrls.addAll(mediasURL);
        this.retweetCount = retweetCount;
        this.favoriteCount = favoriteCount;
    }

    public String getContent() {
        return content;
    }

    public String getUsername() {
        return username;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public List<String> getMediaUrls() {
        return mediaUrls;
    }

    public int getRetweetCount() {
        return retweetCount;
    }

    public int getLikeCount() {
        return favoriteCount;
    }
}
