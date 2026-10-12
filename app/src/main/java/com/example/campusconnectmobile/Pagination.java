package com.example.campusconnectmobile;

import org.json.JSONObject;

/** Mirrors the `pagination` object present on every paginated response. */
public class Pagination {
    public int page = 1;
    public int perPage = 20;
    public int total = 0;

    public static Pagination fromJson(JSONObject json) {
        Pagination pagination = new Pagination();
        JSONObject node = json == null ? null : json.optJSONObject("pagination");
        if (node != null) {
            pagination.page = node.optInt("page", 1);
            pagination.perPage = node.optInt("per_page", 20);
            pagination.total = node.optInt("total", 0);
        }
        return pagination;
    }

    public boolean hasMore(int loadedSoFar) {
        return loadedSoFar < total;
    }
}
