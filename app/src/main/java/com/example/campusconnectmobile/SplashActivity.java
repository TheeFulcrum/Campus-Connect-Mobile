package com.example.campusconnectmobile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

public class SplashActivity extends AppCompatActivity {

    public static final String EXTRA_WELCOME_EMAIL = "extra_welcome_email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); // shows your logo + ProgressBar

        String welcomeEmail = getIntent().getStringExtra(EXTRA_WELCOME_EMAIL);
        if (welcomeEmail != null) {
            DatabaseHelper.UserProfile profile = new DatabaseHelper(this).getProfile(welcomeEmail);
            TextView welcome = findViewById(R.id.tvSplashWelcome);
            welcome.setText("Welcome, " + (profile != null ? profile.username : "Student"));
            welcome.setVisibility(android.view.View.VISIBLE);
        }

        final boolean[] isReady = {false};
        splashScreen.setKeepOnScreenCondition(() -> !isReady[0]);

        new android.os.Handler().postDelayed(() -> {
            isReady[0] = true;
            SessionManager sessionManager = new SessionManager(SplashActivity.this);
            if (sessionManager.isLoggedIn()) {
                Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
                intent.putExtra(HomeActivity.EXTRA_EMAIL, sessionManager.getLoggedInEmail());
                startActivity(intent);
            } else {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            }
            finish();
        }, 1200);
    }
}