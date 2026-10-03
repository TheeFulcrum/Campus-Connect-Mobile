package com.example.campusconnectmobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListingsActivity extends Activity {

    private ListView listingsListView;
    private Spinner campusSpinner, categorySpinner;
    private Button searchButton, createButton;
    private String token;
    private List<JSONObject> listings = new ArrayList<>();
    private ListingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listings);

        token = getSharedPreferences("ccw", MODE_PRIVATE).getString("token", "");
        if (token.isEmpty()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        listingsListView = findViewById(R.id.listingsListView);
        campusSpinner = findViewById(R.id.campusSpinner);
        categorySpinner = findViewById(R.id.categorySpinner);
        searchButton = findViewById(R.id.searchButton);
        createButton = findViewById(R.id.createButton);

        // Setup spinners
        ArrayAdapter<String> campusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
            new String[]{"All Campuses", "Main Campus", "Great East Campus"});
        campusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        campusSpinner.setAdapter(campusAdapter);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
            new String[]{"All Categories", "Textbooks", "Electronics", "Furniture", "Clothing", "Services", "Housing", "Rides", "Other"});
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        adapter = new ListingAdapter(this, listings);
        listingsListView.setAdapter(adapter);

        searchButton.setOnClickListener(v -> loadListings());
        createButton.setOnClickListener(v -> startActivity(new Intent(this, CreateListingActivity.class)));

        loadListings();
    }

    private void loadListings() {
        String campus = campusSpinner.getSelectedItem().toString();
        String category = categorySpinner.getSelectedItem().toString();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                StringBuilder url = new StringBuilder("https://auth.campusconnect.ink/api/listings/list.php?status=active");
                if (!campus.equals("All Campuses")) {
                    url.append("&campus=").append(campus.replace(" ", "%20"));
                }
                if (!category.equals("All Categories")) {
                    url.append("&category=").append(category.replace(" ", "%20"));
                }

                HttpURLConnection connection = (HttpURLConnection) new URL(url.toString()).openConnection();
                connection.setRequestMethod("GET");
                connection.setReadTimeout(10000);

                int status = connection.getResponseCode();
                String response = readResponse(status < 400 ? connection.getInputStream() : connection.getErrorStream());
                JSONObject json = new JSONObject(response);

                if (json.optBoolean("success")) {
                    JSONArray listingsArray = json.optJSONArray("listings");
                    listings.clear();
                    if (listingsArray != null) {
                        for (int i = 0; i < listingsArray.length(); i++) {
                            listings.add(listingsArray.getJSONObject(i));
                        }
                    }
                }

                runOnUiThread(() -> adapter.notifyDataSetChanged());
            } catch (Exception e) {
                e.printStackTrace();
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

    class ListingAdapter extends android.widget.BaseAdapter {
        private Activity context;
        private List<JSONObject> items;

        ListingAdapter(Activity context, List<JSONObject> items) {
            this.context = context;
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.listing_item, null);
            }

            JSONObject listing = items.get(position);
            TextView titleView = convertView.findViewById(R.id.listingTitle);
            TextView priceView = convertView.findViewById(R.id.listingPrice);
            TextView categoryView = convertView.findViewById(R.id.listingCategory);
            TextView usernameView = convertView.findViewById(R.id.listingUsername);

            titleView.setText(listing.optString("title", ""));
            priceView.setText(listing.optDouble("price", 0) > 0 ? "$" + listing.optDouble("price") : "Negotiable");
            categoryView.setText(listing.optString("category", ""));
            usernameView.setText("by " + listing.optString("username", ""));

            final int listingId = listing.optInt("id");
            convertView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ListingDetailActivity.class);
                intent.putExtra("listing_id", listingId);
                context.startActivity(intent);
            });

            return convertView;
        }
    }
}
