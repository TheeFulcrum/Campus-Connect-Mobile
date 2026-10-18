package com.example.campusconnectmobile;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class NotificationsFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private MaterialButton btnMarkAllRead;
    private SessionManager sessionManager;
    private final List<AppNotification> notifications = new ArrayList<>();
    private NotificationsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);
        sessionManager = new SessionManager(requireContext());
        swipeRefreshLayout = view.findViewById(R.id.swipeNotifications);
        recyclerView = view.findViewById(R.id.rvNotifications);
        tvEmpty = view.findViewById(R.id.tvNotificationsEmpty);
        btnMarkAllRead = view.findViewById(R.id.btnMarkNotificationsRead);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NotificationsAdapter();
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout.setOnRefreshListener(this::loadNotifications);
        btnMarkAllRead.setOnClickListener(v -> markAllRead());
        loadNotifications();
        return view;
    }

    private void loadNotifications() {
        CcwApiClient.listNotifications(1, sessionManager.getAuthToken(),
                new CcwApiClient.Callback<ListPage<AppNotification>>() {
                    @Override
                    public void onSuccess(ListPage<AppNotification> result) {
                        if (!isAdded()) return;
                        swipeRefreshLayout.setRefreshing(false);
                        notifications.clear();
                        if (result != null) notifications.addAll(result.items);
                        adapter.notifyDataSetChanged();
                        tvEmpty.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded()) return;
                        swipeRefreshLayout.setRefreshing(false);
                        if (notifications.isEmpty()) tvEmpty.setVisibility(View.VISIBLE);
                        Toast.makeText(requireContext(),
                                message == null || message.isEmpty()
                                        ? getString(R.string.generic_offline_message)
                                        : message,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void markAllRead() {
        CcwApiClient.markAllNotificationsRead(sessionManager.getAuthToken(),
                new CcwApiClient.Callback<JSONObject>() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        for (AppNotification n : notifications) n.isRead = true;
                        adapter.notifyDataSetChanged();
                        Toast.makeText(requireContext(), R.string.notifications_mark_all_read,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(requireContext(),
                                message == null || message.isEmpty()
                                        ? getString(R.string.generic_offline_message)
                                        : message,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private final class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationHolder> {
        @NonNull
        @Override
        public NotificationHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new NotificationHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NotificationHolder holder, int position) {
            AppNotification notification = notifications.get(position);
            String actor = notification.actorUsername == null || notification.actorUsername.isEmpty()
                    ? getString(R.string.notifications_actor_fallback)
                    : "@" + notification.actorUsername;
            String body = notification.body == null || notification.body.isEmpty()
                    ? notification.type : notification.body;
            holder.body.setText("event_reminder".equals(notification.type) ? body : actor + " · " + body);
            holder.meta.setText(toTimeAgo(notification.createdAt));
            holder.unreadDot.setVisibility(notification.isRead ? View.GONE : View.VISIBLE);
            Glide.with(holder.avatar).load(CcwHttp.resolveUrl(notification.actorAvatarUrl))
                    .placeholder(R.drawable.bg_avatar_circle)
                    .error(R.drawable.bg_avatar_circle)
                    .circleCrop()
                    .into(holder.avatar);
            holder.itemView.setOnClickListener(v -> {
                if (getActivity() instanceof HomeActivity && isListingAlert(notification.type)) {
                    ((HomeActivity) getActivity()).openListingById(notification.entityId);
                } else if (getActivity() instanceof HomeActivity && notification.actorUsername != null
                        && !notification.actorUsername.isEmpty()) {
                    ((HomeActivity) getActivity()).openProfile(notification.actorUsername);
                }
            });
        }

        @Override
        public int getItemCount() {
            return notifications.size();
        }

        final class NotificationHolder extends RecyclerView.ViewHolder {
            final ImageView avatar;
            final TextView body;
            final TextView meta;
            final View unreadDot;

            NotificationHolder(@NonNull View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.ivNotificationAvatar);
                body = itemView.findViewById(R.id.tvNotificationBody);
                meta = itemView.findViewById(R.id.tvNotificationMeta);
                unreadDot = itemView.findViewById(R.id.vUnreadDot);
            }
        }
    }

    private static boolean isListingAlert(String type) {
        return "listing_limited".equals(type) || "low_stock".equals(type) || "event_reminder".equals(type);
    }

    private String toTimeAgo(String value) {
        if (value == null || value.isEmpty()) return "";
        String[] patterns = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss"};
        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                format.setTimeZone(TimeZone.getDefault());
                Date date = format.parse(value);
                if (date != null) {
                    return DateUtils.getRelativeTimeSpanString(date.getTime(),
                            System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString();
                }
            } catch (Exception ignored) {
            }
        }
        return value;
    }
}
