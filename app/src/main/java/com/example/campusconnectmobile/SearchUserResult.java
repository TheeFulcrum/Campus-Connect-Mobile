package com.example.campusconnectmobile;

import org.json.JSONObject;

/** A user/listing search result from GET /api/search/index.php */
public class SearchUserResult {
    public String username;
    public String displayName;
    public String realName;
    public String bio;
    public String campus;
    public String avatarUrl;

    public static SearchUserResult fromJson(JSONObject json) {
        SearchUserResult result = new SearchUserResult();
        result.username = json.optString("username", "");
        result.displayName = json.optString("display_name", "");
        result.realName = json.optString("real_name", "");
        result.bio = json.optString("bio", "");
        result.campus = json.optString("campus", "");
        result.avatarUrl = json.optString("avatar_url", "");
        return result;
    }
}
