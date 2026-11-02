package com.alpha10.aimshot.controller;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.Looper;

import com.alpha10.aimshot.model.ReleaseManifest;
import com.alpha10.aimshot.service.ApkDownloadService;
import com.alpha10.aimshot.service.UpdateAuthService;
import com.alpha10.aimshot.util.ApkSecurityVerifier;
import com.alpha10.aimshot.util.NetworkUtil;
import com.alpha10.aimshot.util.PackageInstallerHelper;

import java.io.File;

/**
 * Coordinates the full update pipeline:
 * Network check -> Server Reachability -> Auth token -> Streamed Download ->
 * SHA-256 verification -> Certificate fingerprint check -> Installer Dispatch.
 */
public class UpdateController {

    public interface UpdateListener {
        void onStatusUpdate(String statusMessage, int progressPercent);
        void onUpdateReadyToInstall(File verifiedApk);
        void onUpdateComplete();
        void onUpdateUpToDate(int currentVersionCode);
        void onUpdateError(String errorMessage, boolean canRetry);
    }

    private final Context context;
    private final UpdateAuthService authService;
    private final ApkDownloadService downloadService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ReleaseManifest activeManifest;
    private boolean isRunning = false;

    public UpdateController(Context context) {
        this.context = context.getApplicationContext();
        this.authService = new UpdateAuthService();
        this.downloadService = new ApkDownloadService();
    }

    public synchronized boolean isRunning() {
        return isRunning;
    }

    public void cancel() {
        downloadService.cancelDownload();
        isRunning = false;
    }

    public int getInstalledVersionCode() {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionCode;
        } catch (Exception e) {
            return 1;
        }
    }

    public String getInstalledVersionName() {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    /**
     * Checks if an update is available against the established Release Manifest.
     */
    public void checkForUpdates(final ReleaseManifest manifest, final UpdateListener listener) {
        this.activeManifest = manifest != null ? manifest : getAuthoritativeReleaseManifest();
        int currentCode = getInstalledVersionCode();

        if (activeManifest.getVersionCode() <= currentCode) {
            if (listener != null) {
                listener.onUpdateUpToDate(currentCode);
            }
        } else {
            if (listener != null) {
                listener.onStatusUpdate("New version v" + activeManifest.getVersionName() + " available", 0);
            }
        }
    }

    /**
     * Starts the full secure update flow.
     */
    public void startUpdatePipeline(final Activity activity, final UpdateListener listener) {
        if (isRunning) return;
        isRunning = true;

        if (activeManifest == null) {
            activeManifest = getAuthoritativeReleaseManifest();
        }

        // Step 1: Network & Server Reachability Check
        if (listener != null) listener.onStatusUpdate("Verifying network connectivity...", 5);

        if (!NetworkUtil.isNetworkConnected(context)) {
            isRunning = false;
            if (listener != null) listener.onUpdateError("No internet connection available. Please connect and retry.", true);
            return;
        }

        NetworkUtil.verifyServerReachability(context, NetworkUtil.ALPHA10_BASE_URL, new NetworkUtil.ConnectivityCallback() {
            @Override
            public void onResult(boolean isConnected) {
                if (!isConnected) {
                    isRunning = false;
                    if (listener != null) listener.onUpdateError("Unable to reach Alpha10 server at alpha10.freedev.app", true);
                    return;
                }

                // Step 2: Request temporary download token
                if (listener != null) listener.onStatusUpdate("Authenticating with Alpha10 server...", 15);

                authService.authenticate(new UpdateAuthService.AuthCallback() {
                    @Override
                    public void onAuthSuccess(String temporaryToken, long expiresInSeconds) {
                        // Step 3: Stream APK to private internal storage
                        if (listener != null) listener.onStatusUpdate("Connecting to secure APK stream...", 20);

                        downloadService.downloadApk(context, temporaryToken, activeManifest.getVersionCode(), new ApkDownloadService.DownloadCallback() {
                            @Override
                            public void onDownloadProgress(long bytesDownloaded, long totalBytes, int percent) {
                                int mappedPercent = 20 + (int) (percent * 0.55); // 20% to 75%
                                if (listener != null) {
                                    listener.onStatusUpdate("Downloading update: " + percent + "%", mappedPercent);
                                }
                            }

                            @Override
                            public void onDownloadComplete(File stagedApkFile) {
                                // Step 4: Cryptographic SHA-256 and Certificate Verification
                                if (listener != null) listener.onStatusUpdate("Verifying SHA-256 and Release Certificate...", 85);

                                ApkSecurityVerifier.VerificationResult verifyResult =
                                        ApkSecurityVerifier.verifyApk(context, stagedApkFile, activeManifest);

                                if (!verifyResult.isSuccess()) {
                                    // Security check failed -> quarantine / delete immediately
                                    if (stagedApkFile.exists()) stagedApkFile.delete();
                                    isRunning = false;
                                    if (listener != null) listener.onUpdateError("Security verification failed: " + verifyResult.getErrorMessage(), false);
                                    return;
                                }

                                // Step 5: Atomically promote staged .tmp file to validated .apk
                                File finalApkFile = new File(stagedApkFile.getParentFile(), "AimShot_v" + activeManifest.getVersionCode() + ".apk");
                                if (finalApkFile.exists()) finalApkFile.delete();

                                boolean renamed = stagedApkFile.renameTo(finalApkFile);
                                File targetApk = renamed ? finalApkFile : stagedApkFile;

                                if (listener != null) listener.onStatusUpdate("Verification passed! Preparing installer...", 95);

                                // Step 6: Dispatch to Package Installer
                                isRunning = false;
                                if (listener != null) {
                                    listener.onUpdateReadyToInstall(targetApk);
                                }

                                // Dispatch installation
                                if (activity != null) {
                                    if (!PackageInstallerHelper.canInstallPackages(activity)) {
                                        PackageInstallerHelper.requestInstallPermission(activity);
                                    } else {
                                        PackageInstallerHelper.installApk(activity, targetApk);
                                    }
                                } else {
                                    PackageInstallerHelper.installApk(context, targetApk);
                                }

                                if (listener != null) listener.onUpdateComplete();
                            }

                            @Override
                            public void onDownloadError(String errorMessage, int httpCode) {
                                isRunning = false;
                                if (listener != null) listener.onUpdateError(errorMessage, true);
                            }
                        });
                    }

                    @Override
                    public void onAuthFailure(String errorMessage, int httpCode) {
                        isRunning = false;
                        if (listener != null) listener.onUpdateError(errorMessage, true);
                    }
                });
            }
        });
    }

    /**
     * Authoritative Release Manifest matching GitHub release asset and permanent certificate identity.
     */
    public ReleaseManifest getAuthoritativeReleaseManifest() {
        return new ReleaseManifest(
                2,
                "1.0.1",
                "https://alpha10.freedev.app/api/aimshot-download.php",
                null, // Manifest will verify certificate and version; SHA-256 validated when provided
                ReleaseManifest.PERMANENT_CERT_SHA256,
                "AimShot Engine 1.0.1: Enhanced Guideline Engine, Secure Token Downloads, and Tactical HUD Stability.",
                28500000L
        );
    }
}
