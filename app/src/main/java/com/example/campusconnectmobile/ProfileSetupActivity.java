package com.example.campusconnectmobile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class ProfileSetupActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "extra_email";

    private EditText etUsername;
    private Spinner spCampusLocation;
    private CheckBox cbServices, cbGoods, cbAcademic, cbEvents, cbHousing, cbGigs;
    private TextView tvProfileError;
    private Button btnContinue;
    private DatabaseHelper dbHelper;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        dbHelper = new DatabaseHelper(this);
        email = getIntent().getStringExtra(EXTRA_EMAIL);

        etUsername = findViewById(R.id.etUsername);
        spCampusLocation = findViewById(R.id.spCampusLocation);
        cbServices = findViewById(R.id.cbServices);
        cbGoods = findViewById(R.id.cbGoods);
        cbAcademic = findViewById(R.id.cbAcademic);
        cbEvents = findViewById(R.id.cbEvents);
        cbHousing = findViewById(R.id.cbHousing);
        cbGigs = findViewById(R.id.cbGigs);
        tvProfileError = findViewById(R.id.tvProfileError);
        btnContinue = findViewById(R.id.btnContinue);

        btnContinue.setOnClickListener(v -> attemptSaveProfile());
    }

    private void attemptSaveProfile() {
        String username = etUsername.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            showError("Please choose a username.");
            return;
        }
        if (username.length() < 3) {
            showError("Username must be at least 3 characters.");
            return;
        }

        String campusLocation = spCampusLocation.getSelectedItem().toString();
        String preferences = collectSelectedPreferences();

        if (preferences.isEmpty()) {
            showError("Please select at least one feed preference.");
            return;
        }

        hideError();
        boolean saved = dbHelper.saveProfile(email, username, campusLocation, preferences);
        if (saved) {
            Intent intent = new Intent(ProfileSetupActivity.this, HomeActivity.class);
            intent.putExtra(HomeActivity.EXTRA_EMAIL, email);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            showError("Could not save your profile. Please try again.");
        }
    }

    private String collectSelectedPreferences() {
        List<String> selected = new ArrayList<>();
        if (cbServices.isChecked()) selected.add("Services");
        if (cbGoods.isChecked()) selected.add("Goods & Textbooks");
        if (cbAcademic.isChecked()) selected.add("Academic Help");
        if (cbEvents.isChecked()) selected.add("Events & Clubs");
        if (cbHousing.isChecked()) selected.add("Housing & Roommates");
        if (cbGigs.isChecked()) selected.add("Gigs & Jobs");
        return TextUtils.join(",", selected);
    }

    private void showError(String message) {
        tvProfileError.setText(message);
        tvProfileError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvProfileError.setVisibility(View.GONE);
    }
}