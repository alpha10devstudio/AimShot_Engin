package com.alpha10.aimshot.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.alpha10.aimshot.BuildConfig;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Direct Alpha10 Website APK Authentication Service:
 * - Authenticates directly against POST https://alpha10.freedev.app/api/auth.php
 *   using the 64-character X-App-Key secret header.
 * - Automatically solves InfinityFree / ByetHost slowAES (aes.js "__test" cookie) anti-bot
 *   HTML challenges using standard AES/CBC/NoPadding decryption so Android clients receive
 *   clean JSON and can stream the APK binary directly without WebView or external brokers.
 */
public class UpdateAuthService {

    public static final String AUTH_ENDPOINT = "https://alpha10.freedev.app/api/auth.php";
    public static final String SECRET_APP_KEY =
            "856b5c17909bcab591c7a41f3eb427296210e007889b2a595817a26596c62a10";

    public static final String BROWSER_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36";

    private static final Pattern AES_HEX_PATTERN =
            Pattern.compile("toNumbers\\(\\s*\"([0-9a-fA-F]{32})\"\\s*\\)");

    private static volatile String cachedSessionCookie = null;

    public static String getCachedSessionCookie() {
        return cachedSessionCookie;
    }

    public static void setCachedSessionCookie(String cookie) {
        if (cookie != null && !cookie.trim().isEmpty()) {
            cachedSessionCookie = cookie.trim();
        }
    }

    public static class AuthSessionResult {
        private final String temporaryToken;
        private final long expiresInSeconds;
        private final long issuedAtTimestamp;
        private final String expectedSha256;
        private final String trustedCertSha256;
        private final int versionCode;
        private final String versionName;
        private final String downloadUrl;
        private final String sessionCookie;

        public AuthSessionResult(String temporaryToken,
                                 long expiresInSeconds,
                                 long issuedAtTimestamp,
                                 String expectedSha256,
                                 String trustedCertSha256,
                                 int versionCode,
                                 String versionName,
                                 String downloadUrl,
                                 String sessionCookie) {
            this.temporaryToken = temporaryToken;
            this.expiresInSeconds = expiresInSeconds;
            this.issuedAtTimestamp = issuedAtTimestamp;
            this.expectedSha256 = expectedSha256;
            this.trustedCertSha256 = trustedCertSha256;
            this.versionCode = versionCode;
            this.versionName = versionName;
            this.downloadUrl = downloadUrl;
            this.sessionCookie = sessionCookie;
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

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public String getSessionCookie() {
            return sessionCookie;
        }
    }

    public interface AuthCallback {
        void onAuthSuccess(AuthSessionResult sessionResult);
        void onAuthFailure(String errorMessage, int httpCode);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final String endpointUrl;
    private final String appKey;

    public UpdateAuthService() {
        this(resolveDefaultAuthEndpoint(), resolveDefaultSecretKey());
    }

    public UpdateAuthService(String endpointUrl) {
        this(endpointUrl, resolveDefaultSecretKey());
    }

    public UpdateAuthService(String endpointUrl, String appKey) {
        this.endpointUrl = (endpointUrl != null && !endpointUrl.trim().isEmpty())
                ? endpointUrl.trim()
                : resolveDefaultAuthEndpoint();
        this.appKey = (appKey != null && !appKey.trim().isEmpty())
                ? appKey.trim()
                : resolveDefaultSecretKey();
    }

    private static String resolveDefaultAuthEndpoint() {
        try {
            String configured = BuildConfig.ALPHA10_AUTH_URL;
            if (configured != null && !configured.trim().isEmpty()
                    && !configured.contains("launcher-auth.php")) {
                return configured.trim();
            }
        } catch (Throwable ignored) {
        }
        return AUTH_ENDPOINT;
    }

    private static String resolveDefaultSecretKey() {
        try {
            String configuredKey = BuildConfig.ALPHA10_SECRET_APP_KEY;
            if (configuredKey != null && configuredKey.trim().length() == 64) {
                return configuredKey.trim();
            }
        } catch (Throwable ignored) {
        }
        return SECRET_APP_KEY;
    }

