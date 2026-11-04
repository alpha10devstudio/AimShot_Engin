package com.alpha10.aimshot.controller;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.alpha10.aimshot.BuildConfig;
import com.alpha10.aimshot.model.ReleaseManifest;
import com.alpha10.aimshot.runtime.PrivateEngineRuntime;
import com.alpha10.aimshot.runtime.PrivateEngineVault;
import com.alpha10.aimshot.service.ApkDownloadService;
import com.alpha10.aimshot.service.UpdateAuthService;
import com.alpha10.aimshot.util.ApkSecurityVerifier;
import com.alpha10.aimshot.util.NetworkUtil;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Coordinates the complete AimShot Engine download, verification, private vault provisioning,
 * and hidden in-app runtime launch flow:
 * 1. Internet & Alpha10 Website Connection Check
 * 2. Direct Authentication with https://alpha10.freedev.app/api/auth.php
 * 3. Direct APK Binary Streaming into Private Internal Storage
 * 4. APK Package ID ("com.alpha10.aimshot") & Integrity Verification
 * 5. Hidden Private Vault Storage (Read-Only DEX + ABI Native .so Extraction — Zero OS Install)
 * 6. License-Gated In-App Execution inside AimShot Launcher
 */
public class UpdateController {

    private static final String TAG = "AimShot_UpdateCtrl";

    public enum Stage {
        CHECKING_INTERNET,
        CONNECTING_SERVER,
        AUTHENTICATING,
        DOWNLOADING,
        VERIFYING_SECURITY,
        WAITING_CONFIRMATION,
        INSTALLING,
        INSTALL_SUCCESS,
        OPENING_APP,
        ERROR
    }

    public interface UpdateListener {
        void onStageChanged(Stage stage, String message, int progressPercent);
        void onDownloadProgress(long bytesDownloaded, long totalBytes, int percent);
        void onInstallationSuccess();
        void onInstallationAborted();
        void onUpdateUpToDate(int currentVersionCode);
        void onError(String errorMessage, boolean canRetry);
    }

    private final Context context;
    private final UpdateAuthService authService;
    private final ApkDownloadService downloadService;
    private final ExecutorService vaultExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ReleaseManifest activeManifest;
    private UpdateListener listener;

    private File pendingVerifiedApk = null;
    private ApkSecurityVerifier.VerificationResult pendingVerifyResult = null;
    private volatile boolean isRunning = false;

    public UpdateController(Context context) {
        this.context = context.getApplicationContext();
        this.authService = new UpdateAuthService();
        this.downloadService = new ApkDownloadService();
    }

    public synchronized boolean isRunning() {
        return isRunning;
    }

    public void setListener(UpdateListener listener) {
        this.listener = listener;
    }

    public void cancel() {
        downloadService.cancelDownload();
        isRunning = false;
        cleanupPendingApk();
    }

    public File getPendingVerifiedApk() {
        return pendingVerifiedApk;
    }

    public int getInstalledVersionCode() {
        return PackageInstallerHelper.getInstalledPackageVersionCode(context, PackageInstallerHelper.TARGET_PACKAGE_NAME);
    }

    public String getInstalledVersionName() {
        String ver = PackageInstallerHelper.getInstalledEngineVersionName(context);
        return ver != null ? ver : "Not Installed";
    }

    public void checkForUpdates(final ReleaseManifest manifest, final UpdateListener listener) {
        this.activeManifest = manifest != null ? manifest : createBaselineManifest();
        int currentCode = getInstalledVersionCode();

        if (currentCode != -1 && activeManifest.getVersionCode() <= currentCode) {
            if (listener != null) {
                listener.onUpdateUpToDate(currentCode);
            }
        } else {
            if (listener != null) {
                String label = currentCode == -1
                        ? "Main AimShot Engine ready for Private Vault setup"
                        : "New version v" + activeManifest.getVersionName() + " available";
                listener.onStageChanged(Stage.CHECKING_INTERNET, label, 0);
            }
        }
    }

