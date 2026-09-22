package com.example.campusconnectmobile;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AuthApiClient {

    public static final class AuthResult {
        public final String username;
        public final String email;
        public final String campus;
        public final String token;

        AuthResult(String username, String email, String campus, String token) {
            this.username = username;
            this.email = email;
            this.campus = campus;
            this.token = token;
        }
    }

    public interface Callback {
        void onSuccess(AuthResult result);
        void onError(String message);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_THREAD = new Handler(Looper.getMainLooper());

    private AuthApiClient() {
    }

    public static void login(String identifier, String password, Callback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("action", "login");
            body.put("identifier", identifier);
            body.put("password", password);
        } catch (Exception exception) {
            callback.onError("Unable to prepare login request.");
            return;
        }
        send(body, null, callback);
    }

    public static void register(String username, String email, String campus, String password, Callback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("action", "register");
            body.put("username", username);
            body.put("email", email);
            body.put("campus", campus);
            body.put("password", password);
        } catch (Exception exception) {
            callback.onError("Unable to prepare registration request.");
            return;
        }
        send(body, null, callback);
    }

    public static void requestOtp(String email, Callback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("action", "request_otp");
            body.put("email", email);
        } catch (Exception exception) {
            callback.onError("Unable to prepare sign-in request.");
            return;
        }
        send(body, null, callback);
    }

    public static void verifyOtp(String email, String code, Callback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("action", "verify_otp");
            body.put("email", email);
            body.put("code", code);
        } catch (Exception exception) {
            callback.onError("Unable to verify sign-in code.");
            return;
        }
        send(body, null, callback);
    }

    public static void validateSession(String token, Callback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("action", "session");
        } catch (Exception exception) {
            callback.onError("Unable to validate session.");
            return;
        }
        send(body, token, callback);
    }

    public static void logout(String token, Callback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("action", "logout");
        } catch (Exception exception) {
            callback.onError("Unable to log out.");
            return;
        }
        send(body, token, callback);
    }

    private static void send(JSONObject body, String token, Callback callback) {
        EXECUTOR.execute(() -> {
            try {
                URL url = new URL(apiUrl());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(10_000);
                connection.setReadTimeout(10_000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");
                if (token != null && !token.isEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer " + token);
                }

                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                int status = connection.getResponseCode();
                String response = readResponse(status < 400 ? connection.getInputStream() : connection.getErrorStream());
                JSONObject json = new JSONObject(response);
                if (status >= 400 || !json.optBoolean("success")) {
                    postError(callback, json.optString("error", "Authentication request failed."));
                    return;
                }

                JSONObject user = json.optJSONObject("user");
                if (user == null) {
                    postSuccess(callback, new AuthResult(null, null, null, null));
                    return;
                }
                postSuccess(callback, new AuthResult(
                        user.optString("username"),
                        user.optString("email"),
                        user.optString("campus"),
                        json.optString("token", null)
                ));
            } catch (Exception exception) {
                postError(callback, "Unable to reach the Campus Connect server.");
            }
        });
    }

    private static String apiUrl() {
        String baseUrl = BuildConfig.CCW_AUTH_API_BASE_URL;
        return (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + "auth_api.php";
    }

    private static String readResponse(InputStream stream) throws Exception {
        if (stream == null) return "{}";
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
        }
        return response.toString();
    }

    private static void postSuccess(Callback callback, AuthResult result) {
        MAIN_THREAD.post(() -> callback.onSuccess(result));
    }

    private static void postError(Callback callback, String message) {
        MAIN_THREAD.post(() -> callback.onError(message));
    }
}
