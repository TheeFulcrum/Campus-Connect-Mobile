package com.example.campusconnectmobile;

import org.json.JSONObject;

/** An entry from GET /api/follow/followers.php or following.php */
public class FollowUser {
    public String username;
    public String displayName;
    public String realName;
    public String bio;
    public String campus;
    public String avatarUrl;
    public String followedAt;
    public boolean isFollowedByMe;

    public static FollowUser fromJson(JSONObject json) {
        FollowUser user = new FollowUser();
        user.username = json.optString("username", "");
        user.displayName = json.optString("display_name", "");
        user.realName = json.optString("real_name", "");
        user.bio = json.optString("bio", "");
        user.campus = json.optString("campus", "");
        user.avatarUrl = json.optString("avatar_url", "");
        user.followedAt = json.optString("followed_at", "");
        user.isFollowedByMe = json.optBoolean("is_following", false);
        return user;
    }
}
