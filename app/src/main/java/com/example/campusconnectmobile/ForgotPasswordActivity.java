package com.example.campusconnectmobile;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ForgotPasswordActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "extra_email";

    private static final int MIN_PASSWORD_LENGTH = 8;

    private EditText etEmail, etCode, etNewPassword, etConfirmPassword;
    private TextView tvError, tvInfo;
    private Button btnSendLink, btnReset;
    private View resetSection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail = findViewById(R.id.etForgotEmail);
        etCode = findViewById(R.id.etResetCode);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmNewPassword);
        tvError = findViewById(R.id.tvForgotError);
        tvInfo = findViewById(R.id.tvForgotInfo);
        btnSendLink = findViewById(R.id.btnSendResetLink);
        btnReset = findViewById(R.id.btnResetPassword);
        resetSection = findViewById(R.id.resetSection);

        String prefilledEmail = getIntent().getStringExtra(EXTRA_EMAIL);
        if (prefilledEmail != null && prefilledEmail.contains("@")) {
            etEmail.setText(prefilledEmail);
        }

        btnSendLink.setOnClickListener(v -> requestReset());
        btnReset.setOnClickListener(v -> submitReset());
        findViewById(R.id.tvBackToLogin).setOnClickListener(v -> finish());
    }

    private void requestReset() {
        String email = etEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address.");
            return;
        }

        hideError();
        btnSendLink.setEnabled(false);
        PasswordRecoveryApiClient.requestReset(email, new PasswordRecoveryApiClient.RequestCallback() {
            @Override
            public void onSuccess(String message, String devResetCode) {
                if (isFinishing() || isDestroyed()) return;
                btnSendLink.setEnabled(true);
                btnSendLink.setText("Resend reset link");
                tvInfo.setText(message + " Enter the 6-digit code below to choose a new password.");
                tvInfo.setVisibility(View.VISIBLE);
                resetSection.setVisibility(View.VISIBLE);
                if (!TextUtils.isEmpty(devResetCode)) {
                    etCode.setText(devResetCode);
                }
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                btnSendLink.setEnabled(true);
                showError(message);
            }
        });
    }

    private void submitReset() {
        String email = etEmail.getText().toString().trim();
        String code = etCode.getText().toString().trim();
        String password = etNewPassword.getText().toString();
        String confirm = etConfirmPassword.getText().toString();

        if (!code.matches("\\d{6}")) {
            showError("Please enter the 6-digit code from your email.");
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            showError("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }

        hideError();
        btnReset.setEnabled(false);
        PasswordRecoveryApiClient.resetPassword(email, code, password, new PasswordRecoveryApiClient.ResetCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(getApplicationContext(),
                        "Password reset. Log in with your new password.", Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                if (!isFinishing()) finish();
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                btnReset.setEnabled(true);
                showError(message);
            }
        });
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvError.setVisibility(View.GONE);
    }
}
