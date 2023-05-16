package com.polytech.poubelledroid;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.polytech.poubelledroid.googlemaps.MapsActivity;
import com.polytech.poubelledroid.session.LoginActivity;
import com.polytech.poubelledroid.settings.SettingsActivity;
import com.polytech.poubelledroid.settings.UserSettings;
import com.polytech.poubelledroid.utils.FirebaseUtils;
import com.polytech.poubelledroid.utils.NotificationUtils;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    ImageView background;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        background = findViewById(R.id.bg);
        Objects.requireNonNull(getSupportActionBar()).hide();

        NotificationUtils notificationUtils = new NotificationUtils(this);
        notificationUtils.createNotificationChannel();

        FirebaseUtils.updateFcmTokenIfNeeded();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUser
                    .getIdToken(true)
                    .addOnCompleteListener(
                            task -> {
                                if (task.isSuccessful() && currentUser.isEmailVerified()) {
                                    Intent intent =
                                            new Intent(MainActivity.this, MapsActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });
        } else {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void loadTheme() {
        SharedPreferences sharedPreferences =
                getSharedPreferences(SettingsActivity.SHAREDPREFS, MODE_PRIVATE);

        int isDarkTheme = sharedPreferences.getInt(UserSettings.DARK_THEME.name(), -1);
        if (isDarkTheme == -1)
            isDarkTheme =
                    AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
                                    || AppCompatDelegate.getDefaultNightMode()
                                            == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                            ? 1
                            : 0;

        if (isDarkTheme == 1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
