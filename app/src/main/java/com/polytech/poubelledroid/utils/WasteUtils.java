package com.polytech.poubelledroid.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.polytech.poubelledroid.fields.WasteFields;
import com.polytech.poubelledroid.utils.geocoder.GeocodeResultListener;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class WasteUtils {

    private static final String UNKNOWN_LOCATION_ERROR = "Unknown location";

    private WasteUtils() {
        // Prevent instantiation
    }

    public static boolean isValidWaste(QueryDocumentSnapshot document, int maxDays) {
        Timestamp creationDate = document.getTimestamp(WasteFields.DATE);
        Timestamp today = new Timestamp(new Date());

        assert creationDate != null;
        long diffInMillis = today.toDate().getTime() - creationDate.toDate().getTime();
        long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);

        int type = Objects.requireNonNull(document.getLong(WasteFields.TYPE)).intValue();
        boolean cleaned = Objects.requireNonNull(document.getBoolean(WasteFields.CLEANED));

        return !(maxDays != 51 && diffInDays > maxDays) && type != 0 && !cleaned;
    }

    public static void getAddressFromCoordinatesTiramisu(
            Context context,
            double latitude,
            double longitude,
            GeocodeResultListener resultListener) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        Geocoder.GeocodeListener geocodeListener = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocodeListener =
                    new Geocoder.GeocodeListener() {
                        @Override
                        public void onGeocode(@NonNull List<Address> addresses) {
                            if (!addresses.isEmpty()) {
                                Address address = addresses.get(0);
                                String locationName =
                                        address.getLocality() + ", " + address.getCountryName();
                                resultListener.onGeocodeResult(locationName);
                            } else resultListener.onGeocodeResult(UNKNOWN_LOCATION_ERROR);
                        }

                        @Override
                        public void onError(String message) {
                            resultListener.onGeocodeResult(UNKNOWN_LOCATION_ERROR);
                        }
                    };
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            geocoder.getFromLocation(latitude, longitude, 1, geocodeListener);
        else resultListener.onGeocodeResult(UNKNOWN_LOCATION_ERROR);
    }

    public static String getAddressFromCoordinates(
            Context context, double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                return address.getLocality() + ", " + address.getCountryName();
            }
        } catch (IOException e) {
            Log.e("MapsActivity", "Error getting address from coordinates: ", e);
        }
        return UNKNOWN_LOCATION_ERROR;
    }
}
