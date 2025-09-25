package com.inout.attendancemanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.inout.attendancemanager.R;
import com.inout.attendancemanager.adapters.AdminApprovalAdapter;
import com.inout.attendancemanager.models.Employee;
import com.inout.attendancemanager.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class AdminApprovalActivity extends AppCompatActivity {
    private static final String TAG = "AdminApproval";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private AdminApprovalAdapter adapter;
    private final List<Employee> pendingList = new ArrayList<>();

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private ListenerRegistration registration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_approval);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Guard: if not signed in, return to Splash
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, SplashActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }

        recyclerView = findViewById(R.id.rv_pending);
        progressBar = findViewById(R.id.pb_loading);

        adapter = new AdminApprovalAdapter(pendingList, this::onStatusChange);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        detachListener();
    }

    private void attachListener() {
        if (registration != null) return;
        progressBar.setVisibility(View.VISIBLE);

        // Option B: no orderBy, avoids composite index and missing-field issues
        registration = firestore.collection(Constants.COLLECTION_EMPLOYEES)
                .whereEqualTo("approvalStatus", "pending")
                .addSnapshotListener(this, new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        progressBar.setVisibility(View.GONE);

                        if (e != null) {
                            // Keep current list visible to avoid blank UI on transient errors
                            Log.e(TAG, "pending listener error: code=" + e.getCode() + ", msg=" + e.getMessage(), e);
                            Toast.makeText(AdminApprovalActivity.this,
                                    "Failed to load pending: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();

                            // If auth became null mid-session, bounce to Splash
                            if (auth.getCurrentUser() == null) {
                                startActivity(new Intent(AdminApprovalActivity.this, SplashActivity.class)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                finish();
                            }
                            return;
                        }

                        pendingList.clear();
                        if (snapshots != null) {
                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                Employee eEmp = doc.toObject(Employee.class);
                                if (eEmp != null) {
                                    eEmp.setUid(doc.getId());
                                    pendingList.add(eEmp);
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();

                        if (pendingList.isEmpty()) {
                            Toast.makeText(AdminApprovalActivity.this,
                                    "No pending approvals", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void detachListener() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    private void onStatusChange(Employee employee, String newStatus) {
        progressBar.setVisibility(View.VISIBLE);
        firestore.collection(Constants.COLLECTION_EMPLOYEES)
                .document(employee.getUid())
                .update("approvalStatus", newStatus)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            employee.getFullName() + " " + newStatus,
                            Toast.LENGTH_SHORT).show();
                    // Listener will auto-refresh list
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "onStatusChange failure: ", e);
                    Toast.makeText(this, "Update failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
