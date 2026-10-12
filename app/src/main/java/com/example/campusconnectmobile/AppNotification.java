package com.example.campusconnectmobile;

import org.json.JSONObject;

/** An entry from GET /api/notifications/list.php */
public class AppNotification {
    public int id;
    public String type; // follow | message | review
    public String actorUsername;
    public String actorAvatarUrl;
    public String body;
    public String createdAt;
    public boolean isRead;

    public static AppNotification fromJson(JSONObject json) {
        AppNotification notification = new AppNotification();
        notification.id = json.optInt("id");
        notification.type = json.optString("type", "");
        notification.actorUsername = json.optString("actor_username", "");
        notification.actorAvatarUrl = json.optString("actor_avatar_url", "");
        notification.body = json.optString("body", json.optString("message", ""));
        notification.createdAt = json.optString("created_at", "");
        notification.isRead = json.optBoolean("is_read", json.optBoolean("read", false));
        return notification;
    }
}
