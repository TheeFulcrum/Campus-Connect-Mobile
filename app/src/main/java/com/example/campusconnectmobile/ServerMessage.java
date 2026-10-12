package com.example.campusconnectmobile;

import org.json.JSONObject;

/** A single message from GET /api/messages/thread.php */
public class ServerMessage {
    public int id;
    public int conversationId;
    public int senderId;
    public String senderUsername;
    public String body;
    public String createdAt;
    public boolean isMine;

    public static ServerMessage fromJson(JSONObject json, String myUsername) {
        ServerMessage message = new ServerMessage();
        message.id = json.optInt("id");
        message.conversationId = json.optInt("conversation_id");
        message.senderId = json.optInt("sender_id");
        message.senderUsername = json.optString("sender_username", "");
        message.body = json.optString("body", "");
        message.createdAt = json.optString("created_at", "");
        message.isMine = myUsername != null && myUsername.equalsIgnoreCase(message.senderUsername);
        return message;
    }
}
