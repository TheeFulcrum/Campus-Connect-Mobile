package com.example.campusconnectmobile;

import org.json.JSONObject;

/** A conversation summary from GET /api/messages/conversations.php */
public class ServerConversation {
    public int id;
    public int listingId;
    public String listingTitle;
    public String otherUsername;
    public String otherAvatarUrl;
    public String lastMessage;
    public String lastMessageAt;
    public int unread;

    public static ServerConversation fromJson(JSONObject json) {
        ServerConversation conversation = new ServerConversation();
        conversation.id = json.optInt("id", json.optInt("conversation_id"));
        conversation.listingId = json.optInt("listing_id");
        conversation.listingTitle = json.optString("listing_title", "");
        conversation.otherUsername = json.optString("other_username", "");
        conversation.otherAvatarUrl = json.optString("other_avatar_url", "");
        conversation.lastMessage = json.optString("last_message", "");
        conversation.lastMessageAt = json.optString("last_message_at", json.optString("updated_at", ""));
        conversation.unread = json.optInt("unread", 0);
        return conversation;
    }
}
