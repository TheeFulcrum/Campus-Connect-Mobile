package com.example.campusconnectmobile;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); // shows your logo + ProgressBar

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