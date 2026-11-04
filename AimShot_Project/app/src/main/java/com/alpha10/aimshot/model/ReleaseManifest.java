package com.alpha10.aimshot.model;

/**
 * Release Manifest encapsulating version data, optional server hashes,
 * and target package identity for Main AimShot Engine ("com.alpha10.aimshot").
 */
public class ReleaseManifest {

    // Launcher & Server release certificate fingerprints
    public static final String PERMANENT_CERT_SHA256 =
            "1C:8D:56:83:62:EE:D9:04:02:36:43:0D:AC:8D:1F:BE:30:06:0E:9F:FD:2D:F4:01:AB:69:04:E7:94:86:A5:A2";

    public static final String LIVE_ENGINE_CERT_SHA256 =
            "A4:0D:A8:0A:59:D1:70:CA:A9:50:CF:15:C1:8C:45:4D:47:A3:9B:26:98:9D:8B:64:0E:CD:74:5B:A7:1B:F5:DC";

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
        this.trustedCertFingerprint = trustedCertFingerprint;
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
