package com.example.campusconnectmobile;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {

    public static void applyTheme(int mode) {
        AppCompatDelegate.setDefaultNightMode(mode);
    }
    
    public static void applySavedTheme(SessionManager sessionManager) {
        applyTheme(sessionManager.getThemeMode());
    }
}