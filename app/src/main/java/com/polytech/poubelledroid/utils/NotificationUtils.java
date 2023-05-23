package com.polytech.poubelledroid.utils;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.polytech.poubelledroid.Poubelledroid;
import com.polytech.poubelledroid.R;
import com.polytech.poubelledroid.fields.ActionFields;
import com.polytech.poubelledroid.fields.LocalNotificationFields;
import com.polytech.poubelledroid.googlemaps.MapsActivity;
import com.polytech.poubelledroid.notificationcenter.Notification;
import com.polytech.poubelledroid.report.CleanBroadcastReceiver;
import com.polytech.poubelledroid.report.FirebaseNotificationService;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NotificationUtils {

    public static final String NOTIFICATION_PREFS = "notification_prefs";
    public static final String NOTIFICATION_LIST = "notification_list";
    private static boolean newNotification = false;
    private final Context context;

    public NotificationUtils(Context context) {
        this.context = context;
    }

    public void createNotificationChannel() {
        CharSequence channelName = "Poubelledroid Notification Channel";
        String description = "Channel for Poubelledroid app notifications";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel =
                new NotificationChannel("APP_CHANNEL_ID", channelName, importance);
        channel.setDescription(description);
        channel.setShowBadge(true);
        channel.setBypassDnd(true);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Used for notifications with Approve and Reject buttons
     *
     * @param firebaseNotificationService
     * @param notificationObject
     */
    public void sendNotification(
            FirebaseNotificationService firebaseNotificationService,
            Notification notificationObject) {
        Context context = firebaseNotificationService.getApplicationContext();

        NotificationUtils notificationUtils = new NotificationUtils(context);

        notificationUtils.sendNotificationWithImgUrl(context, notificationObject, true, true);
    }

    public void sendNotificationWithImgUrl(
            Context context, Notification notif, boolean showDirectly, boolean showButtons) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(
                () -> {
                    try {
                        URL url = new URL(notif.getImageUrl());
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream inputStream = connection.getInputStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        notif.setImage(bitmap);

                        new Handler(Looper.getMainLooper())
                                .post(
                                        () ->
                                                sendNotification(
                                                        context, notif, showDirectly, showButtons));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    public void sendNotification(
            Context context,
            String title,
            String message,
            String imageUrl,
            boolean showDirectly,
            boolean showButtons) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(
                () -> {
                    try {
                        URL url = new URL(imageUrl);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream inputStream = connection.getInputStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                        new Handler(Looper.getMainLooper())
                                .post(
                                        () ->
                                                sendNotification(
                                                        context,
                                                        title,
                                                        message,
                                                        bitmap,
                                                        showDirectly,
                                                        showButtons));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    public void sendNotification(
            Context context, String title, String message, String imageUrl, boolean showDirectly) {
        sendNotification(context, title, message, imageUrl, showDirectly, false);
    }

    public void sendNotification(
            Context context, String title, String message, boolean showDirectly) {
        sendNotification(context, title, message, (Bitmap) null, showDirectly);
    }

    public void sendNotification(
            Context context, String title, String message, Bitmap image, boolean showDirectly) {
        sendNotification(context, title, message, image, showDirectly, false);
    }

    public void sendNotification(
            Context context,
            String title,
            String message,
            Bitmap image,
            boolean showDirectly,
            boolean showButtons) {
        Notification notif = new Notification(title, message, image);
        sendNotification(context, notif, showDirectly, showButtons);
    }

    public void sendNotification(
            Context context, Notification notif, boolean showDirectly, boolean showButtons) {
        updateNotificationList(notif);

        // Build the notification
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, "APP_CHANNEL_ID")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(notif.getTitle())
                        .setContentText(notif.getMessage())
                        .setPriority(
                                showDirectly
                                        ? NotificationCompat.PRIORITY_HIGH
                                        : NotificationCompat.PRIORITY_DEFAULT);

        if (notif.getImage() != null) {
            NotificationCompat.BigPictureStyle style =
                    new NotificationCompat.BigPictureStyle()
                            .bigPicture(notif.getImage())
                            .setBigContentTitle(notif.getTitle())
                            .setSummaryText(notif.getMessage());
            builder.setStyle(style);
        }

        if (showButtons) {
            Intent approuverIntent = new Intent(context, CleanBroadcastReceiver.class);
            approuverIntent.setAction(ActionFields.APPROVE_ACTION);
            approuverIntent.putExtra(LocalNotificationFields.ID, notif.getId());
            approuverIntent.putExtra(
                    LocalNotificationFields.TRASH_ID,
                    String.valueOf(notif.getExtras().get(LocalNotificationFields.TRASH_ID)));
            approuverIntent.putExtra(
                    LocalNotificationFields.CLEANER_ID,
                    String.valueOf(notif.getExtras().get(LocalNotificationFields.CLEANER_ID)));
            approuverIntent.putExtra(
                    LocalNotificationFields.CLEANING_REQUEST_ID,
                    String.valueOf(
                            notif.getExtras().get(LocalNotificationFields.CLEANING_REQUEST_ID)));
            PendingIntent approuverPendingIntent =
                    PendingIntent.getBroadcast(
                            context,
                            0,
                            approuverIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            NotificationCompat.Action approuverAction =
                    new NotificationCompat.Action.Builder(0, "Approuver", approuverPendingIntent)
                            .build();

            Intent rejeterIntent = new Intent(context, CleanBroadcastReceiver.class);
            rejeterIntent.setAction(ActionFields.REJECT_ACTION);
            rejeterIntent.putExtra(LocalNotificationFields.ID, notif.getId());
            rejeterIntent.putExtra(
                    LocalNotificationFields.TRASH_ID,
                    String.valueOf(notif.getExtras().get(LocalNotificationFields.TRASH_ID)));
            rejeterIntent.putExtra(
                    LocalNotificationFields.CLEANER_ID,
                    String.valueOf(notif.getExtras().get(LocalNotificationFields.CLEANER_ID)));
            rejeterIntent.putExtra(
                    LocalNotificationFields.CLEANING_REQUEST_ID,
                    String.valueOf(
                            notif.getExtras().get(LocalNotificationFields.CLEANING_REQUEST_ID)));
            PendingIntent rejeterPendingIntent =
                    PendingIntent.getBroadcast(
                            context,
                            1,
                            rejeterIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            NotificationCompat.Action rejeterAction =
                    new NotificationCompat.Action.Builder(0, "Rejeter", rejeterPendingIntent)
                            .build();

            builder.addAction(approuverAction);
            builder.addAction(rejeterAction);
        }

        if (showDirectly) {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        }

        // Display the notification
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)) {
            ActivityCompat.requestPermissions(
                    (Activity) context, new String[] {Manifest.permission.POST_NOTIFICATIONS}, 0);
        }

        builder.setSmallIcon(R.drawable.logo);
        getNotificationManager().notify(notif.getId(), builder.build());
    }

    public static void updateNotificationList(Notification notification) {
        Context context = Poubelledroid.getContext();
        SharedPreferences prefs =
                context.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE);
        String notificationListJson = prefs.getString(NOTIFICATION_LIST, null);
        JSONArray jsonArray;

        if (notificationListJson != null) {
            try {
                jsonArray = new JSONArray(notificationListJson);
            } catch (JSONException e) {
                e.printStackTrace();
                jsonArray = new JSONArray();
            }
        } else {
            jsonArray = new JSONArray();
        }

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(LocalNotificationFields.ID, notification.getId());
            jsonObject.put(LocalNotificationFields.TITLE, notification.getTitle());
            jsonObject.put(LocalNotificationFields.MESSAGE, notification.getMessage());
            jsonObject.put(LocalNotificationFields.TIMESTAMP, notification.getTimestamp());
            if (notification.getImage() != null) {
                jsonObject.put(
                        LocalNotificationFields.IMAGE,
                        ImgUtils.bitmapToBase64(notification.getImage()));
            }
            if (!notification.getExtras().isEmpty()) {
                jsonObject.put(
                        LocalNotificationFields.EXTRAS, new JSONObject(notification.getExtras()));
            }
            jsonArray.put(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(NOTIFICATION_LIST, jsonArray.toString());
        editor.apply();
        updateIcon(true);
    }

    public static void updateIcon(boolean n) {
        newNotification = n;
        if (MapsActivity.fabNotificationCenter != null && newNotification) {
            MapsActivity.fabNotificationCenter.setImageResource(R.drawable.notif_ping);
        } else if (MapsActivity.fabNotificationCenter != null) {
            MapsActivity.fabNotificationCenter.setImageResource(R.drawable.notif_none);
        }
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
}