    public void startUpdatePipeline(final Activity activity, final UpdateListener listener) {
        if (isRunning) return;
        isRunning = true;
        this.listener = listener;

        if (activeManifest == null) {
            activeManifest = createBaselineManifest();
        }

        notifyStage(Stage.CHECKING_INTERNET, "Checking active internet connection...", 5);
        if (!NetworkUtil.isNetworkConnected(context)) {
            isRunning = false;
            notifyError("No internet connection available. Please turn on Wi-Fi or Mobile Data and try again.", true);
            return;
        }

        NetworkUtil.verifyInternetAccess(context, new NetworkUtil.ConnectivityCallback() {
            @Override
            public void onResult(boolean hasRealInternet) {
                if (!isRunning) return;
                if (!hasRealInternet) {
                    isRunning = false;
                    notifyError("Wi-Fi or Mobile Data is turned on, but there is no active internet access.", true);
                    return;
                }

                notifyStage(Stage.CONNECTING_SERVER, "Connecting to Alpha10 Website...", 10);
                NetworkUtil.verifyServerReachability(context, NetworkUtil.ALPHA10_SERVER_CHECK_URL, new NetworkUtil.ConnectivityCallback() {
                    @Override
                    public void onResult(boolean isReachable) {
                        if (!isRunning) return;
                        if (!isReachable) {
                            isRunning = false;
                            notifyError("Internet is connected, but https://alpha10.freedev.app could not be reached. Please retry.", true);
                            return;
                        }

                        notifyStage(Stage.AUTHENTICATING, "Requesting download token from Alpha10 Server...", 15);
                        authService.authenticate(context, new UpdateAuthService.AuthCallback() {
                            @Override
                            public void onAuthSuccess(UpdateAuthService.AuthSessionResult sessionResult) {
                                if (!isRunning) return;

                                activeManifest = mergeServerManifest(activeManifest, sessionResult);

                                notifyStage(Stage.DOWNLOADING, "Downloading AimShot Engine v" + activeManifest.getVersionName() + "...", 20);

                                downloadService.downloadApk(
                                        context,
                                        activeManifest.getDownloadUrl(),
                                        sessionResult.getTemporaryToken(),
                                        sessionResult.getSessionCookie(),
                                        sessionResult.getIssuedAtTimestamp(),
                                        sessionResult.getExpiresInSeconds(),
                                        activeManifest.getVersionCode(),
                                        new ApkDownloadService.DownloadCallback() {
                                            @Override
                                            public void onDownloadProgress(long bytesDownloaded, long totalBytes, int percent) {
                                                int mappedPercent = 20 + (int) (percent * 0.55f);
                                                if (UpdateController.this.listener != null) {
                                                    UpdateController.this.listener.onDownloadProgress(bytesDownloaded, totalBytes, percent);
                                                    UpdateController.this.listener.onStageChanged(
                                                            Stage.DOWNLOADING,
                                                            "Downloading Engine Package: " + percent + "%",
                                                            mappedPercent
                                                    );
                                                }
                                            }

                                            @Override
                                            public void onDownloadComplete(File stagedApkFile) {
                                                if (!isRunning) {
                                                    if (stagedApkFile != null && stagedApkFile.exists()) {
                                                        stagedApkFile.delete();
                                                    }
                                                    return;
                                                }

                                                String streamShaHeader = downloadService.getLastServerSha256Header();
                                                if ((activeManifest.getExpectedSha256() == null || activeManifest.getExpectedSha256().isEmpty())
                                                        && streamShaHeader != null && !streamShaHeader.isEmpty()) {
                                                    activeManifest = new ReleaseManifest(
                                                            activeManifest.getVersionCode(),
                                                            activeManifest.getVersionName(),
                                                            activeManifest.getDownloadUrl(),
                                                            streamShaHeader,
                                                            activeManifest.getTrustedCertFingerprint(),
                                                            activeManifest.getReleaseNotes(),
                                                            activeManifest.getFileSizeBytes()
                                                    );
                                                }

                                                installVerifiedApk(activity, stagedApkFile);
                                            }

                                            @Override
                                            public void onDownloadError(String errorMessage, int httpCode) {
                                                isRunning = false;
                                                notifyError(errorMessage, true);
                                            }
                                        }
                                );
                            }

                            @Override
                            public void onAuthFailure(String errorMessage, int httpCode) {
                                isRunning = false;
                                notifyError(errorMessage, true);
                            }
                        });
                    }
                });
            }
        });
    }

