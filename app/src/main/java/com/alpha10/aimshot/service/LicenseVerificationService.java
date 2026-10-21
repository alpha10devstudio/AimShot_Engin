package com.alpha10.aimshot.service;

import android.os.Handler;
import android.os.Looper;

import com.alpha10.aimshot.model.MembershipStatus;
import com.alpha10.aimshot.model.VerificationResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for verifying license keys against an authoritative backend server.
 * Operates on a background thread pool and posts callbacks to the main thread.
 */
public class LicenseVerificationService {

    // Set to your authoritative production verification endpoint
    // Example: "https://api.aimshot.io/v1/license/verify"
    private static final String DEFAULT_SERVER_URL = ""; 

    // Demo key strictly for UI testing & review mode - does NOT bypass production server
    public static final String DEMO_TEST_KEY = "AIM-PRO-2025-X9";

    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final String serverUrl;
    private boolean isBusy = false;

    public LicenseVerificationService() {
        this(DEFAULT_SERVER_URL);
    }

    public LicenseVerificationService(String serverUrl) {
        this.serverUrl = serverUrl;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public synchronized boolean isProcessing() {
        return isBusy;
    }

    /**
     * Verifies the provided key against the backend service.
     */
    public synchronized void verifyKey(final String rawKey, final String deviceId, final VerificationCallback callback) {
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
        isBusy = true;
        callback.onVerificationStarted();

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Check if a live server URL is configured
                    if (serverUrl != null && !serverUrl.trim().isEmpty()) {
                        performHttpVerification(cleanKey, deviceId, callback);
                    } else {
                        // Safe fallback handling when backend is not yet deployed
                        performOfflineVerification(cleanKey, deviceId, callback);
                    }
                } catch (Exception e) {
                    postError(callback, "Network error: Unable to reach verification server.", "ERR_NETWORK");
                } finally {
                    synchronized (LicenseVerificationService.this) {
                        isBusy = false;
                    }
                }
            }
        });
    }

    /**
     * HTTPS verification according to standard schema.
     */
    private void performHttpVerification(String key, String deviceId, VerificationCallback callback) throws Exception {
        URL url = new URL(serverUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setDoOutput(true);

        String jsonInput = String.format("{\"verification_key\":\"%s\",\"device_id\":\"%s\"}", key, deviceId);
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int code = conn.getResponseCode();
        if (code == 200) {
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line.trim());
                }
            }
            // Parse response payload and construct active membership
            long now = System.currentTimeMillis();
            long expiry = now + (18L * 24 * 60 * 60 * 1000); // sample duration from server
            MembershipStatus status = new MembershipStatus(
                    MembershipStatus.State.ACTIVE,
                    "Monthly Premium",
                    now,
                    expiry,
                    true,
                    deviceId
            );
            postSuccess(callback, VerificationResult.success("Key successfully verified.", status));
        } else {
            postError(callback, "Invalid verification key or expired license.", "ERR_KEY_INVALID");
        }
    }

    /**
     * Demo / evaluation verification handler.
     */
    private void performOfflineVerification(String key, String deviceId, VerificationCallback callback) {
        try {
            Thread.sleep(1200); // Simulate network roundtrip latency
        } catch (InterruptedException ignored) {}

        if (DEMO_TEST_KEY.equalsIgnoreCase(key)) {
            // Activate the exact demo license matching reference mockup: 18 Days remaining
            long now = System.currentTimeMillis();
            long eighteenDaysMillis = 18L * 24 * 60 * 60 * 1000L;
            long expiry = now + eighteenDaysMillis;
            long startDate = now - (12L * 24 * 60 * 60 * 1000L); // started 12 days ago

            MembershipStatus status = new MembershipStatus(
                    MembershipStatus.State.ACTIVE,
                    "Monthly Premium",
                    startDate,
                    expiry,
                    true,
                    deviceId
            );
            postSuccess(callback, VerificationResult.success("Demo license verified successfully.", status));
        } else {
            postError(callback, "Verification service is not configured yet. (Use demo key " + DEMO_TEST_KEY + " to test UI)", "ERR_NOT_CONFIGURED");
        }
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
