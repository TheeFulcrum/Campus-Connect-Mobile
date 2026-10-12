package com.example.campusconnectmobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MessagesFragment extends Fragment implements ConversationAdapter.OnConversationClickListener {

    private static final String ARG_EMAIL = "arg_email";
    private static final long POLL_INTERVAL_MS = 5_000L;

    private RecyclerView rvConversations;
    private TextView tvEmpty;
    private final List<ServerConversation> conversations = new ArrayList<>();
    private ConversationAdapter adapter;
    private SessionManager sessionManager;
    private ProfileCacheDbHelper cacheDb;
    private final android.os.Handler pollHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            refreshConversations();
            pollHandler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    public static MessagesFragment newInstance(String email) {
        MessagesFragment fragment = new MessagesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);
        sessionManager = new SessionManager(requireContext());
        cacheDb = new ProfileCacheDbHelper(requireContext());
        rvConversations = view.findViewById(R.id.rvConversations);
        tvEmpty = view.findViewById(R.id.tvEmptyMessages);
        rvConversations.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ConversationAdapter(conversations, this);
        rvConversations.setAdapter(adapter);
        loadCachedConversations();
        refreshConversations();
        return view;
    }

    private void loadCachedConversations() {
        List<JSONObject> cached = cacheDb.getConversations();
        if (cached.isEmpty()) return;
        conversations.clear();
        for (JSONObject json : cached) {
            conversations.add(ServerConversation.fromJson(json));
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void refreshConversations() {
        String token = sessionManager.getAuthToken();
        if (token == null) return;
        CcwApiClient.listConversations(token, new CcwApiClient.Callback<List<ServerConversation>>() {
            @Override
            public void onSuccess(List<ServerConversation> result) {
                if (!isAdded()) return;
                conversations.clear();
                if (result != null) conversations.addAll(result);
                adapter.notifyDataSetChanged();
                updateEmptyState();
                JSONArray cacheArray = new JSONArray();
                for (ServerConversation c : conversations) {
                    JSONObject json = new JSONObject();
                    try {
                        json.put("id", c.id);
                        json.put("listing_id", c.listingId);
                        json.put("listing_title", c.listingTitle);
                        json.put("other_username", c.otherUsername);
                        json.put("other_avatar_url", c.otherAvatarUrl);
                        json.put("last_message", c.lastMessage);
                        json.put("last_message_at", c.lastMessageAt);
                        json.put("unread", c.unread);
                        cacheArray.put(json);
                    } catch (Exception ignored) {
                    }
                }
                List<JSONObject> list = new ArrayList<>();
                for (int i = 0; i < cacheArray.length(); i++) {
                    JSONObject item = cacheArray.optJSONObject(i);
                    if (item != null) list.add(item);
                }
                cacheDb.putConversations(list);
            }

            @Override
            public void onError(String message) {
                updateEmptyState();
            }
        });
    }

    private void updateEmptyState() {
        boolean empty = conversations.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvConversations.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onConversationClick(ServerConversation conversation) {
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversation.id);
        String title = conversation.otherUsername;
        if (conversation.listingTitle != null && !conversation.listingTitle.isEmpty()) {
            title = title + " • " + conversation.listingTitle;
        }
        intent.putExtra(ChatActivity.EXTRA_CONVERSATION_TITLE, title);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        pollHandler.post(pollRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }
}
