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
import com.inout.attendancemanager.MainActivity;
import com.inout.attendancemanager.R;
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
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();

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
        new Handler(Looper.getMainLooper()).postDelayed(this::checkUserSession, SPLASH_DELAY);
    }

    private void checkUserSession() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
        boolean perms = PermissionUtils.hasAllRequiredPermissions(this);
        boolean phoneVerified = prefs.getBoolean(Constants.PREF_PHONE_VERIFIED, false);
        String userId = prefs.getString(Constants.PREF_USER_ID, null);

        if (!perms) { navigateToPermissions(); return; }
        if (!phoneVerified) { navigateToMobileVerification(); return; }

        // Resolve UID once and keep it effectively final for lambdas
        String uid = userId;
        if (uid == null || uid.isEmpty()) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            } else {
                navigateToMobileVerification();
                return;
            }
        }
        final String finalUid = uid; // effectively final for lambda capture [OK]

        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_EMPLOYEES)
                .document(finalUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        // Persist canonical state for future launches
                        SharedPreferences.Editor ed = prefs.edit();
                        ed.putString(Constants.PREF_USER_ID, finalUid);
                        ed.putBoolean(Constants.PREF_PROFILE_COMPLETED, true);
                        ed.apply();

                        String status = doc.getString("approvalStatus");
                        if ("approved".equals(status)) {
                            navigateToDashboard();
                        } else if ("pending".equals(status) || status == null) {
                            navigateToPendingApproval();
                        } else {
                            // rejected or unknown → registration to correct profile
                            navigateToEmployeeRegistration();
                        }
                    } else {
                        // No profile yet
                        navigateToEmployeeRegistration();
                    }
                })
                .addOnFailureListener(e -> {
                    // Network/read error: avoid dead-end; show pending screen
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
        startActivity(i);
        finish();
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
}
