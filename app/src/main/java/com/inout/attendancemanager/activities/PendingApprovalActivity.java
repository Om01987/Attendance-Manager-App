package com.inout.attendancemanager.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.inout.attendancemanager.MainActivity;
import com.inout.attendancemanager.R;
import com.inout.attendancemanager.utils.Constants;

public class PendingApprovalActivity extends AppCompatActivity {

    private static final String TAG = "PendingApproval";
    private static final int CHECK_STATUS_INTERVAL = 30000; // 30 seconds

    private ImageView ivPendingIcon;
    private TextView tvTitle;
    private TextView tvMessage;
    private TextView tvEmployeeName;
    private TextView tvEmployeeId;
    private TextView tvStatus;
    private MaterialButton btnCheckStatus;
    private MaterialButton btnLogout;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private SharedPreferences sharedPreferences;
    private Handler statusCheckHandler;
    private Runnable statusCheckRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_approval);

        initFirebase();
        initViews();
        setupClickListeners();
        loadEmployeeInfo();
        startPeriodicStatusCheck();
    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
    }

    private void initViews() {
        ivPendingIcon = findViewById(R.id.iv_pending_icon);
        tvTitle = findViewById(R.id.tv_title);
        tvMessage = findViewById(R.id.tv_message);
        tvEmployeeName = findViewById(R.id.tv_employee_name);
        tvEmployeeId = findViewById(R.id.tv_employee_id);
        tvStatus = findViewById(R.id.tv_status);
        btnCheckStatus = findViewById(R.id.btn_check_status);
        btnLogout = findViewById(R.id.btn_logout);
    }

    private void setupClickListeners() {
        btnCheckStatus.setOnClickListener(v -> checkApprovalStatus());
        btnLogout.setOnClickListener(v -> logout());
    }

    private void loadEmployeeInfo() {
        String employeeName = sharedPreferences.getString("full_name", "Employee");
        String employeeId = sharedPreferences.getString("employee_id", "");

        if (!employeeName.equals("Employee")) {
            tvEmployeeName.setText("Welcome, " + employeeName);
            tvEmployeeName.setVisibility(View.VISIBLE);
        }

        if (!employeeId.isEmpty()) {
            tvEmployeeId.setText("ID: " + employeeId);
            tvEmployeeId.setVisibility(View.VISIBLE);
        }
    }

    private void checkApprovalStatus() {
        String uid = firebaseAuth.getCurrentUser() != null ? firebaseAuth.getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "Authentication error. Please login again.", Toast.LENGTH_SHORT).show();
            logout();
            return;
        }

        btnCheckStatus.setEnabled(false);
        btnCheckStatus.setText("Checking...");

        firestore.collection("employees")
                .document(uid)
                .get()
                .addOnSuccessListener(this::handleStatusResult)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check status", e);
                    btnCheckStatus.setEnabled(true);
                    btnCheckStatus.setText("Check Status");
                    Toast.makeText(this, "Failed to check status. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void handleStatusResult(DocumentSnapshot document) {
        btnCheckStatus.setEnabled(true);
        btnCheckStatus.setText("Check Status");

        if (document.exists()) {
            String status = document.getString("approvalStatus");

            if ("approved".equals(status)) {
                // Update SharedPreferences and navigate to dashboard
                updateApprovalStatusInPrefs("approved");
                Toast.makeText(this, "Congratulations! Your profile has been approved.", Toast.LENGTH_LONG).show();
                navigateToDashboard();
            } else if ("rejected".equals(status)) {
                updateApprovalStatusInPrefs("rejected");
                showRejectedStatus();
            } else {
                // Still pending
                tvStatus.setText("Status: Pending Review");
                tvStatus.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Your profile is still under review.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Profile not found. Please contact administrator.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateApprovalStatusInPrefs(String status) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("approval_status", status);
        editor.putLong("approval_status_updated", System.currentTimeMillis());
        editor.apply();
    }

    private void showRejectedStatus() {
        tvTitle.setText("Profile Rejected");
        tvMessage.setText("Your profile has been rejected. Please contact HR for more information.");
        tvStatus.setText("Status: Rejected");
        tvStatus.setVisibility(View.VISIBLE);

        // Change icon color to indicate rejection
        ivPendingIcon.setImageResource(R.drawable.ic_error);
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("message", "Welcome! Your profile has been approved.");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void logout() {
        // Clear all preferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        // Sign out from Firebase
        firebaseAuth.signOut();

        // Navigate to splash
        Intent intent = new Intent(this, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void startPeriodicStatusCheck() {
        statusCheckHandler = new Handler(Looper.getMainLooper());
        statusCheckRunnable = new Runnable() {
            @Override
            public void run() {
                checkApprovalStatus();
                statusCheckHandler.postDelayed(this, CHECK_STATUS_INTERVAL);
            }
        };

        // Start checking after 30 seconds
        statusCheckHandler.postDelayed(statusCheckRunnable, CHECK_STATUS_INTERVAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statusCheckHandler != null && statusCheckRunnable != null) {
            statusCheckHandler.removeCallbacks(statusCheckRunnable);
        }
    }

    @Override
    public void onBackPressed() {
        // Prevent back navigation to registration
        // User must either be approved or logout
    }
}
