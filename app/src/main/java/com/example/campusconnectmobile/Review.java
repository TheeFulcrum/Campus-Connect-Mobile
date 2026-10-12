package com.example.campusconnectmobile;

import org.json.JSONObject;

/** A review left on a seller's profile: /api/reviews/*.php */
public class Review {
    public int id;
    public int listingId;
    public String reviewerUsername;
    public String reviewerAvatarUrl;
    public int rating;
    public String comment;
    public String createdAt;

    public static Review fromJson(JSONObject json) {
        Review review = new Review();
        review.id = json.optInt("id");
        review.listingId = json.optInt("listing_id");
        review.reviewerUsername = json.optString("reviewer_username", json.optString("username", ""));
        review.reviewerAvatarUrl = json.optString("reviewer_avatar_url", json.optString("avatar_url", ""));
        review.rating = json.optInt("rating", 0);
        review.comment = json.optString("comment", "");
        review.createdAt = json.optString("created_at", "");
        return review;
    }
}
