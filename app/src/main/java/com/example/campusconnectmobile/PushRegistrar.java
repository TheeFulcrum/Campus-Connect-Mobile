package com.example.campusconnectmobile;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

/**
 * Keeps this device's Firebase token registered with the server for the signed-in user.
 * Does nothing until the Firebase client settings are provided at build time.
 */
public final class PushRegistrar {

    private static final String PREFS = "ccw_push";
    private static final String KEY_TOKEN = "fcm_token";
    private static final String KEY_REGISTERED_FOR = "registered_for";

    private PushRegistrar() {
    }

    public static boolean isConfigured() {
        return !BuildConfig.FCM_PROJECT_ID.isEmpty() && !BuildConfig.FCM_APP_ID.isEmpty()
                && !BuildConfig.FCM_API_KEY.isEmpty() && !BuildConfig.FCM_SENDER_ID.isEmpty();
    }

    /** Initializes Firebase from build-time settings (no google-services.json needed). */
    public static void init(Context context) {
        if (!isConfigured() || !FirebaseApp.getApps(context).isEmpty()) return;
        FirebaseApp.initializeApp(context, new FirebaseOptions.Builder()
                .setProjectId(BuildConfig.FCM_PROJECT_ID)
                .setApplicationId(BuildConfig.FCM_APP_ID)
                .setApiKey(BuildConfig.FCM_API_KEY)
                .setGcmSenderId(BuildConfig.FCM_SENDER_ID)
                .build());
    }

    private static boolean ready(Context context) {
        return isConfigured() && !FirebaseApp.getApps(context).isEmpty();
    }

    /** Fetches the current token and registers it if the server has not seen it for this account yet. */
    public static void sync(Context context) {
        if (!ready(context)) return;
        Context appContext = context.getApplicationContext();
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
            if (token != null && !token.isEmpty()) registerToken(appContext, token);
        });
    }

    public static void registerToken(Context context, String token) {
        SessionManager session = new SessionManager(context);
        String authToken = session.getAuthToken();
        String email = session.getLoggedInEmail();
        if (authToken == null || authToken.isEmpty() || email == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String registeredFor = email + "|" + token;
        if (registeredFor.equals(prefs.getString(KEY_REGISTERED_FOR, ""))) return;

        try {
            JSONObject body = new JSONObject();
            body.put("token", token);
            body.put("platform", "android");
            CcwHttp.postJson(CcwHttp.endpoint("/api/devices/register.php"), body, authToken,
                    new CcwHttp.JsonCallback() {
                        @Override
                        public void onSuccess(JSONObject json) {
                            prefs.edit().putString(KEY_TOKEN, token).putString(KEY_REGISTERED_FOR, registeredFor).apply();
                        }

                        @Override
                        public void onError(String message, int statusCode) {
                            // Retried on the next app start
                        }
                    });
        } catch (Exception ignored) {
        }
    }

    /** Stops this device receiving the account's notifications; call before the session is cleared. */
    public static void unregister(Context context, String authToken) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String token = prefs.getString(KEY_TOKEN, "");
        prefs.edit().remove(KEY_TOKEN).remove(KEY_REGISTERED_FOR).apply();
        if (token.isEmpty() || authToken == null || authToken.isEmpty()) return;
        try {
            JSONObject body = new JSONObject();
            body.put("token", token);
            CcwHttp.postJson(CcwHttp.endpoint("/api/devices/unregister.php"), body, authToken,
                    new CcwHttp.JsonCallback() {
                        @Override
                        public void onSuccess(JSONObject json) {
                        }

                        @Override
                        public void onError(String message, int statusCode) {
                        }
                    });
        } catch (Exception ignored) {
        }
    }
}
