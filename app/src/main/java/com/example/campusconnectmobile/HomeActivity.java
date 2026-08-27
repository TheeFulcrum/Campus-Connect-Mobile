package com.example.campusconnectmobile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "extra_email";

    private TextView tvWelcomeEmail, tvCampus, tvPreferences;
    private Button btnLogout;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        dbHelper = new DatabaseHelper(this);

        tvWelcomeEmail = findViewById(R.id.tvWelcomeEmail);
        tvCampus = findViewById(R.id.tvCampus);
        tvPreferences = findViewById(R.id.tvPreferences);
        btnLogout = findViewById(R.id.btnLogout);

        String email = getIntent().getStringExtra(EXTRA_EMAIL);
        if (email != null) {
            DatabaseHelper.UserProfile profile = dbHelper.getProfile(email);
            if (profile != null && profile.username != null) {
                tvWelcomeEmail.setText("Welcome, " + profile.username + "!");
                tvCampus.setText("Campus: " + profile.campusLocation);
                tvPreferences.setText("Your feed: " + profile.preferences.replace(",", ", "));
            } else {
                // Edge case: logged-in user with no profile row yet (e.g. pre-existing account)
                tvWelcomeEmail.setText("Welcome, " + email);
                tvCampus.setText("");
                tvPreferences.setText("Complete your profile to personalize your feed.");
            }
        }

        btnLogout.setOnClickListener(v -> logOut());
    }

    private void logOut() {
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}