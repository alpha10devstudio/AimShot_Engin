package com.alpha10.aimshot.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.alpha10.aimshot.model.MembershipStatus;
import com.alpha10.aimshot.repository.MembershipRepository;

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

/**
 * Handles zero-secret server authentication via the Alpha10 Server-Side Token Broker
 * (POST https://alpha10.freedev.app/api/launcher-auth.php).
 *
 * SECURITY ARCHITECTURE:
 * - ZERO 64-character Secret App Key is stored in the Android APK or sent from the client.
 * - The server broker holds the 64-character X-App-Key server-side and requests the
 *   60-second single-use temporary token from POST /api/auth.php.
 * - Surfaces exact server error messages and supports authoritative release metadata
 *   (expected_sha256, trusted_cert_sha256, version_code, version_name) without guessing.
 */
public class UpdateAuthService {

    public static final String BROKER_AUTH_ENDPOINT = "https://alpha10.freedev.app/api/launcher-auth.php";

    public static class AuthSessionResult {
        private final String temporaryToken;
        private final long expiresInSeconds;
        private final long issuedAtTimestamp;
        private final String expectedSha256;
        private final String trustedCertSha256;
        private final int versionCode;
        private final String versionName;

        public AuthSessionResult(String temporaryToken,
                                 long expiresInSeconds,
                                 long issuedAtTimestamp,
                                 String expectedSha256,
                                 String trustedCertSha256,
                                 int versionCode,
                                 String versionName) {
            this.temporaryToken = temporaryToken;
            this.expiresInSeconds = expiresInSeconds;
            this.issuedAtTimestamp = issuedAtTimestamp;
            this.expectedSha256 = expectedSha256;
            this.trustedCertSha256 = trustedCertSha256;
            this.versionCode = versionCode;
            this.versionName = versionName;
        }

        public String getTemporaryToken() {
            return temporaryToken;
        }

        public long getExpiresInSeconds() {
            return expiresInSeconds;
        }

        public long getIssuedAtTimestamp() {
            return issuedAtTimestamp;
        }

        public String getExpectedSha256() {
            return expectedSha256;
        }

        public String getTrustedCertSha256() {
            return trustedCertSha256;
        }

        public int getVersionCode() {
            return versionCode;
        }

        public String getVersionName() {
            return versionName;
        }
    }

    public interface AuthCallback {
        void onAuthSuccess(AuthSessionResult sessionResult);
        void onAuthFailure(String errorMessage, int httpCode);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final String endpointUrl;

    public UpdateAuthService() {
        this(BROKER_AUTH_ENDPOINT);
    }

    public UpdateAuthService(String endpointUrl) {
        this.endpointUrl = (endpointUrl != null && !endpointUrl.trim().isEmpty())
                ? endpointUrl.trim()
                : BROKER_AUTH_ENDPOINT;
    }

    /**
     * Requests Play Integrity attestation and authenticates with the Server-Side Token Broker
     * without embedding any 64-character Secret App Key in the Android client.
     */
    public void authenticate(final Context context, final AuthCallback callback) {
        if (callback == null) return;
        if (context == null) {
            callback.onAuthFailure("Application context unavailable for authentication.", -1);
            return;
        }

        final Context appContext = context.getApplicationContext();
        final String deviceId = new DeviceIdManager(appContext).getFormattedDeviceId();
        final MembershipStatus membership = MembershipRepository.getInstance(appContext).getCurrentStatus();
        final boolean isLicenseActive = membership != null && membership.getState() == MembershipStatus.State.ACTIVE;
        final String planType = membership != null ? membership.getPlanType() : "";

        IntegrityTokenManager.requestAttestation(appContext, deviceId, new IntegrityTokenManager.AttestationCallback() {
            @Override
            public void onAttestationReady(final String nonce, final String integrityToken) {
                executeBrokerRequest(appContext.getPackageName(), deviceId, isLicenseActive, planType, nonce, integrityToken, callback);
            }
        });
    }

