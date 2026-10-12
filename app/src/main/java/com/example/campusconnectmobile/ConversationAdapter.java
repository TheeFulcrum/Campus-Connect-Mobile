package com.example.campusconnectmobile;

import android.text.format.DateUtils;
import android.widget.ImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    private final List<ServerConversation> conversations;
    private final OnConversationClickListener listener;

    public interface OnConversationClickListener {
        void onConversationClick(ServerConversation conversation);
    }

    public ConversationAdapter(List<ServerConversation> conversations, OnConversationClickListener listener) {
        this.conversations = conversations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ServerConversation conversation = conversations.get(position);
        holder.tvName.setText(conversation.otherUsername);
        holder.tvLastMsg.setText(conversation.lastMessage);

        long timestamp = parseServerTime(conversation.lastMessageAt);
        String timeAgo = DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS).toString();
        holder.tvTime.setText(timeAgo);
        holder.tvContext.setText(conversation.listingTitle == null || conversation.listingTitle.isEmpty()
                ? holder.itemView.getContext().getString(R.string.messages_listing_unknown)
                : conversation.listingTitle);
        holder.tvUnread.setVisibility(conversation.unread > 0 ? View.VISIBLE : View.GONE);
        holder.tvUnread.setText(conversation.unread > 99 ? "99+" : String.valueOf(conversation.unread));
        Glide.with(holder.avatar).load(CcwHttp.resolveUrl(conversation.otherAvatarUrl))
                .placeholder(R.drawable.bg_avatar_circle)
                .error(R.drawable.bg_avatar_circle)
                .circleCrop()
                .into(holder.avatar);

        holder.itemView.setOnClickListener(v -> listener.onConversationClick(conversation));
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView tvName, tvLastMsg, tvTime, tvContext, tvUnread;

        ViewHolder(View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.ivConversationAvatar);
            tvName = itemView.findViewById(R.id.tvConvName);
            tvLastMsg = itemView.findViewById(R.id.tvConvLastMsg);
            tvTime = itemView.findViewById(R.id.tvConvTime);
            tvContext = itemView.findViewById(R.id.tvConvListingContext);
            tvUnread = itemView.findViewById(R.id.tvConvUnread);
        }
    }

    private static long parseServerTime(String raw) {
        if (raw == null || raw.isEmpty()) return System.currentTimeMillis();
        String[] patterns = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss"};
        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                format.setTimeZone(TimeZone.getDefault());
                java.util.Date date = format.parse(raw);
                if (date != null) return date.getTime();
            } catch (Exception ignored) {
            }
        }
        return System.currentTimeMillis();
    }
}