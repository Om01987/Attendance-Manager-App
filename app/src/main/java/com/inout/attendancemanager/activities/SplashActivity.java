package com.inout.attendancemanager.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
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
            // Initialize Firebase
            FirebaseApp.initializeApp(this);

            // Initialize Firebase services
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();

            Log.d(TAG, "Firebase initialized successfully");
            tvLoading.setText("Firebase connected...");

        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed: " + e.getMessage());
            tvLoading.setText("Connection failed");
            Toast.makeText(this, "Firebase connection failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void generateDeviceFingerprint() {
        try {
            // Generate unique device ID
            deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

            // Store device ID
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(Constants.PREF_DEVICE_ID, deviceId);
            editor.apply();

            Log.d(TAG, "Device ID generated: " + deviceId);
            tvLoading.setText("Device registered...");

        } catch (Exception e) {
            Log.e(TAG, "Device fingerprinting failed: " + e.getMessage());
        }
    }

    private void startSplashTimer() {
        new Handler().postDelayed(() -> {
            checkUserSession();
        }, SPLASH_DELAY);
    }

    private void checkUserSession() {
        // Check if user is logged in
        String userId = sharedPreferences.getString(Constants.PREF_USER_ID, null);

        if (userId != null && !userId.isEmpty()) {
            // User is logged in - go to dashboard
            tvLoading.setText("Welcome back!");
            navigateToDashboard();
        } else {
            // User not logged in - go to login/registration
            tvLoading.setText("Getting started...");
            navigateToLogin();
        }
    }

    private void navigateToDashboard() {
        // TODO: Navigate to Dashboard (will implement later)
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.putExtra("message", "Dashboard coming soon!");
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        // TODO: Navigate to Login Activity (will implement later)
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.putExtra("message", "Login screen coming soon!");
        startActivity(intent);
        finish();
    }
}
