package com.polytech.poubelledroid.googlemaps;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.Timestamp;
import com.polytech.poubelledroid.R;

public class Waste {
    private final String id;
    private final String userId;
    private final BitmapDescriptor icon;
    private final int type;
    private final String description;
    private final LatLng position;
    private final String imageURL;
    private final Timestamp date;

    public Waste(
            String id,
            int type,
            String description,
            LatLng position,
            String imageURL,
            String userId,
            Timestamp date) {
        this.id = id;
        switch (type) {
            case 1:
                icon = BitmapDescriptorFactory.fromResource(R.drawable.poubelle_1);
                break;
            case 2:
                icon = BitmapDescriptorFactory.fromResource(R.drawable.poubelle_2);
                break;
            case 3:
                icon = BitmapDescriptorFactory.fromResource(R.drawable.poubelle_3);
                break;
            default:
                icon = BitmapDescriptorFactory.fromResource(R.drawable.poubelle_4);
                break;
        }
        this.type = type;
        this.position = position;
        this.description = description;
        this.imageURL = imageURL;
        this.userId = userId;
        this.date = date;
    }

    public BitmapDescriptor getIcon() {
        return icon;
    }

    public LatLng getPosition() {
        return position;
    }

    public int getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getImageURL() {
        return imageURL;
    }

    public int getIconDrawableId() {
        switch (type) {
            case 1:
                return R.drawable.poubelle_1;
            case 2:
                return R.drawable.poubelle_2;
            case 3:
                return R.drawable.poubelle_3;
            default:
                return R.drawable.poubelle_4;
        }
    }

    public String getUserId() {
        return userId;
    }

    public String getId() {
        return id;
    }

    public Timestamp getDate() {
        return date;
    }
}
