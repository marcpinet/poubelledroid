package com.polytech.poubelledroid.utils;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.polytech.poubelledroid.fields.UsersFields;
import java.util.Objects;
import java.util.function.Consumer;

public class FirebaseUtils {

    public static void getFcmToken(Consumer<String> onTokenReceived) {
        FirebaseMessaging.getInstance()
                .getToken()
                .addOnCompleteListener(
                        task -> {
                            if (!task.isSuccessful()) {
                                Log.w(
                                        "RegisterActivity",
                                        "Fetching FCM registration token failed",
                                        task.getException());
                                onTokenReceived.accept(null);
                                return;
                            }
                            // Get the token from the task
                            String token = task.getResult();
                            onTokenReceived.accept(token);
                        });
    }

    public static void updateFcmTokenIfNeeded() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w("UpdateFcmToken", "User is not logged in");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userDocRef = db.collection("users").document(user.getUid());
        userDocRef
                .get()
                .addOnCompleteListener(
                        task -> {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document != null
                                        && document.exists()
                                        && !document.contains(UsersFields.FCM_TOKEN)) {
                                    Log.w(
                                            "UpdateFcmToken",
                                            "User document does not contain fcmToken, creating one...");
                                    getFcmToken(
                                            fcmToken ->
                                                    userDocRef.update(
                                                            UsersFields.FCM_TOKEN, fcmToken));
                                }
                                // Else if token is not equal to the one in the database, update it
                                else if (document != null
                                        && document.exists()
                                        && document.contains(UsersFields.FCM_TOKEN)
                                        && !Objects.equals(
                                                document.get(UsersFields.FCM_TOKEN),
                                                FirebaseMessaging.getInstance().getToken())) {
                                    Log.w(
                                            "UpdateFcmToken",
                                            "User document contains fcmToken, updating it...");
                                    getFcmToken(
                                            fcmToken ->
                                                    userDocRef.update(
                                                            UsersFields.FCM_TOKEN, fcmToken));
                                }
                            } else {
                                Log.w(
                                        "UpdateFcmToken",
                                        "Error getting user document",
                                        task.getException());
                            }
                        });
    }

    private static String getCurrentUserId() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            return null;
        }
        return auth.getCurrentUser().getUid();
    }

    public static void updateUserFcmToken(String token) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(userId)
                .update(UsersFields.FCM_TOKEN, token)
                .addOnSuccessListener(
                        aVoid ->
                                Log.d(
                                        "FirebaseNotificationService",
                                        "FCM Token updated successfully"))
                .addOnFailureListener(
                        e -> Log.e("FirebaseNotificationService", "Error updating FCM Token", e));
    }
}
