package com.inout.attendancemanager.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.inout.attendancemanager.MainActivity;
import com.inout.attendancemanager.R;
import com.inout.attendancemanager.models.Employee;
import com.inout.attendancemanager.utils.Constants;
import com.inout.attendancemanager.utils.ValidationUtils;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class EmployeeRegistrationActivity extends AppCompatActivity {

    private static final String TAG = "EmployeeRegistration";

    // UI Components
    private CircleImageView ivProfilePicture;
    private MaterialButton btnTakePhoto, btnChoosePhoto;
    private TextInputEditText etFullName, etEmployeeId, etDesignation;
    private AutoCompleteTextView actvDepartment, actvOfficeLocation;
    private TextInputEditText etJoinDate, etReportingManager;
    private TextInputEditText etEmergencyContact, etEmergencyPhone;
    private MaterialButton btnSaveDraft, btnSubmit;
    private LinearProgressIndicator progressIndicator;

    // Input Layouts for validation
    private TextInputLayout tilFullName, tilEmployeeId, tilDepartment;
    private TextInputLayout tilDesignation, tilJoinDate, tilReportingManager;
    private TextInputLayout tilOfficeLocation, tilEmergencyContact, tilEmergencyPhone;

    // Firebase
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    // Data
    private SharedPreferences sharedPreferences;
    private Uri selectedImageUri;
    private String profileImageUrl;
    private Calendar joinDateCalendar;

    // Activity Result Launchers
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<PickVisualMediaRequest> photoPickerLauncher;

    // Dropdown Data
    private final String[] departments = {
            "Information Technology", "Human Resources", "Finance", "Marketing",
            "Operations", "Sales", "Customer Support", "Research & Development",
            "Quality Assurance", "Administration"
    };

    private final String[] officeLocations = {
            "Head Office - Mumbai", "Branch Office - Delhi", "Branch Office - Bangalore",
            "Branch Office - Hyderabad", "Branch Office - Pune", "Branch Office - Chennai",
            "Regional Office - Kolkata", "Regional Office - Ahmedabad"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_registration);

        initFirebase();
        initViews();
        setupDropdowns();
        setupActivityResultLaunchers();
        initClickListeners();
        initBackHandler();
        loadSavedData();
    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }

    private void initViews() {
        // Profile Picture
        ivProfilePicture = findViewById(R.id.iv_profile_picture);
        btnTakePhoto = findViewById(R.id.btn_take_photo);
        btnChoosePhoto = findViewById(R.id.btn_choose_photo);

        // Personal Information
        etFullName = findViewById(R.id.et_full_name);
        etEmployeeId = findViewById(R.id.et_employee_id);
        actvDepartment = findViewById(R.id.actv_department);
        etDesignation = findViewById(R.id.et_designation);

        // Employment Details
        etJoinDate = findViewById(R.id.et_join_date);
        etReportingManager = findViewById(R.id.et_reporting_manager);
        actvOfficeLocation = findViewById(R.id.actv_office_location);

        // Contact Information
        etEmergencyContact = findViewById(R.id.et_emergency_contact);
        etEmergencyPhone = findViewById(R.id.et_emergency_phone);

        // Input Layouts for validation
        tilFullName = findViewById(R.id.til_full_name);
        tilEmployeeId = findViewById(R.id.til_employee_id);
        tilDepartment = findViewById(R.id.til_department);
        tilDesignation = findViewById(R.id.til_designation);
        tilJoinDate = findViewById(R.id.til_join_date);
        tilReportingManager = findViewById(R.id.til_reporting_manager);
        tilOfficeLocation = findViewById(R.id.til_office_location);
        tilEmergencyContact = findViewById(R.id.til_emergency_contact);
        tilEmergencyPhone = findViewById(R.id.til_emergency_phone);

        // Action Buttons
        btnSaveDraft = findViewById(R.id.btn_save_draft);
        btnSubmit = findViewById(R.id.btn_submit);
        progressIndicator = findViewById(R.id.progress_indicator);

        // SharedPreferences
        sharedPreferences = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);

        // Initialize calendar
        joinDateCalendar = Calendar.getInstance();
    }

    private void setupDropdowns() {
        // Department Dropdown
        ArrayAdapter<String> departmentAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, departments);
        actvDepartment.setAdapter(departmentAdapter);

        // Office Location Dropdown
        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, officeLocations);
        actvOfficeLocation.setAdapter(locationAdapter);
    }

    private void setupActivityResultLaunchers() {
        // Camera Launcher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null && extras.get("data") != null) {
                            Bitmap imageBitmap = (Bitmap) extras.get("data");
                            if (imageBitmap != null) {
                                ivProfilePicture.setImageBitmap(imageBitmap);
                                selectedImageUri = getImageUriFromBitmap(imageBitmap);
                            }
                        }
                    }
                }
        );

        // Photo Picker Launcher
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        Glide.with(this)
                                .load(uri)
                                .centerCrop()
                                .into(ivProfilePicture);
                    }
                }
        );
    }

    private void initClickListeners() {
        btnTakePhoto.setOnClickListener(v -> takePhoto());
        btnChoosePhoto.setOnClickListener(v -> chooseFromGallery());
        etJoinDate.setOnClickListener(v -> showDatePicker());
        btnSaveDraft.setOnClickListener(v -> saveDraft());
        btnSubmit.setOnClickListener(v -> submitProfile());
    }

    private void initBackHandler() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (hasUnsavedChanges()) {
                    showUnsavedChangesDialog();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void chooseFromGallery() {
        photoPickerLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    joinDateCalendar.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    etJoinDate.setText(sdf.format(joinDateCalendar.getTime()));
                },
                joinDateCalendar.get(Calendar.YEAR),
                joinDateCalendar.get(Calendar.MONTH),
                joinDateCalendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set max date to today
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void saveDraft() {
        if (validateRequiredFields(false)) {
            showProgress(true);
            saveDraftToPreferences();
            showProgress(false);
            Toast.makeText(this, "Draft saved successfully", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitProfile() {
        if (validateRequiredFields(true)) {
            showProgress(true);

            if (selectedImageUri != null) {
                uploadProfileImage();
            } else {
                saveEmployeeToFirestore();
            }
        }
    }

    private boolean validateRequiredFields(boolean isSubmission) {
        boolean isValid = true;

        // Clear previous errors
        clearAllErrors();

        // Full Name
        if (TextUtils.isEmpty(etFullName.getText())) {
            tilFullName.setError("Full name is required");
            isValid = false;
        }

        // Employee ID
        if (TextUtils.isEmpty(etEmployeeId.getText())) {
            tilEmployeeId.setError("Employee ID is required");
            isValid = false;
        } else if (!ValidationUtils.isValidEmployeeId(etEmployeeId.getText().toString())) {
            tilEmployeeId.setError("Invalid Employee ID format");
            isValid = false;
        }

        // Department
        if (TextUtils.isEmpty(actvDepartment.getText())) {
            tilDepartment.setError("Department is required");
            isValid = false;
        }

        // Designation
        if (TextUtils.isEmpty(etDesignation.getText())) {
            tilDesignation.setError("Designation is required");
            isValid = false;
        }

        // Join Date
        if (TextUtils.isEmpty(etJoinDate.getText())) {
            tilJoinDate.setError("Join date is required");
            isValid = false;
        }

        // Reporting Manager
        if (TextUtils.isEmpty(etReportingManager.getText())) {
            tilReportingManager.setError("Reporting manager is required");
            isValid = false;
        }

        // Office Location
        if (TextUtils.isEmpty(actvOfficeLocation.getText())) {
            tilOfficeLocation.setError("Office location is required");
            isValid = false;
        }

        // For submission, validate optional but recommended fields
        if (isSubmission) {
            // Emergency Contact
            if (!TextUtils.isEmpty(etEmergencyContact.getText()) &&
                    TextUtils.isEmpty(etEmergencyPhone.getText())) {
                tilEmergencyPhone.setError("Emergency phone is required when contact name is provided");
                isValid = false;
            }

            // Emergency Phone validation
            if (!TextUtils.isEmpty(etEmergencyPhone.getText()) &&
                    !ValidationUtils.isValidPhoneNumber(etEmergencyPhone.getText().toString())) {
                tilEmergencyPhone.setError("Invalid phone number format");
                isValid = false;
            }
        }

        return isValid;
    }

    private void clearAllErrors() {
        tilFullName.setError(null);
        tilEmployeeId.setError(null);
        tilDepartment.setError(null);
        tilDesignation.setError(null);
        tilJoinDate.setError(null);
        tilReportingManager.setError(null);
        tilOfficeLocation.setError(null);
        tilEmergencyContact.setError(null);
        tilEmergencyPhone.setError(null);
    }

    private void uploadProfileImage() {
        String uid = firebaseAuth.getCurrentUser() != null ? firebaseAuth.getCurrentUser().getUid() : null;
        if (uid == null) {
            Log.e(TAG, "No authenticated user; cannot upload image");
            Toast.makeText(this, "Please re-login before uploading image", Toast.LENGTH_SHORT).show();
            showProgress(false);
            return;
        }

        // Path that matches your rules: profile_images/{userId}/...
        String fileName = "profile_images/" + uid + "/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = storageRef.child(fileName);

        // Always provide JPEG metadata so rules with request.resource.contentType pass
        com.google.firebase.storage.StorageMetadata metadata =
                new com.google.firebase.storage.StorageMetadata.Builder()
                        .setContentType("image/jpeg")
                        .build();

        try {
            // Decide: direct upload vs compression, based on content length
            long approxBytes = queryContentLength(selectedImageUri);
            boolean needsCompression = approxBytes < 0 || approxBytes > 4_800_000L; // ~4.8 MB threshold

            if (!needsCompression) {
                // Fast path: direct upload
                imageRef.putFile(selectedImageUri, metadata)
                        .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    profileImageUrl = uri.toString();
                                    saveEmployeeToFirestore();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "getDownloadUrl failed", e);
                                    showProgress(false);
                                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                                }))
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "putFile failed", e);
                            showProgress(false);
                            Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                        });
                return;
            }

            // Compression path: decode → compress JPEG → upload bytes
            Bitmap bmp = decodeBitmapCompat(selectedImageUri);
            if (bmp == null) {
                Log.e(TAG, "Bitmap decode returned null");
                showProgress(false);
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
                return;
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // Start with 85 quality; you can loop to reduce further if still > 5MB
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
            byte[] data = out.toByteArray();

            imageRef.putBytes(data, metadata)
                    .addOnSuccessListener(t -> imageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                profileImageUrl = uri.toString();
                                saveEmployeeToFirestore();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "getDownloadUrl failed", e);
                                showProgress(false);
                                Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                            }))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "putBytes failed", e);
                        showProgress(false);
                        Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception ex) {
            Log.e(TAG, "Image handling failed", ex);
            showProgress(false);
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Return the content length from the ContentResolver if available, else -1.
     */
    private long queryContentLength(Uri uri) {
        try (android.database.Cursor c = getContentResolver().query(
                uri, new String[]{android.provider.OpenableColumns.SIZE}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(android.provider.OpenableColumns.SIZE);
                if (idx >= 0) {
                    return c.getLong(idx);
                }
            }
        } catch (Exception ignored) { }
        return -1L;
    }

    /**
     * Decode a bitmap from a content Uri using ImageDecoder on API 28+,
     * falling back to MediaStore for older devices.
     */
    private Bitmap decodeBitmapCompat(Uri uri) throws java.io.IOException {
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            android.graphics.ImageDecoder.Source src =
                    android.graphics.ImageDecoder.createSource(getContentResolver(), uri);
            // You may downscale here via OnHeaderDecodedListener if desired
            return android.graphics.ImageDecoder.decodeBitmap(src);
        } else {
            @SuppressWarnings("deprecation")
            Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            return bmp;
        }
    }



    private void saveEmployeeToFirestore() {
        String userId = firebaseAuth.getCurrentUser().getUid();

        Employee employee = new Employee();
        employee.setUserId(userId);
        employee.setFullName(etFullName.getText().toString().trim());
        employee.setEmployeeId(etEmployeeId.getText().toString().trim());
        employee.setDepartment(actvDepartment.getText().toString());
        employee.setDesignation(etDesignation.getText().toString().trim());
        employee.setJoinDate(etJoinDate.getText().toString());
        employee.setReportingManager(etReportingManager.getText().toString().trim());
        employee.setOfficeLocation(actvOfficeLocation.getText().toString());
        employee.setEmergencyContactName(etEmergencyContact.getText().toString().trim());
        employee.setEmergencyContactPhone(etEmergencyPhone.getText().toString().trim());
        employee.setProfileImageUrl(profileImageUrl);
        // Keep using the stored verified phone and device id
        employee.setPhoneNumber(sharedPreferences.getString(Constants.PREF_PHONE_NUMBER, ""));
        employee.setDeviceId(sharedPreferences.getString(Constants.PREF_DEVICE_ID, ""));
        employee.setRegistrationDate(new Date());
        employee.setIsActive(true);
        employee.setApprovalStatus("pending");

        // Save to Firestore
        firestore.collection("employees")
                .document(userId)
                .set(employee)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Employee profile saved successfully");

                    // Mark profile as completed ONLY after Firestore write succeeds
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(Constants.PREF_USER_ID, userId);
                    editor.putBoolean(Constants.PREF_PROFILE_COMPLETED, true);   // canonical flag
                    editor.putBoolean("profile_completed", true);                // legacy flag (back-compat)
                    // Optional persisted fields used elsewhere in the app
                    editor.putString("employee_id", employee.getEmployeeId());
                    editor.putString("full_name", employee.getFullName());
                    editor.putString("department", employee.getDepartment());
                    editor.apply();

                    showProgress(false);
                    clearDraft();
                    showSuccessDialog();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save employee profile", e);
                    showProgress(false);
                    Toast.makeText(this, "Failed to save profile. Please try again.", Toast.LENGTH_LONG).show();
                });
    }

    private void saveDraftToPreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("draft_full_name", etFullName.getText().toString());
        editor.putString("draft_employee_id", etEmployeeId.getText().toString());
        editor.putString("draft_department", actvDepartment.getText().toString());
        editor.putString("draft_designation", etDesignation.getText().toString());
        editor.putString("draft_join_date", etJoinDate.getText().toString());
        editor.putString("draft_reporting_manager", etReportingManager.getText().toString());
        editor.putString("draft_office_location", actvOfficeLocation.getText().toString());
        editor.putString("draft_emergency_contact", etEmergencyContact.getText().toString());
        editor.putString("draft_emergency_phone", etEmergencyPhone.getText().toString());

        if (selectedImageUri != null) {
            editor.putString("draft_image_uri", selectedImageUri.toString());
        }

        editor.putLong("draft_saved_time", System.currentTimeMillis());
        editor.apply();
    }

    private void loadSavedData() {
        // Load draft data if exists
        if (sharedPreferences.contains("draft_full_name")) {
            etFullName.setText(sharedPreferences.getString("draft_full_name", ""));
            etEmployeeId.setText(sharedPreferences.getString("draft_employee_id", ""));
            actvDepartment.setText(sharedPreferences.getString("draft_department", ""));
            etDesignation.setText(sharedPreferences.getString("draft_designation", ""));
            etJoinDate.setText(sharedPreferences.getString("draft_join_date", ""));
            etReportingManager.setText(sharedPreferences.getString("draft_reporting_manager", ""));
            actvOfficeLocation.setText(sharedPreferences.getString("draft_office_location", ""));
            etEmergencyContact.setText(sharedPreferences.getString("draft_emergency_contact", ""));
            etEmergencyPhone.setText(sharedPreferences.getString("draft_emergency_phone", ""));

            String imageUriString = sharedPreferences.getString("draft_image_uri", "");
            if (!imageUriString.isEmpty()) {
                selectedImageUri = Uri.parse(imageUriString);
                Glide.with(this)
                        .load(selectedImageUri)
                        .centerCrop()
                        .into(ivProfilePicture);
            }

            Toast.makeText(this, "Draft data loaded", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearDraft() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("draft_full_name");
        editor.remove("draft_employee_id");
        editor.remove("draft_department");
        editor.remove("draft_designation");
        editor.remove("draft_join_date");
        editor.remove("draft_reporting_manager");
        editor.remove("draft_office_location");
        editor.remove("draft_emergency_contact");
        editor.remove("draft_emergency_phone");
        editor.remove("draft_image_uri");
        editor.remove("draft_saved_time");
        editor.apply();
    }

    private boolean hasUnsavedChanges() {
        return !TextUtils.isEmpty(etFullName.getText()) ||
                !TextUtils.isEmpty(etEmployeeId.getText()) ||
                !TextUtils.isEmpty(actvDepartment.getText()) ||
                !TextUtils.isEmpty(etDesignation.getText()) ||
                !TextUtils.isEmpty(etJoinDate.getText()) ||
                selectedImageUri != null;
    }

    private void showUnsavedChangesDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("You have unsaved changes. Do you want to save them as draft before leaving?")
                .setPositiveButton("Save Draft", (dialog, which) -> {
                    saveDraft();
                    finish();
                })
                .setNegativeButton("Discard", (dialog, which) -> finish())
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Profile Submitted Successfully!")
                .setMessage("Your profile has been submitted for approval. You will be notified once it's approved.")
                .setPositiveButton("Continue", (d, w) -> navigateToPendingApproval())
                .setCancelable(false)
                .show(); // [594]
    }

    private void navigateToPendingApproval() {
        Intent i = new Intent(this, PendingApprovalActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish(); // [594]
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("message", "Profile submitted successfully! Waiting for approval.");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showProgress(boolean show) {
        progressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!show);
        btnSaveDraft.setEnabled(!show);
    }

    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap,
                "Profile_" + System.currentTimeMillis(), null);
        return Uri.parse(path);
    }
}
