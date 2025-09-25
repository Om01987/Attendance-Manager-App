package com.inout.attendancemanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.imageview.ShapeableImageView;
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

    // Header UI
    private MaterialToolbar topAppBar;
    private ShapeableImageView ivAdminAvatar;
    private View btnHeaderLogout;
    private View emptyState;
    private Chip chipPending;
    private View tvAdminName;
    private View tvAdminPhone;

    // Content UI
    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    // Data
    private final List<Employee> pendingList = new ArrayList<>();

    // Firebase
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private ListenerRegistration registration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_approval);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, SplashActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }

        bindViews();
        setupToolbar();
        setupBackHandler();
        setupList();
        loadAdminHeader();
    }

    private void bindViews() {
        topAppBar = findViewById(R.id.topAppBar);
        ivAdminAvatar = findViewById(R.id.iv_admin_avatar);
        btnHeaderLogout = findViewById(R.id.btn_header_logout);
        chipPending = findViewById(R.id.chip_pending);
        tvAdminName = findViewById(R.id.tv_admin_name);
        tvAdminPhone = findViewById(R.id.tv_admin_phone);
        recyclerView = findViewById(R.id.rv_pending);
        progressBar = findViewById(R.id.pb_loading);
        emptyState = findViewById(R.id.empty_state);

        btnHeaderLogout.setOnClickListener(v -> logout());
    }

    private void setupToolbar() {
        topAppBar.setNavigationOnClickListener(v -> {
            startActivity(new Intent(this, SplashActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });
    }

    private void setupBackHandler() {
        OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                startActivity(new Intent(AdminApprovalActivity.this, SplashActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
    }

    private void setupList() {
        AdminApprovalAdapter adapter = new AdminApprovalAdapter(pendingList, this::onStatusChange);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadAdminHeader() {
        String uid = auth.getCurrentUser().getUid();
        firestore.collection(Constants.COLLECTION_EMPLOYEES)
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    Employee me = doc.toObject(Employee.class);
                    String name = (me != null && me.getFullName() != null) ? me.getFullName() : "Administrator";
                    String phone = (me != null) ? maskPhone(me.getPhoneNumber()) : "";
                    ((android.widget.TextView) tvAdminName).setText(name);
                    ((android.widget.TextView) tvAdminPhone).setText(phone);

                    String photoUrl = (me != null) ? me.getProfileImageUrl() : null;
                    Glide.with(this)
                            .load(photoUrl)
                            .placeholder(R.drawable.ic_admin_avatar)
                            .error(R.drawable.ic_admin_avatar)
                            .into(ivAdminAvatar);
                })
                .addOnFailureListener(e -> {
                    ((android.widget.TextView) tvAdminName).setText("Administrator");
                    ((android.widget.TextView) tvAdminPhone).setText("");
                    Glide.with(this).load(R.drawable.ic_admin_avatar).into(ivAdminAvatar);
                });
    }

    private String maskPhone(String p) {
        if (p == null || p.isEmpty()) return "";
        String cc = "";
        String rest = p;
        if (p.startsWith("+") && p.length() > 3) {
            cc = p.substring(0, 3); // e.g., +91
            rest = p.substring(3);
        }
        String digits = rest.replaceAll("\\D", "");
        if (digits.length() <= 2) return cc + " ••";
        String last2 = digits.substring(digits.length() - 2);
        return (cc + " •••••••• " + last2).trim();
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

        registration = firestore.collection(Constants.COLLECTION_EMPLOYEES)
                .whereEqualTo("approvalStatus", "pending")
                .addSnapshotListener(this, new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        progressBar.setVisibility(View.GONE);

                        if (e != null) {
                            Log.e(TAG, "pending listener error: code=" + e.getCode() + ", msg=" + e.getMessage(), e);
                            Toast.makeText(AdminApprovalActivity.this,
                                    "Failed to load pending: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            if (auth.getCurrentUser() == null) {
                                logout();
                                return;
                            }
                            toggleEmpty();
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
                        recyclerView.getAdapter().notifyDataSetChanged();
                        updatePendingChip();
                        toggleEmpty();
                    }
                });
    }

    private void updatePendingChip() {
        chipPending.setText("Pending: " + pendingList.size());
    }

    private void toggleEmpty() {
        if (pendingList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.INVISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
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
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "onStatusChange failure: ", e);
                    Toast.makeText(this, "Update failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void logout() {
        try { FirebaseAuth.getInstance().signOut(); } catch (Exception ignored) {}
        startActivity(new Intent(this, SplashActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
