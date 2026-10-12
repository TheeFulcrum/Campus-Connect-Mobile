package com.example.campusconnectmobile;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.List;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_CONVERSATION_TITLE = "conversation_title";

    private static final long POLL_INTERVAL_MS = 3_000L;

    private int conversationId;
    private String token;
    private String myUsername;
    private MessageAdapter adapter;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private Button btnSend;
    private final android.os.Handler pollHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            pollMessages(adapter.getLastMessageId());
            pollHandler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        SessionManager sessionManager = new SessionManager(this);
        token = sessionManager.getAuthToken();
        myUsername = sessionManager.getLoggedInUsername();
        conversationId = getIntent().getIntExtra(EXTRA_CONVERSATION_ID, 0);
        if (conversationId <= 0 || TextUtils.isEmpty(token)) {
            finish();
            return;
        }

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etChatMessage);
        btnSend = findViewById(R.id.btnSendChat);
        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView tvChatTitle = findViewById(R.id.tvChatTitle);
        String title = getIntent().getStringExtra(EXTRA_CONVERSATION_TITLE);
        tvChatTitle.setText(title == null || title.isEmpty()
                ? getString(R.string.messages_title) : title);
        btnBack.setContentDescription(getString(R.string.cd_navigate_back));
        btnBack.setOnClickListener(v -> finish());

        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setStackFromEnd(true);
        rvMessages.setLayoutManager(manager);
        adapter = new MessageAdapter();
        rvMessages.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());
        pollMessages(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }

    private void pollMessages(int afterId) {
        CcwApiClient.getThread(conversationId, afterId, token, myUsername,
                new CcwApiClient.Callback<List<ServerMessage>>() {
                    @Override
                    public void onSuccess(List<ServerMessage> result) {
                        if (result == null || result.isEmpty()) return;
                        boolean firstLoad = afterId == 0 && adapter.getLastMessageId() == 0;
                        if (firstLoad) {
                            adapter.submitMessages(result);
                        } else {
                            adapter.appendMessages(result);
                        }
                        rvMessages.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
                        if (afterId == 0) {
                            CcwApiClient.markConversationRead(conversationId, token,
                                    new CcwApiClient.Callback<JSONObject>() {
                                        @Override public void onSuccess(JSONObject result) {}
                                        @Override public void onError(String message) {}
                                    });
                        }
                    }

                    @Override
                    public void onError(String message) {
                        // keep current thread visible when polling fails
                    }
                });
    }

    private void sendMessage() {
        String body = etMessage.getText() == null ? "" : etMessage.getText().toString().trim();
        if (body.isEmpty()) return;
        if (body.length() > 2000) {
            etMessage.setError(getString(R.string.messages_body_too_long));
            return;
        }
        btnSend.setEnabled(false);
        CcwApiClient.sendMessageToConversation(conversationId, body, token,
                new CcwApiClient.Callback<JSONObject>() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        etMessage.setText("");
                        btnSend.setEnabled(true);
                        pollMessages(adapter.getLastMessageId());
                    }

                    @Override
                    public void onError(String message) {
                        btnSend.setEnabled(true);
                        Toast.makeText(ChatActivity.this,
                                message == null || message.isEmpty()
                                        ? getString(R.string.generic_offline_message)
                                        : message,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
