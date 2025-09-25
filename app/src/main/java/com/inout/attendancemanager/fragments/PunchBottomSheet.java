package com.inout.attendancemanager.fragments;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.inout.attendancemanager.R;
import com.inout.attendancemanager.repositories.AttendanceRepository;
import com.inout.attendancemanager.utils.GeofenceUtils;

public class PunchBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_IS_IN   = "is_punched_in";
    private static final String ARG_OLAT    = "office_lat";
    private static final String ARG_OLNG    = "office_lng";
    private static final String ARG_UID     = "user_id";
    private static final String ARG_RADIUS  = "radius_m";

    private boolean isPunchedIn;
    private double officeLat;
    private double officeLng;
    private String userId;
    private float radiusM;

    private FusedLocationProviderClient fused;
    private AttendanceRepository repo;

    public static void show(@NonNull androidx.fragment.app.Fragment parent,
                            boolean isPunchedIn,
                            double officeLat,
                            double officeLng,
                            float radiusMeters,
                            String userId) {
        PunchBottomSheet sheet = new PunchBottomSheet();
        Bundle b = new Bundle();
        b.putBoolean(ARG_IS_IN, isPunchedIn);
        b.putDouble(ARG_OLAT, officeLat);
        b.putDouble(ARG_OLNG, officeLng);
        b.putFloat(ARG_RADIUS, radiusMeters);
        b.putString(ARG_UID, userId);
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
        TextView tvTitle   = v.findViewById(R.id.tv_punch_title);
        TextView tvStatus  = v.findViewById(R.id.tv_office_status);
        TextView tvDist    = v.findViewById(R.id.tv_distance_info);
        CircularProgressIndicator progress = v.findViewById(R.id.progress_location);
        MaterialButton btnConfirm = v.findViewById(R.id.btn_confirm_punch);
        MaterialButton btnCancel  = v.findViewById(R.id.btn_cancel);

        tvTitle.setText(isPunchedIn ? "Punch Out" : "Punch In");
        btnConfirm.setText(isPunchedIn ? "Punch Out" : "Punch In");
        btnConfirm.setEnabled(false);
        btnConfirm.setAlpha(0.5f);
        btnConfirm.setOnClickListener(null);
        progress.setVisibility(View.VISIBLE);

        // Guard: office coords must exist
        if (officeLat == 0.0 || officeLng == 0.0) {
            tvStatus.setText("Office location not set");
            progress.setVisibility(View.GONE);
            btnConfirm.setEnabled(false);
            btnConfirm.setAlpha(0.5f);
            btnConfirm.setOnClickListener(null);
            btnCancel.setOnClickListener(v1 -> dismissAllowingStateLoss());
            return;
        }

        fused.getLastLocation()
                .addOnSuccessListener(loc -> {
                    progress.setVisibility(View.GONE);
                    if (loc == null) {
                        tvStatus.setText("Location unavailable");
                        tvDist.setText("Distance: -- m");
                        btnConfirm.setEnabled(false);
                        btnConfirm.setAlpha(0.5f);
                        btnConfirm.setOnClickListener(null);
                        return;
                    }
                    float dist = GeofenceUtils.distanceMeters(
                            loc.getLatitude(), loc.getLongitude(), officeLat, officeLng);

                    tvDist.setText("Distance: " + Math.round(dist) + " m");

                    if (dist <= radiusM) {
                        tvStatus.setText("Inside office radius (" + (int) radiusM + " m)");
                        btnConfirm.setEnabled(true);
                        btnConfirm.setAlpha(1f);
                        btnConfirm.setOnClickListener(v2 -> doPunch(loc, btnConfirm));
                    } else {
                        tvStatus.setText("Outside office radius (" + (int) radiusM + " m)");
                        btnConfirm.setEnabled(false);
                        btnConfirm.setAlpha(0.5f);
                        btnConfirm.setOnClickListener(null);
                    }
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    tvStatus.setText("Location error");
                    btnConfirm.setEnabled(false);
                    btnConfirm.setAlpha(0.5f);
                    btnConfirm.setOnClickListener(null);
                });

        btnCancel.setOnClickListener(v1 -> dismissAllowingStateLoss());
    }

    private void doPunch(@Nullable Location location, @NonNull MaterialButton btnConfirm) {
        btnConfirm.setEnabled(false);
        btnConfirm.setAlpha(0.5f);

        String deviceId = android.provider.Settings.Secure.getString(
                requireContext().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        if (isPunchedIn) {
            repo.punchOut(deviceId,
                            location != null ? location.getLatitude() : null,
                            location != null ? location.getLongitude() : null)
                    .addOnSuccessListener(v -> {
                        Toast.makeText(getContext(), "Punched Out Successfully", Toast.LENGTH_SHORT).show();
                        dismissAllowingStateLoss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnConfirm.setEnabled(true);
                        btnConfirm.setAlpha(1f);
                    });
        } else {
            repo.punchIn(deviceId,
                            location != null ? location.getLatitude() : null,
                            location != null ? location.getLongitude() : null)
                    .addOnSuccessListener(v -> {
                        Toast.makeText(getContext(), "Punched In Successfully", Toast.LENGTH_SHORT).show();
                        dismissAllowingStateLoss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnConfirm.setEnabled(true);
                        btnConfirm.setAlpha(1f);
                    });
        }
    }
}
