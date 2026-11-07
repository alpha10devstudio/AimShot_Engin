package com.alpha10.aimshot.service;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.alpha10.aimshot.BuildConfig;
import com.alpha10.aimshot.model.MembershipStatus;
import com.alpha10.aimshot.model.VerificationResult;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Authoritative Server-Side License Verification Service.
 * Communicates directly with https://alpha10.freedev.app/api/index.php?action=activate_license
 * Automatically solves InfinityFree / ByetHost slowAES anti-bot challenges so Android clients
 * receive clean JSON payloads and valid session state without third-party brokers.
 */
public class LicenseVerificationService {

    public static final String PRODUCTION_ENDPOINT =
            "https://alpha10.freedev.app/api/index.php?action=activate_license";

    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final String serverUrl;
    private boolean isBusy = false;

    public LicenseVerificationService() {
        this(PRODUCTION_ENDPOINT);
    }

    public LicenseVerificationService(String serverUrl) {
        this.serverUrl = (serverUrl != null && !serverUrl.trim().isEmpty())
                ? serverUrl.trim()
                : PRODUCTION_ENDPOINT;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public synchronized boolean isProcessing() {
        return isBusy;
    }

    public void verifyKey(final String rawKey, final String deviceId, final VerificationCallback callback) {
        String deviceModel = (Build.MANUFACTURER != null ? Build.MANUFACTURER : "Android")
                + " " + (Build.MODEL != null ? Build.MODEL : "Device");
        String appVersion = BuildConfig.VERSION_NAME != null ? BuildConfig.VERSION_NAME : "2.4.0";
        verifyKey(rawKey, deviceId, deviceModel, appVersion, callback);
    }

    /**
     * Authoritative license verification against the official AimShot Engine licensing server.
     */
    public synchronized void verifyKey(final String rawKey,
                                      final String deviceId,
                                      final String deviceModel,
                                      final String appVersion,
                                      final VerificationCallback callback) {
        if (callback == null) return;

        if (isBusy) {
            callback.onVerificationError("A verification request is already in progress.", "BUSY");
            return;
        }

        if (rawKey == null || rawKey.trim().isEmpty()) {
            callback.onVerificationError("Verification key cannot be empty.", "ERR_EMPTY_KEY");
            return;
        }

        final String cleanKey = rawKey.trim();
        final String effectiveDeviceId = (deviceId != null && !deviceId.trim().isEmpty())
                ? deviceId.trim()
                : "UNKNOWN_DEVICE";
        final String effectiveModel = (deviceModel != null && !deviceModel.trim().isEmpty())
                ? deviceModel.trim()
                : "Android Device";
        final String effectiveVersion = (appVersion != null && !appVersion.trim().isEmpty())
                ? appVersion.trim()
                : "2.4.0";

        isBusy = true;
        callback.onVerificationStarted();

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    performServerVerification(cleanKey, effectiveDeviceId, effectiveModel, effectiveVersion, callback);
                } catch (Exception e) {
                    postError(callback,
                            "Unable to connect to security server. Please check your internet connection.",
                            "ERR_NETWORK");
                } finally {
                    synchronized (LicenseVerificationService.this) {
                        isBusy = false;
                    }
                }
            }
        });
    }

    private void performServerVerification(String key,
                                           String deviceId,
                                           String deviceModel,
                                           String appVersion,
                                           VerificationCallback callback) throws Exception {

        String activeCookie = UpdateAuthService.getCachedSessionCookie();
        if (activeCookie == null || activeCookie.trim().isEmpty()) {
            activeCookie = UpdateAuthService.solveCookieFromGet(serverUrl);
        }

        String rawResponse = executeHttpVerificationRequest(key, deviceId, deviceModel, appVersion, activeCookie);

        // Check if InfinityFree / ByetHost anti-bot challenge is triggered OR if response is 403 / non-JSON
        if (UpdateAuthService.isAntiBotChallenge(rawResponse) || !rawResponse.contains("{")) {
            String solvedCookie = UpdateAuthService.solveCookieFromGet(serverUrl);
            if (solvedCookie != null && !solvedCookie.isEmpty()) {
                activeCookie = solvedCookie;
                UpdateAuthService.setCachedSessionCookie(solvedCookie);
                // Retry request with the solved __test cookie
                rawResponse = executeHttpVerificationRequest(key, deviceId, deviceModel, appVersion, solvedCookie);
            }
        }

        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            postError(callback, "Empty response from security server. Please try again.", "ERR_EMPTY_RESPONSE");
            return;
        }

        // Clean UTF-8 BOM if present
        rawResponse = rawResponse.trim();
        if (rawResponse.startsWith("\uFEFF")) {
            rawResponse = rawResponse.substring(1).trim();
        }

        // Extract JSON if wrapped in any stray HTML tags
        int firstBrace = rawResponse.indexOf('{');
        int lastBrace = rawResponse.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            rawResponse = rawResponse.substring(firstBrace, lastBrace + 1);
        }

        try {
            JSONObject responseJson = new JSONObject(rawResponse);
            boolean isValid = responseJson.optBoolean("valid", false);
            String status = responseJson.optString("status", "").trim().toLowerCase(Locale.US);
            String serverMessage = responseJson.optString("message", "").trim();

            if (isValid && "active".equals(status)) {
                String expiresAtStr = responseJson.optString("expires_at", "");
                int durationHours = responseJson.optInt("duration_hours", 72);
                long now = System.currentTimeMillis();
                long expiryMillis = 0;

                if (!expiresAtStr.isEmpty()) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                        Date parsedDate = sdf.parse(expiresAtStr);
                        if (parsedDate != null) {
                            expiryMillis = parsedDate.getTime();
                        }
                    } catch (Exception ignored) {
                    }
                }

                if (expiryMillis <= 0) {
                    expiryMillis = now + (durationHours * 3600L * 1000L);
                }

                MembershipStatus membershipStatus = new MembershipStatus(
                        MembershipStatus.State.ACTIVE,
                        "AimShot Pro License",
                        now,
                        expiryMillis,
                        true,
                        deviceId
                );

                String successMessage = !serverMessage.isEmpty()
                        ? serverMessage
                        : "License activated successfully.";
                postSuccess(callback, VerificationResult.success(successMessage, membershipStatus));
            } else {
                String displayError;
                if ("invalid".equals(status)) {
                    displayError = "Invalid license key. Please generate a real key from the website.";
                } else if ("expired".equals(status)) {
                    displayError = "Your license has expired. Please generate a new key to continue.";
                } else if ("revoked".equals(status)) {
                    displayError = "This key has been revoked by administration due to policy violation.";
                } else if ("device_limit_reached".equals(status)) {
                    displayError = "Access Denied: This key is already registered to another device.";
                } else if ("device_revoked".equals(status)) {
                    displayError = "This device has been suspended from accessing AimShot Engine.";
                } else if (!serverMessage.isEmpty()) {
                    displayError = serverMessage;
                } else {
                    displayError = "Verification failed. Please check your key and try again.";
                }

                String code = !status.isEmpty() ? "STATUS_" + status.toUpperCase(Locale.US) : "ERR_VERIFICATION";
                postError(callback, displayError, code);
            }
        } catch (Exception parseEx) {
            postError(callback, "Security verification failed. Please check your connection and try again.", "ERR_PARSE");
        }
    }

    private String executeHttpVerificationRequest(String key,
                                                 String deviceId,
                                                 String deviceModel,
                                                 String appVersion,
                                                 String cookieHeader) throws Exception {
        URL url = new URL(serverUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", UpdateAuthService.BROWSER_USER_AGENT);

        if (cookieHeader != null && !cookieHeader.trim().isEmpty()) {
            conn.setRequestProperty("Cookie", cookieHeader.trim());
        }

        conn.setConnectTimeout(14000);
        conn.setReadTimeout(14000);
        conn.setDoOutput(true);
        conn.setDoInput(true);

        JSONObject requestJson = new JSONObject();
        requestJson.put("license_key", key);
        requestJson.put("device_id", deviceId);
        requestJson.put("device_model", deviceModel);
        requestJson.put("app_version", appVersion);

        byte[] inputBytes = requestJson.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(inputBytes, 0, inputBytes.length);
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        InputStream stream = (responseCode >= 200 && responseCode < 400)
                ? conn.getInputStream()
                : conn.getErrorStream();

        StringBuilder sb = new StringBuilder();
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
        }

        return sb.toString().trim();
    }

    private void postSuccess(final VerificationCallback callback, final VerificationResult result) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onVerificationSuccess(result);
            }
        });
    }

    private void postError(final VerificationCallback callback, final String message, final String code) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onVerificationError(message, code);
            }
        });
    }
}
