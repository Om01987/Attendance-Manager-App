package com.inout.attendancemanager.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.inout.attendancemanager.R;
import com.inout.attendancemanager.fragments.AttendanceFragment;
import com.inout.attendancemanager.fragments.DashboardFragment;
import com.inout.attendancemanager.fragments.LeaveFragment;
import com.inout.attendancemanager.models.Attendance;
import com.inout.attendancemanager.models.AttendanceSummary;
import com.inout.attendancemanager.models.Employee;
import com.inout.attendancemanager.utils.Constants;
import com.inout.attendancemanager.utils.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class DashboardActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "DashboardActivity";
    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    // UI Components
    private BottomNavigationView bottomNavigation;

    // Firebase
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private FusedLocationProviderClient fusedLocationClient;

    // Data
    private SharedPreferences sharedPreferences;
    private Employee currentEmployee;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initFirebase();
        initViews();
        setupBottomNavigation();
        loadUserProfile();

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
        }
    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        sharedPreferences = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);

        if (firebaseAuth.getCurrentUser() != null) {
            currentUserId = firebaseAuth.getCurrentUser().getUid();
        }
    }

    private void initViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnNavigationItemSelectedListener(this);
    }

    private void loadUserProfile() {
        if (currentUserId == null) {
            navigateToSplash();
            return;
        }

        firestore.collection(Constants.COLLECTION_EMPLOYEES)
                .document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentEmployee = doc.toObject(Employee.class);
                        if (currentEmployee != null) {
                            // Check approval status
                            String status = currentEmployee.getApprovalStatus();
                            if (!"approved".equals(status)) {
                                if ("pending".equals(status)) {
                                    navigateToPendingApproval();
                                } else {
                                    navigateToRegistration();
                                }
                                return;
                            }
                        }
                    } else {
                        navigateToRegistration();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user profile", e);
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;

        int itemId = item.getItemId();
        if (itemId == R.id.nav_dashboard) {
            selectedFragment = new DashboardFragment();
        } else if (itemId == R.id.nav_attendance) {
            selectedFragment = new AttendanceFragment();
        } else if (itemId == R.id.nav_leave) {
            selectedFragment = new LeaveFragment();
        }

        return loadFragment(selectedFragment);
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    public Employee getCurrentEmployee() {
        return currentEmployee;
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    private void navigateToSplash() {
        Intent intent = new Intent(this, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToPendingApproval() {
        Intent intent = new Intent(this, PendingApprovalActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToRegistration() {
        Intent intent = new Intent(this, EmployeeRegistrationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Prevent going back to splash/login screens
        moveTaskToBack(true);
    }
}