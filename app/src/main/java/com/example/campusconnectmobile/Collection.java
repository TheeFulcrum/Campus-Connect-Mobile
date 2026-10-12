package com.example.campusconnectmobile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** A user's saved collection of listings: GET /api/collections/list.php */
public class Collection {
    public int id;
    public String name;
    public String createdAt;
    public int itemCount;
    public final List<Integer> listingIds = new ArrayList<>();

    public static Collection fromJson(JSONObject json) {
        Collection collection = new Collection();
        collection.id = json.optInt("id");
        collection.name = json.optString("name", "");
        collection.createdAt = json.optString("created_at", "");
        collection.itemCount = json.optInt("item_count", 0);
        JSONArray items = json.optJSONArray("items");
        if (items != null) {
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item != null) {
                    collection.listingIds.add(item.optInt("listing_id", item.optInt("id", 0)));
                }
            }
        }
        return collection;
    }

    public static List<Collection> listFromJson(JSONArray array) {
        List<Collection> collections = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item != null) collections.add(fromJson(item));
        }
        return collections;
    }
}
