package com.example.campusconnectmobile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SignUpActivity extends AppCompatActivity {

    private EditText etSignupEmail, etSignupPassword, etConfirmPassword;
    private TextView tvSignupError, tvLoginLink;
    private Button btnSignup;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        dbHelper = new DatabaseHelper(this);

        etSignupEmail = findViewById(R.id.etSignupEmail);
        etSignupPassword = findViewById(R.id.etSignupPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        tvSignupError = findViewById(R.id.tvSignupError);
        btnSignup = findViewById(R.id.btnSignup);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        btnSignup.setOnClickListener(v -> attemptSignUp());
        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
            finish();
        });
    }

    private void attemptSignUp() {
        String email = etSignupEmail.getText().toString().trim();
        String password = etSignupPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            showError("Please enter your university email.");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address.");
            return;
        }
        if (!email.toLowerCase().endsWith("@edenuniversity.education")) {
            showError("Please use your university (@edenuniversity.education) email.");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            showError("Please enter a password.");
            return;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }
        if (!password.equals(confirmPass)) {
            showError("Passwords do not match.");
            return;
        }

        if (dbHelper.userExists(email)) {
            showError("An account with this email already exists.");
            return;
        }
        hideError();
        boolean success = dbHelper.registerUser(email, password);
        if (success) {
            Toast.makeText(this, "Account created for " + email, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, ProfileSetupActivity.class);
            intent.putExtra(ProfileSetupActivity.EXTRA_EMAIL, email);
            startActivity(intent);
            finish();
        } else {
            showError("Something went wrong. Please try again.");
        }
    }

    private void showError(String message) {
        tvSignupError.setText(message);
        tvSignupError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvSignupError.setVisibility(View.GONE);
    }
}