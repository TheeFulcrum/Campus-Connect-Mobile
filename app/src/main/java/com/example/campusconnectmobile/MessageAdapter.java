package com.example.campusconnectmobile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_MINE = 1;
    private static final int VIEW_TYPE_THEIRS = 2;

    private final List<ServerMessage> messages = new ArrayList<>();

    public void submitMessages(List<ServerMessage> latest) {
        messages.clear();
        if (latest != null) messages.addAll(latest);
        notifyDataSetChanged();
    }

    public void appendMessages(List<ServerMessage> more) {
        if (more == null || more.isEmpty()) return;
        int start = messages.size();
        messages.addAll(more);
        notifyItemRangeInserted(start, more.size());
    }

    public int getLastMessageId() {
        if (messages.isEmpty()) return 0;
        return messages.get(messages.size() - 1).id;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isMine ? VIEW_TYPE_MINE : VIEW_TYPE_THEIRS;
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
        ServerMessage message = messages.get(position);
        if (holder instanceof MineViewHolder) {
            ((MineViewHolder) holder).tvText.setText(message.body);
        } else if (holder instanceof TheirsViewHolder) {
            ((TheirsViewHolder) holder).tvText.setText(message.body);
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