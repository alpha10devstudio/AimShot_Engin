package com.alpha10.aimshot.service;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles authentication against the Alpha10 backend to acquire
 * single-use temporary tokens for secure APK streaming.
 */
public class UpdateAuthService {

    public static final String AUTH_ENDPOINT = "https://alpha10.freedev.app/api/auth.php";
    public static final String DEFAULT_APP_KEY = "AIMSHOT-ALPHA10-ENGINE-2026";

    public interface AuthCallback {
        void onAuthSuccess(String temporaryToken, long expiresInSeconds);
        void onAuthFailure(String errorMessage, int httpCode);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final String appKey;

    public UpdateAuthService() {
        this(DEFAULT_APP_KEY);
    }

    public UpdateAuthService(String appKey) {
        this.appKey = appKey != null ? appKey : DEFAULT_APP_KEY;
    }

    /**
     * Authenticates with the Alpha10 server and returns a single-use token.
     */
    public void authenticate(final AuthCallback callback) {
        if (callback == null) return;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(AUTH_ENDPOINT);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("User-Agent", "AimShotEngine/1.0 (Android)");
                    conn.setRequestProperty("X-App-Key", appKey);
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.setDoOutput(true);

                    // Send basic request payload
                    byte[] payload = "action=request_download_token".getBytes(StandardCharsets.UTF_8);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(payload);
                    }

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        StringBuilder sb = new StringBuilder();
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = br.readLine()) != null) {
                                sb.append(line);
                            }
                        }

                        String responseBody = sb.toString().trim();
                        String token = null;
                        long expiresIn = 60; // 60s default per spec

                        try {
                            JSONObject json = new JSONObject(responseBody);
                            if (json.has("token")) {
                                token = json.getString("token");
                            } else if (json.has("access_token")) {
                                token = json.getString("access_token");
                            } else if (json.has("data")) {
                                JSONObject data = json.getJSONObject("data");
                                token = data.optString("token", data.optString("access_token", null));
                            }

                            if (json.has("expires_in")) {
                                expiresIn = json.getLong("expires_in");
                            }
                        } catch (Exception ex) {
                            // If response is raw token string
                            if (responseBody.length() >= 16 && !responseBody.startsWith("<")) {
                                token = responseBody;
                            }
                        }

                        if (token != null && !token.isEmpty()) {
                            final String finalToken = token;
                            final long finalExpiresIn = expiresIn;
                            mainHandler.post(() -> callback.onAuthSuccess(finalToken, finalExpiresIn));
                            return;
                        } else {
                            mainHandler.post(() -> callback.onAuthFailure("Invalid authentication response from server", responseCode));
                            return;
                        }
                    } else {
                        mainHandler.post(() -> callback.onAuthFailure("Server rejected authentication (HTTP " + responseCode + ")", responseCode));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onAuthFailure("Network error: " + e.getMessage(), -1));
                } finally {
                    if (conn != null) {
                        try { conn.disconnect(); } catch (Exception ignored) {}
                    }
                }
            }
        });
    }
}
