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
            // Access to ensure instances are ready (optional)
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

    // Updated routing: compute permission status at runtime instead of using a stored flag
    private void checkUserSession() {
        boolean permissionsGranted = PermissionUtils.hasAllRequiredPermissions(this);
        boolean phoneVerified = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE)
                .getBoolean("phone_verified", false);
        boolean profileCompleted = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE)
                .getBoolean(Constants.PREF_PROFILE_COMPLETED, false);
        String userId = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE)
                .getString(Constants.PREF_USER_ID, null);

        if (!permissionsGranted) {
            tvLoading.setText("Setting up permissions...");
            navigateToPermissions();
            return;
        }

        if (!phoneVerified) {
            tvLoading.setText("Verifying mobile number...");
            navigateToMobileVerification();
            return;
        }

        if (!profileCompleted) {
            tvLoading.setText("Setting up profile...");
            navigateToEmployeeRegistration();
            return;
        }

        if (userId != null && !userId.isEmpty()) {
            tvLoading.setText("Welcome back!");
            navigateToDashboard();
        } else {
            tvLoading.setText("Setting up profile...");
            navigateToEmployeeRegistration();
        }
    }


    private void navigateToMobileVerification() {
        Intent intent = new Intent(SplashActivity.this, MobileVerificationActivity.class);
        startActivity(intent);
        finish();
    }


    private void navigateToPermissions() {
        Intent intent = new Intent(SplashActivity.this, PermissionActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToDashboard() {
        // TODO: Replace MainActivity with actual Dashboard Activity
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.putExtra("message", "Dashboard coming soon!");
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        // Placeholder for future login screen if needed
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.putExtra("message", "Login screen coming soon!");
        startActivity(intent);
        finish();
    }

    private void navigateToEmployeeRegistration() {
        Intent intent = new Intent(SplashActivity.this, EmployeeRegistrationActivity.class);
        startActivity(intent);
        finish();
    }
}
