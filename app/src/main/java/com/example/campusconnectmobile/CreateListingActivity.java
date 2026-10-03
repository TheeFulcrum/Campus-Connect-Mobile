package com.example.campusconnectmobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.text.TextWatcher;
import android.text.Editable;
import org.json.JSONObject;

public class CreateListingActivity extends Activity {

    private EditText titleInput, descriptionInput, priceInput;
    private Spinner categorySpinner, campusSpinner;
    private Button postButton;
    private TextView titleHint, formMessage;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_listing);

        token = getSharedPreferences("ccw", MODE_PRIVATE).getString("token", "");
        if (token.isEmpty()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        titleInput = findViewById(R.id.titleInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        priceInput = findViewById(R.id.priceInput);
        categorySpinner = findViewById(R.id.categorySpinner);
        campusSpinner = findViewById(R.id.campusSpinner);
        postButton = findViewById(R.id.postButton);
        titleHint = findViewById(R.id.titleHint);
        formMessage = findViewById(R.id.formMessage);

        // Setup spinners
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
            new String[]{"Textbooks", "Electronics", "Furniture", "Clothing", "Services", "Housing", "Rides", "Other"});
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        ArrayAdapter<String> campusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
            new String[]{"Main Campus", "Great East Campus"});
        campusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        campusSpinner.setAdapter(campusAdapter);

        // Update title hint
        titleInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                titleHint.setText(s.length() + " / 160 characters");
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        postButton.setOnClickListener(v -> submitListing());
    }

    private void submitListing() {
        String title = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String category = categorySpinner.getSelectedItem().toString();
        String campus = campusSpinner.getSelectedItem().toString();
        String priceStr = priceInput.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty()) {
            showMessage("Title and description are required.", "error");
            return;
        }

        postButton.setEnabled(false);
        postButton.setText("Posting...");

        JSONObject body = new JSONObject();
        try {
            body.put("action", "create_listing");
            body.put("title", title);
            body.put("description", description);
            body.put("category", category);
            body.put("campus", campus);
            if (!priceStr.isEmpty()) {
                body.put("price", Double.parseDouble(priceStr));
            }
        } catch (Exception e) {
            showMessage("Error: " + e.getMessage(), "error");
            postButton.setEnabled(true);
            postButton.setText("Post Listing");
            return;
        }

        AuthApiClient.send(body, token, new AuthApiClient.Callback() {
            @Override
            public void onSuccess(AuthApiClient.AuthResult result) {
                showMessage("Listing posted successfully!", "success");
                new Thread(() -> {
                    try {
                        Thread.sleep(1500);
                        startActivity(new Intent(CreateListingActivity.this, ListingsActivity.class));
                        finish();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }

            @Override
            public void onError(String message) {
                showMessage("Error: " + message, "error");
                postButton.setEnabled(true);
                postButton.setText("Post Listing");
            }
        });
    }

    private void showMessage(String msg, String type) {
        formMessage.setText(msg);
        formMessage.setTextColor(type.equals("error") ? 0xFFCC3333 : 0xFF33CC33);
    }
}
