package com.polytech.poubelledroid.report;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.polytech.poubelledroid.R;
import com.polytech.poubelledroid.fields.FirebaseStorageFields;
import com.polytech.poubelledroid.fields.WasteFields;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SendReport extends AppCompatActivity {
    public static final int CAMERA_REQUEST = 1888;
    private FusedLocationProviderClient fusedLocationClient;
    private ImageView imageView;
    private TextInputLayout dropdownLayout;
    private String currentPhotoPath;
    private com.google.android.material.textfield.TextInputEditText descriptionEditText;
    private Button postButton;
    private LatLng currentPosition;
    public static String[] options =
            new String[] {
                "Sélectionnez un type de déchet...",
                "🍏 Déchet Alimentaire",
                "📦 Emballage, Bouteille, Carton",
                "🗄 Meuble abandonné",
                "🗑 Autre type de déchet"
            };
    private AlertDialog loadingDialog;

    @SuppressWarnings("java:S1874")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_report);

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

        dropdownLayout = findViewById(R.id.dropdown_layout);
        descriptionEditText = findViewById(R.id.description);

        AlertDialog.Builder loadingDialogBuilder = new AlertDialog.Builder(this);
        loadingDialogBuilder.setView(R.layout.dialog_loading);
        loadingDialog = loadingDialogBuilder.create();
        loadingDialog.setMessage("Envoi en cours...");

        setupDropdown();
    }

    private void handlePostButton() {
        String description = Objects.requireNonNull(descriptionEditText.getText()).toString();
        String dropdown = Objects.requireNonNull(dropdownLayout.getEditText()).getText().toString();

        if (description.isEmpty()) {
            descriptionEditText.setError("Veuillez saisir une description");
            return;
        }

        if (dropdown.isEmpty() || Arrays.asList(options).indexOf(dropdown) == 0) {
            dropdownLayout.setError("Veuillez sélectionner un type de déchet");
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

    private void setupDropdown() {
        ArrayAdapter<String> dropdownArrayAdapter =
                new ArrayAdapter<>(this, R.layout.dropdown_item, options);

        com.google.android.material.textfield.MaterialAutoCompleteTextView dropdown =
                (com.google.android.material.textfield.MaterialAutoCompleteTextView)
                        dropdownLayout.getEditText();
        if (dropdown != null) {
            dropdown.setAdapter(dropdownArrayAdapter);

            int padding = getResources().getDimensionPixelSize(R.dimen.dropdown_padding);
            dropdown.setPadding(padding, 0, padding, 0);

            dropdown.setOnItemClickListener(
                    (parent, view, position, id) -> {
                        String selectedItem = parent.getItemAtPosition(position).toString();
                        dropdown.setText(selectedItem, false);
                    });

            dropdown.setText(options[0], false);
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

            int rotationAngle = getRotationAngleFromExif(currentPhotoPath);
            Bitmap rotatedPhoto = rotateBitmap(photo, rotationAngle);
            int quality = 70; // Default quality is 70 cuz I decided so
            String compressedImagePath = currentPhotoPath.replace(".jpg", "_compressed.jpg");
            compressAndSaveImage(rotatedPhoto, compressedImagePath, quality);
            currentPhotoPath = compressedImagePath;
            imageView.setImageBitmap(rotatedPhoto);
            this.retreiveLocation();

        } else {
            finish();
        }
    }

    private void compressAndSaveImage(Bitmap bitmap, String outputFilePath, int quality) {
        try {
            FileOutputStream outputStream = new FileOutputStream(outputFilePath);
            // Compress the image as JPEG with the specified quality
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            Toast.makeText(
                            this,
                            "Erreur lors de la compression ou de la sauvegarde de l'image",
                            Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private int getRotationAngleFromExif(String imagePath) {
        int rotation = 0;
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation =
                    exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotation = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotation = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotation = 270;
                    break;

                default:
                    break;
            }
        } catch (IOException e) {
            Toast.makeText(
                            this,
                            "Erreur lors de la récupération de l'orientation de l'image",
                            Toast.LENGTH_SHORT)
                    .show();
        }
        return rotation;
    }

    private void uploadImageAndData() {
        // Post image on Firebase Storage
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        Uri fileUri = Uri.fromFile(new File(currentPhotoPath));
        StorageReference imageRef =
                storageReference.child(
                        FirebaseStorageFields.WASTE_STORAGE_PATH + fileUri.getLastPathSegment());

        imageRef.putFile(fileUri)
                .addOnSuccessListener(
                        taskSnapshot ->
                                imageRef.getDownloadUrl()
                                        .addOnSuccessListener(
                                                uri -> {
                                                    FirebaseFirestore db =
                                                            FirebaseFirestore.getInstance();
                                                    FirebaseAuth mAuth = FirebaseAuth.getInstance();
                                                    FirebaseUser user = mAuth.getCurrentUser();
                                                    String userId =
                                                            user != null ? user.getUid() : null;
                                                    GeoPoint coordinates =
                                                            new GeoPoint(
                                                                    currentPosition.latitude,
                                                                    currentPosition.longitude);
                                                    String description =
                                                            Objects.requireNonNull(
                                                                            descriptionEditText
                                                                                    .getText())
                                                                    .toString();
                                                    Map<String, Object> wasteData = new HashMap<>();
                                                    wasteData.put(WasteFields.USER_ID, userId);
                                                    wasteData.put(
                                                            WasteFields.TYPE,
                                                            Arrays.asList(options)
                                                                    .indexOf(
                                                                            Objects.requireNonNull(
                                                                                            dropdownLayout
                                                                                                    .getEditText())
                                                                                    .getText()
                                                                                    .toString()));
                                                    wasteData.put(
                                                            WasteFields.COORDINATES, coordinates);
                                                    wasteData.put(
                                                            WasteFields.IMAGE, uri.toString());
                                                    wasteData.put(WasteFields.CLEANED, false);
                                                    wasteData.put(
                                                            WasteFields.DESCRIPTION, description);
                                                    wasteData.put(
                                                            WasteFields.DATE,
                                                            FieldValue.serverTimestamp());

                                                    db.collection(WasteFields.COLLECTION_NAME)
                                                            .add(wasteData)
                                                            .addOnSuccessListener(
                                                                    documentReference -> {
                                                                        documentReference.update(
                                                                                WasteFields.ID,
                                                                                documentReference
                                                                                        .getId());
                                                                        loadingDialog.dismiss();
                                                                        Toast.makeText(
                                                                                        this,
                                                                                        "Votre rapport a été envoyé avec succès",
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
                                                                                        "Erreur lors de l'authentification",
                                                                                        Toast
                                                                                                .LENGTH_SHORT)
                                                                                .show();
                                                                        finish();
                                                                    });
                                                }))
                .addOnFailureListener(
                        e -> {
                            Toast.makeText(
                                            this,
                                            "Erreur durant l'envoi de la photo vers nos serveurs",
                                            Toast.LENGTH_SHORT)
                                    .show();
                            loadingDialog.dismiss();
                        });
    }

    private void retreiveLocation() {
        getLocation(
                new OnLocationReceivedListener() {
                    @Override
                    public void onLocationReceived(LatLng location) {
                        currentPosition = location;
                    }

                    @Override
                    public void onLocationError() {
                        loadingDialog.dismiss();
                        Toast.makeText(
                                        SendReport.this,
                                        "Erreur lors de la récupération de votre géolocalisation",
                                        Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }

    public interface OnLocationReceivedListener {
        void onLocationReceived(LatLng location);

        void onLocationError();
    }

    private void getLocation(OnLocationReceivedListener listener) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Task<Location> lastLocation = fusedLocationClient.getLastLocation();
            lastLocation.addOnSuccessListener(
                    this,
                    location -> {
                        if (location != null) {
                            currentPosition =
                                    new LatLng(location.getLatitude(), location.getLongitude());
                            listener.onLocationReceived(currentPosition);
                        } else {
                            listener.onLocationError();
                        }
                    });
        } else {
            requestLocationPermissions();
        }
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(
                this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }
}
