package com.polytech.poubelledroid.session;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.polytech.poubelledroid.R;
import com.polytech.poubelledroid.fields.UsersFields;
import com.polytech.poubelledroid.googlemaps.MapsActivity;
import com.polytech.poubelledroid.settings.SettingsActivity;
import com.polytech.poubelledroid.utils.FirebaseUtils;

public class LoginActivity extends AppCompatActivity {

    private EditText emailOrPseudoEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView registerTextView;
    private TextView forgotPasswordTextView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (!isGranted) {
                            Toast.makeText(
                                            this,
                                            "Vous ne recevrez des notifications que via le centre interne de l'app",
                                            Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        emailOrPseudoEditText = findViewById(R.id.email_editText);
        passwordEditText = findViewById(R.id.password_editText);
        loginButton = findViewById(R.id.login_button);
        registerTextView = findViewById(R.id.register_textView);
        forgotPasswordTextView = findViewById(R.id.forgot_password_textView);

        loginButton.setOnClickListener(v -> loginUser());
        registerTextView.setOnClickListener(v -> openRegisterActivity());
        forgotPasswordTextView.setOnClickListener(v -> sendResetPasswordLink());

        // Ask for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        // Ask for notification permission
        askForNotification();

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Connexion en cours...");
    }

    private void askForNotification() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                return;
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Voulez-vous activer les notifications ?");
                builder.setPositiveButton(
                        "Oui",
                        (dialog, which) ->
                                requestPermissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS));
                builder.setNegativeButton(
                        "Non",
                        (dialog, which) -> {
                            // Do nothing
                        });
                builder.show();
            }
        }
    }

    private void loginUser() {
        String emailOrPseudo = emailOrPseudoEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (emailOrPseudo.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez au moins remplir votre email", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        progressDialog.show();
        if (emailOrPseudo.contains("@")) {
            signInWithEmailAndPassword(emailOrPseudo, password);
        } else {
            db.collection("users")
                    .whereEqualTo(UsersFields.USERNAME, emailOrPseudo)
                    .get()
                    .addOnCompleteListener(
                            task -> {
                                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                    SettingsActivity.username = emailOrPseudo;
                                    String email =
                                            task.getResult()
                                                    .getDocuments()
                                                    .get(0)
                                                    .getString(UsersFields.EMAIL);
                                    signInWithEmailAndPassword(email, password);
                                } else {
                                    progressDialog.dismiss();
                                    Toast.makeText(
                                                    LoginActivity.this,
                                                    "Erreur de connexion",
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                }
                            });
        }
    }

    private void signInWithEmailAndPassword(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(
                        this,
                        task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null && user.isEmailVerified()) {
                                    FirebaseUtils.updateFcmTokenIfNeeded();
                                    progressDialog.dismiss();
                                    Toast.makeText(
                                                    LoginActivity.this,
                                                    "Connexion réussie",
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                    Intent intent =
                                            new Intent(LoginActivity.this, MapsActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    progressDialog.dismiss();
                                    Toast.makeText(
                                                    LoginActivity.this,
                                                    "Veuillez vérifier votre adresse e-mail",
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                }
                            } else {
                                progressDialog.dismiss();
                                Toast.makeText(
                                                LoginActivity.this,
                                                "Erreur de connexion",
                                                Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
    }

    private void openRegisterActivity() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }

    private void sendResetPasswordLink() {
        String emailOrPseudo = emailOrPseudoEditText.getText().toString().trim();
        if (emailOrPseudo.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        if (emailOrPseudo.contains("@")) {
            mAuth.sendPasswordResetEmail(emailOrPseudo)
                    .addOnCompleteListener(
                            task -> {
                                if (task.isSuccessful()) {
                                    progressDialog.dismiss();
                                    Toast.makeText(
                                                    LoginActivity.this,
                                                    "Un e-mail de réinitialisation a été envoyé",
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                } else {
                                    progressDialog.dismiss();
                                    Toast.makeText(
                                                    LoginActivity.this,
                                                    "Erreur lors de l'envoi de l'e-mail de réinitialisation",
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                }
                            });
        } else {
            db.collection("users")
                    .whereEqualTo(UsersFields.USERNAME, emailOrPseudo)
                    .get()
                    .addOnCompleteListener(
                            task -> {
                                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                    String email =
                                            task.getResult()
                                                    .getDocuments()
                                                    .get(0)
                                                    .getString(UsersFields.EMAIL);
                                    mAuth.sendPasswordResetEmail(email)
                                            .addOnCompleteListener(
                                                    task1 -> {
                                                        if (task1.isSuccessful()) {
                                                            progressDialog.dismiss();
                                                            Toast.makeText(
                                                                            LoginActivity.this,
                                                                            "Un e-mail de réinitialisation a été envoyé",
                                                                            Toast.LENGTH_SHORT)
                                                                    .show();
                                                        } else {
                                                            progressDialog.dismiss();
                                                            Toast.makeText(
                                                                            LoginActivity.this,
                                                                            "Le mail n'a pas pu être envoyé",
                                                                            Toast.LENGTH_SHORT)
                                                                    .show();
                                                        }
                                                    });
                                } else {
                                    progressDialog.dismiss();
                                    Toast.makeText(
                                                    LoginActivity.this,
                                                    "Aucun email associé à ce pseudo",
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                }
                            });
        }
    }
}
