package com.polytech.poubelledroid.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.polytech.poubelledroid.R;
import com.polytech.poubelledroid.fields.UsersFields;
import com.polytech.poubelledroid.session.LoginActivity;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    private TextView userNameTextView;
    private TextView emailTextView;
    private Button logoutButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SwitchMaterial darkModeSwitch;
    private SeekBar seekBar;
    private TextView sliderValue;
    private Button saveButton;

    public static String username = null;
    public static String email = null;
    public static int darkThemeValue = 0;
    public static int wasteDaysOldValue = 0;

    public static String SHAREDPREFS = "settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        userNameTextView = findViewById(R.id.user_name_text_view);
        emailTextView = findViewById(R.id.email_text_view);
        logoutButton = findViewById(R.id.logout_button);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (SettingsActivity.username == null) {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                db.collection("users")
                        .document(user.getUid())
                        .get()
                        .addOnCompleteListener(
                                task -> {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();
                                        if (document != null && document.exists()) {
                                            String userName =
                                                    document.getString(UsersFields.USERNAME);
                                            userNameTextView.setText("Pseudo : " + userName);
                                            emailTextView.setText("Email : " + user.getEmail());
                                            SettingsActivity.username = userName;
                                            SettingsActivity.email = user.getEmail();
                                        }
                                    }
                                });
            }
        } else {
            userNameTextView.setText("Pseudo : " + SettingsActivity.username);
            emailTextView.setText("Email : " + SettingsActivity.email);
        }

        logoutButton.setOnClickListener(
                v -> {
                    mAuth.signOut();
                    Toast.makeText(
                                    SettingsActivity.this,
                                    "Déconnexion effectuée",
                                    Toast.LENGTH_SHORT)
                            .show();
                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                });

        saveButton = findViewById(R.id.save_settings_button);
        saveButton.setOnClickListener(v -> saveSettings());

        darkModeSwitch = findViewById(R.id.dark_mode_switch);
        darkModeSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    darkThemeValue = isChecked ? 1 : 0;
                });

        seekBar = findViewById(R.id.custom_seekbar);
        sliderValue = findViewById(R.id.slider_value);

        loadSettings();
    }

    private void loadSettings() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHAREDPREFS, MODE_PRIVATE);

        /* WASTE DAYS OLD */
        wasteDaysOldValue =
                sharedPreferences.getInt(UserSettings.WASTE_DAYS_OLD.name(), seekBar.getMax());
        seekBar.setProgress(wasteDaysOldValue);
        sliderValue.setText(
                wasteDaysOldValue == seekBar.getMax()
                        ? "Illimitée"
                        : String.valueOf(wasteDaysOldValue));
        seekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (progress < 1) {
                            seekBar.setProgress(1);
                            progress = 1;
                        }

                        if (progress == seekBar.getMax()) {
                            sliderValue.setText("Illimitée");
                        } else {
                            sliderValue.setText(String.valueOf(progress));
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}
                });

        /* DARK MODE */
        darkThemeValue = sharedPreferences.getInt(UserSettings.DARK_THEME.name(), -1);
        if (darkThemeValue == -1)
            darkThemeValue =
                    AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
                                    || AppCompatDelegate.getDefaultNightMode()
                                            == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                            ? 1
                            : 0;
        darkModeSwitch.setChecked(darkThemeValue == 1);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveSettings() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHAREDPREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt(UserSettings.DARK_THEME.name(), darkThemeValue);
        editor.putInt(UserSettings.WASTE_DAYS_OLD.name(), seekBar.getProgress());

        if (darkThemeValue == 1)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        editor.apply();

        Toast.makeText(
                        SettingsActivity.this,
                        "Paramètres sauvegardés avec succès",
                        Toast.LENGTH_SHORT)
                .show();

        Intent intent = getIntent();
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
