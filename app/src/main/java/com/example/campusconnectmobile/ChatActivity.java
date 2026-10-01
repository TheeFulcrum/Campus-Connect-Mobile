package com.example.campusconnectmobile;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_PARTICIPANT_EMAIL = "participant_email";
    public static final String EXTRA_PARTICIPANT_NAME = "participant_name";
    private static final long POLL_INTERVAL_MS = 2500;

    private String currentUserId;
    private String participantEmail;
    private String participantName;
    
    private DatabaseHelper dbHelper;
    private List<ChatMessage> messageList;
    private MessageAdapter adapter;
    
    private RecyclerView rvMessages;
    private EditText etMessage;
    private Button btnSend;
    private ImageButton btnBack;
    private TextView tvChatTitle;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            refreshMessages();
            pollHandler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        dbHelper = new DatabaseHelper(this);
        currentUserId = new SessionManager(this).getLoggedInEmail();
        participantEmail = getIntent().getStringExtra(EXTRA_PARTICIPANT_EMAIL);
        participantName = getIntent().getStringExtra(EXTRA_PARTICIPANT_NAME);

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etChatMessage);
        btnSend = findViewById(R.id.btnSendChat);
        btnBack = findViewById(R.id.btnBack);
        tvChatTitle = findViewById(R.id.tvChatTitle);

        tvChatTitle.setText(participantName);
        btnBack.setOnClickListener(v -> finish());

        messageList = dbHelper.getMessagesBetween(currentUserId, participantEmail);
        adapter = new MessageAdapter(messageList, currentUserId);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void refreshMessages() {
        if (currentUserId == null || participantEmail == null) return;
        List<ChatMessage> latest = dbHelper.getMessagesBetween(currentUserId, participantEmail);
        if (latest.size() > messageList.size()) {
            int previousSize = messageList.size();
            for (int i = previousSize; i < latest.size(); i++) {
                messageList.add(latest.get(i));
            }
            adapter.notifyItemRangeInserted(previousSize, latest.size() - previousSize);
            rvMessages.smoothScrollToPosition(messageList.size() - 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        pollHandler.post(pollRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        if (dbHelper.insertMessage(currentUserId, participantEmail, text)) {
            ChatMessage newMessage = new ChatMessage(currentUserId, participantEmail, text, System.currentTimeMillis());
            messageList.add(newMessage);
            adapter.notifyItemInserted(messageList.size() - 1);
            rvMessages.smoothScrollToPosition(messageList.size() - 1);
            etMessage.setText("");
        }
    }
}