package com.inout.attendancemanager;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

public class AttendanceApplication extends Application {

    private static final String TAG = "AttendanceApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase first
        FirebaseApp.initializeApp(this);
        Log.d(TAG, "Firebase initialized");

        // Initialize App Check with Play Integrity
        try {
            FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
            firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
            );
            Log.d(TAG, "Firebase App Check initialized with Play Integrity");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize App Check", e);
        }
    }
}