    /**
     * Authenticates directly with https://alpha10.freedev.app/api/auth.php using X-App-Key,
     * automatically solving any InfinityFree aes.js (__test cookie) challenge.
     */
    public void authenticate(final Context context, final AuthCallback callback) {
        if (callback == null) return;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String activeCookie = cachedSessionCookie;
                    RawHttpResponse firstAttempt = performPostAuth(endpointUrl, appKey, activeCookie);

                    // If hosting returned aes.js browser challenge, solve __test cookie and retry immediately
                    if (isAntiBotChallenge(firstAttempt.body)) {
                        String solvedCookie = solveAntiBotCookie(firstAttempt.body);
                        if (solvedCookie != null && !solvedCookie.isEmpty()) {
                            activeCookie = solvedCookie;
                            cachedSessionCookie = solvedCookie;
                            firstAttempt = performPostAuth(endpointUrl, appKey, activeCookie);
                        }
                    } else if (firstAttempt.setCookie != null && !firstAttempt.setCookie.isEmpty()) {
                        activeCookie = firstAttempt.setCookie;
                        cachedSessionCookie = activeCookie;
                    }

                    final int responseCode = firstAttempt.httpCode;
                    final String rawResponseBody = firstAttempt.body;
                    final String sanitizedBody = sanitizeJsonPayload(rawResponseBody);

                    if (responseCode != 200) {
                        final String fallbackHttpErr = "Server authentication rejected (HTTP " + responseCode + ")";
                        final String serverError = extractServerErrorMessage(sanitizedBody, rawResponseBody, fallbackHttpErr);
                        mainHandler.post(() -> callback.onAuthFailure(serverError, responseCode));
                        return;
                    }

                    if (sanitizedBody.isEmpty()) {
                        mainHandler.post(() -> callback.onAuthFailure("Empty response received from authentication server.", responseCode));
                        return;
                    }

                    JSONObject json;
                    try {
                        json = new JSONObject(sanitizedBody);
                    } catch (Exception parseEx) {
                        final String diagnostic = diagnoseNonJsonResponse(rawResponseBody, responseCode);
                        mainHandler.post(() -> callback.onAuthFailure(diagnostic, responseCode));
                        return;
                    }

                    if (json.has("success") && !json.optBoolean("success", true)) {
                        final String explicitErr = extractServerErrorMessage(sanitizedBody, rawResponseBody,
                                "Server reported authentication failure.");
                        mainHandler.post(() -> callback.onAuthFailure(explicitErr, responseCode));
                        return;
                    }
                    if ("error".equalsIgnoreCase(json.optString("status", ""))) {
                        final String explicitErr = extractServerErrorMessage(sanitizedBody, rawResponseBody,
                                "Server returned error status.");
                        mainHandler.post(() -> callback.onAuthFailure(explicitErr, responseCode));
                        return;
                    }

                    String token = extractRecognizedToken(json);
                    if (token == null || token.isEmpty()) {
                        final String detailErr = extractServerErrorMessage(sanitizedBody, rawResponseBody,
                                "Authentication response did not contain a download token.");
                        mainHandler.post(() -> callback.onAuthFailure(detailErr, responseCode));
                        return;
                    }

                    long expiresIn = json.optLong("expires_in", 60L);
                    if (expiresIn <= 0L) expiresIn = 60L;

                    String expectedSha256 = extractMetadataField(json, new String[]{"expected_sha256", "sha256", "apk_sha256"});
                    if (expectedSha256 == null) {
                        expectedSha256 = firstAttempt.expectedShaHeader;
                    }

                    String trustedCertSha256 = extractMetadataField(json, new String[]{"trusted_cert_sha256", "cert_sha256"});
                    if (trustedCertSha256 == null) {
                        trustedCertSha256 = firstAttempt.trustedCertHeader;
                    }

                    int versionCode = json.optInt("version_code", 0);
                    if (versionCode <= 0 && json.optJSONObject("data") != null) {
                        versionCode = json.optJSONObject("data").optInt("version_code", 0);
                    }
                    String versionName = extractMetadataField(json, new String[]{"version_name", "version"});
                    String downloadUrl = extractMetadataField(json, new String[]{"download_url", "apk_url", "url"});

                    final AuthSessionResult result = new AuthSessionResult(
                            token,
                            expiresIn,
                            System.currentTimeMillis(),
                            expectedSha256,
                            trustedCertSha256,
                            versionCode,
                            versionName,
                            downloadUrl,
                            activeCookie
                    );

                    mainHandler.post(() -> callback.onAuthSuccess(result));

                } catch (Exception e) {
                    final String msg = e.getMessage() != null ? e.getMessage() : "Connection failed";
                    mainHandler.post(() -> callback.onAuthFailure("Network error during authentication: " + msg, -1));
                }
            }
        });
    }

    private static class RawHttpResponse {
        final int httpCode;
        final String body;
        final String setCookie;
        final String expectedShaHeader;
        final String trustedCertHeader;

        RawHttpResponse(int httpCode, String body, String setCookie, String expectedShaHeader, String trustedCertHeader) {
            this.httpCode = httpCode;
            this.body = body != null ? body : "";
            this.setCookie = setCookie;
            this.expectedShaHeader = expectedShaHeader;
            this.trustedCertHeader = trustedCertHeader;
        }
    }

    private static RawHttpResponse performPostAuth(String targetUrl, String secretKey, String cookieHeader) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(targetUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("User-Agent", BROWSER_USER_AGENT);
            conn.setRequestProperty("X-App-Key", secretKey);
            conn.setRequestProperty("Accept", "application/json, text/plain, */*");
            conn.setRequestProperty("Cache-Control", "no-cache");
            if (cookieHeader != null && !cookieHeader.trim().isEmpty()) {
                conn.setRequestProperty("Cookie", cookieHeader.trim());
            }
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(12000);
            conn.setDoOutput(true);
            conn.setFixedLengthStreamingMode(0);

            OutputStream os = conn.getOutputStream();
            os.flush();
            os.close();

            int code = conn.getResponseCode();
            InputStream stream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            String body = readStreamSafely(stream);

            String extractedCookie = extractCookieHeader(conn.getHeaderFields());
            String shaHeader = cleanHexHeader(conn.getHeaderField("X-APK-SHA256"));
            if (shaHeader == null) {
                shaHeader = cleanHexHeader(conn.getHeaderField("X-Expected-Sha256"));
            }
            String certHeader = conn.getHeaderField("X-Trusted-Cert-Sha256");
            if (certHeader != null) {
                certHeader = certHeader.trim();
                if (certHeader.isEmpty()) certHeader = null;
            }

            return new RawHttpResponse(code, body, extractedCookie, shaHeader, certHeader);
        } finally {
            if (conn != null) {
                try { conn.disconnect(); } catch (Exception ignored) {}
            }
        }
    }

    public static boolean isAntiBotChallenge(String body) {
        if (body == null || body.isEmpty()) return false;
        String lower = body.toLowerCase(Locale.US);
        return (lower.contains("aes.js") || lower.contains("slowaes") || lower.contains("__test="))
                && lower.contains("tonumbers(");
    }

    /**
     * Solves InfinityFree / ByetHost slowAES.decrypt(c, 2, a, b) challenge directly in Java
     * using AES/CBC/NoPadding and returns "__test=<32-char lowercase hex>".
     */
    public static String solveAntiBotCookie(String html) {
        if (html == null || html.isEmpty()) return null;
        try {
            Matcher matcher = AES_HEX_PATTERN.matcher(html);
            String keyHex = matcher.find() ? matcher.group(1) : null;
            String ivHex = matcher.find() ? matcher.group(1) : null;
            String cipherHex = matcher.find() ? matcher.group(1) : null;

            if (keyHex == null || ivHex == null || cipherHex == null) {
                return null;
            }

            byte[] keyBytes = hexToBytes(keyHex);
            byte[] ivBytes = hexToBytes(ivHex);
            byte[] cipherBytes = hexToBytes(cipherHex);

            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);
            byte[] decrypted = cipher.doFinal(cipherBytes);

            return "__test=" + bytesToLowerHex(decrypted);
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(hex.charAt(i), 16);
            int lo = Character.digit(hex.charAt(i + 1), 16);
            out[i / 2] = (byte) ((hi << 4) + lo);
        }
        return out;
    }

    private static String bytesToLowerHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format(Locale.US, "%02x", b & 0xff));
        }
        return sb.toString();
    }

    private static String extractCookieHeader(Map<String, List<String>> headers) {
        if (headers == null) return null;
        StringBuilder cookieBuilder = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() != null && "Set-Cookie".equalsIgnoreCase(entry.getKey()) && entry.getValue() != null) {
                for (String rawCookie : entry.getValue()) {
                    if (rawCookie == null) continue;
                    int semi = rawCookie.indexOf(';');
                    String pair = (semi > 0 ? rawCookie.substring(0, semi) : rawCookie).trim();
                    if (!pair.isEmpty()) {
                        if (cookieBuilder.length() > 0) cookieBuilder.append("; ");
                        cookieBuilder.append(pair);
                    }
                }
            }
        }
        return cookieBuilder.length() > 0 ? cookieBuilder.toString() : null;
    }

    public static String sanitizeJsonPayload(String raw) {
        if (raw == null) return "";
        String cleaned = raw.replace("\uFEFF", "").trim();
        if (cleaned.isEmpty()) return "";

        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            String candidate = cleaned.substring(firstBrace, lastBrace + 1);
            try {
                new JSONObject(candidate);
                return candidate;
            } catch (Exception ignored) {
            }
        }
        return cleaned;
    }

    public static String diagnoseNonJsonResponse(String rawBody, int httpCode) {
        if (rawBody == null || rawBody.trim().isEmpty()) {
            return "Empty response received from authentication server (HTTP " + httpCode + ").";
        }
        String lower = rawBody.toLowerCase(Locale.US);
        if (lower.contains("aes.js") || lower.contains("slowaes") || lower.contains("__test=")) {
            return "Hosting security check (aes.js) interrupted response. Please tap Retry.";
        }
        if (lower.contains("404") || lower.contains("not found") || lower.contains("page not found")) {
            return "Authentication endpoint /api/auth.php was not found (HTTP 404).";
        }
        if (lower.contains("open_basedir") || lower.contains("fatal error") || lower.contains("parse error")) {
            return "Server PHP error in /api/auth.php prevented JSON output.";
        }
        return "Invalid JSON format in authentication response.";
    }

    private static String extractRecognizedToken(JSONObject json) {
        if (json == null) return null;
        String[] tokenKeys = new String[]{
                "download_token",
                "token",
                "temporary_token",
                "temp_token",
                "access_token",
                "auth_token"
        };
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

    private static String extractMetadataField(JSONObject json, String[] keys) {
        if (json == null || keys == null) return null;
        for (String key : keys) {
            String val = optNullableString(json, key);
            if (val != null) return val;
        }
        JSONObject dataObj = json.optJSONObject("data");
        if (dataObj != null) {
            for (String key : keys) {
                String val = optNullableString(dataObj, key);
                if (val != null) return val;
            }
        }
        return null;
    }

    private static String cleanHexHeader(String headerVal) {
        if (headerVal == null) return null;
        String trimmed = headerVal.trim();
        return trimmed.matches("^[0-9a-fA-F]{64}$") ? trimmed.toLowerCase(Locale.US) : null;
    }

    private static String optNullableString(JSONObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.isNull(key)) return null;
        String val = obj.optString(key, "").trim();
        return val.isEmpty() || "null".equalsIgnoreCase(val) ? null : val;
    }

    private static String extractServerErrorMessage(String sanitizedBody, String rawBody, String fallbackMessage) {
        if (sanitizedBody != null && !sanitizedBody.isEmpty()) {
            try {
                JSONObject errJson = new JSONObject(sanitizedBody);
                String err = optNullableString(errJson, "error");
                if (err != null) return err;
                String msg = optNullableString(errJson, "message");
                if (msg != null) return msg;
                String detail = optNullableString(errJson, "detail");
                if (detail != null) return detail;
            } catch (Exception ignored) {
            }
        }
        if (rawBody != null && !rawBody.trim().isEmpty()) {
            String lower = rawBody.toLowerCase(Locale.US);
            if (lower.contains("aes.js") || lower.contains("slowaes")) {
                return "Server hosting security filter (aes.js) challenged request. Please retry.";
            }
        }
        return fallbackMessage;
    }

    private static String readStreamSafely(InputStream stream) {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(line);
            }
        } catch (Exception ignored) {
        }
        return sb.toString().trim();
    }
}
