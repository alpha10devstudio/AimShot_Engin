package com.alpha10.aimshot.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;

import com.alpha10.aimshot.model.ReleaseManifest;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

/**
 * Performs strict, zero-bypass cryptographic & structural verification on downloaded APK files:
 * 1. Non-empty file & valid ZIP/APK magic header (PK\x03\x04)
 * 2. Android PackageManager archive parsing & exact Package ID verification ("com.alpha10.aimshot")
 * 3. Mandatory Trusted SHA-256 binary digest verification (blocks if hash is missing or mismatched)
 * 4. Mandatory Trusted X.509 Release Signing Certificate SHA-256 verification (zero debug/exception bypass)
 */
public class ApkSecurityVerifier {

    public static class VerificationResult {
        private final boolean success;
        private final String errorMessage;
        private final String calculatedSha256;
        private final String extractedCertFingerprint;
        private final int parsedVersionCode;
        private final String parsedVersionName;

        public VerificationResult(boolean success,
                                  String errorMessage,
                                  String calculatedSha256,
                                  String extractedCertFingerprint,
                                  int parsedVersionCode,
                                  String parsedVersionName) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.calculatedSha256 = calculatedSha256;
            this.extractedCertFingerprint = extractedCertFingerprint;
            this.parsedVersionCode = parsedVersionCode;
            this.parsedVersionName = parsedVersionName;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getCalculatedSha256() {
            return calculatedSha256;
        }

        public String getExtractedCertFingerprint() {
            return extractedCertFingerprint;
        }

        public int getParsedVersionCode() {
            return parsedVersionCode;
        }

        public String getParsedVersionName() {
            return parsedVersionName;
        }
    }

    /**
     * Executes strict 4-tier verification on the staged APK in private internal storage.
     */
    public static VerificationResult verifyApk(Context context, File apkFile, ReleaseManifest manifest) {
        if (context == null) {
            return new VerificationResult(false, "Context unavailable for APK verification.", null, null, 0, null);
        }
        if (apkFile == null || !apkFile.exists() || apkFile.length() == 0) {
            return new VerificationResult(false, "Downloaded APK file does not exist or is empty.", null, null, 0, null);
        }
        if (!hasZipMagicBytes(apkFile)) {
            return new VerificationResult(false, "Downloaded file is not a valid Android APK archive.", null, null, 0, null);
        }

        // 1. Calculate actual SHA-256 digest of the downloaded binary
        String calculatedSha256;
        try {
            calculatedSha256 = calculateFileSha256(apkFile);
        } catch (Exception e) {
            return new VerificationResult(false, "Failed to compute SHA-256 digest: " + e.getMessage(), null, null, 0, null);
        }

        // 2. Enforce mandatory Trusted SHA-256 comparison (never skip when expectedSha256 is null/missing)
        if (manifest == null || manifest.getExpectedSha256() == null || manifest.getExpectedSha256().trim().isEmpty()) {
            return new VerificationResult(
                    false,
                    "Trusted SHA-256 digest is not configured on the server manifest. Installation blocked for security.",
                    calculatedSha256,
                    null,
                    0,
                    null
            );
        }

        String cleanExpectedSha = cleanHex(manifest.getExpectedSha256());
        String cleanCalculatedSha = cleanHex(calculatedSha256);
        if (cleanExpectedSha.length() != 64 || !isHex64(cleanExpectedSha)) {
            return new VerificationResult(
                    false,
                    "Invalid Trusted SHA-256 format received from release manifest.",
                    calculatedSha256,
                    null,
                    0,
                    null
            );
        }

        if (!cleanCalculatedSha.equalsIgnoreCase(cleanExpectedSha)) {
            return new VerificationResult(
                    false,
                    "SHA-256 integrity mismatch! APK may be corrupted or tampered.",
                    calculatedSha256,
                    null,
                    0,
                    null
            );
        }

        // 3. Parse APK structure & package identity via Android PackageManager (zero ZipFile bypass)
        PackageManager pm = context.getPackageManager();
        int flags = PackageManager.GET_META_DATA;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            flags |= PackageManager.GET_SIGNING_CERTIFICATES;
        } else {
            flags |= PackageManager.GET_SIGNATURES;
        }

        PackageInfo packageInfo = pm.getPackageArchiveInfo(apkFile.getAbsolutePath(), flags);
        if (packageInfo == null) {
            return new VerificationResult(
                    false,
                    "APK structure is invalid or unreadable by Android PackageManager.",
                    calculatedSha256,
                    null,
                    0,
                    null
            );
        }

        if (packageInfo.applicationInfo != null) {
            packageInfo.applicationInfo.sourceDir = apkFile.getAbsolutePath();
            packageInfo.applicationInfo.publicSourceDir = apkFile.getAbsolutePath();
        }

