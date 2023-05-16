package com.polytech.poubelledroid;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import com.google.firebase.FirebaseApp;

// Singleton
public class Poubelledroid extends Application {
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        context = getApplicationContext();
    }

    public static Context getContext() {
        return context;
    }
}
