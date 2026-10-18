package com.example.campusconnectmobile;

import android.content.Context;
import android.text.format.DateFormat;

import java.util.Date;

/** Display text for limited stock, priority and event dates on a listing. */
public final class ListingUrgency {

    private ListingUrgency() {
    }

    /** "Only 2 left" / "Sold out", or null when the seller did not limit stock. */
    public static String stockText(Context context, Listing listing) {
        if (!listing.hasLimitedStock()) return null;
        if (listing.quantity == 0) return context.getString(R.string.listing_sold_out);
        return listing.quantity == 1
                ? context.getString(R.string.listing_only_left_one)
                : context.getString(R.string.listing_only_left, listing.quantity);
    }

    public static String eventText(Context context, Listing listing) {
        if (!listing.isEvent()) return null;
        return context.getString(R.string.listing_event_format, formatEventTime(context, listing.eventAt));
    }

    /** Short label for grid tiles; stock wins over priority, which wins over the event date. */
    public static String badgeText(Context context, Listing listing) {
        String stock = stockText(context, listing);
        if (stock != null && listing.quantity <= 5) return stock;
        if (listing.priority) return context.getString(R.string.listing_priority_badge);
        if (listing.isEvent()) return DateFormat.format("d MMM", new Date(listing.eventAt * 1000L)).toString();
        return stock;
    }

    public static String formatEventTime(Context context, long epochSeconds) {
        Date date = new Date(epochSeconds * 1000L);
        return DateFormat.getMediumDateFormat(context).format(date) + " "
                + DateFormat.getTimeFormat(context).format(date);
    }
}
