package com.inout.attendancemanager.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.inout.attendancemanager.R;
import com.inout.attendancemanager.activities.DashboardActivity;
import com.inout.attendancemanager.models.Attendance;
import com.inout.attendancemanager.models.AttendanceSummary;
import com.inout.attendancemanager.models.Employee;
import com.inout.attendancemanager.utils.Constants;
import com.inout.attendancemanager.utils.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";
    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    // UI Components
    private CircleImageView ivProfileImage;
    private TextView tvWelcomeMessage;
    private TextView tvEmployeeName;
    private TextView tvEmployeeDesignation;
    private TextView tvCurrentDate;
    private Chip chipApprovalStatus;

    // Punch Card
    private MaterialCardView cardPunch;
    private TextView tvShiftTime;
    private TextView tvCurrentStatus;
    private TextView tvInTime;
    private TextView tvOutTime;
    private TextView tvWorkingHours;
    private MaterialButton btnPunchAction;
    private TextView tvCountdown;

    // Summary Cards
    private TextView tvPresentDays;
    private TextView tvAbsentDays;
    private TextView tvWeekoffDays;
    private TextView tvMissedDays;
    private TextView tvTotalLeaves;
    private TextView tvUsedLeaves;
    private TextView tvRemainingLeaves;

    // Quick Links
    private MaterialCardView cardAnnouncements;
    private MaterialCardView cardHolidays;
    private MaterialCardView cardSalarySlip;
    private MaterialCardView cardReports;

    // Firebase & Data
    private FirebaseFirestore firestore;
    private FusedLocationProviderClient fusedLocationClient;
    private DashboardActivity dashboardActivity;
    private Employee currentEmployee;
    private String currentUserId;
    private Attendance todayAttendance;
    private AttendanceSummary monthSummary;

    // UI State
    private Handler countdownHandler;
    private Runnable countdownRunnable;
    private boolean isPunchedIn = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initFirebase();
        initViews(view);
        setupClickListeners();
        loadDashboardData();
        startCountdownTimer();
    }

    private void initFirebase() {
        firestore = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        dashboardActivity = (DashboardActivity) getActivity();

        if (dashboardActivity != null) {
            currentEmployee = dashboardActivity.getCurrentEmployee();
            currentUserId = dashboardActivity.getCurrentUserId();
        }
    }

    private void initViews(View view) {
        // Header
        ivProfileImage = view.findViewById(R.id.iv_profile_image);
        tvWelcomeMessage = view.findViewById(R.id.tv_welcome_message);
        tvEmployeeName = view.findViewById(R.id.tv_employee_name);
        tvEmployeeDesignation = view.findViewById(R.id.tv_employee_designation);
        tvCurrentDate = view.findViewById(R.id.tv_current_date);
        chipApprovalStatus = view.findViewById(R.id.chip_approval_status);

        // Punch Card
        cardPunch = view.findViewById(R.id.card_punch);
        tvShiftTime = view.findViewById(R.id.tv_shift_time);
        tvCurrentStatus = view.findViewById(R.id.tv_current_status);
        tvInTime = view.findViewById(R.id.tv_in_time);
        tvOutTime = view.findViewById(R.id.tv_out_time);
        tvWorkingHours = view.findViewById(R.id.tv_working_hours);
        btnPunchAction = view.findViewById(R.id.btn_punch_action);
        tvCountdown = view.findViewById(R.id.tv_countdown);

        // Summary Cards
        tvPresentDays = view.findViewById(R.id.tv_present_days);
        tvAbsentDays = view.findViewById(R.id.tv_absent_days);
        tvWeekoffDays = view.findViewById(R.id.tv_weekoff_days);
        tvMissedDays = view.findViewById(R.id.tv_missed_days);
        tvTotalLeaves = view.findViewById(R.id.tv_total_leaves);
        tvUsedLeaves = view.findViewById(R.id.tv_used_leaves);
        tvRemainingLeaves = view.findViewById(R.id.tv_remaining_leaves);

        // Quick Links
        cardAnnouncements = view.findViewById(R.id.card_announcements);
        cardHolidays = view.findViewById(R.id.card_holidays);
        cardSalarySlip = view.findViewById(R.id.card_salary_slip);
        cardReports = view.findViewById(R.id.card_reports);

        setupInitialUI();
    }

    private void setupInitialUI() {
        // Set current date
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault());
        tvCurrentDate.setText(dateFormat.format(new Date()));

        // Set shift time (default 9 AM - 6 PM)
        tvShiftTime.setText("09:00 AM - 06:00 PM");

        if (currentEmployee != null) {
            // Set employee info
            tvEmployeeName.setText(currentEmployee.getFullName());
            tvEmployeeDesignation.setText(currentEmployee.getDesignation() + " • " + currentEmployee.getDepartment());

            // Load profile image
            if (currentEmployee.getProfileImageUrl() != null && !currentEmployee.getProfileImageUrl().isEmpty()) {
                Glide.with(this)
                        .load(currentEmployee.getProfileImageUrl())
                        .placeholder(R.drawable.ic_person_placeholder)
                        .circleCrop()
                        .into(ivProfileImage);
            }

            // Set approval status
            String status = currentEmployee.getApprovalStatus();
            if ("approved".equals(status)) {
                chipApprovalStatus.setText("✓ Approved");
                chipApprovalStatus.setChipBackgroundColorResource(R.color.status_approved);
                btnPunchAction.setEnabled(true);
            } else {
                chipApprovalStatus.setText("⏳ Pending");
                chipApprovalStatus.setChipBackgroundColorResource(R.color.status_pending);
                btnPunchAction.setEnabled(false);
                btnPunchAction.setText("Approval Pending");
            }
        }
    }

    private void setupClickListeners() {
        btnPunchAction.setOnClickListener(v -> handlePunchAction());

        cardAnnouncements.setOnClickListener(v -> Toast.makeText(getContext(), "Announcements - Coming Soon", Toast.LENGTH_SHORT).show());
        cardHolidays.setOnClickListener(v -> Toast.makeText(getContext(), "Holidays - Coming Soon", Toast.LENGTH_SHORT).show());
        cardSalarySlip.setOnClickListener(v -> Toast.makeText(getContext(), "Salary Slip - Coming Soon", Toast.LENGTH_SHORT).show());
        cardReports.setOnClickListener(v -> Toast.makeText(getContext(), "Reports - Coming Soon", Toast.LENGTH_SHORT).show());
    }

    private void loadDashboardData() {
        loadTodayAttendance();
        loadMonthSummary();
    }

    private void loadTodayAttendance() {
        if (currentUserId == null) return;

        String todayDateId = DateUtils.getTodayDateId();

        firestore.collection("attendance")
                .document(currentUserId)
                .collection("days")
                .document(todayDateId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        todayAttendance = doc.toObject(Attendance.class);
                        updatePunchCardUI();
                    } else {
                        // No attendance record for today
                        todayAttendance = null;
                        updatePunchCardUI();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load today's attendance", e);
                });
    }

    private void loadMonthSummary() {
        if (currentUserId == null) return;

        String currentMonth = DateUtils.getCurrentMonthId();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date monthStart = cal.getTime();

        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date monthEnd = cal.getTime();

        firestore.collection("attendance")
                .document(currentUserId)
                .collection("days")
                .whereGreaterThanOrEqualTo("createdAt", monthStart)
                .whereLessThanOrEqualTo("createdAt", monthEnd)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    monthSummary = new AttendanceSummary();
                    monthSummary.setMonth(currentMonth);

                    int present = 0, absent = 0, weekoff = 0, missed = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Attendance attendance = doc.toObject(Attendance.class);
                        String status = attendance.getStatus();

                        if ("present".equals(status)) present++;
                        else if ("absent".equals(status)) absent++;
                        else if ("weekoff".equals(status)) weekoff++;
                        else if ("missed".equals(status)) missed++;
                    }

                    monthSummary.setPresentDays(present);
                    monthSummary.setAbsentDays(absent);
                    monthSummary.setWeekoffDays(weekoff);
                    monthSummary.setMissedPunchDays(missed);

                    updateSummaryUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load month summary", e);
                });
    }

    private void updatePunchCardUI() {
        if (todayAttendance == null) {
            // No punch yet today
            isPunchedIn = false;
            tvCurrentStatus.setText("Not Started");
            tvInTime.setText("--:--");
            tvOutTime.setText("--:--");
            tvWorkingHours.setText("00:00");
            btnPunchAction.setText("Punch In");
            btnPunchAction.setEnabled(currentEmployee != null && "approved".equals(currentEmployee.getApprovalStatus()));
        } else {
            if (todayAttendance.getInTime() != null && todayAttendance.getOutTime() == null) {
                // Punched in, not out
                isPunchedIn = true;
                tvCurrentStatus.setText("On Duty");
                tvInTime.setText(DateUtils.formatTime(todayAttendance.getInTime()));
                tvOutTime.setText("--:--");
                updateWorkingHours();
                btnPunchAction.setText("Punch Out");
                btnPunchAction.setEnabled(true);
            } else if (todayAttendance.getInTime() != null && todayAttendance.getOutTime() != null) {
                // Complete day
                isPunchedIn = false;
                tvCurrentStatus.setText("Day Complete");
                tvInTime.setText(DateUtils.formatTime(todayAttendance.getInTime()));
                tvOutTime.setText(DateUtils.formatTime(todayAttendance.getOutTime()));
                tvWorkingHours.setText(DateUtils.formatDuration(todayAttendance.getTotalMinutes()));
                btnPunchAction.setText("Day Complete");
                btnPunchAction.setEnabled(false);
            }
        }
    }

    private void updateWorkingHours() {
        if (todayAttendance != null && todayAttendance.getInTime() != null) {
            long workingMinutes = (System.currentTimeMillis() - todayAttendance.getInTime().getTime()) / (1000 * 60);
            tvWorkingHours.setText(DateUtils.formatDuration(workingMinutes));
        }
    }

    private void updateSummaryUI() {
        if (monthSummary != null) {
            tvPresentDays.setText(String.valueOf(monthSummary.getPresentDays()));
            tvAbsentDays.setText(String.valueOf(monthSummary.getAbsentDays()));
            tvWeekoffDays.setText(String.valueOf(monthSummary.getWeekoffDays()));
            tvMissedDays.setText(String.valueOf(monthSummary.getMissedPunchDays()));
        }

        // TODO: Load leave data
        tvTotalLeaves.setText("20");
        tvUsedLeaves.setText("5");
        tvRemainingLeaves.setText("15");
    }

    private void handlePunchAction() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

        btnPunchAction.setEnabled(false);
        btnPunchAction.setText("Processing...");

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (isPunchedIn) {
                        punchOut(location);
                    } else {
                        punchIn(location);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get location", e);
                    // Proceed without location
                    if (isPunchedIn) {
                        punchOut(null);
                    } else {
                        punchIn(null);
                    }
                });
    }

    private void punchIn(Location location) {
        String todayDateId = DateUtils.getTodayDateId();
        Date now = new Date();

        Attendance attendance = new Attendance();
        attendance.setUserId(currentUserId);
        attendance.setDateId(todayDateId);
        attendance.setInTime(now);
        attendance.setStatus("present");
        attendance.setMethod("manual");
        attendance.setCreatedAt(now);
        attendance.setUpdatedAt(now);

        if (location != null) {
            attendance.setLatitude(location.getLatitude());
            attendance.setLongitude(location.getLongitude());
        }

        firestore.collection("attendance")
                .document(currentUserId)
                .collection("days")
                .document(todayDateId)
                .set(attendance)
                .addOnSuccessListener(aVoid -> {
                    todayAttendance = attendance;
                    isPunchedIn = true;
                    updatePunchCardUI();
                    Toast.makeText(getContext(), "Punched In Successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to punch in", e);
                    btnPunchAction.setEnabled(true);
                    btnPunchAction.setText("Punch In");
                    Toast.makeText(getContext(), "Failed to punch in. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void punchOut(Location location) {
        if (todayAttendance == null) {
            btnPunchAction.setEnabled(true);
            btnPunchAction.setText("Punch Out");
            Toast.makeText(getContext(), "No punch in record found", Toast.LENGTH_SHORT).show();
            return;
        }

        Date now = new Date();
        todayAttendance.setOutTime(now);

        if (todayAttendance.getInTime() != null) {
            long totalMinutes = (now.getTime() - todayAttendance.getInTime().getTime()) / (1000 * 60);
            todayAttendance.setTotalMinutes(totalMinutes);
        }

        todayAttendance.setUpdatedAt(now);

        if (location != null) {
            todayAttendance.setLatitude(location.getLatitude());
            todayAttendance.setLongitude(location.getLongitude());
        }

        firestore.collection("attendance")
                .document(currentUserId)
                .collection("days")
                .document(todayAttendance.getDateId())
                .set(todayAttendance)
                .addOnSuccessListener(aVoid -> {
                    isPunchedIn = false;
                    updatePunchCardUI();
                    Toast.makeText(getContext(), "Punched Out Successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to punch out", e);
                    btnPunchAction.setEnabled(true);
                    btnPunchAction.setText("Punch Out");
                    Toast.makeText(getContext(), "Failed to punch out. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void startCountdownTimer() {
        countdownHandler = new Handler(Looper.getMainLooper());
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                updateCountdown();
                updateWorkingHours();
                countdownHandler.postDelayed(this, 60000); // Update every minute
            }
        };
        countdownHandler.post(countdownRunnable);
    }

    private void updateCountdown() {
        // Calculate time remaining until 6 PM
        Calendar now = Calendar.getInstance();
        Calendar endOfDay = Calendar.getInstance();
        endOfDay.set(Calendar.HOUR_OF_DAY, 18);
        endOfDay.set(Calendar.MINUTE, 0);
        endOfDay.set(Calendar.SECOND, 0);

        if (now.after(endOfDay)) {
            tvCountdown.setText("Day Ended");
            return;
        }

        long diffInMillis = endOfDay.getTimeInMillis() - now.getTimeInMillis();
        long hours = diffInMillis / (1000 * 60 * 60);
        long minutes = (diffInMillis % (1000 * 60 * 60)) / (1000 * 60);

        tvCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d remaining", hours, minutes));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handlePunchAction();
            } else {
                Toast.makeText(getContext(), "Location permission required for attendance", Toast.LENGTH_SHORT).show();
                btnPunchAction.setEnabled(true);
                btnPunchAction.setText(isPunchedIn ? "Punch Out" : "Punch In");
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
    }
}

