package com.example.campusconnectmobile;

public class ChatMessage {
    public String senderEmail;
    public String receiverEmail;
    public String messageText;
    public long timestamp;

    public ChatMessage(String senderEmail, String receiverEmail, String messageText, long timestamp) {
        this.senderEmail = senderEmail;
        this.receiverEmail = receiverEmail;
        this.messageText = messageText;
        this.timestamp = timestamp;
    }
}