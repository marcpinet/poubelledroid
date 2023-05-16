package com.polytech.poubelledroid.notificationcenter;

import android.graphics.Bitmap;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Notification {
    private final int id;
    private final String title;
    private final String message;
    private Bitmap image;
    private final long timestamp;
    private String imageUrl;
    public static final int VISIBILITY_PUBLIC = 1;
    private final Map<String, String> extraInformation = new HashMap<>();

    public Notification(int id, String title, String message, Bitmap image, long timestamp) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.image = image;
        this.timestamp = timestamp;
    }

    public Notification(String title, String message, Bitmap image, long timestamp) {
        this.id = (int) System.currentTimeMillis();
        this.title = title;
        this.message = message;
        this.image = image;
        this.timestamp = timestamp;
    }

    public Notification(String title, String message, Bitmap image) {
        this.id = (int) System.currentTimeMillis();
        this.title = title;
        this.message = message;
        this.image = image;
        this.timestamp = new Date().getTime();
    }

    public Notification(String title, String message, String imageUrl) {
        this.id = (int) System.currentTimeMillis();
        this.title = title;
        this.message = message;
        this.imageUrl = imageUrl;
        this.timestamp = new Date().getTime();
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public Bitmap getImage() {
        return image;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void addExtraInformation(Map<String, String> extraInformation) {
        this.extraInformation.putAll(extraInformation);
    }

    public Map<String, String> getExtras() {
        return extraInformation;
    }
}
