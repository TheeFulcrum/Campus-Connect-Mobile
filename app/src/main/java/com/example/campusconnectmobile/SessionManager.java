package com.example.campusconnectmobile;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "campus_connect_session";
    private static final String KEY_EMAIL = "logged_in_email";

    private static final String KEY_THEME = "app_theme";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setTheme(int mode) {
        prefs.edit().putInt(KEY_THEME, mode).apply();
    }

    public int getThemeMode() {
        // Default to follow system (-1 is AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        return prefs.getInt(KEY_THEME, -1);
    }

    public void createSession(String email) {
        prefs.edit().putString(KEY_EMAIL, email).apply();
    }

    public boolean isLoggedIn() {
        return prefs.getString(KEY_EMAIL, null) != null;
    }

    public String getLoggedInEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    public void clearSession() {
        prefs.edit().remove(KEY_EMAIL).apply();
    }
}