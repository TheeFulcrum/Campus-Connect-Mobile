package com.example.campusconnectmobile;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

/** Android notification channels, so users can mute each kind of alert separately. */
public final class NotificationChannels {

    public static final String MESSAGES = "messages";
    public static final String LISTINGS = "listings";
    public static final String EVENTS = "events";
    public static final String ACTIVITY = "activity";

    private NotificationChannels() {
    }

    public static void create(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        manager.createNotificationChannel(channel(context.getString(R.string.channel_messages), MESSAGES,
                NotificationManager.IMPORTANCE_HIGH, context.getString(R.string.channel_messages_description)));
        manager.createNotificationChannel(channel(context.getString(R.string.channel_listings), LISTINGS,
                NotificationManager.IMPORTANCE_HIGH, context.getString(R.string.channel_listings_description)));
        manager.createNotificationChannel(channel(context.getString(R.string.channel_events), EVENTS,
                NotificationManager.IMPORTANCE_HIGH, context.getString(R.string.channel_events_description)));
        manager.createNotificationChannel(channel(context.getString(R.string.channel_activity), ACTIVITY,
                NotificationManager.IMPORTANCE_DEFAULT, context.getString(R.string.channel_activity_description)));
    }

    private static NotificationChannel channel(String name, String id, int importance, String description) {
        NotificationChannel channel = new NotificationChannel(id, name, importance);
        channel.setDescription(description);
        return channel;
    }
}
