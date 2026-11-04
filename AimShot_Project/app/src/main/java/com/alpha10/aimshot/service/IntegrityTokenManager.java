package com.alpha10.aimshot.service;

import android.content.Context;
import android.util.Base64;

import com.google.android.play.core.integrity.IntegrityManager;
import com.google.android.play.core.integrity.IntegrityManagerFactory;
import com.google.android.play.core.integrity.IntegrityTokenRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Generates cryptographic nonces and requests Google Play Integrity API attestation tokens
 * so client authenticity is verified server-side without embedding any 64-character Secret Key in the APK.
 */
public class IntegrityTokenManager {

    /**
     * Optional Google Cloud Project Number for Play Integrity verification.
     * Leave 0L until linked in Google Play Console / Cloud Console; when > 0L, hardware-backed
     * Play Integrity token retrieval executes automatically before broker authentication.
     */
    public static final long GOOGLE_CLOUD_PROJECT_NUMBER = 0L;

    public interface AttestationCallback {
        void onAttestationReady(String nonce, String integrityToken);
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static void requestAttestation(final Context context,
                                          final String deviceId,
                                          final AttestationCallback callback) {
        if (callback == null) return;

        final String nonce = generateSafeNonce(context, deviceId);

        if (context == null || GOOGLE_CLOUD_PROJECT_NUMBER <= 0L) {
            // Play Integrity Cloud Project Number not yet bound; supply cryptographic nonce
            // for server-side license & session broker verification.
            callback.onAttestationReady(nonce, null);
            return;
        }

        try {
            IntegrityManager integrityManager = IntegrityManagerFactory.create(context.getApplicationContext());
            IntegrityTokenRequest request = IntegrityTokenRequest.builder()
                    .setCloudProjectNumber(GOOGLE_CLOUD_PROJECT_NUMBER)
                    .setNonce(nonce)
                    .build();

            integrityManager.requestIntegrityToken(request)
                    .addOnSuccessListener(response -> {
                        String token = response != null ? response.token() : null;
                        callback.onAttestationReady(nonce, token);
                    })
                    .addOnFailureListener(e -> {
                        // Deliver nonce so server broker policy decides whether to reject or allow
                        callback.onAttestationReady(nonce, null);
                    });
        } catch (Throwable t) {
            callback.onAttestationReady(nonce, null);
        }
    }

    /**
     * Generates a Play-Integrity-compliant URL-safe Base64 nonce (>= 16 bytes, no padding/wrapping).
     */
    public static String generateSafeNonce(Context context, String deviceId) {
        try {
            byte[] randomBytes = new byte[16];
            SECURE_RANDOM.nextBytes(randomBytes);

            String pkg = context != null ? context.getPackageName() : "com.alpha10.aimshot.launcher";
            String seed = pkg + "|" + (deviceId != null ? deviceId : "unknown") + "|" + System.currentTimeMillis();

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(seed.getBytes(StandardCharsets.UTF_8));
            digest.update(randomBytes);
            byte[] hash = digest.digest();

            return Base64.encodeToString(hash, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
        } catch (Exception e) {
            byte[] fallback = new byte[24];
            SECURE_RANDOM.nextBytes(fallback);
            return Base64.encodeToString(fallback, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
        }
    }
}
