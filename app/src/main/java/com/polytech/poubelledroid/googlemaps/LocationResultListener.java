package com.polytech.poubelledroid.googlemaps;

import com.google.firebase.firestore.GeoPoint;

public interface LocationResultListener {
    void onLocationResult(GeoPoint location);
}
