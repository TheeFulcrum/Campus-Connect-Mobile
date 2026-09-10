package com.example.campusconnectmobile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SignUpActivity extends AppCompatActivity {

    private EditText etSignupUsername, etSignupEmail, etSignupPassword, etConfirmPassword;
    private Spinner spSignupCampus;
    private TextView tvSignupError, tvLoginLink;
    private Button btnSignup;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        dbHelper = new DatabaseHelper(this);

        etSignupUsername = findViewById(R.id.etSignupUsername);
        etSignupEmail = findViewById(R.id.etSignupEmail);
        spSignupCampus = findViewById(R.id.spSignupCampus);
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
        String username = etSignupUsername.getText().toString().trim();
        String email = etSignupEmail.getText().toString().trim();
        String campusLocation = spSignupCampus.getSelectedItem().toString();
        String password = etSignupPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            showError("Please choose a username.");
            return;
        }
        if (username.length() < 3) {
            showError("Username must be at least 3 characters.");
            return;
        }
        if (dbHelper.usernameExists(username)) {
            showError("That username is already taken.");
            return;
        }
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
        if (dbHelper.userExists(email)) {
            showError("An account with this email already exists.");
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

        hideError();
        boolean success = dbHelper.registerUser(email, password, username, campusLocation);
        if (success) {
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