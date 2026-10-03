package com.example.campusconnectmobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListingDetailActivity extends Activity {

    private ImageView listingImage;
    private TextView titleView, priceView, categoryView, campusView, descriptionView;
    private TextView sellerNameView, sellerUsernameView;
    private Button messageButton;
    private int listingId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listing_detail);

        listingId = getIntent().getIntExtra("listing_id", 0);
        if (listingId == 0) {
            finish();
            return;
        }

        listingImage = findViewById(R.id.listingImage);
        titleView = findViewById(R.id.listingTitle);
        priceView = findViewById(R.id.listingPrice);
        categoryView = findViewById(R.id.listingCategory);
        campusView = findViewById(R.id.listingCampus);
        descriptionView = findViewById(R.id.listingDescription);
        sellerNameView = findViewById(R.id.sellerName);
        sellerUsernameView = findViewById(R.id.sellerUsername);
        messageButton = findViewById(R.id.messageButton);

        messageButton.setOnClickListener(v -> {
            // TODO: Implement messaging
            showMessage("Messaging feature coming soon!");
        });

        loadListing();
    }

    private void loadListing() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                String url = "https://auth.campusconnect.ink/api/listings/view.php?id=" + listingId;
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                connection.setReadTimeout(10000);

                int status = connection.getResponseCode();
                String response = readResponse(status < 400 ? connection.getInputStream() : connection.getErrorStream());
                JSONObject json = new JSONObject(response);

                if (json.optBoolean("success")) {
                    JSONObject listing = json.optJSONObject("listing");
                    JSONObject userProfile = listing.optJSONObject("user_profile");

                    runOnUiThread(() -> {
                        titleView.setText(listing.optString("title", ""));
                        double price = listing.optDouble("price", 0);
                        priceView.setText(price > 0 ? "$" + price : "Negotiable");
                        categoryView.setText(listing.optString("category", ""));
                        campusView.setText(listing.optString("campus", ""));
                        descriptionView.setText(listing.optString("description", ""));
                        
                        if (userProfile != null) {
                            sellerNameView.setText(userProfile.optString("real_name", userProfile.optString("username", "")));
                            sellerUsernameView.setText("@" + userProfile.optString("username", ""));
                        }
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> showMessage("Error loading listing: " + e.getMessage()));
            }
        });
    }

    private String readResponse(InputStream stream) throws Exception {
        if (stream == null) return "{}";
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
        }
        return response.toString();
    }

    private void showMessage(String msg) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
    }
}
