package com.example.campusconnectmobile;

import org.json.JSONObject;

/** Client for the /api/password-recovery/ endpoints shared with the web app. */
public final class PasswordRecoveryApiClient {

    public interface RequestCallback {
        /** devResetToken is only non-empty when the server runs in development mode. */
        void onSuccess(String message, String devResetToken);
        void onError(String message);
    }

    public interface ResetCallback {
        void onSuccess(String message);
        void onError(String message);
    }

    private PasswordRecoveryApiClient() {
    }

    public static void requestReset(String email, RequestCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("action", "request_reset");
            body.put("email", email);
        } catch (Exception exception) {
            callback.onError("Unable to prepare the reset request.");
            return;
        }
        CcwHttp.postJson(CcwHttp.endpoint("/api/password-recovery/request-reset.php"), body, null,
                new CcwHttp.JsonCallback() {
                    @Override
                    public void onSuccess(JSONObject json) {
                        callback.onSuccess(
                                json.optString("message", "If an account with that email exists, a password reset link will be sent."),
                                json.optString("reset_token", ""));
                    }

                    @Override
                    public void onError(String message, int statusCode) {
                        callback.onError(message);
                    }
                });
    }

    public static void resetPassword(String token, String newPassword, ResetCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("action", "reset_password");
            body.put("token", token);
            body.put("password", newPassword);
        } catch (Exception exception) {
            callback.onError("Unable to prepare the password reset.");
            return;
        }
        CcwHttp.postJson(CcwHttp.endpoint("/api/password-recovery/reset-password.php"), body, null,
                new CcwHttp.JsonCallback() {
                    @Override
                    public void onSuccess(JSONObject json) {
                        callback.onSuccess(json.optString("message", "Password has been reset successfully."));
                    }

                    @Override
                    public void onError(String message, int statusCode) {
                        callback.onError(message);
                    }
                });
    }
}
