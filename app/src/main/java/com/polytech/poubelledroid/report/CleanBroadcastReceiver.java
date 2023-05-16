package com.polytech.poubelledroid.report;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.core.app.NotificationManagerCompat;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.HashMap;
import java.util.Map;

public class CleanBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int notificationId = intent.getIntExtra("notification_id", -1);
        String trashId = intent.getStringExtra("trashId");
        String cleanerId = intent.getStringExtra("cleanerId");
        String cleaningRequestId = intent.getStringExtra("cleaningRequestId");

        if ("APPROUVER_ACTION".equals(action)) {
            approve(cleaningRequestId, trashId, cleanerId);
        } else if ("REJETER_ACTION".equals(action)) {
            reject(cleaningRequestId, trashId, cleanerId);
        }

        if (notificationId != -1) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.cancel(notificationId);
        }
    }

    public static void checkCleaningRequestStatus(
            String cleaningRequestId, OnStatusCheckedListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("cleaningRequests")
                .document(cleaningRequestId)
                .get()
                .addOnSuccessListener(
                        documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                int status = documentSnapshot.getLong("status").intValue();
                                listener.onStatusChecked(status);
                            } else {
                                Log.e("CleaningRequest", "CleaningRequest not found");
                                listener.onStatusChecked(
                                        -1); // Statut invalide si la cleaningRequest n'est pas
                                // trouvée
                            }
                        })
                .addOnFailureListener(
                        e -> Log.e("CleaningRequest", "Error checking CleaningRequest status", e));
    }

    public interface OnStatusCheckedListener {
        void onStatusChecked(int status);
    }

    public static void callSendNotificationToCleaner(
            String cleaningRequestId,
            String trashId,
            String cleanerId,
            boolean approved,
            Runnable onSuccess) {
        FirebaseFunctions functions = FirebaseFunctions.getInstance("europe-west1");

        checkCleaningRequestStatus(
                cleaningRequestId,
                status -> {
                    if (status != 0) {
                        Log.e("sendNotificationToCleaner", "CleaningRequest status is not 0");
                        return;
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("id", cleaningRequestId);
                    data.put("trashId", trashId);
                    data.put("cleanerId", cleanerId);
                    data.put("approved", approved);

                    functions
                            .getHttpsCallable("sendNotificationToCleaner")
                            .call(data)
                            .addOnCompleteListener(
                                    task -> {
                                        if (task.isSuccessful()) {
                                            Log.d(
                                                    "sendNotificationToCleaner",
                                                    "Notification sent successfully");
                                            if (onSuccess != null) {
                                                onSuccess.run();
                                            }
                                        } else {
                                            Log.e(
                                                    "sendNotificationToCleaner",
                                                    "Error sending notification",
                                                    task.getException());
                                        }
                                    });
                });
    }

    private void approve(String cleaningRequestId, String trashId, String cleanerId) {
        callSendNotificationToCleaner(cleaningRequestId, trashId, cleanerId, true, null);
    }

    private void reject(String cleaningRequestId, String trashId, String cleanerId) {
        callSendNotificationToCleaner(cleaningRequestId, trashId, cleanerId, false, null);
    }
}
