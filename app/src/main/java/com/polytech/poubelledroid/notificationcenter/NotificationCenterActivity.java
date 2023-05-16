package com.polytech.poubelledroid.notificationcenter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.polytech.poubelledroid.R;
import com.polytech.poubelledroid.utils.ImgUtils;
import com.polytech.poubelledroid.utils.NotificationUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NotificationCenterActivity extends AppCompatActivity
        implements AdapterView.OnItemLongClickListener {

    private NotificationAdapter adapter;
    private ProgressDialog progressDialog;

    private static ArrayList<Notification> notifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_center);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        NotificationUtils.updateIcon(false);

        new LoadNotificationsTask().execute();

        FloatingActionButton sortButton = findViewById(R.id.sort_button);
        sortButton.setOnClickListener(v -> showSortDialog());
    }

    private void showProgressDialog(boolean show) {
        if (show) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Chargement des notifications...");
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();
        } else {
            if (progressDialog != null) {
                progressDialog.dismiss();
                progressDialog = null;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static void applyDefaultSort() {
        notifications.sort((n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Supprimer la notification")
                .setMessage("Voulez-vous supprimer cette notification ?")
                .setPositiveButton(
                        "Supprimer",
                        (dialog, which) -> new RemoveNotificationTask().execute(position))
                .setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss())
                .create()
                .show();

        return true;
    }

    private void showSortDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Trier les notifications par :")
                .setItems(
                        new CharSequence[] {"Date (ascendante)", "Date (descendante)", "Nom"},
                        (dialog, which) -> {
                            switch (which) {
                                case 0:
                                    sortByDate(true);
                                    break;
                                case 1:
                                    sortByDate(false);
                                    break;
                                case 2:
                                    sortByName();
                                    break;
                                default:
                                    break;
                            }
                        })
                .create()
                .show();
    }

    private void sortByDate(boolean ascending) {
        if (ascending) {
            notifications.sort(Comparator.comparingLong(Notification::getTimestamp));
        } else {
            notifications.sort((n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));
        }
        adapter.notifyDataSetChanged();
    }

    private void sortByName() {
        notifications.sort((n1, n2) -> n1.getTitle().compareToIgnoreCase(n2.getTitle()));
        adapter.notifyDataSetChanged();
    }

    private void loadNotifications() {
        SharedPreferences prefs =
                getSharedPreferences(NotificationUtils.NOTIFICATION_PREFS, Context.MODE_PRIVATE);
        String notificationListJson = prefs.getString(NotificationUtils.NOTIFICATION_LIST, null);
        if (notificationListJson != null) {
            try {
                JSONArray jsonArray = new JSONArray(notificationListJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    int id = jsonObject.getInt("id");
                    String title = jsonObject.getString("title");
                    String message = jsonObject.getString("message");
                    long timestamp = jsonObject.getLong("timestamp");
                    String imageBase64 = jsonObject.optString("image", null);
                    Bitmap image =
                            imageBase64 != null ? ImgUtils.base64ToBitmap(imageBase64) : null;

                    Notification notif = new Notification(id, title, message, image, timestamp);

                    // If it has extras
                    JSONObject extrasJson = jsonObject.optJSONObject("extras");
                    if (extrasJson != null) {
                        String trashId = extrasJson.optString("trashId", null);
                        String cleanerId = extrasJson.optString("cleanerId", null);
                        String cleaningRequestId = extrasJson.optString("cleaningRequestId", null);

                        if (trashId != null
                                && cleanerId != null
                                && cleaningRequestId != null
                                && !trashId.isEmpty()
                                && !cleanerId.isEmpty()
                                && !cleaningRequestId.isEmpty()) {
                            Map<String, String> data = new HashMap<>();
                            data.put("trashId", trashId);
                            data.put("cleanerId", cleanerId);
                            data.put("cleaningRequestId", cleaningRequestId);
                            notif.addExtraInformation(data);
                        }
                    }
                    notifications.add(notif);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveNotifications() {
        SharedPreferences prefs =
                getSharedPreferences(NotificationUtils.NOTIFICATION_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        JSONArray jsonArray = new JSONArray();

        for (Notification notification : notifications) {

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("id", notification.getId());
                jsonObject.put("title", notification.getTitle());
                jsonObject.put("message", notification.getMessage());
                jsonObject.put("timestamp", notification.getTimestamp());
                if (notification.getImage() != null) {
                    jsonObject.put("image", ImgUtils.bitmapToBase64(notification.getImage()));
                }
                if (!notification.getExtras().isEmpty()) {
                    jsonObject.put("extras", new JSONObject(notification.getExtras()));
                }
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        editor.putString(NotificationUtils.NOTIFICATION_LIST, jsonArray.toString());
        editor.apply();
    }

    private class LoadNotificationsTask extends AsyncTask<Void, Void, List<Notification>> {

        @Override
        protected void onPreExecute() {
            showProgressDialog(true);
        }

        @Override
        protected List<Notification> doInBackground(Void... voids) {
            notifications = new ArrayList<>();
            loadNotifications();
            applyDefaultSort();
            return notifications;
        }

        @Override
        protected void onPostExecute(List<Notification> result) {
            adapter = new NotificationAdapter(NotificationCenterActivity.this, notifications);
            ListView listView = findViewById(R.id.notification_list_view);
            listView.setAdapter(adapter);
            listView.setOnItemLongClickListener(NotificationCenterActivity.this);
            showProgressDialog(false);
        }
    }

    private class RemoveNotificationTask extends AsyncTask<Integer, Void, Void> {

        @Override
        protected void onPreExecute() {
            showProgressDialog(true);
        }

        @Override
        protected Void doInBackground(Integer... positions) {
            int position = positions[0];
            notifications.remove(position);
            saveNotifications();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            adapter.notifyDataSetChanged();
            showProgressDialog(false);
            Toast.makeText(
                            NotificationCenterActivity.this,
                            "Notification supprimée",
                            Toast.LENGTH_SHORT)
                    .show();
        }
    }
}
