package com.alpha10.aimshot.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.alpha10.aimshot.BuildConfig;
import com.alpha10.aimshot.model.ReleaseManifest;
import com.alpha10.aimshot.receiver.PackageInstallStatusReceiver;
import com.alpha10.aimshot.service.ApkDownloadService;
import com.alpha10.aimshot.service.UpdateAuthService;
import com.alpha10.aimshot.util.ApkSecurityVerifier;
import com.alpha10.aimshot.util.NetworkUtil;
import com.alpha10.aimshot.util.PackageInstallerHelper;

import java.io.File;

/**
 * Coordinates the complete AimShot Engine download, verification, installation & launch flow:
 * 1. Internet & Alpha10 Website Connection Check
 * 2. Direct Authentication with https://alpha10.freedev.app/api/auth.php (64-char secret key + aes.js bypass)
 * 3. Direct APK Binary Streaming into Private Internal Storage
 * 4. APK Package ID ("com.alpha10.aimshot") & Integrity Verification
 * 5. Official Android PackageInstaller.Session Execution & Unknown Sources Permission Handling
 * 6. User-Consented Main AimShot Engine Launch
 */
public class UpdateController implements PackageInstallStatusReceiver.InstallStatusListener {

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
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ReleaseManifest activeManifest;
    private UpdateListener listener;
    private Activity attachedActivity;

