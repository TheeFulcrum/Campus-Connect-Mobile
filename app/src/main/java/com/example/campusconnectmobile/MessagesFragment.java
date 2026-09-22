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

import java.util.List;

public class MessagesFragment extends Fragment implements ConversationAdapter.OnConversationClickListener {

    private static final String ARG_EMAIL = "arg_email";
    private String userEmail;
    private DatabaseHelper dbHelper;
    private RecyclerView rvConversations;
    private TextView tvEmpty;

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

        userEmail = getArguments() != null ? getArguments().getString(ARG_EMAIL) : null;
        dbHelper = new DatabaseHelper(requireContext());

        rvConversations = view.findViewById(R.id.rvConversations);
        tvEmpty = view.findViewById(R.id.tvEmptyMessages);

        rvConversations.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        loadConversations();

        return view;
    }

    private void loadConversations() {
        if (userEmail == null) return;
        List<Conversation> conversations = dbHelper.getConversationsFor(userEmail);
        
        if (conversations.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvConversations.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvConversations.setVisibility(View.VISIBLE);
            ConversationAdapter adapter = new ConversationAdapter(conversations, this);
            rvConversations.setAdapter(adapter);
        }
    }

    @Override
    public void onConversationClick(Conversation conversation) {
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_PARTICIPANT_EMAIL, conversation.participantEmail);
        intent.putExtra(ChatActivity.EXTRA_PARTICIPANT_NAME, conversation.participantUsername);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadConversations();
    }
}