package com.inout.attendancemanager.fragments;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.inout.attendancemanager.R;
import com.inout.attendancemanager.models.BeaconConfig;
import com.inout.attendancemanager.models.BeaconScanResult;
import com.inout.attendancemanager.repositories.AttendanceRepository;
import com.inout.attendancemanager.utils.BeaconScanner;
import com.inout.attendancemanager.utils.GeofenceUtils;

import java.util.ArrayList;
import java.util.List;

public class PunchBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_IS_IN   = "is_punched_in";
    private static final String ARG_OLAT    = "office_lat";
    private static final String ARG_OLNG    = "office_lng";
    private static final String ARG_UID     = "user_id";
    private static final String ARG_RADIUS  = "radius_m";
    private static final String ARG_BEACON_REQUIRED = "beacon_required";

    private static final int BLUETOOTH_PERMISSION_REQUEST = 1002;

    private boolean isPunchedIn;
    private double officeLat;
    private double officeLng;
    private String userId;
    private float radiusM;
    private boolean beaconRequired;

    private FusedLocationProviderClient fused;
    private AttendanceRepository repo;
    private BeaconScanner beaconScanner;
    private BeaconScanResult lastBeaconResult;

    // UI
    private TextView tvTitle;
    private TextView tvStatus;
    private Chip chipDist;
    private Chip chipBeacon;
    private CircularProgressIndicator progressLocation;
    private CircularProgressIndicator progressBeacon;
    private MaterialButton btnConfirm;
    private MaterialButton btnBeacon;
    private MaterialButton btnCancel;

    public static void show(@NonNull androidx.fragment.app.Fragment parent,
                            boolean isPunchedIn,
                            double officeLat,
                            double officeLng,
                            float radiusMeters,
                            String userId,
                            boolean beaconRequired) {
        PunchBottomSheet sheet = new PunchBottomSheet();
        Bundle b = new Bundle();
        b.putBoolean(ARG_IS_IN, isPunchedIn);
        b.putDouble(ARG_OLAT, officeLat);
        b.putDouble(ARG_OLNG, officeLng);
        b.putFloat(ARG_RADIUS, radiusMeters);
        b.putString(ARG_UID, userId);
        b.putBoolean(ARG_BEACON_REQUIRED, beaconRequired);
        sheet.setArguments(b);
        sheet.show(parent.getParentFragmentManager(), "PunchBottomSheet");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle a = getArguments();
        if (a != null) {
            isPunchedIn = a.getBoolean(ARG_IS_IN);
            officeLat   = a.getDouble(ARG_OLAT);
            officeLng   = a.getDouble(ARG_OLNG);
            radiusM     = a.getFloat(ARG_RADIUS, 500f);
            userId      = a.getString(ARG_UID);
            beaconRequired = a.getBoolean(ARG_BEACON_REQUIRED, false);
        }
        fused = LocationServices.getFusedLocationProviderClient(requireContext());
        repo  = new AttendanceRepository(userId);
        setCancelable(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_punch_sheet, parent, false);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        initViews(v);
        setupInitialState();
        setupClickListeners();

        // Start location check
        checkLocation();

        // Start beacon scan if needed
        checkBeacons();
    }

    private void initViews(View v) {
        tvTitle = v.findViewById(R.id.tv_punch_title);
        tvStatus = v.findViewById(R.id.tv_office_status);
        chipDist = v.findViewById(R.id.chip_distance);
        chipBeacon = v.findViewById(R.id.chip_beacon_status);
        progressLocation = v.findViewById(R.id.progress_location);
        progressBeacon = v.findViewById(R.id.progress_beacon);
        btnConfirm = v.findViewById(R.id.btn_confirm_punch);
        btnBeacon = v.findViewById(R.id.btn_punch_beacon);
        btnCancel = v.findViewById(R.id.btn_cancel);
    }

    private void setupInitialState() {
        tvTitle.setText(isPunchedIn ? "Punch Out" : "Punch In");
        btnConfirm.setText(isPunchedIn ? "Punch Out" : "Punch In");
        btnBeacon.setText(isPunchedIn ? "Punch Out via Beacon" : "Punch In via Beacon");

        btnConfirm.setEnabled(false);
        btnConfirm.setAlpha(0.5f);
        btnBeacon.setEnabled(false);
        btnBeacon.setAlpha(0.5f);

        progressLocation.setVisibility(View.VISIBLE);
        progressBeacon.setVisibility(View.VISIBLE);
    }

    private void setupClickListeners() {
        btnConfirm.setOnClickListener(v -> doPunch(false));
        btnBeacon.setOnClickListener(v -> doPunch(true));
        btnCancel.setOnClickListener(v -> dismissAllowingStateLoss());
    }

    private void checkLocation() {
        if (officeLat == 0.0 || officeLng == 0.0) {
            tvStatus.setText("Office location not set");
            chipDist.setText("Distance: -- m");
            progressLocation.setVisibility(View.GONE);
            return;
        }

        fused.getLastLocation()
                .addOnSuccessListener(loc -> {
                    progressLocation.setVisibility(View.GONE);
                    updateLocationStatus(loc);
                })
                .addOnFailureListener(e -> {
                    progressLocation.setVisibility(View.GONE);
                    tvStatus.setText("Location error");
                    chipDist.setText("Distance: -- m");
                });
    }

    private void updateLocationStatus(@Nullable Location location) {
        if (location == null) {
            tvStatus.setText("Location unavailable");
            chipDist.setText("Distance: -- m");
            return;
        }

        float dist = GeofenceUtils.distanceMeters(
                location.getLatitude(), location.getLongitude(), officeLat, officeLng);

        chipDist.setText("Distance: " + Math.round(dist) + " m");

        if (dist <= radiusM) {
            tvStatus.setText("Inside office radius (" + (int) radiusM + " m)");
            if (!beaconRequired) {
                btnConfirm.setEnabled(true);
                btnConfirm.setAlpha(1f);
            }
        } else {
            tvStatus.setText("Outside office radius (" + (int) radiusM + " m)");
            btnConfirm.setEnabled(false);
            btnConfirm.setAlpha(0.5f);
        }

        updatePunchButtonStates();
    }

    private void checkBeacons() {
        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions();
            return;
        }
        startBeaconScan();
    }

    private void startBeaconScan() {
        // TODO: Replace with beacons loaded from Firebase org settings
        List<BeaconConfig> beacons = getSampleBeaconConfigs();

        if (beacons.isEmpty()) {
            progressBeacon.setVisibility(View.GONE);
            chipBeacon.setText("Beacon: not configured");
            return;
        }

        beaconScanner = new BeaconScanner(requireContext(), beacons, new BeaconScanner.ScanCallback() {
            @Override
            public void onScanStarted() {
                chipBeacon.setText("Beacon: scanning...");
                progressBeacon.setVisibility(View.VISIBLE);
            }

            @Override
            public void onBeaconFound(BeaconScanResult result) {
                if (result.isValid()) {
                    chipBeacon.setText("Beacon: " + result.getLabel() + " (" + result.getRssi() + " dBm)");
                }
            }

            @Override
            public void onScanComplete(BeaconScanResult bestResult) {
                progressBeacon.setVisibility(View.GONE);
                lastBeaconResult = bestResult;
                updateBeaconStatus(bestResult);
            }

            @Override
            public void onScanError(String error) {
                progressBeacon.setVisibility(View.GONE);
                chipBeacon.setText("Beacon: " + error);
            }
        });

        beaconScanner.startScan();
    }

    private void updateBeaconStatus(BeaconScanResult result) {
        if (result.isValid()) {
            chipBeacon.setText("Beacon: " + result.getLabel() + " (" + result.getRssi() + " dBm)");
            btnBeacon.setEnabled(true);
            btnBeacon.setAlpha(1f);

            if (beaconRequired) {
                btnConfirm.setEnabled(true);
                btnConfirm.setAlpha(1f);
            }
        } else {
            chipBeacon.setText("Beacon: not found or too far");
            btnBeacon.setEnabled(false);
            btnBeacon.setAlpha(0.5f);
        }

        updatePunchButtonStates();
    }

    private void updatePunchButtonStates() {
        if (beaconRequired && (lastBeaconResult == null || !lastBeaconResult.isValid())) {
            btnConfirm.setEnabled(false);
            btnConfirm.setAlpha(0.5f);
        }
    }

    private void doPunch(boolean useBeacon) {
        if (useBeacon && (lastBeaconResult == null || !lastBeaconResult.isValid())) {
            Toast.makeText(getContext(), "Valid beacon required for this punch method", Toast.LENGTH_SHORT).show();
            return;
        }

        MaterialButton targetButton = useBeacon ? btnBeacon : btnConfirm;
        targetButton.setEnabled(false);
        targetButton.setAlpha(0.5f);

        String deviceId = android.provider.Settings.Secure.getString(
                requireContext().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        String beaconId = useBeacon && lastBeaconResult != null ? lastBeaconResult.getBeaconId() : null;
        Integer beaconRssi = useBeacon && lastBeaconResult != null ? lastBeaconResult.getRssi() : null;

        if (isPunchedIn) {
            repo.punchOut(deviceId, null, null, beaconId, beaconRssi)
                    .addOnSuccessListener(v -> {
                        String method = useBeacon ? "via Beacon" : "";
                        Toast.makeText(getContext(), "Punched Out Successfully " + method, Toast.LENGTH_SHORT).show();
                        dismissAllowingStateLoss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        targetButton.setEnabled(true);
                        targetButton.setAlpha(1f);
                    });
        } else {
            repo.punchIn(deviceId, null, null, beaconId, beaconRssi)
                    .addOnSuccessListener(v -> {
                        String method = useBeacon ? "via Beacon" : "";
                        Toast.makeText(getContext(), "Punched In Successfully " + method, Toast.LENGTH_SHORT).show();
                        dismissAllowingStateLoss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        targetButton.setEnabled(true);
                        targetButton.setAlpha(1f);
                    });
        }
    }

    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestBluetoothPermissions() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT
            };
        } else {
            permissions = new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            };
        }

        requestPermissions(permissions, BLUETOOTH_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == BLUETOOTH_PERMISSION_REQUEST) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                startBeaconScan();
            } else {
                progressBeacon.setVisibility(View.GONE);
                chipBeacon.setText("Beacon: permission denied");
            }
        }
    }

    private List<BeaconConfig> getSampleBeaconConfigs() {
        // NOTE: Replace with Firestore-backed configs in production
        List<BeaconConfig> configs = new ArrayList<>();

        BeaconConfig config = new BeaconConfig();
        config.setBeaconId("office-main");
        config.setUuid("e2c56db5-dffb-48d2-b060-d0f5a71096e0");
        config.setMajor(1);
        config.setMinor(1);
        config.setRssiThreshold(-70);
        config.setLabel("Office Main");
        config.setEnabled(true);
        configs.add(config);

        return configs;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (beaconScanner != null) {
            beaconScanner.stopScan();
        }
    }
}