    /**
     * Verifies the downloaded APK and hands off to the Android OS PackageInstaller.
     */
    private void installVerifiedApk(final Activity activity, final File stagedApkFile) {
        notifyStage(Stage.VERIFYING_SECURITY, "Verifying AimShot Engine integrity & signature...", 80);

        vaultExecutor.execute(() -> {
            try {
                ApkSecurityVerifier.VerificationResult verifyResult =
                        ApkSecurityVerifier.verifyApk(context, stagedApkFile, activeManifest);

                if (!verifyResult.isSuccess()) {
                    if (stagedApkFile != null && stagedApkFile.exists()) {
                        stagedApkFile.delete();
                    }
                    mainHandler.post(() -> {
                        isRunning = false;
                        notifyError("APK verification failed: " + verifyResult.getErrorMessage(), true);
                    });
                    return;
                }

                pendingVerifiedApk = stagedApkFile;
                pendingVerifyResult = verifyResult;

                mainHandler.post(() -> {
                    notifyStage(
                            Stage.WAITING_CONFIRMATION,
                            "Please confirm installation in the Android system dialog...",
                            90
                    );

                    boolean started = PackageInstallerHelper.installPackage(activity, stagedApkFile);
                    if (!started) {
                        isRunning = false;
                        notifyError("Unable to open Android Package Installer", true);
                    }
                });

            } catch (Throwable t) {
                Log.e(TAG, "Installation error: " + t.getMessage(), t);
                mainHandler.post(() -> {
                    isRunning = false;
                    notifyError("Installation error: " + t.getMessage(), true);
                });
            }
        });
    }

    public void proceedToInstallation(final Activity activity) {
        if (pendingVerifiedApk != null && pendingVerifiedApk.exists() && pendingVerifyResult != null) {
            isRunning = true;
            installVerifiedApk(activity, pendingVerifiedApk);
        } else {
            startUpdatePipeline(activity, listener);
        }
    }

    public void onResumeCheck(Activity activity) {
        if (PackageInstallerHelper.isMainApkInstalled(activity)) {
            isRunning = false;
            cleanupPendingApk();
            notifyStage(
                    Stage.INSTALL_SUCCESS,
                    "AimShot Engine installed successfully! Tap LAUNCH AIMSHOT ENGINE to open.",
                    100
            );
            if (listener != null) {
                listener.onInstallationSuccess();
            }
        }
    }

    public boolean launchInstalledApp(Context launchContext) {
        notifyStage(Stage.OPENING_APP, "Launching AimShot Engine...", 100);
        return PackageInstallerHelper.launchInstalledApp(launchContext != null ? launchContext : context);
    }

    public boolean launchInstalledApp() {
        return launchInstalledApp(context);
    }

    private void cleanupPendingApk() {
        if (pendingVerifiedApk != null && pendingVerifiedApk.exists()) {
            try {
                pendingVerifiedApk.delete();
            } catch (Exception ignored) {
            }
        }
        pendingVerifiedApk = null;
        pendingVerifyResult = null;
        ApkDownloadService.cleanupPrivateDownloads(context);
    }

    private void notifyStage(Stage stage, String message, int progress) {
        if (listener != null) {
            listener.onStageChanged(stage, message, progress);
        }
    }

    private void notifyError(String error, boolean canRetry) {
        if (listener != null) {
            listener.onError(error, canRetry);
        }
    }

    private static ReleaseManifest mergeServerManifest(ReleaseManifest base,
                                                       UpdateAuthService.AuthSessionResult sessionResult) {
        int verCode = sessionResult.getVersionCode() > 0 ? sessionResult.getVersionCode() : base.getVersionCode();
        String verName = (sessionResult.getVersionName() != null && !sessionResult.getVersionName().isEmpty())
                ? sessionResult.getVersionName()
                : base.getVersionName();
        String downloadUrl = (sessionResult.getDownloadUrl() != null && !sessionResult.getDownloadUrl().isEmpty())
                ? sessionResult.getDownloadUrl()
                : base.getDownloadUrl();
        String expectedSha256 = (sessionResult.getExpectedSha256() != null && !sessionResult.getExpectedSha256().isEmpty())
                ? sessionResult.getExpectedSha256()
                : base.getExpectedSha256();
        String trustedCert = (sessionResult.getTrustedCertSha256() != null && !sessionResult.getTrustedCertSha256().isEmpty())
                ? sessionResult.getTrustedCertSha256()
                : base.getTrustedCertFingerprint();

        return new ReleaseManifest(
                verCode,
                verName,
                downloadUrl,
                expectedSha256,
                trustedCert,
                base.getReleaseNotes(),
                base.getFileSizeBytes()
        );
    }

    public ReleaseManifest createBaselineManifest() {
        String configuredSha256 = null;
        try {
            if (BuildConfig.MAIN_APK_SHA256 != null
                    && BuildConfig.MAIN_APK_SHA256.trim().matches("^[0-9a-fA-F]{64}$")) {
                configuredSha256 = BuildConfig.MAIN_APK_SHA256.trim();
            }
        } catch (Throwable ignored) {
        }
        return new ReleaseManifest(
                1,
                "1.0.0",
                ApkDownloadService.DOWNLOAD_ENDPOINT,
                configuredSha256,
                null,
                "Main AimShot Engine (com.alpha10.aimshot) private container package.",
                0L
        );
    }
}
