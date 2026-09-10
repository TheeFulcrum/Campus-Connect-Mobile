package com.example.campusconnectmobile;

import android.app.Application;

public class CampusConnectApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SessionManager sessionManager = new SessionManager(this);
        ThemeHelper.applySavedTheme(sessionManager);
    }
}