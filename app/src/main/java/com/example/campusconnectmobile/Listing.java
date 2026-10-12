package com.example.campusconnectmobile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Mirrors a listing object returned by the various /api/listings/*.php and /api/profile/*.php endpoints. */
public class Listing implements Serializable {
    public int id;
    public String title;
    public String description;
    public double price;
    public String category;
    public String campus;
    public String status; // active | sold | archived
    public String createdAt;
    public String soldAt;
    public String sellerUsername;
    public String coverImage;
    public boolean isSaved;
    public int saves;
    public final List<String> imageUrls = new ArrayList<>();

    public static Listing fromJson(JSONObject json) {
        Listing listing = new Listing();
        listing.id = json.optInt("id");
        listing.title = json.optString("title", "");
        listing.description = json.optString("description", "");
        listing.price = json.optDouble("price", 0);
        listing.category = json.optString("category", "");
        listing.campus = json.optString("campus", "");
        listing.status = json.optString("status", "active");
        listing.createdAt = json.optString("created_at", "");
        listing.soldAt = json.optString("sold_at", "");
        listing.sellerUsername = json.optString("seller_username", json.optString("username", ""));
        listing.coverImage = json.optString("cover_image", "");
        listing.isSaved = json.optBoolean("is_saved", false);
        listing.saves = json.optInt("saves", 0);
        JSONArray images = json.optJSONArray("images");
        if (images != null) {
            for (int i = 0; i < images.length(); i++) {
                JSONObject image = images.optJSONObject(i);
                if (image != null) {
                    listing.imageUrls.add(image.optString("image_url", ""));
                } else {
                    listing.imageUrls.add(images.optString(i, ""));
                }
            }
        }
        return listing;
    }

    public static List<Listing> listFromJson(JSONArray array) {
        List<Listing> listings = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item != null) listings.add(fromJson(item));
        }
        return listings;
    }

    public boolean isSold() {
        return "sold".equalsIgnoreCase(status);
    }

    public boolean isArchived() {
        return "archived".equalsIgnoreCase(status) || !soldAt.isEmpty() && "archived".equalsIgnoreCase(status);
    }
}
