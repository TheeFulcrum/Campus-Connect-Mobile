package com.example.campusconnectmobile;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class FeedCategoryMapper {

    private FeedCategoryMapper() {
    }

    public static String[] expand(String[] selected) {
        Set<String> categories = new LinkedHashSet<>();
        if (selected == null) return new String[0];

        for (String value : selected) {
            if (value == null) continue;
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            switch (normalized) {
                case "services":
                    add(categories, "services", "tutoring", "repairs", "beauty", "creative");
                    break;
                case "goods & textbooks":
                    add(categories, "goods & textbooks", "electronics", "books", "furniture");
                    break;
                case "academic help":
                case "academic & help":
                    add(categories, "academic help", "academic & help", "tutoring", "books");
                    break;
                case "events & clubs":
                    add(categories, "events & clubs", "creative", "campus-help");
                    break;
                case "housing & roommates":
                    add(categories, "housing & roommates", "furniture", "campus-help");
                    break;
                case "gigs & jobs":
                    add(categories, "gigs & jobs", "campus-help", "creative", "repairs");
                    break;
                default:
                    if (!normalized.isEmpty()) categories.add(normalized);
                    break;
            }
        }
        return categories.toArray(new String[0]);
    }

    private static void add(Set<String> categories, String... values) {
        for (String value : values) categories.add(value);
    }
}