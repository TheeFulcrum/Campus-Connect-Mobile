package com.example.campusconnectmobile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_MINE = 1;
    private static final int VIEW_TYPE_THEIRS = 2;

    private final List<ChatMessage> messages;
    private final String currentUserId;

    public MessageAdapter(List<ChatMessage> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        if (messages.get(position).senderEmail.equals(currentUserId)) {
            return VIEW_TYPE_MINE;
        } else {
            return VIEW_TYPE_THEIRS;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_MINE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_mine, parent, false);
            return new MineViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_theirs, parent, false);
            return new TheirsViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof MineViewHolder) {
            ((MineViewHolder) holder).tvText.setText(message.messageText);
        } else if (holder instanceof TheirsViewHolder) {
            ((TheirsViewHolder) holder).tvText.setText(message.messageText);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MineViewHolder extends RecyclerView.ViewHolder {
        TextView tvText;
        MineViewHolder(View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tvMessageMine);
        }
    }

    static class TheirsViewHolder extends RecyclerView.ViewHolder {
        TextView tvText;
        TheirsViewHolder(View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tvMessageTheirs);
        }
    }
}