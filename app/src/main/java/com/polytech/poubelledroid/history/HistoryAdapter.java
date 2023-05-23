package com.polytech.poubelledroid.history;

import static com.polytech.poubelledroid.Poubelledroid.getContext;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.polytech.poubelledroid.R;
import com.polytech.poubelledroid.googlemaps.MapsActivity;
import com.polytech.poubelledroid.report.SendReport;
import com.polytech.poubelledroid.utils.WasteUtils;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final List<Object> historyItems;

    public HistoryAdapter(Context context, List<Object> historyItems) {
        this.context = context;
        this.historyItems = historyItems;
    }

    @Override
    public int getItemViewType(int position) {
        Object item = historyItems.get(position);
        if (item instanceof QueryDocumentSnapshot) {
            QueryDocumentSnapshot document = (QueryDocumentSnapshot) item;
            return document.contains("type") ? 0 : 1;
        }
        return 1; // Default to CleaningRequest view type if not a QueryDocumentSnapshot
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == 0) {
            View view = inflater.inflate(R.layout.item_waste_history, parent, false);
            return new WasteViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_cleaning_request_history, parent, false);
            return new CleaningRequestViewHolder(view);
        }
    }

    public static class WasteViewHolder extends RecyclerView.ViewHolder {
        TextView wasteLocation;
        TextView wasteDescription;
        TextView wasteType;
        TextView wasteDate;
        ImageView wasteStatusIcon;
        ImageView wasteImage;

        public WasteViewHolder(@NonNull View itemView) {
            super(itemView);
            wasteLocation = itemView.findViewById(R.id.waste_location);
            wasteDescription = itemView.findViewById(R.id.waste_description);
            wasteType = itemView.findViewById(R.id.waste_type);
            wasteStatusIcon = itemView.findViewById(R.id.waste_status_icon);
            wasteImage = itemView.findViewById(R.id.waste_image);
            wasteDate = itemView.findViewById(R.id.waste_date);
        }
    }

    public static class CleaningRequestViewHolder extends RecyclerView.ViewHolder {
        TextView requestLocation;
        TextView requestMessage;
        TextView cleaningRequestDate;
        ImageView requestStatusIcon;
        ImageView requestImage;

        public CleaningRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            requestLocation = itemView.findViewById(R.id.cleaning_request_location);
            requestMessage = itemView.findViewById(R.id.cleaning_request_message);
            requestStatusIcon = itemView.findViewById(R.id.cleaning_request_status_icon);
            requestImage = itemView.findViewById(R.id.cleaning_request_image);
            cleaningRequestDate = itemView.findViewById(R.id.cleaning_request_date);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = historyItems.get(position);
        if (holder.getItemViewType() == 0) {
            WasteViewHolder wasteViewHolder = (WasteViewHolder) holder;
            QueryDocumentSnapshot document = (QueryDocumentSnapshot) item;

            boolean cleaned = Boolean.TRUE.equals(document.getBoolean("cleaned"));
            String description = document.getString("description");
            int type = Objects.requireNonNull(document.getLong("type")).intValue();
            String image = document.getString("image");
            Timestamp timestamp = document.getTimestamp("date");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            String dateText = sdf.format(timestamp.toDate());
            wasteViewHolder.wasteDate.setText(dateText);

            GeoPoint wasteLocation = document.getGeoPoint("coordinates");
            if (wasteLocation != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    WasteUtils.getAddressFromCoordinatesTiramisu(
                            context,
                            wasteLocation.getLatitude(),
                            wasteLocation.getLongitude(),
                            locationName -> wasteViewHolder.wasteLocation.setText(locationName));
                else
                    wasteViewHolder.wasteLocation.setText(
                            WasteUtils.getAddressFromCoordinates(
                                    getContext(),
                                    wasteLocation.getLatitude(),
                                    wasteLocation.getLongitude()));
            }
            wasteViewHolder.wasteDescription.setText(description);
            wasteViewHolder.wasteType.setText(SendReport.options[type]);
            wasteViewHolder.wasteStatusIcon.setImageResource(
                    cleaned ? R.drawable.cleaned : R.drawable.waste);

            Glide.with(context).load(image).into(wasteViewHolder.wasteImage);

        } else {
            CleaningRequestViewHolder requestViewHolder = (CleaningRequestViewHolder) holder;
            QueryDocumentSnapshot document = (QueryDocumentSnapshot) item;

            String message = document.getString("message");
            String photoUrl = document.getString("cleanedPhotoUrl");
            int status = Objects.requireNonNull(document.getLong("status")).intValue();
            Timestamp timestamp = document.getTimestamp("date");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            String dateText = sdf.format(timestamp.toDate());
            requestViewHolder.cleaningRequestDate.setText(dateText);

            String trashId = document.getString("trashId");

            MapsActivity.getLocationOfCleaningRequestFromTrashId(
                    trashId,
                    requestLocation -> {
                        if (requestLocation != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                WasteUtils.getAddressFromCoordinatesTiramisu(
                                        context,
                                        requestLocation.getLatitude(),
                                        requestLocation.getLongitude(),
                                        locationName ->
                                                requestViewHolder.requestLocation.setText(
                                                        locationName));
                            } else {
                                requestViewHolder.requestLocation.setText(
                                        WasteUtils.getAddressFromCoordinates(
                                                getContext(),
                                                requestLocation.getLatitude(),
                                                requestLocation.getLongitude()));
                            }
                        }
                    });

            requestViewHolder.requestMessage.setText(message);

            int statusIcon;
            switch (status) {
                case 0:
                    statusIcon = R.drawable.pending;
                    break;
                case 1:
                    statusIcon = R.drawable.approved;
                    break;
                default:
                    statusIcon = R.drawable.rejected;
                    break;
            }

            requestViewHolder.requestStatusIcon.setImageResource(statusIcon);

            Glide.with(context).load(photoUrl).into(requestViewHolder.requestImage);
        }
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }
}
