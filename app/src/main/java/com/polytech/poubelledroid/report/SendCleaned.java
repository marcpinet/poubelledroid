package com.polytech.poubelledroid.report;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.polytech.poubelledroid.R;
import com.polytech.poubelledroid.fields.CleaningRequestsFields;
import com.polytech.poubelledroid.fields.FirebaseStorageFields;
import com.polytech.poubelledroid.utils.ImgUtils;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SendCleaned extends AppCompatActivity {
    public static final int CAMERA_REQUEST = 1888;
    private ImageView imageView;
    private String currentPhotoPath;
    private com.google.android.material.textfield.TextInputEditText descriptionEditText;

    private AlertDialog loadingDialog;
    private String reporterId;
    private String trashId;
    private String cleanerId;
    private String trashImgUrl;

    @SuppressWarnings("java:S1874")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Button postButton;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_cleaned);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        postButton = findViewById(R.id.post_button);
        postButton.setOnClickListener(v -> handlePostButton());

        this.imageView = (ImageView) this.findViewById(R.id.camerabackground);
        Intent intent = getIntent();
        Bitmap photo =
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                        ? intent.getParcelableExtra("PIC", Bitmap.class)
                        : intent.getParcelableExtra("PIC");
        imageView.setImageBitmap(photo);

        goToCamera();

        descriptionEditText = findViewById(R.id.description);

        AlertDialog.Builder loadingDialogBuilder = new AlertDialog.Builder(this);
        loadingDialogBuilder.setView(R.layout.dialog_loading);
        loadingDialog = loadingDialogBuilder.create();
        loadingDialog.setMessage("Envoi en cours...");

        reporterId = intent.getStringExtra(CleaningRequestsFields.REPORTER_ID);
        trashId = intent.getStringExtra(CleaningRequestsFields.TRASH_ID);
        cleanerId = intent.getStringExtra(CleaningRequestsFields.CLEANER_ID);
        trashImgUrl = intent.getStringExtra(CleaningRequestsFields.TRASH_IMG_URL);
    }

    private void handlePostButton() {
        String description = Objects.requireNonNull(descriptionEditText.getText()).toString();

        if (description.isEmpty()) {
            descriptionEditText.setError(
                    "Veuillez décrire la manière dont vous avez procédé pour le nettoyage");
            return;
        }

        loadingDialog.show();
        this.uploadImageAndData();
    }

    private void goToCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {

            String filename = "IMG" + System.currentTimeMillis();
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

            File image = new File(storageDir, filename + ".jpg");
            currentPhotoPath = image.getAbsolutePath();
            Uri imageUri =
                    FileProvider.getUriForFile(
                            this, "com.polytech.poubelledroid.fileprovider", image);

            // Starting the camera
            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(cameraIntent, CAMERA_REQUEST);
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Votre rapport sera perdu, êtes-vous sûr de vouloir quitter ?")
                .setPositiveButton("Yes", (dialog, which) -> finish())
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            new AlertDialog.Builder(this)
                    .setMessage("Votre rapport sera perdu, êtes-vous sûr de vouloir quitter ?")
                    .setPositiveButton("Yes", (dialog, which) -> finish())
                    .setNegativeButton("No", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    @Deprecated
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            Bitmap photo = null;
            try {
                photo = BitmapFactory.decodeFile(currentPhotoPath);
            } catch (OutOfMemoryError e) {
                // Scale down the image
                Toast.makeText(this, "Image trop lourde, elle sera compressée", Toast.LENGTH_LONG)
                        .show();
                BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
                bitmapOptions.inSampleSize = 2;
                photo = BitmapFactory.decodeFile(currentPhotoPath, bitmapOptions);
            }
            if (photo == null) {
                finish();
                return;
            }

            int rotationAngle = ImgUtils.getRotationAngleFromExif(currentPhotoPath);
            Bitmap rotatedPhoto = ImgUtils.rotateBitmap(photo, rotationAngle);
            int quality = 70; // Default quality is 70 cuz I decided so
            String compressedImagePath = currentPhotoPath.replace(".jpg", "_compressed.jpg");
            ImgUtils.compressAndSaveImage(rotatedPhoto, compressedImagePath, quality);
            currentPhotoPath = compressedImagePath;
            imageView.setImageBitmap(rotatedPhoto);

        } else {
            finish();
        }
    }

    private void uploadImageAndData() {
        // Post image on Firebase Storage
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        Uri fileUri = Uri.fromFile(new File(currentPhotoPath));
        StorageReference imageRef =
                storageReference.child(
                        FirebaseStorageFields.CLEANED_STORAGE_PATH + fileUri.getLastPathSegment());

        imageRef.putFile(fileUri)
                .addOnSuccessListener(
                        taskSnapshot ->
                                imageRef.getDownloadUrl()
                                        .addOnSuccessListener(
                                                uri -> {
                                                    FirebaseFirestore db =
                                                            FirebaseFirestore.getInstance();
                                                    Map<String, Object> cleaningRequest =
                                                            new HashMap<>();
                                                    cleaningRequest.put(
                                                            CleaningRequestsFields.CLEANER_ID,
                                                            cleanerId);
                                                    cleaningRequest.put(
                                                            CleaningRequestsFields.REPORTER_ID,
                                                            reporterId);
                                                    cleaningRequest.put(
                                                            CleaningRequestsFields.TRASH_ID,
                                                            trashId);
                                                    cleaningRequest.put(
                                                            CleaningRequestsFields.TRASH_IMG_URL,
                                                            trashImgUrl);
                                                    cleaningRequest.put(
                                                            CleaningRequestsFields
                                                                    .CLEANED_PHOTO_URL,
                                                            uri.toString());
                                                    cleaningRequest.put(
                                                            CleaningRequestsFields.MESSAGE,
                                                            Objects.requireNonNull(
                                                                            descriptionEditText
                                                                                    .getText())
                                                                    .toString());
                                                    cleaningRequest.put(
                                                            CleaningRequestsFields.STATUS,
                                                            0); // 0 = pending
                                                    cleaningRequest.put(
                                                            CleaningRequestsFields.DATE,
                                                            FieldValue.serverTimestamp());

                                                    db.collection(
                                                                    CleaningRequestsFields
                                                                            .COLLECTION_NAME)
                                                            .add(cleaningRequest)
                                                            .addOnSuccessListener(
                                                                    documentReference -> {
                                                                        // Set id of cleaning
                                                                        // request
                                                                        documentReference.update(
                                                                                CleaningRequestsFields
                                                                                        .ID,
                                                                                documentReference
                                                                                        .getId());
                                                                        loadingDialog.dismiss();

                                                                        FirebaseFunctions
                                                                                functions =
                                                                                        FirebaseFunctions
                                                                                                .getInstance(
                                                                                                        "europe-west1");
                                                                        Map<String, Object> data =
                                                                                new HashMap<>();
                                                                        data.put(
                                                                                CleaningRequestsFields
                                                                                        .REPORTER_ID,
                                                                                reporterId);
                                                                        data.put(
                                                                                CleaningRequestsFields
                                                                                        .CLEANER_ID,
                                                                                cleanerId);
                                                                        data.put(
                                                                                CleaningRequestsFields
                                                                                        .TRASH_ID,
                                                                                trashId);
                                                                        data.put(
                                                                                CleaningRequestsFields
                                                                                        .DESCRIPTION,
                                                                                descriptionEditText
                                                                                        .getText()
                                                                                        .toString());
                                                                        data.put(
                                                                                CleaningRequestsFields
                                                                                        .IMAGE_URL,
                                                                                uri.toString());
                                                                        data.put(
                                                                                CleaningRequestsFields
                                                                                        .ID,
                                                                                documentReference
                                                                                        .getId());

                                                                        functions
                                                                                .getHttpsCallable(
                                                                                        "sendNotificationToReporter")
                                                                                .call(data)
                                                                                .addOnCompleteListener(
                                                                                        task -> {
                                                                                            if (task
                                                                                                    .isSuccessful()) {
                                                                                                Log
                                                                                                        .d(
                                                                                                                "sendNotificationToReporter",
                                                                                                                "Notification sent successfully");
                                                                                            } else {
                                                                                                Log
                                                                                                        .e(
                                                                                                                "sendNotificationToReporter",
                                                                                                                "Error sending notification",
                                                                                                                task
                                                                                                                        .getException());
                                                                                            }
                                                                                        });

                                                                        Toast.makeText(
                                                                                        this,
                                                                                        "Votre nettoyage a été soumis avec succès",
                                                                                        Toast
                                                                                                .LENGTH_SHORT)
                                                                                .show();
                                                                        finish();
                                                                    })
                                                            .addOnFailureListener(
                                                                    e -> {
                                                                        loadingDialog.dismiss();
                                                                        Toast.makeText(
                                                                                        this,
                                                                                        "Erreur lors de l'envoi de la demande",
                                                                                        Toast
                                                                                                .LENGTH_SHORT)
                                                                                .show();
                                                                        finish();
                                                                    });
                                                })
                                        .addOnFailureListener(
                                                e -> {
                                                    Toast.makeText(
                                                                    this,
                                                                    "Erreur durant l'envoi de la photo vers nos serveurs",
                                                                    Toast.LENGTH_SHORT)
                                                            .show();
                                                    loadingDialog.dismiss();
                                                }));
    }
}
