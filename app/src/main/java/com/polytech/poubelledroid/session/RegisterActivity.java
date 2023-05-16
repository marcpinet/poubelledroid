package com.polytech.poubelledroid.session;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.polytech.poubelledroid.R;
import com.polytech.poubelledroid.fields.UsersFields;
import com.polytech.poubelledroid.utils.FirebaseUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.mindrot.jbcrypt.BCrypt;

public class RegisterActivity extends AppCompatActivity {

    private EditText registerEmailEditText;
    private EditText registerPasswordEditText;
    private EditText registerPseudoEditText;
    private Button registerButton;
    private TextView loginTextView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;

    private static final String USER_TABLE = "users";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        registerEmailEditText = findViewById(R.id.register_email_editText);
        registerPasswordEditText = findViewById(R.id.register_password_editText);
        registerPseudoEditText = findViewById(R.id.register_pseudo_editText);
        registerButton = findViewById(R.id.register_button);
        loginTextView = findViewById(R.id.login_textView);

        registerButton.setOnClickListener(v -> registerUser());
        loginTextView.setOnClickListener(v -> openLoginActivity());

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Inscription en cours...");
    }

    private boolean checkInformations(String email, String password, String pseudo) {
        if (email.isEmpty() || password.isEmpty() || pseudo.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return false;
        } else if (password.length() < 6) {
            Toast.makeText(
                            this,
                            "Le mot de passe doit contenir au moins 6 caractères",
                            Toast.LENGTH_SHORT)
                    .show();
            return false;
        } else if (pseudo.length() < 3) {
            Toast.makeText(
                            this,
                            "Le pseudo doit contenir au moins 3 caractères",
                            Toast.LENGTH_SHORT)
                    .show();
            return false;
        } else if (Pattern.compile("[^a-zA-Z0-9]").matcher(pseudo).find()) {
            Toast.makeText(
                            this,
                            "Le pseudo ne doit contenir que des lettres et des chiffres",
                            Toast.LENGTH_SHORT)
                    .show();
            return false;
        } else if (Pattern.compile("[^a-zA-Z0-9@._-]").matcher(email).find()) {
            Toast.makeText(this, "L'email n'est pas valide", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void registerUser() {
        String email = registerEmailEditText.getText().toString().trim();
        String password = registerPasswordEditText.getText().toString().trim();
        String pseudo = registerPseudoEditText.getText().toString().trim();

        if (!checkInformations(email, password, pseudo)) {
            return;
        }

        progressDialog.show();

        // Vérifier si l'email ou le pseudo sont déjà utilisés
        db.collection(USER_TABLE)
                .whereEqualTo(UsersFields.EMAIL, email)
                .get()
                .addOnCompleteListener(
                        task -> {
                            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                progressDialog.dismiss();
                                Toast.makeText(
                                                RegisterActivity.this,
                                                "Cet email est déjà utilisé",
                                                Toast.LENGTH_SHORT)
                                        .show();
                                return;
                            }

                            db.collection(USER_TABLE)
                                    .whereEqualTo(UsersFields.USERNAME, pseudo)
                                    .get()
                                    .addOnCompleteListener(
                                            task2 -> {
                                                if (task2.isSuccessful()
                                                        && !task2.getResult().isEmpty()) {
                                                    progressDialog.dismiss();
                                                    Toast.makeText(
                                                                    RegisterActivity.this,
                                                                    "Ce pseudo est déjà utilisé",
                                                                    Toast.LENGTH_SHORT)
                                                            .show();
                                                    return;
                                                }

                                                // Créer le compte utilisateur
                                                mAuth.createUserWithEmailAndPassword(
                                                                email, password)
                                                        .addOnCompleteListener(
                                                                RegisterActivity.this,
                                                                task3 -> {
                                                                    if (task3.isSuccessful()) {
                                                                        FirebaseUser user =
                                                                                mAuth
                                                                                        .getCurrentUser();
                                                                        assert user != null;
                                                                        sendVerificationEmail(user);

                                                                        String hashedPassword =
                                                                                BCrypt.hashpw(
                                                                                        password,
                                                                                        BCrypt
                                                                                                .gensalt());

                                                                        FirebaseUtils.getFcmToken(
                                                                                fcmToken -> {
                                                                                    Map<
                                                                                                    String,
                                                                                                    Object>
                                                                                            userData =
                                                                                                    new HashMap<>();
                                                                                    userData.put(
                                                                                            UsersFields
                                                                                                    .ID,
                                                                                            user
                                                                                                    .getUid());
                                                                                    userData.put(
                                                                                            UsersFields
                                                                                                    .USERNAME,
                                                                                            pseudo);
                                                                                    userData.put(
                                                                                            UsersFields
                                                                                                    .EMAIL,
                                                                                            email);
                                                                                    userData.put(
                                                                                            UsersFields
                                                                                                    .PASSWORD,
                                                                                            hashedPassword);
                                                                                    userData.put(
                                                                                            UsersFields
                                                                                                    .FCM_TOKEN,
                                                                                            fcmToken);

                                                                                    db.collection(
                                                                                                    USER_TABLE)
                                                                                            .document(
                                                                                                    user
                                                                                                            .getUid())
                                                                                            .set(
                                                                                                    userData)
                                                                                            .addOnSuccessListener(
                                                                                                    aVoid ->
                                                                                                            Toast
                                                                                                                    .makeText(
                                                                                                                            RegisterActivity
                                                                                                                                    .this,
                                                                                                                            "Utilisateur enregistré avec succès",
                                                                                                                            Toast
                                                                                                                                    .LENGTH_SHORT)
                                                                                                                    .show())
                                                                                            .addOnFailureListener(
                                                                                                    e ->
                                                                                                            Toast
                                                                                                                    .makeText(
                                                                                                                            RegisterActivity
                                                                                                                                    .this,
                                                                                                                            "Erreur lors de l'enregistrement de l'utilisateur",
                                                                                                                            Toast
                                                                                                                                    .LENGTH_SHORT)
                                                                                                                    .show());
                                                                                    setResult(
                                                                                            Activity
                                                                                                    .RESULT_OK);
                                                                                    progressDialog
                                                                                            .dismiss();
                                                                                    finish();
                                                                                });
                                                                    } else {
                                                                        progressDialog.dismiss();
                                                                        Toast.makeText(
                                                                                        RegisterActivity
                                                                                                .this,
                                                                                        "Erreur lors de l'inscription",
                                                                                        Toast
                                                                                                .LENGTH_SHORT)
                                                                                .show();
                                                                    }
                                                                });
                                            });
                        });
    }

    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(
                        this,
                        task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(
                                                RegisterActivity.this,
                                                "E-mail de vérification envoyé",
                                                Toast.LENGTH_SHORT)
                                        .show();
                            } else {
                                Toast.makeText(
                                                RegisterActivity.this,
                                                "Erreur lors de l'envoi de l'e-mail de vérification",
                                                Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
    }

    private void openLoginActivity() {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }
}
