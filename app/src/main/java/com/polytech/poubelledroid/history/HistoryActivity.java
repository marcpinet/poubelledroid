package com.polytech.poubelledroid.history;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
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
import java.util.ArrayList;
import java.util.Objects;

public class HistoryActivity extends AppCompatActivity {

    private HistoryAdapter historyAdapter;
    private final ArrayList<Object> historyItems = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private ProgressDialog progressDialog;
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

    @SuppressLint("NotifyDataSetChanged")
    private void loadHistoryData() {
        // Clear existing data
        historyItems.clear();

        // Load wastes
        db.collection("waste")
                .whereEqualTo("uid", userId)
                .get()
                .addOnCompleteListener(
                        task -> {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    historyItems.add(document);
                                }

                                // Load cleaning requests
                                db.collection("cleaningRequests")
                                        .whereEqualTo("cleanerId", userId)
                                        .get()
                                        .addOnCompleteListener(
                                                task2 -> {
                                                    if (task2.isSuccessful()) {
                                                        for (QueryDocumentSnapshot document :
                                                                task2.getResult()) {
                                                            historyItems.add(document);
                                                        }

                                                        // Sort history items by date (descending)
                                                        historyItems.sort(
                                                                (o1, o2) -> {
                                                                    Timestamp date1 =
                                                                            ((QueryDocumentSnapshot)
                                                                                            o1)
                                                                                    .getTimestamp(
                                                                                            "date");
                                                                    Timestamp date2 =
                                                                            ((QueryDocumentSnapshot)
                                                                                            o2)
                                                                                    .getTimestamp(
                                                                                            "date");
                                                                    return date2.compareTo(date1);
                                                                });

                                                        historyAdapter.notifyDataSetChanged();
                                                        swipeRefreshLayout.setRefreshing(false);
                                                    } else {
                                                        Log.d(
                                                                "HistoryActivity",
                                                                "Error getting cleaning requests: ",
                                                                task2.getException());
                                                        swipeRefreshLayout.setRefreshing(false);
                                                    }
                                                });
                            } else {
                                Log.d(
                                        "HistoryActivity",
                                        "Error getting wastes: ",
                                        task.getException());
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        });
    }
}