    private File pendingVerifiedApk = null;
    private boolean isRunning = false;
    private boolean isWaitingForPermission = false;
    private boolean isSessionCommitted = false;
    private int baselineVersionCode = -1;

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
        PackageInstallStatusReceiver.clearStatusListener();
        isRunning = false;
        isWaitingForPermission = false;
        isSessionCommitted = false;
        cleanupPendingApk();
    }

    public File getPendingVerifiedApk() {
        return pendingVerifiedApk;
    }

    public int getInstalledVersionCode() {
        return PackageInstallerHelper.getInstalledPackageVersionCode(context, PackageInstallerHelper.TARGET_PACKAGE_NAME);
    }

    public String getInstalledVersionName() {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(PackageInstallerHelper.TARGET_PACKAGE_NAME, 0);
            return pInfo.versionName;
        } catch (Exception e) {
            return "Not Installed";
        }
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
                        ? "Main AimShot Engine ready to download & install"
                        : "New version v" + activeManifest.getVersionName() + " available";
                listener.onStageChanged(Stage.CHECKING_INTERNET, label, 0);
            }
        }
    }

    public void startUpdatePipeline(final Activity activity, final UpdateListener listener) {
        if (isRunning) return;
        isRunning = true;
        isWaitingForPermission = false;
        isSessionCommitted = false;
        this.attachedActivity = activity;
        this.listener = listener;
        this.baselineVersionCode = getInstalledVersionCode();

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
                                                            "Downloading APK: " + percent + "%",
                                                            mappedPercent
                                                    );
                                                }
                                            }

                                            @Override
                                            public void onDownloadComplete(File stagedApkFile) {
                                                if (!isRunning) {
                                                    if (stagedApkFile != null && stagedApkFile.exists()) stagedApkFile.delete();
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

                                                notifyStage(Stage.VERIFYING_SECURITY,
                                                        "Verifying AimShot Engine APK package...", 80);

                                                ApkSecurityVerifier.VerificationResult verifyResult =
                                                        ApkSecurityVerifier.verifyApk(context, stagedApkFile, activeManifest);

                                                if (!verifyResult.isSuccess()) {
                                                    if (stagedApkFile.exists()) stagedApkFile.delete();
                                                    isRunning = false;
                                                    notifyError("APK verification failed: " + verifyResult.getErrorMessage(), true);
                                                    return;
                                                }

                                                File finalApkFile = new File(
                                                        stagedApkFile.getParentFile(),
                                                        "AimShot_Engine_v" + verifyResult.getParsedVersionCode() + ".apk"
                                                );
                                                if (finalApkFile.exists()) finalApkFile.delete();

                                                boolean renamed = stagedApkFile.renameTo(finalApkFile);
                                                pendingVerifiedApk = renamed ? finalApkFile : stagedApkFile;

                                                proceedToInstallation(attachedActivity);
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

    public void proceedToInstallation(final Activity activity) {
        if (pendingVerifiedApk == null || !pendingVerifiedApk.exists()) {
            isRunning = false;
            notifyError("Verified APK is missing from private storage.", true);
            return;
        }

        if (activity != null && !PackageInstallerHelper.canInstallPackages(activity)) {
            isWaitingForPermission = true;
            isSessionCommitted = false;
            notifyStage(Stage.WAITING_CONFIRMATION,
                    "Unknown App Installation permission required. Opening Settings...", 84);
            PackageInstallerHelper.requestInstallPermission(activity);
            return;
        }

        isWaitingForPermission = false;
        notifyStage(Stage.WAITING_CONFIRMATION, "Preparing Android PackageInstaller session...", 88);
        notifyStage(Stage.INSTALLING, "Opening Android PackageInstaller...", 92);

        int sessionId = PackageInstallerHelper.installApkWithSession(
                activity != null ? activity : context,
                pendingVerifiedApk,
                this
        );

        if (sessionId == -1) {
            isRunning = false;
            isSessionCommitted = false;
            notifyError("Failed to create Android PackageInstaller session.", true);
        } else {
            isSessionCommitted = true;
        }
    }

    public void onResumeCheck(Activity activity) {
        this.attachedActivity = activity;

        if (isWaitingForPermission) {
            if (pendingVerifiedApk != null && pendingVerifiedApk.exists()) {
                if (PackageInstallerHelper.canInstallPackages(activity)) {
                    Log.i(TAG, "Unknown apps permission granted. Resuming PackageInstaller session.");
                    proceedToInstallation(activity);
                } else {
                    Log.w(TAG, "Unknown apps permission denied by user.");
                    isWaitingForPermission = false;
                    isRunning = false;
                    notifyError("Unknown App Installation permission was not granted. Tap Retry after enabling permission.", true);
                }
            }
            return;
        }

        if (isSessionCommitted && isRunning) {
            boolean isNowInstalled = PackageInstallerHelper.isMainApkInstalled(context);
            int currentCode = getInstalledVersionCode();
            if (isNowInstalled && (baselineVersionCode == -1 || currentCode > baselineVersionCode)) {
                Log.i(TAG, "Confirmed Main AimShot Engine installation on OS resume (vCode " + currentCode + ").");
                onInstallSuccess(PackageInstallerHelper.TARGET_PACKAGE_NAME);
            }
        }
    }

    @Override
    public void onUserActionRequired(Intent confirmationIntent) {
        mainHandler.post(() -> {
            notifyStage(Stage.WAITING_CONFIRMATION, "Please confirm installation in the Android system dialog...", 90);
            try {
                if (attachedActivity != null && !attachedActivity.isFinishing()) {
                    attachedActivity.startActivity(confirmationIntent);
                } else {
                    confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(confirmationIntent);
                }
            } catch (Exception e) {
                isRunning = false;
                isSessionCommitted = false;
                notifyError("Unable to open system installation confirmation prompt: " + e.getMessage(), true);
            }
        });
    }

    @Override
    public void onInstallSuccess(String packageName) {
        mainHandler.post(() -> {
            if (!isRunning && !isSessionCommitted) {
                return;
            }
            isRunning = false;
            isSessionCommitted = false;

            notifyStage(Stage.INSTALL_SUCCESS,
                    "Main AimShot Engine installed successfully! Tap OPEN AIMSHOT ENGINE to launch.", 100);

            context.getSharedPreferences("aimshot_update_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("last_update_success", true)
                    .putLong("last_update_timestamp", System.currentTimeMillis())
                    .apply();

            cleanupPendingApk();

            if (listener != null) {
                listener.onInstallationSuccess();
            }
        });
    }

    @Override
    public void onInstallFailed(int statusCode, String message) {
        mainHandler.post(() -> {
            isRunning = false;
            isSessionCommitted = false;
            cleanupPendingApk();
            notifyError("Installation failed: " + message, true);
        });
    }

    @Override
    public void onInstallAborted() {
        mainHandler.post(() -> {
            isRunning = false;
            isSessionCommitted = false;
            notifyStage(Stage.ERROR, "Installation cancelled by user.", 0);
            if (listener != null) {
                listener.onInstallationAborted();
            }
        });
    }

    public boolean launchInstalledApp() {
        notifyStage(Stage.OPENING_APP, "Opening AimShot Engine...", 100);
        String trustedCert = activeManifest != null ? activeManifest.getTrustedCertFingerprint() : null;
        return PackageInstallerHelper.launchInstalledApp(context, trustedCert);
    }

    private void cleanupPendingApk() {
        if (pendingVerifiedApk != null && pendingVerifiedApk.exists()) {
            try { pendingVerifiedApk.delete(); } catch (Exception ignored) {}
        }
        pendingVerifiedApk = null;
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
                "Main AimShot Engine (com.alpha10.aimshot) installation package.",
                0L
        );
    }
}
