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

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_PREFILLED_IDENTIFIER = "extra_prefilled_identifier";
    public static final String EXTRA_CONTINUE_TO_PREFERENCES = "extra_continue_to_preferences";

    private EditText etEmail, etPassword;
    private TextView tvError, tvSignUp;
    private Button btnLogin;
    private DatabaseHelper dbHelper;
    private boolean continueToPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // no splash install here anymore — SplashActivity owns that
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tvError = findViewById(R.id.tvError);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUp = findViewById(R.id.tvSignUp);
        continueToPreferences = getIntent().getBooleanExtra(EXTRA_CONTINUE_TO_PREFERENCES, false);
        String prefilledIdentifier = getIntent().getStringExtra(EXTRA_PREFILLED_IDENTIFIER);
        if (prefilledIdentifier != null) {
            etEmail.setText(prefilledIdentifier);
            Toast.makeText(this, "Email verified. Log in to continue.", Toast.LENGTH_SHORT).show();
        }

        btnLogin.setOnClickListener(v -> attemptLogin());
        tvSignUp.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SignUpActivity.class)));
    }

    private void attemptLogin() {
        String identifier = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(identifier)) {
            showError("Please enter your email or username.");
            return;
        }

        // Basic validation if it looks like an email
        if (identifier.contains("@")) {
            if (!identifier.matches("^\\d{10}@edenuniversity\\.education$")) {
                showError("Please enter a valid email address.");
                return;
            }
        }

        if (TextUtils.isEmpty(password)) {
            showError("Please enter your password.");
            return;
        }

        hideError();
        btnLogin.setEnabled(false);
        AuthApiClient.login(identifier, password, new AuthApiClient.Callback() {
            @Override
            public void onSuccess(AuthApiClient.AuthResult result) {
                dbHelper.cacheRemoteUser(result.email, result.username, result.campus);
                Intent intent = continueToPreferences
                        ? new Intent(MainActivity.this, ProfileSetupActivity.class)
                        : new Intent(MainActivity.this, HomeActivity.class);
                intent.putExtra(continueToPreferences ? ProfileSetupActivity.EXTRA_EMAIL : HomeActivity.EXTRA_EMAIL, result.email);
                if (continueToPreferences) {
                    intent.putExtra(ProfileSetupActivity.EXTRA_AUTH_TOKEN, result.token);
                } else {
                    new SessionManager(MainActivity.this).createSession(result.email, result.token);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                btnLogin.setEnabled(true);
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