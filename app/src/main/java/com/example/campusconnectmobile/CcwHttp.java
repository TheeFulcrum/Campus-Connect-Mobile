package com.example.campusconnectmobile;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Shared HTTP plumbing for every Campus Connect API client. Mirrors the
 * HttpURLConnection + bearer-token approach already used by AuthApiClient so
 * the whole app keeps a single, consistent networking style (no Retrofit/OkHttp).
 */
public final class CcwHttp {

    /** A single file to upload as part of a multipart request. */
    public static final class FilePart {
        public final String fieldName;
        public final String fileName;
        public final byte[] bytes;
        public final String mimeType;

        public FilePart(String fieldName, String fileName, byte[] bytes, String mimeType) {
            this.fieldName = fieldName;
            this.fileName = fileName;
            this.bytes = bytes;
            this.mimeType = mimeType;
        }
    }

    public interface JsonCallback {
        void onSuccess(JSONObject json);
        void onError(String message, int statusCode);
    }

    public static final class ApiException extends Exception {
        public final int statusCode;
        public ApiException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }
    }

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);
    private static final Handler MAIN_THREAD = new Handler(Looper.getMainLooper());
    private static final int TIMEOUT_MS = 15_000;

    private CcwHttp() {
    }

    public static String baseUrl() {
        String baseUrl = BuildConfig.CCW_AUTH_API_BASE_URL;
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public static String endpoint(String path) {
        return baseUrl() + (path.startsWith("/") ? path : "/" + path);
    }

    public static String resolveUrl(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isEmpty()) return "";
        return pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")
                ? pathOrUrl : endpoint(pathOrUrl);
    }

    public static String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (Exception exception) {
            return "";
        }
    }

    public static void runAsync(Runnable task) {
        EXECUTOR.execute(task);
    }

    private static void postSuccess(JsonCallback callback, JSONObject json) {
        MAIN_THREAD.post(() -> callback.onSuccess(json));
    }

    private static void postError(JsonCallback callback, String message, int status) {
        MAIN_THREAD.post(() -> callback.onError(message, status));
    }

    /** GET request, optional bearer token, returns parsed JSON on the main thread. */
    public static void get(String url, String token, JsonCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                JSONObject json = getSync(url, token);
                postSuccess(callback, json);
            } catch (ApiException apiException) {
                postError(callback, apiException.getMessage(), apiException.statusCode);
            } catch (Exception exception) {
                postError(callback, "Unable to reach the Campus Connect server.", 0);
            }
        });
    }

    /** Synchronous GET; throws ApiException on failure. Call only off the main thread. */
    public static JSONObject getSync(String url, String token) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setRequestProperty("Accept", "application/json");
        applyAuth(connection, token);
        return finishJson(connection);
    }

    /** POST a JSON body, optional bearer token, returns parsed JSON on the main thread. */
    public static void postJson(String url, JSONObject body, String token, JsonCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                JSONObject json = postJsonSync(url, body, token);
                postSuccess(callback, json);
            } catch (ApiException apiException) {
                postError(callback, apiException.getMessage(), apiException.statusCode);
            } catch (Exception exception) {
                postError(callback, "Unable to reach the Campus Connect server.", 0);
            }
        });
    }

    public static JSONObject postJsonSync(String url, JSONObject body, String token) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        applyAuth(connection, token);
        try (OutputStream output = connection.getOutputStream()) {
            output.write((body == null ? new JSONObject() : body).toString().getBytes(StandardCharsets.UTF_8));
        }
        return finishJson(connection);
    }

    /**
     * POST a multipart/form-data request with plain text fields and file parts.
     * Reports upload progress (0-100) on the main thread as bytes are written.
     */
    public static void postMultipart(String url, Map<String, String> fields, List<FilePart> files,
                                     String token, ProgressCallback progress, JsonCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                JSONObject json = postMultipartSync(url, fields, files, token, progress);
                postSuccess(callback, json);
            } catch (ApiException apiException) {
                postError(callback, apiException.getMessage(), apiException.statusCode);
            } catch (Exception exception) {
                postError(callback, "Unable to reach the Campus Connect server.", 0);
            }
        });
    }

    public interface ProgressCallback {
        void onProgress(int percent);
    }

    public static JSONObject postMultipartSync(String url, Map<String, String> fields, List<FilePart> files,
                                                String token, ProgressCallback progress) throws Exception {
        String boundary = "CCWBoundary" + System.currentTimeMillis();
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS * 3);
        connection.setDoOutput(true);
        connection.setChunkedStreamingMode(64 * 1024);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setRequestProperty("Accept", "application/json");
        applyAuth(connection, token);

        long totalBytes = estimateMultipartSize(boundary, fields, files);
        final long[] written = {0L};

        try (OutputStream rawOutput = connection.getOutputStream()) {
            if (fields != null) {
                for (Map.Entry<String, String> entry : fields.entrySet()) {
                    String part = "--" + boundary + "\r\n"
                            + "Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n"
                            + entry.getValue() + "\r\n";
                    byte[] partBytes = part.getBytes(StandardCharsets.UTF_8);
                    rawOutput.write(partBytes);
                    written[0] += partBytes.length;
                    reportProgress(progress, written[0], totalBytes);
                }
            }
            if (files != null) {
                for (FilePart file : files) {
                    String header = "--" + boundary + "\r\n"
                            + "Content-Disposition: form-data; name=\"" + file.fieldName + "\"; filename=\"" + file.fileName + "\"\r\n"
                            + "Content-Type: " + file.mimeType + "\r\n\r\n";
                    byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
                    rawOutput.write(headerBytes);
                    written[0] += headerBytes.length;

                    int chunkSize = 16 * 1024;
                    for (int offset = 0; offset < file.bytes.length; offset += chunkSize) {
                        int length = Math.min(chunkSize, file.bytes.length - offset);
                        rawOutput.write(file.bytes, offset, length);
                        written[0] += length;
                        reportProgress(progress, written[0], totalBytes);
                    }
                    byte[] trailing = "\r\n".getBytes(StandardCharsets.UTF_8);
                    rawOutput.write(trailing);
                    written[0] += trailing.length;
                }
            }
            byte[] closing = ("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
            rawOutput.write(closing);
            written[0] += closing.length;
            reportProgress(progress, written[0], totalBytes);
        }

        return finishJson(connection);
    }

    private static void reportProgress(ProgressCallback progress, long written, long total) {
        if (progress == null || total <= 0) return;
        int percent = (int) Math.min(100, (written * 100) / total);
        MAIN_THREAD.post(() -> progress.onProgress(percent));
    }

    private static long estimateMultipartSize(String boundary, Map<String, String> fields, List<FilePart> files) {
        long size = 0;
        if (fields != null) {
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                size += ("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n"
                        + entry.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8).length;
            }
        }
        if (files != null) {
            for (FilePart file : files) {
                size += file.bytes.length + 256;
            }
        }
        return size + 64;
    }

    private static void applyAuth(HttpURLConnection connection, String token) {
        if (token != null && !token.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + token);
        }
    }

    private static JSONObject finishJson(HttpURLConnection connection) throws Exception {
        int status = connection.getResponseCode();
        String response = readResponse(status < 400 ? connection.getInputStream() : connection.getErrorStream());
        JSONObject json;
        try {
            json = new JSONObject(response);
        } catch (Exception parseException) {
            throw new ApiException("The server returned an unexpected response.", status);
        }
        if (status >= 400 || !json.optBoolean("success", status < 400)) {
            throw new ApiException(json.optString("error", "Request failed (" + status + ")."), status);
        }
        return json;
    }

    private static String readResponse(InputStream stream) throws IOException {
        if (stream == null) return "{}";
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
        }
        return response.length() == 0 ? "{}" : response.toString();
    }

    public static JSONArray optArray(JSONObject json, String key) {
        JSONArray array = json.optJSONArray(key);
        return array == null ? new JSONArray() : array;
    }
}
