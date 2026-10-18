package com.example.campusconnectmobile;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Map;

/** Builds the system notification for a push message and routes a tap into the app. */
public final class AppNotifier {

    public static final String EXTRA_PUSH_TYPE = "extra_push_type";
    public static final String EXTRA_PUSH_ENTITY_ID = "extra_push_entity_id";
    public static final String EXTRA_PUSH_TITLE = "extra_push_title";

    private AppNotifier() {
    }

    public static void show(Context context, Map<String, String> data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        // Notifications only make sense for a signed-in user
        if (!new SessionManager(context).isLoggedIn()) return;

        String type = valueOf(data, "type");
        String title = valueOf(data, "title");
        String body = valueOf(data, "body");
        int entityId = parseInt(valueOf(data, "entity_id"));
        String channel = valueOf(data, "channel");
        if (channel.isEmpty()) channel = NotificationChannels.ACTIVITY;

        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EXTRA_PUSH_TYPE, type);
        intent.putExtra(EXTRA_PUSH_ENTITY_ID, entityId);
        intent.putExtra(EXTRA_PUSH_TITLE, title);

        // One notification per conversation or listing, so a newer alert replaces the older one
        int notificationId = (type + ":" + entityId).hashCode();
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_tab_notifications)
                .setContentTitle(title.isEmpty() ? context.getString(R.string.app_name) : title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory("message".equals(type) ? NotificationCompat.CATEGORY_MESSAGE
                        : "event_reminder".equals(type) ? NotificationCompat.CATEGORY_EVENT
                        : NotificationCompat.CATEGORY_RECOMMENDATION);

        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }

    private static String valueOf(Map<String, String> data, String key) {
        String value = data.get(key);
        return value == null ? "" : value;
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
