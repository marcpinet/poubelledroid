package com.polytech.poubelledroid.utils.geocoder;

import com.google.firebase.firestore.GeoPoint;

public interface LocationResultListener {
    void onLocationResult(GeoPoint location);
}
