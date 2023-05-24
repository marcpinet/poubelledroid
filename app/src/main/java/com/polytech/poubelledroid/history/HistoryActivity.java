package com.polytech.poubelledroid.history;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.polytech.poubelledroid.R;
import com.polytech.poubelledroid.fields.CleaningRequestsFields;
import com.polytech.poubelledroid.fields.WasteFields;
import java.util.ArrayList;
import java.util.Objects;

public class HistoryActivity extends AppCompatActivity {

    private HistoryAdapter historyAdapter;
    private final ArrayList<Object> historyItems = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private AlertDialog loadingDialog;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        RecyclerView historyRecyclerView;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        historyRecyclerView = findViewById(R.id.recycler_view);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        AlertDialog.Builder loadingDialogBuilder = new AlertDialog.Builder(this);
        loadingDialogBuilder.setView(R.layout.dialog_loading);
        loadingDialog = loadingDialogBuilder.create();
        loadingDialog.setMessage("Actualisation des déchets en cours...");

        loadingDialog.show();
        loadHistoryData();

        historyAdapter = new HistoryAdapter(this, historyItems);
        historyRecyclerView.setAdapter(historyAdapter);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this::loadHistoryData);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadWastes() {
        db.collection(WasteFields.COLLECTION_NAME)
                .whereEqualTo(WasteFields.USER_ID, userId)
                .get()
                .addOnCompleteListener(
                        task -> {
                            if (task.isSuccessful()) {
                                historyItems.addAll(task.getResult().getDocuments());

                                // After loading wastes, load cleaning requests
                                loadCleaningRequests();
                            } else {
                                Log.d(
                                        "HistoryActivity",
                                        "Error getting wastes: ",
                                        task.getException());
                                swipeRefreshLayout.setRefreshing(false);
                                loadingDialog.dismiss();
                            }
                        });
    }

    private void loadCleaningRequests() {
        db.collection(CleaningRequestsFields.COLLECTION_NAME)
                .whereEqualTo(CleaningRequestsFields.CLEANER_ID, userId)
                .get()
                .addOnCompleteListener(
                        task -> {
                            if (task.isSuccessful()) {
                                historyItems.addAll(task.getResult().getDocuments());

                                // After loading cleaning requests, sort and refresh
                                sortHistoryItemsAndRefresh();
                            } else {
                                Log.d(
                                        "HistoryActivity",
                                        "Error getting cleaning requests: ",
                                        task.getException());
                                swipeRefreshLayout.setRefreshing(false);
                                loadingDialog.dismiss();
                            }
                        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void sortHistoryItemsAndRefresh() {
        // Sort history items by date (descending)
        historyItems.sort(
                (o1, o2) -> {
                    Timestamp date1 = ((QueryDocumentSnapshot) o1).getTimestamp("date");
                    Timestamp date2 = ((QueryDocumentSnapshot) o2).getTimestamp("date");
                    assert date1 != null && date2 != null;
                    return date2.compareTo(date1);
                });

        historyAdapter.notifyDataSetChanged();
        swipeRefreshLayout.setRefreshing(false);
        loadingDialog.dismiss();
    }

    private void loadHistoryData() {
        // Clear existing data
        historyItems.clear();

        // Start loading wastes which will then load cleaning requests and sort the history items
        loadWastes();
    }
}
