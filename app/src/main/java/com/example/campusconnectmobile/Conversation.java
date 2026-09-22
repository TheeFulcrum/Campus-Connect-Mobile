package com.example.campusconnectmobile;

public class Conversation {
    public String participantEmail;
    public String participantUsername;
    public String lastMessage;
    public long lastTimestamp;

    public Conversation(String participantEmail, String participantUsername, String lastMessage, long lastTimestamp) {
        this.participantEmail = participantEmail;
        this.participantUsername = participantUsername;
        this.lastMessage = lastMessage;
        this.lastTimestamp = lastTimestamp;
    }
}