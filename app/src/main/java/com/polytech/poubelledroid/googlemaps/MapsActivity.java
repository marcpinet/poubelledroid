package com.polytech.poubelledroid.googlemaps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.polytech.poubelledroid.R;
import com.polytech.poubelledroid.betterimageviewer.FullScreenImageActivity;
import com.polytech.poubelledroid.fields.WasteFields;
import com.polytech.poubelledroid.history.HistoryActivity;
import com.polytech.poubelledroid.notificationcenter.NotificationCenterActivity;
import com.polytech.poubelledroid.report.SendCleaned;
import com.polytech.poubelledroid.report.SendReport;
import com.polytech.poubelledroid.settings.SettingsActivity;
import com.polytech.poubelledroid.settings.UserSettings;
import com.polytech.poubelledroid.socialnetflow.TwitterFeedActivity;
import com.polytech.poubelledroid.utils.WasteUtils;
import com.polytech.poubelledroid.utils.geocoder.LocationResultListener;
import java.util.ArrayList;
import java.util.Objects;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private final ArrayList<Waste> currentWastes = new ArrayList<>();

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    public static FloatingActionButton fabNotificationCenter;
    private AlertDialog loadingDialog;

    private ActivityResultLauncher<Intent> settingsLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Button actusButton;
        Button snapButton;
        Button historySettings;
        FloatingActionButton settingsButton;
        FloatingActionButton refreshButton;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        fabNotificationCenter = findViewById(R.id.fab_notification_center);
        fabNotificationCenter.setOnClickListener(
                v -> {
                    Intent intent = new Intent(MapsActivity.this, NotificationCenterActivity.class);
                    startActivity(intent);
                });

        refreshButton = findViewById(R.id.fab_refresh);
        refreshButton.setOnClickListener(
                v -> {
                    loadingDialog.show();
                    loadTrashData();
                });

        settingsButton = findViewById(R.id.fab_settings);
        settingsButton.setOnClickListener(v -> openSettingsActivity());

        actusButton = findViewById(R.id.actus);
        snapButton = findViewById(R.id.snap);
        historySettings = findViewById(R.id.history);

        AlertDialog.Builder loadingDialogBuilder = new AlertDialog.Builder(this);
        loadingDialogBuilder.setView(R.layout.dialog_loading);
        loadingDialog = loadingDialogBuilder.create();
        loadingDialog.setMessage("Actualisation des déchets en cours...");

        actusButton.setOnClickListener(v -> openTwitterActivity());
        historySettings.setOnClickListener(v -> openHistoryActivity());
        snapButton.setOnClickListener(v -> openSendReportActivity());

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        settingsLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == Activity.RESULT_OK) loadTrashData();
                        });
    }

    public static void getLocationOfCleaningRequestFromTrashId(
            String trashId, LocationResultListener resultListener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(WasteFields.COLLECTION_NAME)
                .whereEqualTo(WasteFields.ID, trashId)
                .get()
                .addOnCompleteListener(
                        task -> {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document :
                                        Objects.requireNonNull(task.getResult())) {
                                    GeoPoint location = document.getGeoPoint("coordinates");
                                    resultListener.onLocationResult(location);
                                }
                            } else
                                Log.d(
                                        "MapsActivity",
                                        "Error getting documents: ",
                                        task.getException());
                        });
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Êtes vous sûr de vouloir quitter l'application ?")
                .setPositiveButton("Yes", (dialog, which) -> finish())
                .setNegativeButton("No", null)
                .show();
    }

    private void openTwitterActivity() {
        Intent intent = new Intent(MapsActivity.this, TwitterFeedActivity.class);
        startActivity(intent);
    }

    private void openSendReportActivity() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 1);

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(MapsActivity.this, SendReport.class);
                startActivity(intent);
            } else {
                Toast.makeText(
                                this,
                                "Vous devez autoriser l'accès à la caméra pour utiliser cette fonctionnalité",
                                Toast.LENGTH_SHORT)
                        .show();
            }
        } else {
            Intent intent = new Intent(MapsActivity.this, SendReport.class);
            startActivity(intent);
        }
    }

    private void openHistoryActivity() {
        Intent intent = new Intent(MapsActivity.this, HistoryActivity.class);
        startActivity(intent);
    }

    private void openSettingsActivity() {
        Intent intent = new Intent(MapsActivity.this, SettingsActivity.class);
        settingsLauncher.launch(intent);
    }

    private void loadTrashData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Remove all current markers
        this.currentWastes.clear();
        mMap.clear();

        SharedPreferences sharedPreferences =
                getSharedPreferences(SettingsActivity.SHAREDPREFS, MODE_PRIVATE);
        int maxDays = sharedPreferences.getInt(UserSettings.WASTE_DAYS_OLD.name(), 51);

        db.collection(WasteFields.COLLECTION_NAME)
                .get()
                .addOnCompleteListener(
                        task -> {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document :
                                        Objects.requireNonNull(task.getResult())) {
                                    if (!WasteUtils.isValidWaste(document, maxDays)) {
                                        continue;
                                    }

                                    GeoPoint coordinates =
                                            document.getGeoPoint(WasteFields.COORDINATES);
                                    String image = document.getString(WasteFields.IMAGE);
                                    String description =
                                            document.getString(WasteFields.DESCRIPTION);
                                    String id = document.getString(WasteFields.USER_ID);
                                    int type =
                                            Objects.requireNonNull(
                                                            document.getLong(WasteFields.TYPE))
                                                    .intValue();

                                    if (coordinates != null) {
                                        Waste waste =
                                                new Waste(
                                                        document.getId(),
                                                        type,
                                                        description,
                                                        new LatLng(
                                                                coordinates.getLatitude(),
                                                                coordinates.getLongitude()),
                                                        image,
                                                        id);
                                        currentWastes.add(waste);
                                    }
                                }
                                addMarkersToMap();
                            } else {
                                Toast.makeText(
                                                MapsActivity.this,
                                                "Erreur lors du chargement des données",
                                                Toast.LENGTH_SHORT)
                                        .show();
                            }
                            loadingDialog.dismiss();
                        });
    }

    private BitmapDescriptor resizeBitmap(int drawableId, int width, int height) {
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), drawableId);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
        return BitmapDescriptorFactory.fromBitmap(resizedBitmap);
    }

    private void addMarkersToMap() {
        int c = 0;

        Resources r = getResources();
        int widthAndHeightInPx =
                (int)
                        TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, 32, r.getDisplayMetrics());

        for (Waste waste : currentWastes) {
            mMap.addMarker(
                    new MarkerOptions()
                            .position(waste.getPosition())
                            .icon(
                                    resizeBitmap(
                                            waste.getIconDrawableId(),
                                            widthAndHeightInPx,
                                            widthAndHeightInPx))
                            .title(c + ""));
            c++;
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
                                this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        loadTrashData();

        mMap.setOnMarkerClickListener(
                marker -> {
                    showPopupWindow(marker);
                    return true;
                });

        mFusedLocationProviderClient
                .getLastLocation()
                .addOnSuccessListener(
                        this,
                        location -> {
                            if (location != null) {
                                LatLng userLatLng =
                                        new LatLng(location.getLatitude(), location.getLongitude());
                                mMap.animateCamera(
                                        CameraUpdateFactory.newLatLngZoom(userLatLng, 15));
                            }
                        });

        // Load the map style
        int nightModeFlags =
                getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark));
        }
    }

    @SuppressLint("SetTextI18n")
    private void showPopupWindow(Marker marker) {
        PopupWindow mPopupWindow;
        LayoutInflater layoutInflater =
                (LayoutInflater)
                        MapsActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams")
        View customView = layoutInflater.inflate(R.layout.info_window, null);

        Button clearMarker = customView.findViewById(R.id.button_clear_marker);
        TextView titleTextView = customView.findViewById(R.id.title);
        TextView descriptionTextView = customView.findViewById(R.id.description);
        ImageView closeButton = customView.findViewById(R.id.imageview_close_info_window);

        int index = Integer.parseInt(Objects.requireNonNull(marker.getTitle()));
        Waste waste = this.currentWastes.get(index);

        switch (waste.getType()) {
            case (1):
                titleTextView.setText(SendReport.options[1]);
                break;
            case (2):
                titleTextView.setText(SendReport.options[2]);
                break;
            case (3):
                titleTextView.setText(SendReport.options[3]);
                break;
            default:
                titleTextView.setText(SendReport.options[SendReport.options.length - 1]);
                break;
        }
        ImageView imageViewInfoWindow = customView.findViewById(R.id.imageView_info_window);

        Glide.with(this).load(waste.getImageURL()).into(imageViewInfoWindow);

        descriptionTextView.setText(waste.getDescription());

        imageViewInfoWindow.setOnClickListener(
                v -> {
                    Intent intent = new Intent(MapsActivity.this, FullScreenImageActivity.class);
                    String imageUrl = waste.getImageURL();
                    intent.putExtra("image_url", imageUrl);
                    startActivity(intent);
                });

        mPopupWindow =
                new PopupWindow(
                        customView,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);

        mPopupWindow.setElevation(5.0f);

        closeButton.setOnClickListener(view -> mPopupWindow.dismiss());

        clearMarker.setOnClickListener(
                view -> {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        Toast.makeText(
                                        MapsActivity.this,
                                        "Vous devez être connecté pour utiliser cette fonctionnalité",
                                        Toast.LENGTH_SHORT)
                                .show();
                        return;
                    }
                    Intent intent = new Intent(MapsActivity.this, SendCleaned.class);
                    intent.putExtra("cleanerId", user.getUid());
                    intent.putExtra("trashId", waste.getId());
                    intent.putExtra("reporterId", waste.getUserId());
                    intent.putExtra("trashImgUrl", waste.getImageURL());
                    startActivity(intent);
                });

        mPopupWindow.showAtLocation(customView, Gravity.CENTER, 0, 0);
    }
}
