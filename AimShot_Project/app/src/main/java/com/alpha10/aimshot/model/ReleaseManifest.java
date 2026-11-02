package com.alpha10.aimshot.model;

/**
 * Authoritative Release Manifest encapsulating version data, cryptographic hashes,
 * and permanent signing identity for AimShot Engine updates.
 */
public class ReleaseManifest {

    // Established permanent release certificate SHA-256 fingerprint
    public static final String PERMANENT_CERT_SHA256 =
            "1C:8D:56:83:62:EE:D9:04:02:36:43:0D:AC:8D:1F:BE:30:06:0E:9F:FD:2D:F4:01:AB:69:04:E7:94:86:A5:A2";

    public static final String EXPECTED_PACKAGE_NAME = "com.alpha10.aimshot";

    private final int versionCode;
    private final String versionName;
    private final String downloadUrl;
    private final String expectedSha256;
    private final String trustedCertFingerprint;
    private final String releaseNotes;
    private final long fileSizeBytes;

    public ReleaseManifest(int versionCode, String versionName, String downloadUrl,
                           String expectedSha256, String trustedCertFingerprint,
                           String releaseNotes, long fileSizeBytes) {
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.downloadUrl = downloadUrl;
        this.expectedSha256 = expectedSha256;
        this.trustedCertFingerprint = trustedCertFingerprint != null ? trustedCertFingerprint : PERMANENT_CERT_SHA256;
        this.releaseNotes = releaseNotes;
        this.fileSizeBytes = fileSizeBytes;
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

    public String getExpectedSha256() {
        return expectedSha256;
    }

    public String getTrustedCertFingerprint() {
        return trustedCertFingerprint;
    }

    public String getReleaseNotes() {
        return releaseNotes;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }
}
