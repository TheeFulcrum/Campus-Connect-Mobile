package com.example.campusconnectmobile;

import org.json.JSONObject;

/** Mirrors the `profile` object returned by GET /api/profile/get.php */
public class Profile {
    public String username;
    public String displayName;
    public String realName;
    public String bio;
    public String link;
    public String campus;
    public String avatarUrl;
    public String createdAt;
    public int listingsCount;
    public int followersCount;
    public int followingCount;
    public int ratingCount;
    public double ratingAverage;
    public String noteBody;
    public long noteExpiresAt;
    public boolean isMe;
    public boolean isFollowing;

    public static Profile fromJson(JSONObject json) {
        Profile profile = new Profile();
        profile.username = json.optString("username", "");
        profile.displayName = json.optString("display_name", "");
        profile.realName = json.optString("real_name", "");
        profile.bio = json.optString("bio", "");
        profile.link = json.optString("link", "");
        profile.campus = json.optString("campus", "");
        profile.avatarUrl = json.optString("avatar_url", "");
        profile.createdAt = json.optString("created_at", "");
        JSONObject counts = json.optJSONObject("counts");
        if (counts != null) {
            profile.listingsCount = counts.optInt("listings", 0);
            profile.followersCount = counts.optInt("followers", 0);
            profile.followingCount = counts.optInt("following", 0);
        }
        JSONObject rating = json.optJSONObject("rating");
        if (rating != null) {
            profile.ratingCount = rating.optInt("count", 0);
            profile.ratingAverage = rating.optDouble("average", 0);
        }
        JSONObject note = json.optJSONObject("note");
        if (note != null) {
            profile.noteBody = note.optString("body", "");
            profile.noteExpiresAt = note.optLong("expires_at", 0);
        }
        profile.isMe = json.optBoolean("is_me", false);
        profile.isFollowing = json.optBoolean("is_following", false);
        return profile;
    }

    public boolean hasActiveNote() {
        return noteBody != null && !noteBody.isEmpty()
                && noteExpiresAt * 1000L > System.currentTimeMillis();
    }
}
