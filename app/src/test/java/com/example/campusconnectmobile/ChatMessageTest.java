package com.example.campusconnectmobile;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ChatMessageTest {

    @Test
    public void testChatMessageFields() {
        long now = System.currentTimeMillis();
        ChatMessage message = new ChatMessage("sender@edenuniversity.education", "receiver@edenuniversity.education", "Hello world!", now);

        assertEquals("sender@edenuniversity.education", message.sender);
        assertEquals("receiver@edenuniversity.education", message.receiver);
        assertEquals("Hello world!", message.text);
        assertEquals(now, message.timestamp);
    }

    @Test
    public void testConversationFields() {
        long now = System.currentTimeMillis();
        Conversation conversation = new Conversation("alex@edenuniversity.education", "Alex M.", "Latest message text", now);

        assertEquals("alex@edenuniversity.education", conversation.participantEmail);
        assertEquals("Alex M.", conversation.participantUsername);
        assertEquals("Latest message text", conversation.lastMessage);
        assertEquals(now, conversation.timestamp);
    }
}
