package com.inout.attendancemanager.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_approval);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerView = findViewById(R.id.rv_pending);
        progressBar = findViewById(R.id.pb_loading);

        adapter = new AdminApprovalAdapter(pendingList, this::onStatusChange);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadPendingUsers();
    }

    private void loadPendingUsers() {
        progressBar.setVisibility(View.VISIBLE);
        firestore.collection(Constants.COLLECTION_EMPLOYEES)
                .whereEqualTo("approvalStatus", "pending")
                .get()
                .addOnSuccessListener(this::onPendingLoaded)
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "loadPendingUsers failure: ", e);
                    Toast.makeText(this, "Failed to load pending: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void onPendingLoaded(QuerySnapshot snapshot) {
        pendingList.clear();
        if (snapshot != null) {
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Employee e = doc.toObject(Employee.class);
                if (e != null) {
                    e.setUid(doc.getId());
                    pendingList.add(e);
                }
            }
        }
        adapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);
        if (pendingList.isEmpty()) {
            Toast.makeText(this, "No pending approvals", Toast.LENGTH_SHORT).show();
        }
    }

    private void onStatusChange(Employee employee, String newStatus) {
        progressBar.setVisibility(View.VISIBLE);
        firestore.collection(Constants.COLLECTION_EMPLOYEES)
                .document(employee.getUid())
                .update("approvalStatus", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, employee.getFullName() + " " + newStatus, Toast.LENGTH_SHORT).show();
                    loadPendingUsers();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "onStatusChange failure: ", e);
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
