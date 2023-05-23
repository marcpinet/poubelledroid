package com.polytech.poubelledroid.report;

import android.annotation.SuppressLint;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.polytech.poubelledroid.fields.LocalNotificationFields;
import com.polytech.poubelledroid.notificationcenter.Notification;
import com.polytech.poubelledroid.utils.FirebaseUtils;
import com.polytech.poubelledroid.utils.NotificationUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
public class FirebaseNotificationService extends FirebaseMessagingService {

    private FirebaseAuth.AuthStateListener authStateListener;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = remoteMessage.getData();
        if (data != null) {
            String title = data.get("title");
            int type = Integer.parseInt(Objects.requireNonNull(data.get("type")));
            NotificationUtils notificationUtils = new NotificationUtils(this);

            // 0 to reporter, 1 to cleaner
            if (type == 0) {
                Notification notificationObject = parseNotif(remoteMessage);
                notificationUtils.sendNotification(this, notificationObject);

                // If not, it's a notification from the reporter
            } else if (type == 1) {
                String body = data.get("body");
                notificationUtils.sendNotification(this, title, body, true);
            }
        }
    }

    private void setupAuthStateListener() {
        authStateListener =
                firebaseAuth -> {
                    if (firebaseAuth.getCurrentUser() != null) {
                        FirebaseUtils.updateFcmTokenIfNeeded();
                    }
                };
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setupAuthStateListener();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (authStateListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
        }
    }

    private Notification parseNotif(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        String title = data.get(LocalNotificationFields.TITLE);
        String cleaningRequestId = data.get(LocalNotificationFields.CLEANING_REQUEST_ID);
        String cleanerId = data.get(LocalNotificationFields.CLEANER_ID);
        String trashId = data.get(LocalNotificationFields.TRASH_ID);
        String description = data.get(LocalNotificationFields.DESCRIPTION);
        String imageUrl = data.get(LocalNotificationFields.IMAGE_URL);

        Map<String, String> extraInformations = new HashMap<>();
        extraInformations.put(LocalNotificationFields.CLEANER_ID, cleanerId);
        extraInformations.put(LocalNotificationFields.TRASH_ID, trashId);
        extraInformations.put(LocalNotificationFields.CLEANING_REQUEST_ID, cleaningRequestId);

        Notification notif = new Notification(title, description, imageUrl);
        notif.addExtraInformation(extraInformations);

        return notif;
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FirebaseNotificationService", "New token: " + token);

        FirebaseUtils.updateUserFcmToken(token);
    }
}
