package com.inout.attendancemanager.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.inout.attendancemanager.R;
import com.inout.attendancemanager.utils.Constants;
import com.inout.attendancemanager.utils.PermissionUtils;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.Iterator;
import java.util.List;

public class PermissionActivity extends AppCompatActivity {

    private MaterialButton btnGrantPermissions;
    private MaterialButton btnCheckPermissions;
    private TextView tvSkip;

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
        initBackHandler();
        checkCurrentPermissionStatus();
    }

    private void initViews() {
        btnGrantPermissions = findViewById(R.id.btn_grant_permissions);
        btnCheckPermissions = findViewById(R.id.btn_check_permissions);
        tvSkip = findViewById(R.id.tv_skip);

        ivCameraStatus = findViewById(R.id.iv_camera_status);
        ivLocationStatus = findViewById(R.id.iv_location_status);
        ivPhoneStatus = findViewById(R.id.iv_phone_status);
        ivStorageStatus = findViewById(R.id.iv_storage_status);

        sharedPreferences = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);

        // Storage is not required on API > 28; show green to avoid confusion
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            ivStorageStatus.setImageResource(R.drawable.ic_granted);
            ivStorageStatus.setColorFilter(ContextCompat.getColor(this, R.color.success_green));
        }
    }

    private void initClickListeners() {
        btnGrantPermissions.setOnClickListener(v -> requestAllPermissions());

        btnCheckPermissions.setOnClickListener(v -> {
            checkCurrentPermissionStatus();
            showPermissionStatusDialog();
        });

        tvSkip.setOnClickListener(v -> showSkipConfirmationDialog());
    }

    private void initBackHandler() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new AlertDialog.Builder(PermissionActivity.this)
                        .setTitle("Exit Setup?")
                        .setMessage("Exiting now may limit app functionality until permissions are granted.")
                        .setPositiveButton("Exit", (d, w) -> {
                            setEnabled(false);
                            getOnBackPressedDispatcher().onBackPressed();
                            setEnabled(true);
                        })
                        .setNegativeButton("Stay", null)
                        .show();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void requestAllPermissions() {
        String[] reqPerms = PermissionUtils.buildRequiredPermissions(this);

        Dexter.withContext(this)
                .withPermissions(reqPerms)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // After the system dialog(s), compute using our own gate: camera AND (coarse OR fine)
                        boolean coreGranted = PermissionUtils.hasAllRequiredPermissions(PermissionActivity.this);

                        // Build a filtered denied list to ignore storage/media not required on modern Android
                        List<PermissionDeniedResponse> denied = report.getDeniedPermissionResponses();
                        filterOutNonBlockingStorageDenials(denied);

                        if (coreGranted) {
                            updateAllPermissionStatus(true);
                            savePermissionGrantedStatus();
                            proceedToNextActivity();
                            return;
                        }

                        updatePermissionStatusIndividually();

                        if (!denied.isEmpty()) {
                            handleDeniedPermissions(denied);
                            // Only show Settings if any of the still-denied are permanently denied and truly required
                            if (isAnyPermanentlyDeniedRequired(denied)) {
                                showSettingsDialog();
                            }
                        } else {
                            // Nothing critical denied → proceed
                            proceedToNextActivity();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken token) {
                        showPermissionRationaleDialog(token);
                    }
                })
                .check();
    }

    private boolean isAnyPermanentlyDeniedRequired(List<PermissionDeniedResponse> denied) {
        if (denied == null) return false;
        for (PermissionDeniedResponse dr : denied) {
            // If you need to guard on required ones only, check by name here (camera/location)
            String p = dr.getPermissionName();
            boolean required = p.equals(android.Manifest.permission.CAMERA)
                    || p.equals(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    || p.equals(android.Manifest.permission.ACCESS_FINE_LOCATION);
            if (required && dr.isPermanentlyDenied()) return true;
        }
        return false;
    }

    private void filterOutNonBlockingStorageDenials(List<PermissionDeniedResponse> denied) {
        if (denied == null) return;
        Iterator<PermissionDeniedResponse> it = denied.iterator();
        while (it.hasNext()) {
            String p = it.next().getPermissionName();
            boolean isLegacyStorage = p.equals(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    || p.equals(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            boolean isMediaRead = p.equals(android.Manifest.permission.READ_MEDIA_IMAGES)
                    || p.equals(android.Manifest.permission.READ_MEDIA_VIDEO)
                    || p.equals("android.permission.READ_MEDIA_VISUAL_USER_SELECTED");
            boolean storageNotRequired = Build.VERSION.SDK_INT > Build.VERSION_CODES.P && !PermissionUtils.NEEDS_GALLERY_READ;

            if ((isLegacyStorage && storageNotRequired) || (isMediaRead && !PermissionUtils.NEEDS_GALLERY_READ)) {
                it.remove();
            }
        }
    }

    private void checkCurrentPermissionStatus() {
        updatePermissionStatusIcon(ivCameraStatus,
                isPermissionGranted(android.Manifest.permission.CAMERA));

        // Green if either coarse OR fine is granted
        boolean coarse = isPermissionGranted(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        boolean fine = isPermissionGranted(android.Manifest.permission.ACCESS_FINE_LOCATION);
        updatePermissionStatusIcon(ivLocationStatus, (coarse || fine));

        // Phone state not required for this phase; show as granted only if present
        updatePermissionStatusIcon(ivPhoneStatus,
                isPermissionGranted(android.Manifest.permission.READ_PHONE_STATE));

        boolean storageGranted = Build.VERSION.SDK_INT > Build.VERSION_CODES.P
                || isPermissionGranted(android.Manifest.permission.READ_EXTERNAL_STORAGE);
        updatePermissionStatusIcon(ivStorageStatus, storageGranted);

        updateButtonText();
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

    private void updatePermissionStatusIndividually() {
        checkCurrentPermissionStatus();
    }

    private void updateAllPermissionStatus(boolean granted) {
        updatePermissionStatusIcon(ivCameraStatus, granted);
        updatePermissionStatusIcon(ivLocationStatus, granted);
        // Phone is optional in this phase; do not gate on it
        updateButtonText();
    }

    private void updateButtonText() {
        boolean allGranted = PermissionUtils.hasAllRequiredPermissions(this);
        if (allGranted) {
            btnGrantPermissions.setText("All Permissions Granted ✓");
            btnGrantPermissions.setEnabled(false);
        } else {
            btnGrantPermissions.setText("Grant All Permissions");
            btnGrantPermissions.setEnabled(true);
        }
    }

    private void showPermissionRationaleDialog(PermissionToken token) {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage(
                        "This app requires:\n\n" +
                                "• Camera: For attendance photos\n" +
                                "• Location: Coarse or Fine to verify office location\n\n" +
                                "Please grant the required permissions to continue."
                )
                .setPositiveButton("Grant Permissions", (dialog, which) -> token.continuePermissionRequest())
                .setNegativeButton("Cancel", (dialog, which) -> {
                    token.cancelPermissionRequest();
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    private void showPermissionStatusDialog() {
        StringBuilder status = new StringBuilder();
        status.append("Permission Status:\n\n");

        boolean camera = isPermissionGranted(android.Manifest.permission.CAMERA);
        boolean coarse = isPermissionGranted(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        boolean fine = isPermissionGranted(android.Manifest.permission.ACCESS_FINE_LOCATION);

        status.append("• Camera: ").append(camera ? "✓ Granted" : "✗ Not Granted").append("\n");
        status.append("• Location: ").append((coarse || fine) ? "✓ Granted" : "✗ Not Granted").append("\n");

        boolean phone = isPermissionGranted(android.Manifest.permission.READ_PHONE_STATE);
        status.append("• Phone (optional): ").append(phone ? "✓ Granted" : "✗ Not Granted").append("\n");

        boolean storage = Build.VERSION.SDK_INT > Build.VERSION_CODES.P
                || isPermissionGranted(android.Manifest.permission.READ_EXTERNAL_STORAGE);
        status.append("• Storage (not required): ").append(storage ? "✓ Granted" : "✗ Not Granted").append("\n");

        new AlertDialog.Builder(this)
                .setTitle("Permission Status")
                .setMessage(status.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void handleDeniedPermissions(List<PermissionDeniedResponse> deniedResponses) {
        StringBuilder deniedList = new StringBuilder();
        for (PermissionDeniedResponse response : deniedResponses) {
            deniedList.append("• ").append(getPermissionDisplayName(response.getPermissionName())).append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Some Permissions Denied")
                .setMessage("The following permissions were denied:\n\n" + deniedList +
                        "\nThese are required for the app to function properly.")
                .setPositiveButton("Retry", (dialog, which) -> requestAllPermissions())
                .setNegativeButton("Stay Here", null)
                .setCancelable(false)
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
            default:
                return "Storage";
        }
    }

    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission required")
                .setMessage("Some required permissions are permanently denied. Open Settings to enable them.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(android.net.Uri.fromParts("package", getPackageName(), null));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void savePermissionGrantedStatus() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("permissions_granted", PermissionUtils.hasAllRequiredPermissions(this));
        editor.putLong("permissions_granted_time", System.currentTimeMillis());
        editor.apply();
    }

    private void showSkipConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Skip Permissions?")
                .setMessage("Skipping may limit app functionality. Continue anyway?")
                .setPositiveButton("Skip Anyway", (dialog, which) -> {
                    Toast.makeText(this, "Continuing with limited functionality", Toast.LENGTH_LONG).show();
                    proceedToNextActivity();
                })
                .setNegativeButton("Grant Permissions", (dialog, which) -> requestAllPermissions())
                .show();
    }

    private void proceedToNextActivity() {
        Intent intent = new Intent(PermissionActivity.this, MobileVerificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkCurrentPermissionStatus();
    }
}
