package com.polytech.poubelledroid.report;

import android.annotation.SuppressLint;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
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
        String title = data.get("title");
        String cleaningRequestId = data.get("cleaningRequestId");
        String cleanerId = data.get("cleanerId");
        String trashId = data.get("trashId");
        String description = data.get("description");
        String imageUrl = data.get("imageUrl");

        Map<String, String> extraInformations = new HashMap<>();
        extraInformations.put("cleanerId", cleanerId);
        extraInformations.put("trashId", trashId);
        extraInformations.put("cleaningRequestId", cleaningRequestId);

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
