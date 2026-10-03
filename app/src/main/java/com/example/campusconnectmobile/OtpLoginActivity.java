package com.example.campusconnectmobile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class OtpLoginActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "extra_email";
    public static final String EXTRA_CODE_SENT = "extra_code_sent";
    public static final String EXTRA_CONTINUE_TO_LOGIN = "extra_continue_to_login";

    private EditText etEmail;
    private EditText etCode;
    private TextView tvError;
    private Button btnRequestCode;
    private Button btnVerifyCode;
    private String email;
    private boolean continueToLogin;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_login);

        dbHelper = new DatabaseHelper(this);
        etEmail = findViewById(R.id.etOtpEmail);
        etCode = findViewById(R.id.etOtpCode);
        tvError = findViewById(R.id.tvOtpError);
        btnRequestCode = findViewById(R.id.btnRequestOtp);
        btnVerifyCode = findViewById(R.id.btnVerifyOtp);

        email = getIntent().getStringExtra(EXTRA_EMAIL);
        continueToLogin = getIntent().getBooleanExtra(EXTRA_CONTINUE_TO_LOGIN, false);
        if (email != null) {
            etEmail.setText(email);
        }
        if (getIntent().getBooleanExtra(EXTRA_CODE_SENT, false)) {
            etCode.setVisibility(View.VISIBLE);
            btnVerifyCode.setVisibility(View.VISIBLE);
            btnRequestCode.setText("Resend code");
        }

        btnRequestCode.setOnClickListener(view -> requestCode());
        btnVerifyCode.setOnClickListener(view -> verifyCode());
    }

    private void requestCode() {
        String input = etEmail.getText().toString().trim().toLowerCase();
        if (!Patterns.EMAIL_ADDRESS.matcher(input).matches() || !input.endsWith("@edenuniversity.education")) {
            showError("Use your university email address.");
            return;
        }

        hideError();
        btnRequestCode.setEnabled(false);
        AuthApiClient.requestOtp(input, new AuthApiClient.Callback() {
            @Override
            public void onSuccess(AuthApiClient.AuthResult result) {
                email = input;
                etCode.setVisibility(View.VISIBLE);
                btnVerifyCode.setVisibility(View.VISIBLE);
                btnRequestCode.setText("Resend code");
                btnRequestCode.setEnabled(true);
                etCode.requestFocus();
            }

            @Override
            public void onError(String message) {
                btnRequestCode.setEnabled(true);
                showError(message);
            }
        });
    }

    private void verifyCode() {
        String code = etCode.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            showError("Request a sign-in code first.");
            return;
        }
        if (!code.matches("\\d{6}")) {
            showError("Enter the six-digit code from your email.");
            return;
        }

        hideError();
        btnVerifyCode.setEnabled(false);
        AuthApiClient.verifyOtp(email, code, new AuthApiClient.Callback() {
            @Override
            public void onSuccess(AuthApiClient.AuthResult result) {
                dbHelper.cacheRemoteUser(result.email, result.username, result.campus,
                    result.realName, result.bio, result.avatar);
                if (continueToLogin) {
                    Intent intent = new Intent(OtpLoginActivity.this, MainActivity.class);
                    intent.putExtra(MainActivity.EXTRA_PREFILLED_IDENTIFIER, result.email);
                    intent.putExtra(MainActivity.EXTRA_CONTINUE_TO_PREFERENCES, true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    return;
                }

                new SessionManager(OtpLoginActivity.this).createSession(result.email, result.token);
                Intent intent = new Intent(OtpLoginActivity.this, HomeActivity.class);
                intent.putExtra(HomeActivity.EXTRA_EMAIL, result.email);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                btnVerifyCode.setEnabled(true);
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