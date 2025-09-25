package com.inout.attendancemanager.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.inout.attendancemanager.R;
import com.inout.attendancemanager.models.Employee;
import com.inout.attendancemanager.utils.Constants;
import com.inout.attendancemanager.utils.PermissionUtils;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY = 3000; // 3 seconds

    private TextView tvAppName, tvVersion, tvLoading;
    private SharedPreferences sharedPreferences;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initViews();
        initFirebase();
        generateDeviceFingerprint();
        startSplashTimer();
    }

    private void initViews() {
        tvAppName = findViewById(R.id.tv_app_name);
        tvVersion = findViewById(R.id.tv_version);
        tvLoading = findViewById(R.id.tv_loading);
        sharedPreferences = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
    }

    private void initFirebase() {
        try {
            FirebaseApp.initializeApp(this);
            FirebaseAuth.getInstance();
            FirebaseFirestore.getInstance();
            Log.d(TAG, "Firebase initialized successfully");
            tvLoading.setText("Firebase connected...");
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed: " + e.getMessage(), e);
            tvLoading.setText("Connection failed");
            Toast.makeText(this, "Firebase connection failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void generateDeviceFingerprint() {
        try {
            deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(Constants.PREF_DEVICE_ID, deviceId);
            editor.apply();
            Log.d(TAG, "Device ID generated: " + deviceId);
            tvLoading.setText("Device registered...");
        } catch (Exception e) {
            Log.e(TAG, "Device fingerprinting failed: " + e.getMessage(), e);
        }
    }

    private void startSplashTimer() {
        new Handler(Looper.getMainLooper()).postDelayed(this::checkUserSessionAndRoute, SPLASH_DELAY);
    }

    private void checkUserSessionAndRoute() {
        // Permissions and phone verification checks from prefs
        boolean perms = PermissionUtils.hasAllRequiredPermissions(this);
        boolean phoneVerified = sharedPreferences.getBoolean(Constants.PREF_PHONE_VERIFIED, false);

        if (!perms) { navigateToPermissions(); return; }
        if (!phoneVerified) { navigateToMobileVerification(); return; }

        // Resolve UID from Auth or prefs
        String uid = sharedPreferences.getString(Constants.PREF_USER_ID, null);
        if (uid == null || uid.isEmpty()) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            } else {
                navigateToMobileVerification();
                return;
            }
        }
        final String finalUid = uid;

        // Always read Firestore to get authoritative routing state
        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_EMPLOYEES)
                .document(finalUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        navigateToEmployeeRegistration();
                        return;
                    }

                    // Persist canonical state for future launches
                    sharedPreferences.edit()
                            .putString(Constants.PREF_USER_ID, finalUid)
                            .putBoolean(Constants.PREF_PROFILE_COMPLETED, true)
                            .apply();

                    Employee emp = doc.toObject(Employee.class);

                    // 1) Admin first: go to admin approval console
                    if (emp != null && Boolean.TRUE.equals(emp.getIsAdmin())) {
                        navigateToAdminApproval();
                        return;
                    }

                    // 2) Approved non-admin -> Dashboard
                    String status = doc.getString("approvalStatus");
                    if ("approved".equals(status)) {
                        navigateToDashboard();
                        return;
                    }

                    // 3) Pending or unknown -> PendingApproval
                    if ("pending".equals(status) || status == null) {
                        navigateToPendingApproval();
                    } else {
                        // rejected or other -> allow re-registration/fix
                        navigateToEmployeeRegistration();
                    }
                })
                .addOnFailureListener(e -> {
                    // Network/read error: safe default
                    navigateToPendingApproval();
                });
    }

    private void navigateToMobileVerification() {
        Intent i = new Intent(this, MobileVerificationActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i); finish();
    }

    private void navigateToPermissions() {
        Intent i = new Intent(this, PermissionActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i); finish();
    }

    private void navigateToDashboard() {
        Intent i = new Intent(this, DashboardActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i); finish();
    }

    private void navigateToEmployeeRegistration() {
        Intent i = new Intent(this, EmployeeRegistrationActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i); finish();
    }

    private void navigateToPendingApproval() {
        Intent i = new Intent(this, PendingApprovalActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i); finish();
    }

    private void navigateToAdminApproval() {
        Intent i = new Intent(this, AdminApprovalActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i); finish();
    }
}
