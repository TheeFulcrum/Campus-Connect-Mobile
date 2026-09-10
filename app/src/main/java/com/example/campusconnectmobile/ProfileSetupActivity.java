package com.example.campusconnectmobile;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProfileSetupActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "extra_email";
    public static final String EXTRA_EDIT_MODE = "extra_edit_mode"; // true when reached from Profile tab

    private CheckBox cbServices, cbGoods, cbAcademic, cbEvents, cbHousing, cbGigs;
    private TextView tvProfileError;
    private Button btnContinue;
    private DatabaseHelper dbHelper;
    private String email;
    private boolean editMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        dbHelper = new DatabaseHelper(this);
        email = getIntent().getStringExtra(EXTRA_EMAIL);
        editMode = getIntent().getBooleanExtra(EXTRA_EDIT_MODE, false);

        cbServices = findViewById(R.id.cbServices);
        cbGoods = findViewById(R.id.cbGoods);
        cbAcademic = findViewById(R.id.cbAcademic);
        cbEvents = findViewById(R.id.cbEvents);
        cbHousing = findViewById(R.id.cbHousing);
        cbGigs = findViewById(R.id.cbGigs);
        tvProfileError = findViewById(R.id.tvProfileError);
        btnContinue = findViewById(R.id.btnContinue);

        if (editMode) {
            btnContinue.setText("Save Changes");
            prefillExistingPreferences();
        }

        btnContinue.setOnClickListener(v -> attemptSavePreferences());
    }

    private void prefillExistingPreferences() {
        if (email == null) return;
        DatabaseHelper.UserProfile profile = dbHelper.getProfile(email);
        if (profile == null || profile.preferences == null || profile.preferences.isEmpty()) return;

        List<String> selected = Arrays.asList(profile.preferences.split(","));
        cbServices.setChecked(selected.contains("Services"));
        cbGoods.setChecked(selected.contains("Goods & Textbooks"));
        cbAcademic.setChecked(selected.contains("Academic Help"));
        cbEvents.setChecked(selected.contains("Events & Clubs"));
        cbHousing.setChecked(selected.contains("Housing & Roommates"));
        cbGigs.setChecked(selected.contains("Gigs & Jobs"));
    }

    private void attemptSavePreferences() {
        String preferences = collectSelectedPreferences();

        if (preferences.isEmpty()) {
            showError("Please select at least one feed preference.");
            return;
        }

        hideError();
        boolean saved = dbHelper.updatePreferences(email, preferences);
        if (!saved) {
            showError("Could not save your preferences. Please try again.");
            return;
        }

        if (editMode) {
            // Editing from Profile tab: just go back to where the user came from.
            Toast.makeText(this, "Preferences updated.", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            // First-time setup right after sign-up: this is the first time
            // reaching Home, so start it fresh and clear the sign-up flow off the back stack.
            new SessionManager(this).createSession(email);
            android.content.Intent intent = new android.content.Intent(this, HomeActivity.class);
            intent.putExtra(HomeActivity.EXTRA_EMAIL, email);
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
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
        return android.text.TextUtils.join(",", selected);
    }

    private void showError(String message) {
        tvProfileError.setText(message);
        tvProfileError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvProfileError.setVisibility(View.GONE);
    }
}