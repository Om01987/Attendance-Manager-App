package com.inout.attendancemanager.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.hbb20.CountryCodePicker;
import com.inout.attendancemanager.R;
import com.inout.attendancemanager.utils.Constants;

import java.util.concurrent.TimeUnit;

public class MobileVerificationActivity extends AppCompatActivity {

    private static final String TAG = "MobileVerification";
    private static final int OTP_TIMEOUT_SECONDS = 60;

    // UI Components
    private CountryCodePicker ccpCountryPicker;
    private TextInputLayout tilPhoneNumber;
    private TextInputEditText etPhoneNumber;
    private TextView tvCompleteNumber;
    private MaterialButton btnSendOtp;
    private LinearProgressIndicator progressIndicator;

    // Firebase
    private FirebaseAuth firebaseAuth;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    // Data
    private SharedPreferences sharedPreferences;
    private String completePhoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile_verification);

        initViews();
        initFirebase();
        setupListeners();

        // Auto-focus on phone number input
        etPhoneNumber.requestFocus();
    }

    private void initViews() {
        ccpCountryPicker = findViewById(R.id.ccp_country_picker);
        tilPhoneNumber = findViewById(R.id.til_phone_number);
        etPhoneNumber = findViewById(R.id.et_phone_number);
        tvCompleteNumber = findViewById(R.id.tv_complete_number);
        btnSendOtp = findViewById(R.id.btn_send_otp);
        progressIndicator = findViewById(R.id.progress_indicator);


        sharedPreferences = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);

        // Link country code picker with phone number input
        // After ccpCountryPicker = findViewById(R.id.ccp_country_picker);
        ccpCountryPicker.setCountryForNameCode("IN"); // sets selected country to India (use ISO code like "US", "IN")
        ccpCountryPicker.registerCarrierNumberEditText(etPhoneNumber); // keep this for full-number validation

    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    private void setupListeners() {
        // Phone number text change listener
        etPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validatePhoneNumber();
                updateCompleteNumberDisplay();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Country code change listener
        ccpCountryPicker.setOnCountryChangeListener(() -> {
            validatePhoneNumber();
            updateCompleteNumberDisplay();
        });

        // Send OTP button click listener
        btnSendOtp.setOnClickListener(v -> {
            if (validateInput()) {
                sendOtp();
            }
        });
    }

    private void validatePhoneNumber() {
        String phoneNumber = etPhoneNumber.getText().toString().trim();

        if (phoneNumber.isEmpty()) {
            tilPhoneNumber.setError(null);
            btnSendOtp.setEnabled(false);
            return;
        }

        boolean isValid = ccpCountryPicker.isValidFullNumber();
        if (isValid) {
            tilPhoneNumber.setError(null);
            btnSendOtp.setEnabled(true);
        } else {
            tilPhoneNumber.setError(getString(R.string.invalid_phone_number));
            btnSendOtp.setEnabled(false);
        }
    }

    private void updateCompleteNumberDisplay() {
        try {
            completePhoneNumber = ccpCountryPicker.getFullNumberWithPlus();
            tvCompleteNumber.setText(getString(R.string.complete_number, completePhoneNumber));
            tvCompleteNumber.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            tvCompleteNumber.setVisibility(View.GONE);
        }
    }

    private boolean validateInput() {
        String phoneNumber = etPhoneNumber.getText().toString().trim();

        if (phoneNumber.isEmpty()) {
            tilPhoneNumber.setError("Phone number is required");
            etPhoneNumber.requestFocus();
            return false;
        }

        if (!ccpCountryPicker.isValidFullNumber()) {
            tilPhoneNumber.setError(getString(R.string.invalid_phone_number));
            etPhoneNumber.requestFocus();
            return false;
        }

        return true;
    }

    private void sendOtp() {
        setLoadingState(true);

        try {
            completePhoneNumber = ccpCountryPicker.getFullNumberWithPlus();

            PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)
                    .setPhoneNumber(completePhoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override
                        public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                            // Auto-verification completed
                            Log.d(TAG, "onVerificationCompleted: Auto-verification successful");
                            setLoadingState(false);

                            // Auto-verify and proceed
                            signInWithPhoneAuthCredential(phoneAuthCredential);
                        }

                        @Override
                        public void onVerificationFailed(FirebaseException e) {
                            Log.e(TAG, "onVerificationFailed: " + e.getMessage(), e);
                            setLoadingState(false);

                            String errorMessage = getErrorMessage(e);
                            showErrorDialog("Verification Failed", errorMessage);
                        }

                        @Override
                        public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken token) {
                            Log.d(TAG, "onCodeSent: " + s);
                            setLoadingState(false);

                            verificationId = s;
                            resendToken = token;

                            // Save phone number for later use
                            savePhoneNumber();

                            Toast.makeText(MobileVerificationActivity.this,
                                    getString(R.string.otp_sent_success), Toast.LENGTH_SHORT).show();

                            // Navigate to OTP verification
                            navigateToOtpVerification();
                        }
                    })
                    .build();

            PhoneAuthProvider.verifyPhoneNumber(options);

        } catch (Exception e) {
            Log.e(TAG, "Error sending OTP: " + e.getMessage(), e);
            setLoadingState(false);
            showErrorDialog("Error", "Failed to send OTP. Please try again.");
        }
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");

                        // Save user data
                        saveUserData();

                        Toast.makeText(this, getString(R.string.phone_verified_success),
                                Toast.LENGTH_SHORT).show();

                        // Navigate to next activity
                        navigateToNextActivity();
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        showErrorDialog("Verification Failed",
                                "Phone number verification failed. Please try again.");
                    }
                });
    }

    private String getErrorMessage(FirebaseException e) {
        if (e.getMessage().contains("invalid phone number")) {
            return getString(R.string.invalid_phone_number);
        } else if (e.getMessage().contains("network")) {
            return getString(R.string.network_error);
        } else if (e.getMessage().contains("timeout")) {
            return getString(R.string.verification_timeout);
        } else {
            return "Verification failed: " + e.getMessage();
        }
    }

    private void savePhoneNumber() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.PREF_PHONE_NUMBER, completePhoneNumber);
        editor.putString("verification_id", verificationId);
        editor.putLong("verification_time", System.currentTimeMillis());
        editor.apply();
    }

    private void saveUserData() {
        if (firebaseAuth.getCurrentUser() != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(Constants.PREF_USER_ID, firebaseAuth.getCurrentUser().getUid());
            editor.putString(Constants.PREF_PHONE_NUMBER, completePhoneNumber);
            editor.putBoolean("phone_verified", true);
            editor.putLong("verification_time", System.currentTimeMillis());
            editor.apply();
        }
    }

    private void navigateToOtpVerification() {
        Intent intent = new Intent(MobileVerificationActivity.this, OtpVerificationActivity.class);
        intent.putExtra("phone_number", completePhoneNumber);
        intent.putExtra("verification_id", verificationId);
        startActivity(intent);
        // Don't finish this activity in case user wants to go back
    }

    private void navigateToNextActivity() {
        // TODO: Navigate to Employee Registration Activity (will implement later)
        Intent intent = new Intent(MobileVerificationActivity.this,
                com.inout.attendancemanager.MainActivity.class);
        intent.putExtra("message", "Phone verified! Employee registration coming soon...");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoadingState(boolean loading) {
        if (loading) {
            btnSendOtp.setEnabled(false);
            btnSendOtp.setText("Sending...");
            progressIndicator.setVisibility(View.VISIBLE);
            etPhoneNumber.setEnabled(false);
            ccpCountryPicker.setCcpClickable(false);
        } else {
            btnSendOtp.setEnabled(true);
            btnSendOtp.setText(getString(R.string.send_verification_code));
            progressIndicator.setVisibility(View.GONE);
            etPhoneNumber.setEnabled(true);
            ccpCountryPicker.setCcpClickable(true);
        }
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setNeutralButton("Retry", (dialog, which) -> {
                    // Retry sending OTP
                    if (validateInput()) {
                        sendOtp();
                    }
                })
                .show();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Verification?")
                .setMessage("Are you sure you want to go back? You'll need to verify your phone number to continue.")
                .setPositiveButton("Exit", (dialog, which) -> super.onBackPressed())
                .setNegativeButton("Stay", null)
                .show();
    }
}