        // Reject if package name is not strictly "com.alpha10.aimshot" or matches Launcher's own package
        if (!ReleaseManifest.EXPECTED_PACKAGE_NAME.equals(packageInfo.packageName)) {
            return new VerificationResult(
                    false,
                    "Package ID mismatch! Expected " + ReleaseManifest.EXPECTED_PACKAGE_NAME + " but found " + packageInfo.packageName,
                    calculatedSha256,
                    null,
                    0,
                    null
            );
        }

        if (context.getPackageName().equals(packageInfo.packageName)) {
            return new VerificationResult(
                    false,
                    "Downloaded APK shares Launcher package ID instead of standalone Main AimShot Engine.",
                    calculatedSha256,
                    null,
                    0,
                    null
            );
        }

        int parsedVersionCode = packageInfo.versionCode;
        String parsedVersionName = packageInfo.versionName;

        // 4. Strictly extract and verify Release Signing Certificate SHA-256 (zero fallback, zero debug bypass)
        String certFingerprint;
        try {
            certFingerprint = extractSigningCertFingerprint(packageInfo);
        } catch (Exception e) {
            return new VerificationResult(
                    false,
                    "Unable to verify APK signing certificate: " + e.getMessage(),
                    calculatedSha256,
                    null,
                    parsedVersionCode,
                    parsedVersionName
            );
        }

        String expectedFingerprint = manifest.getTrustedCertFingerprint();
        if (expectedFingerprint == null || cleanHex(expectedFingerprint).length() != 64) {
            return new VerificationResult(
                    false,
                    "Trusted signing certificate fingerprint is missing or invalid.",
                    calculatedSha256,
                    certFingerprint,
                    parsedVersionCode,
                    parsedVersionName
            );
        }

        String cleanExpectedCert = cleanHex(expectedFingerprint);
        String cleanExtractedCert = cleanHex(certFingerprint);

        if (!cleanExtractedCert.equalsIgnoreCase(cleanExpectedCert)) {
            return new VerificationResult(
                    false,
                    "Untrusted signing certificate! APK was not signed with the authorized Release Keystore.",
                    calculatedSha256,
                    certFingerprint,
                    parsedVersionCode,
                    parsedVersionName
            );
        }

        return new VerificationResult(true, null, calculatedSha256, certFingerprint, parsedVersionCode, parsedVersionName);
    }

    /**
     * Verifies that an already-installed package on the OS matches the trusted Release Signing Certificate
     * before launching it (protects against package-squatting / impersonation).
     */
    public static boolean verifyInstalledPackageCertificate(Context context, String packageName, String expectedCertSha256) {
        if (context == null || packageName == null || expectedCertSha256 == null) return false;
        String cleanExpected = cleanHex(expectedCertSha256);
        if (cleanExpected.length() != 64) return false;

        try {
            PackageManager pm = context.getPackageManager();
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    ? PackageManager.GET_SIGNING_CERTIFICATES
                    : PackageManager.GET_SIGNATURES;
            PackageInfo pkgInfo = pm.getPackageInfo(packageName, flags);
            if (pkgInfo == null) return false;

            String installedCert = extractSigningCertFingerprint(pkgInfo);
            return cleanHex(installedCert).equalsIgnoreCase(cleanExpected);
        } catch (Exception e) {
            return false;
        }
    }

    public static String calculateFileSha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder(hashBytes.length * 2);
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String extractSigningCertFingerprint(PackageInfo packageInfo) throws Exception {
        Signature[] signatures = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            SigningInfo signingInfo = packageInfo.signingInfo;
            if (signingInfo != null) {
                if (signingInfo.hasMultipleSigners()) {
                    signatures = signingInfo.getApkContentsSigners();
                } else {
                    signatures = signingInfo.getSigningCertificateHistory();
                }
            }
        } else {
            signatures = packageInfo.signatures;
        }

        if (signatures == null || signatures.length == 0 || signatures[0] == null) {
            throw new IllegalStateException("No X.509 signing certificate present in package");
        }

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] certHash = md.digest(signatures[0].toByteArray());

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < certHash.length; i++) {
            if (i > 0) sb.append(":");
            sb.append(String.format("%02X", certHash[i]));
        }
        return sb.toString();
    }

    private static boolean hasZipMagicBytes(File file) {
        if (file == null || file.length() < 4) return false;
        byte[] header = new byte[4];
        try (FileInputStream fis = new FileInputStream(file)) {
            if (fis.read(header) != 4) return false;
            return header[0] == 0x50 && header[1] == 0x4B && header[2] == 0x03 && header[3] == 0x04;
        } catch (Exception e) {
            return false;
        }
    }

    private static String cleanHex(String input) {
        if (input == null) return "";
        return input.replace(":", "").replace(" ", "").trim();
    }

    private static boolean isHex64(String hex) {
        if (hex == null || hex.length() != 64) return false;
        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            boolean valid = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!valid) return false;
        }
        return true;
    }
}
