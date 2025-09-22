package com.inout.attendancemanager.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.inout.attendancemanager.MainActivity;
import com.inout.attendancemanager.R;
import com.inout.attendancemanager.utils.Constants;
import com.inout.attendancemanager.utils.PermissionConstants;

// imports (add if missing)
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;


import java.util.List;

public class PermissionActivity extends AppCompatActivity {

    private static final String TAG = "PermissionActivity";

    // UI Components
    private MaterialButton btnGrantPermissions;
    private MaterialButton btnCheckPermissions;
    private TextView tvSkip;

    // Status ImageViews
    private ImageView ivCameraStatus;
    private ImageView ivLocationStatus;
    private ImageView ivPhoneStatus;
    private ImageView ivStorageStatus;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);

        initViews();
        initClickListeners();
        checkCurrentPermissionStatus();
    }

    private void initViews() {
        btnGrantPermissions = findViewById(R.id.btn_grant_permissions);
        btnCheckPermissions = findViewById(R.id.btn_check_permissions);
        tvSkip = findViewById(R.id.tv_skip);

        // Status indicators
        ivCameraStatus = findViewById(R.id.iv_camera_status);
        ivLocationStatus = findViewById(R.id.iv_location_status);
        ivPhoneStatus = findViewById(R.id.iv_phone_status);
        ivStorageStatus = findViewById(R.id.iv_storage_status);

        sharedPreferences = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
    }

    private void initClickListeners() {
        btnGrantPermissions.setOnClickListener(v -> requestAllPermissions());

        btnCheckPermissions.setOnClickListener(v -> {
            checkCurrentPermissionStatus();
            showPermissionStatusDialog();
        });

        tvSkip.setOnClickListener(v -> {
            showSkipConfirmationDialog();
        });
    }

    private void requestAllPermissions() {
        Dexter.withContext(this)
                .withPermissions(PermissionConstants.REQUIRED_PERMISSIONS)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            updateAllPermissionStatus(true);
                            showSuccessDialog();
                            savePermissionGrantedStatus();
                            proceedToNextActivity();
                        } else {
                            updatePermissionStatusIndividually();
                            // FIX: pass the correct type
                            handleDeniedPermissions(report.getDeniedPermissionResponses());
                            // Optional: handle permanently denied to open Settings
                            if (report.isAnyPermissionPermanentlyDenied()) {
                                showSettingsDialog();
                            }
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                        // Show rationale dialog
                        showPermissionRationaleDialog(permissionToken);
                    }
                })
                .check();
    }

    private void checkCurrentPermissionStatus() {
        // Check Camera Permission
        if (isPermissionGranted(android.Manifest.permission.CAMERA)) {
            updatePermissionStatusIcon(ivCameraStatus, true);
        } else {
            updatePermissionStatusIcon(ivCameraStatus, false);
        }

        // Check Location Permission
        if (isPermissionGranted(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            updatePermissionStatusIcon(ivLocationStatus, true);
        } else {
            updatePermissionStatusIcon(ivLocationStatus, false);
        }

        // Check Phone Permission
        if (isPermissionGranted(android.Manifest.permission.READ_PHONE_STATE)) {
            updatePermissionStatusIcon(ivPhoneStatus, true);
        } else {
            updatePermissionStatusIcon(ivPhoneStatus, false);
        }

        // Check Storage Permission
        if (isPermissionGranted(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            updatePermissionStatusIcon(ivStorageStatus, true);
        } else {
            updatePermissionStatusIcon(ivStorageStatus, false);
        }

        // Update button text based on status
        updateButtonText();
    }

    private void updatePermissionStatusIndividually() {
        checkCurrentPermissionStatus();
    }

    private boolean isPermissionGranted(String permission) {
        return ContextCompat.checkSelfPermission(this, permission)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private void updatePermissionStatusIcon(ImageView imageView, boolean granted) {
        if (granted) {
            imageView.setImageResource(R.drawable.ic_granted);
            imageView.setColorFilter(ContextCompat.getColor(this, R.color.success_green));
        } else {
            imageView.setImageResource(R.drawable.ic_pending);
            imageView.setColorFilter(ContextCompat.getColor(this, R.color.warning_orange));
        }
    }

    private void updateAllPermissionStatus(boolean granted) {
        updatePermissionStatusIcon(ivCameraStatus, granted);
        updatePermissionStatusIcon(ivLocationStatus, granted);
        updatePermissionStatusIcon(ivPhoneStatus, granted);
        updatePermissionStatusIcon(ivStorageStatus, granted);
        updateButtonText();
    }

    private void updateButtonText() {
        boolean allGranted = areAllPermissionsGranted();
        if (allGranted) {
            btnGrantPermissions.setText("All Permissions Granted ✓");
            btnGrantPermissions.setEnabled(false);
        } else {
            btnGrantPermissions.setText("Grant All Permissions");
            btnGrantPermissions.setEnabled(true);
        }
    }

    private boolean areAllPermissionsGranted() {
        for (String permission : PermissionConstants.REQUIRED_PERMISSIONS) {
            if (!isPermissionGranted(permission)) {
                return false;
            }
        }
        return true;
    }

    private void showPermissionRationaleDialog(PermissionToken permissionToken) {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("This app requires the following permissions to function properly:\n\n" +
                        "• Camera: For attendance verification photos\n" +
                        "• Location: To verify office location\n" +
                        "• Phone: For device identification\n" +
                        "• Storage: To save profile pictures\n\n" +
                        "Please grant all permissions to continue.")
                .setPositiveButton("Grant Permissions", (dialog, which) -> {
                    permissionToken.continuePermissionRequest();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    permissionToken.cancelPermissionRequest();
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    // FIX: method signature and usage
    private void handleDeniedPermissions(List<PermissionDeniedResponse> deniedResponses) {
        StringBuilder deniedList = new StringBuilder();
        for (PermissionDeniedResponse response : deniedResponses) {
            deniedList.append("• ")
                    .append(getPermissionDisplayName(response.getPermissionName()))
                    .append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Some Permissions Denied")
                .setMessage("The following permissions were denied:\n\n" + deniedList +
                        "\nThese permissions are required for the app to function properly.")
                .setPositiveButton("Retry", (dialog, which) -> requestAllPermissions())
                .setNegativeButton("Continue Anyway", (dialog, which) -> proceedToNextActivity())
                .setCancelable(false)
                .show();
    }

    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission required")
                .setMessage("Some permissions are permanently denied. Please open Settings and enable them to continue.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(android.net.Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }



    private String getPermissionDisplayName(String permission) {
        switch (permission) {
            case android.Manifest.permission.CAMERA:
                return "Camera";
            case android.Manifest.permission.ACCESS_FINE_LOCATION:
            case android.Manifest.permission.ACCESS_COARSE_LOCATION:
                return "Location";
            case android.Manifest.permission.READ_PHONE_STATE:
                return "Phone";
            case android.Manifest.permission.READ_EXTERNAL_STORAGE:
            case android.Manifest.permission.WRITE_EXTERNAL_STORAGE:
                return "Storage";
            default:
                return permission;
        }
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Granted!")
                .setMessage("All required permissions have been granted successfully. You can now use all features of the app.")
                .setPositiveButton("Continue", (dialog, which) -> {
                    dialog.dismiss();
                    // proceedToNextActivity() will be called after this dialog
                })
                .setCancelable(false)
                .show();
    }

    private void showPermissionStatusDialog() {
        StringBuilder status = new StringBuilder();
        status.append("Permission Status:\n\n");

        for (String permission : PermissionConstants.REQUIRED_PERMISSIONS) {
            String name = getPermissionDisplayName(permission);
            boolean granted = isPermissionGranted(permission);
            status.append("• ").append(name).append(": ")
                    .append(granted ? "✓ Granted" : "✗ Not Granted")
                    .append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Permission Status")
                .setMessage(status.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void showSkipConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Skip Permissions?")
                .setMessage("Warning: Skipping permissions may limit app functionality. Some features may not work properly without required permissions.\n\nAre you sure you want to continue?")
                .setPositiveButton("Skip Anyway", (dialog, which) -> {
                    Toast.makeText(this, "Continuing with limited functionality", Toast.LENGTH_LONG).show();
                    proceedToNextActivity();
                })
                .setNegativeButton("Grant Permissions", (dialog, which) -> {
                    requestAllPermissions();
                })
                .show();
    }

    private void savePermissionGrantedStatus() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("permissions_granted", areAllPermissionsGranted());
        editor.putLong("permissions_granted_time", System.currentTimeMillis());
        editor.apply();
    }

    private void proceedToNextActivity() {
        // For now, navigate to MainActivity
        // Later this will navigate to Mobile Verification Activity
        Intent intent = new Intent(PermissionActivity.this, MainActivity.class);
        intent.putExtra("message", "Permissions setup complete!\nNext: Mobile Verification");
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check permissions when returning from settings
        checkCurrentPermissionStatus();
    }

    @Override
    public void onBackPressed() {
        // Show confirmation dialog before going back
        new AlertDialog.Builder(this)
                .setTitle("Exit Setup?")
                .setMessage("Are you sure you want to exit the permission setup? The app may not function properly without required permissions.")
                .setPositiveButton("Exit", (dialog, which) -> {
                    super.onBackPressed();
                })
                .setNegativeButton("Stay", null)
                .show();
    }
}
