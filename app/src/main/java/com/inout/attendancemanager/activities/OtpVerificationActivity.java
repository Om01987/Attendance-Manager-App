package com.inout.attendancemanager.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.inout.attendancemanager.MainActivity;
import com.inout.attendancemanager.R;
import com.inout.attendancemanager.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class OtpVerificationActivity extends AppCompatActivity {

    private static final String TAG = "OtpVerification";
    private static final int RESEND_TIMEOUT_SECONDS = 60;

    // UI Components
    private TextView tvPhoneNumberDisplay;
    private List<TextInputEditText> otpEditTexts;
    private TextView tvTimer;
    private TextView tvResendOtp;
    private TextView tvChangeNumber;
    private MaterialButton btnVerifyOtp;
    private LinearProgressIndicator progressIndicator;

    // Firebase
    private FirebaseAuth firebaseAuth;

    // Data
    private String phoneNumber;
    private String verificationId;
    private SharedPreferences sharedPreferences;
    private CountDownTimer resendTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        getIntentData();
        initViews();
        initFirebase();
        setupOtpInputs();
        setupListeners();
        startResendTimer();

        // Auto-focus on first OTP input
        if (!otpEditTexts.isEmpty()) {
            otpEditTexts.get(0).requestFocus();
        }
    }

    private void getIntentData() {
        Intent intent = getIntent();
        phoneNumber = intent.getStringExtra("phone_number");
        verificationId = intent.getStringExtra("verification_id");

        // If not from intent, try to get from SharedPreferences
        if (phoneNumber == null || verificationId == null) {
            sharedPreferences = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
            phoneNumber = sharedPreferences.getString(Constants.PREF_PHONE_NUMBER, "");
            verificationId = sharedPreferences.getString("verification_id", "");
        }

        if (phoneNumber.isEmpty() || verificationId.isEmpty()) {
            // Invalid state, go back
            Toast.makeText(this, "Invalid verification state", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    private void initViews() {
        tvPhoneNumberDisplay = findViewById(R.id.tv_phone_number_display);
        tvTimer = findViewById(R.id.tv_timer);
        tvResendOtp = findViewById(R.id.tv_resend_otp);
        tvChangeNumber = findViewById(R.id.tv_change_number);
        btnVerifyOtp = findViewById(R.id.btn_verify_otp);
        progressIndicator = findViewById(R.id.progress_indicator);

        // Initialize OTP input fields
        otpEditTexts = new ArrayList<>();
        otpEditTexts.add(findViewById(R.id.et_otp_1));
        otpEditTexts.add(findViewById(R.id.et_otp_2));
        otpEditTexts.add(findViewById(R.id.et_otp_3));
        otpEditTexts.add(findViewById(R.id.et_otp_4));
        otpEditTexts.add(findViewById(R.id.et_otp_5));
        otpEditTexts.add(findViewById(R.id.et_otp_6));

        sharedPreferences = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);

        // Set phone number in display
        tvPhoneNumberDisplay.setText(getString(R.string.enter_verification_code, phoneNumber));
    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    private void setupOtpInputs() {
        for (int i = 0; i < otpEditTexts.size(); i++) {
            final int index = i;
            TextInputEditText editText = otpEditTexts.get(i);

            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1) {
                        // Move to next field
                        if (index < otpEditTexts.size() - 1) {
                            otpEditTexts.get(index + 1).requestFocus();
                        }
                    }

                    // Enable/disable verify button based on completion
                    updateVerifyButton();

                    // Auto-verify if all fields are filled
                    if (isOtpComplete() && s.length() == 1) {
                        verifyOtp();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            // Handle backspace
            editText.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (editText.getText().toString().isEmpty() && index > 0) {
                        otpEditTexts.get(index - 1).requestFocus();
                        otpEditTexts.get(index - 1).getText().clear();
                    }
                }
                return false;
            });
        }
    }

    private void setupListeners() {
        btnVerifyOtp.setOnClickListener(v -> verifyOtp());

        tvResendOtp.setOnClickListener(v -> resendOtp());

        tvChangeNumber.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Change Number?")
                    .setMessage("Do you want to go back and change your phone number?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        finish(); // Go back to mobile verification
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private void updateVerifyButton() {
        boolean isComplete = isOtpComplete();
        btnVerifyOtp.setEnabled(isComplete);
        btnVerifyOtp.setAlpha(isComplete ? 1.0f : 0.5f);
    }

    private boolean isOtpComplete() {
        for (TextInputEditText editText : otpEditTexts) {
            if (editText.getText().toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String getEnteredOtp() {
        StringBuilder otp = new StringBuilder();
        for (TextInputEditText editText : otpEditTexts) {
            otp.append(editText.getText().toString().trim());
        }
        return otp.toString();
    }

    private void verifyOtp() {
        String otp = getEnteredOtp();

        if (otp.length() != 6) {
            Toast.makeText(this, getString(R.string.invalid_otp), Toast.LENGTH_SHORT).show();
            return;
        }

        setLoadingState(true);

        try {
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
            signInWithPhoneAuthCredential(credential);
        } catch (Exception e) {
            Log.e(TAG, "Error verifying OTP: " + e.getMessage(), e);
            setLoadingState(false);
            showErrorDialog("Verification Error", "Failed to verify OTP. Please try again.");
        }
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    setLoadingState(false);

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

                        // Clear OTP inputs
                        clearOtpInputs();

                        String errorMessage = getString(R.string.otp_verification_failed);
                        if (task.getException() != null) {
                            errorMessage += "\\n" + task.getException().getMessage();
                        }

                        showErrorDialog("Verification Failed", errorMessage);
                    }
                });
    }

    private void resendOtp() {
        // TODO: Implement resend OTP functionality
        Toast.makeText(this, "Resending OTP...", Toast.LENGTH_SHORT).show();

        // For now, restart timer
        startResendTimer();

        // In a real implementation, you would call Firebase's resend method here
    }

    private void clearOtpInputs() {
        for (TextInputEditText editText : otpEditTexts) {
            editText.getText().clear();
        }
        if (!otpEditTexts.isEmpty()) {
            otpEditTexts.get(0).requestFocus();
        }
        updateVerifyButton();
    }

    private void saveUserData() {
        if (firebaseAuth.getCurrentUser() != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(Constants.PREF_USER_ID, firebaseAuth.getCurrentUser().getUid());
            editor.putString(Constants.PREF_PHONE_NUMBER, phoneNumber);
            editor.putBoolean("phone_verified", true);
            editor.putLong("verification_time", System.currentTimeMillis());
            editor.apply();
        }
    }

    private void navigateToNextActivity() {
        // TODO: Navigate to Employee Registration Activity (will implement later)
        Intent intent = new Intent(OtpVerificationActivity.this, MainActivity.class);
        intent.putExtra("message", "Phone verified successfully!\\nEmployee registration coming soon...");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void startResendTimer() {
        tvResendOtp.setVisibility(View.GONE);
        tvTimer.setVisibility(View.VISIBLE);

        if (resendTimer != null) {
            resendTimer.cancel();
        }

        resendTimer = new CountDownTimer(RESEND_TIMEOUT_SECONDS * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = millisUntilFinished / 1000;
                tvTimer.setText(getString(R.string.resend_in, secondsRemaining));
            }

            @Override
            public void onFinish() {
                tvTimer.setVisibility(View.GONE);
                tvResendOtp.setVisibility(View.VISIBLE);
            }
        };

        resendTimer.start();
    }

    private void setLoadingState(boolean loading) {
        if (loading) {
            btnVerifyOtp.setEnabled(false);
            btnVerifyOtp.setText("Verifying...");
            progressIndicator.setVisibility(View.VISIBLE);

            // Disable OTP inputs
            for (TextInputEditText editText : otpEditTexts) {
                editText.setEnabled(false);
            }
        } else {
            btnVerifyOtp.setEnabled(isOtpComplete());
            btnVerifyOtp.setText(getString(R.string.verify_code));
            progressIndicator.setVisibility(View.GONE);

            // Enable OTP inputs
            for (TextInputEditText editText : otpEditTexts) {
                editText.setEnabled(true);
            }
        }
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setNeutralButton("Retry", (dialog, which) -> {
                    clearOtpInputs();
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) {
            resendTimer.cancel();
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Verification?")
                .setMessage("Are you sure you want to cancel the verification process?")
                .setPositiveButton("Cancel Verification", (dialog, which) -> {
                    super.onBackPressed();
                })
                .setNegativeButton("Continue", null)
                .show();
    }
}
