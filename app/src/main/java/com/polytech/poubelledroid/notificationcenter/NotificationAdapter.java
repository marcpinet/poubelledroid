package com.polytech.poubelledroid.notificationcenter;

import static com.polytech.poubelledroid.report.CleanBroadcastReceiver.checkCleaningRequestStatus;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.polytech.poubelledroid.R;
import com.polytech.poubelledroid.report.CleanBroadcastReceiver;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends ArrayAdapter<Notification> {

    public NotificationAdapter(Context context, List<Notification> notifications) {
        super(context, 0, notifications);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Notification notification = getItem(position);

        if (convertView == null) {
            convertView =
                    LayoutInflater.from(getContext())
                            .inflate(R.layout.notification_item, parent, false);
        }

        TextView title = convertView.findViewById(R.id.notification_title);
        TextView message = convertView.findViewById(R.id.notification_message);
        ImageView imageView = convertView.findViewById(R.id.notification_image);
        TextView approveText = convertView.findViewById(R.id.approve_text);
        TextView rejectText = convertView.findViewById(R.id.reject_text);
        LinearLayout actionButtonsContainer =
                convertView.findViewById(R.id.action_buttons_container);

        if (!notification.getExtras().isEmpty()) {
            checkCleaningRequestStatus(
                    notification.getExtras().get("cleaningRequestId"),
                    status -> {
                        if (status == 0) {
                            actionButtonsContainer.setVisibility(View.VISIBLE);

                            approveText.setOnClickListener(
                                    v -> {
                                        approveText.setEnabled(false);
                                        rejectText.setEnabled(false);

                                        CleanBroadcastReceiver.callSendNotificationToCleaner(
                                                notification.getExtras().get("cleaningRequestId"),
                                                notification.getExtras().get("trashId"),
                                                notification.getExtras().get("cleanerId"),
                                                true,
                                                () -> {
                                                    Toast.makeText(
                                                                    getContext(),
                                                                    "Nettoyage approuvé avec succès",
                                                                    Toast.LENGTH_SHORT)
                                                            .show();
                                                    actionButtonsContainer.setVisibility(View.GONE);
                                                    approveText.setEnabled(true);
                                                    rejectText.setEnabled(true);
                                                });
                                    });

                            rejectText.setOnClickListener(
                                    v -> {
                                        approveText.setEnabled(false);
                                        rejectText.setEnabled(false);

                                        CleanBroadcastReceiver.callSendNotificationToCleaner(
                                                notification.getExtras().get("cleaningRequestId"),
                                                notification.getExtras().get("trashId"),
                                                notification.getExtras().get("cleanerId"),
                                                false,
                                                () -> {
                                                    Toast.makeText(
                                                                    getContext(),
                                                                    "Nettoyage rejeté avec succès",
                                                                    Toast.LENGTH_SHORT)
                                                            .show();
                                                    actionButtonsContainer.setVisibility(View.GONE);
                                                    approveText.setEnabled(true);
                                                    rejectText.setEnabled(true);
                                                });
                                    });
                        } else {
                            actionButtonsContainer.setVisibility(View.GONE);
                        }
                    });
        } else {
            actionButtonsContainer.setVisibility(View.GONE);
        }

        title.setText(notification.getTitle());
        message.setText(notification.getMessage());

        if (notification.getImage() != null) {
            imageView.setImageBitmap(notification.getImage());
            imageView.setVisibility(View.VISIBLE);
        } else {
            imageView.setVisibility(View.GONE);
        }

        TextView date = convertView.findViewById(R.id.notification_date);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        date.setText(dateFormat.format(new Date(notification.getTimestamp())));

        return convertView;
    }
}