    private void executeBrokerRequest(final String packageName,
                                      final String deviceId,
                                      final boolean isLicenseActive,
                                      final String planType,
                                      final String nonce,
                                      final String integrityToken,
                                      final AuthCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    JSONObject requestJson = new JSONObject();
                    requestJson.put("package_name", packageName);
                    requestJson.put("device_id", deviceId);
                    requestJson.put("license_active", isLicenseActive);
                    requestJson.put("plan_type", planType != null ? planType : "");
                    requestJson.put("integrity_nonce", nonce != null ? nonce : "");
                    if (integrityToken != null && !integrityToken.isEmpty()) {
                        requestJson.put("integrity_token", integrityToken);
                    }

                    byte[] payloadBytes = requestJson.toString().getBytes(StandardCharsets.UTF_8);

                    URL url = new URL(endpointUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setInstanceFollowRedirects(false);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("User-Agent", "AimShotLauncher/1.0 (Android)");
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setFixedLengthStreamingMode(payloadBytes.length);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setDoOutput(true);

                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(payloadBytes);
                        os.flush();
                    }

                    final int responseCode = conn.getResponseCode();
                    InputStream responseStream = (responseCode >= 200 && responseCode < 300)
                            ? conn.getInputStream()
                            : conn.getErrorStream();

                    String responseBody = readStreamSafely(responseStream);

                    if (responseCode != 200) {
                        final String serverError = extractServerErrorMessage(responseBody,
                                "Server authentication rejected (HTTP " + responseCode + ")");
                        mainHandler.post(() -> callback.onAuthFailure(serverError, responseCode));
                        return;
                    }

                    if (responseBody.isEmpty()) {
                        mainHandler.post(() -> callback.onAuthFailure("Empty response received from authentication server.", responseCode));
                        return;
                    }

                    JSONObject json;
                    try {
                        json = new JSONObject(responseBody);
                    } catch (Exception parseEx) {
                        mainHandler.post(() -> callback.onAuthFailure("Invalid JSON format in authentication response.", responseCode));
                        return;
                    }

                    // Surface explicit server-side failure fields before attempting token extraction
                    if (json.has("success") && !json.optBoolean("success", true)) {
                        final String explicitErr = extractServerErrorMessage(responseBody, "Server reported authentication failure.");
                        mainHandler.post(() -> callback.onAuthFailure(explicitErr, responseCode));
                        return;
                    }
                    if ("error".equalsIgnoreCase(json.optString("status", ""))) {
                        final String explicitErr = extractServerErrorMessage(responseBody, "Server returned error status.");
                        mainHandler.post(() -> callback.onAuthFailure(explicitErr, responseCode));
                        return;
                    }

                    String token = extractRecognizedToken(json);
                    if (token == null || token.isEmpty()) {
                        final String detailErr = extractServerErrorMessage(responseBody,
                                "Authentication response did not contain a recognized temporary token field.");
                        mainHandler.post(() -> callback.onAuthFailure(detailErr, responseCode));
                        return;
                    }

                    long expiresIn = json.optLong("expires_in", 60L);
                    if (expiresIn <= 0L) expiresIn = 60L;

                    String expectedSha256 = optNullableString(json, "expected_sha256");
                    if (expectedSha256 == null) {
                        expectedSha256 = optNullableString(json, "sha256");
                    }

                    String trustedCertSha256 = optNullableString(json, "trusted_cert_sha256");
                    int versionCode = json.optInt("version_code", 0);
                    String versionName = optNullableString(json, "version_name");

                    final AuthSessionResult result = new AuthSessionResult(
                            token,
                            expiresIn,
                            System.currentTimeMillis(),
                            expectedSha256,
                            trustedCertSha256,
                            versionCode,
                            versionName
                    );

                    mainHandler.post(() -> callback.onAuthSuccess(result));

                } catch (Exception e) {
                    final String msg = e.getMessage() != null ? e.getMessage() : "Connection failed";
                    mainHandler.post(() -> callback.onAuthFailure("Network error during authentication: " + msg, -1));
                } finally {
                    if (conn != null) {
                        try { conn.disconnect(); } catch (Exception ignored) {}
                    }
                }
            }
        });
    }

    private static String extractRecognizedToken(JSONObject json) {
        if (json == null) return null;
        String[] tokenKeys = new String[]{"token", "temporary_token", "temp_token", "access_token"};
        for (String key : tokenKeys) {
            String candidate = optNullableString(json, key);
            if (candidate != null && !candidate.isEmpty()) {
                return candidate;
            }
        }
        JSONObject dataObj = json.optJSONObject("data");
        if (dataObj != null) {
            for (String key : tokenKeys) {
                String candidate = optNullableString(dataObj, key);
                if (candidate != null && !candidate.isEmpty()) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static String optNullableString(JSONObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.isNull(key)) return null;
        String val = obj.optString(key, "").trim();
        return val.isEmpty() || "null".equalsIgnoreCase(val) ? null : val;
    }

    private static String extractServerErrorMessage(String rawBody, String fallbackMessage) {
        if (rawBody == null || rawBody.trim().isEmpty()) {
            return fallbackMessage;
        }
        try {
            JSONObject errJson = new JSONObject(rawBody);
            String err = optNullableString(errJson, "error");
            if (err != null) return err;
            String msg = optNullableString(errJson, "message");
            if (msg != null) return msg;
            String detail = optNullableString(errJson, "detail");
            if (detail != null) return detail;
        } catch (Exception ignored) {
            // Non-JSON error body
        }
        return fallbackMessage;
    }

    private static String readStreamSafely(InputStream stream) {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception ignored) {
        }
        return sb.toString().trim();
    }
}
