package com.alpha10.aimshot.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.alpha10.aimshot.model.ReleaseManifest;
import com.alpha10.aimshot.receiver.PackageInstallStatusReceiver;
import com.alpha10.aimshot.service.ApkDownloadService;
import com.alpha10.aimshot.service.UpdateAuthService;
import com.alpha10.aimshot.util.ApkSecurityVerifier;
import com.alpha10.aimshot.util.NetworkUtil;
import com.alpha10.aimshot.util.PackageInstallerHelper;

import java.io.File;

/**
 * Coordinates the complete 9-stage AimShot Engine update & launch sequence:
 * 1. Checking Internet Connection
 * 2. Connecting to Alpha10 Server
 * 3. Authenticating
 * 4. Downloading AimShot Engine
 * 5. Verifying APK Security
 * 6. Waiting for Installation Confirmation
 * 7. Installing AimShot Engine
 * 8. Installation Successful
 * 9. Opening AimShot Engine
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
    }

    public File getPendingVerifiedApk() {
        return pendingVerifiedApk;
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
                listener.onStageChanged(Stage.CHECKING_INTERNET, "New version v" + activeManifest.getVersionName() + " available", 0);
            }
        }
    }

    /**
     * Starts the full 9-stage secure update pipeline.
     */
    public void startUpdatePipeline(final Activity activity, final UpdateListener listener) {
        if (isRunning) return;
        isRunning = true;
        this.attachedActivity = activity;
        this.listener = listener;
        this.baselineVersionCode = getInstalledVersionCode();

        if (activeManifest == null) {
            activeManifest = getAuthoritativeReleaseManifest();
        }

        // STAGE 1: Checking Internet Connection
        notifyStage(Stage.CHECKING_INTERNET, "Checking internet connection...", 5);
        if (!NetworkUtil.isNetworkConnected(context)) {
            isRunning = false;
            notifyError("No internet connection available. Please connect and try again.", true);
            return;
        }

        // STAGE 2: Connecting to Alpha10 Server & STAGE 3: Authenticating
        notifyStage(Stage.CONNECTING_SERVER, "Connecting to Alpha10 Server...", 10);
        notifyStage(Stage.AUTHENTICATING, "Authenticating with Alpha10 Server (auth.php)...", 15);

        authService.authenticate(new UpdateAuthService.AuthCallback() {
            @Override
            public void onAuthSuccess(String temporaryToken, long expiresInSeconds, long issuedAtTimestamp) {
                // STAGE 4: Downloading AimShot Engine
                notifyStage(Stage.DOWNLOADING, "Downloading AimShot Engine v" + activeManifest.getVersionName() + "...", 20);

                downloadService.downloadApk(context, temporaryToken, issuedAtTimestamp, expiresInSeconds, activeManifest.getVersionCode(), new ApkDownloadService.DownloadCallback() {
                    @Override
                    public void onDownloadProgress(long bytesDownloaded, long totalBytes, int percent) {
                        int mappedPercent = 20 + (int) (percent * 0.50); // 20% to 70%
                        if (listener != null) {
                            listener.onDownloadProgress(bytesDownloaded, totalBytes, percent);
                            listener.onStageChanged(Stage.DOWNLOADING, "Downloading update: " + percent + "%", mappedPercent);
                        }
                    }

                    @Override
                    public void onDownloadComplete(File stagedApkFile) {
                        // STAGE 5: Verifying APK Security
                        notifyStage(Stage.VERIFYING_SECURITY, "Verifying SHA-256 integrity & permanent release certificate...", 75);

                        ApkSecurityVerifier.VerificationResult verifyResult =
                                ApkSecurityVerifier.verifyApk(context, stagedApkFile, activeManifest);

                        if (!verifyResult.isSuccess()) {
                            // Security check failed -> quarantine & delete immediately
                            if (stagedApkFile.exists()) stagedApkFile.delete();
                            isRunning = false;
                            notifyError("Security check failed: " + verifyResult.getErrorMessage(), false);
                            return;
                        }

                        // Atomically promote staged .tmp file to validated .apk
                        File finalApkFile = new File(stagedApkFile.getParentFile(), "AimShot_v" + activeManifest.getVersionCode() + ".apk");
                        if (finalApkFile.exists()) finalApkFile.delete();

                        boolean renamed = stagedApkFile.renameTo(finalApkFile);
                        pendingVerifiedApk = renamed ? finalApkFile : stagedApkFile;

                        proceedToInstallation(activity);
                    }

                    @Override
                    public void onDownloadError(String errorMessage, int httpCode) {
                        isRunning = false;
                        notifyError(errorMessage, true);
                    }
                });
            }

            @Override
            public void onAuthFailure(String errorMessage, int httpCode) {
                isRunning = false;
                notifyError(errorMessage, true);
            }
        });
    }

    /**
     * Executes STAGE 6 (Waiting for Confirmation) and STAGE 7 (Installing).
     */
    public void proceedToInstallation(final Activity activity) {
        if (pendingVerifiedApk == null || !pendingVerifiedApk.exists()) {
            notifyError("Verified APK is missing or inaccessible.", false);
            return;
        }

        // Check unknown app permission
        if (activity != null && !PackageInstallerHelper.canInstallPackages(activity)) {
            isWaitingForPermission = true;
            notifyStage(Stage.WAITING_CONFIRMATION, "Install permission required. Redirecting to settings...", 80);
            PackageInstallerHelper.requestInstallPermission(activity);
            return;
        }

        isWaitingForPermission = false;
        notifyStage(Stage.WAITING_CONFIRMATION, "Waiting for installation confirmation...", 85);
        notifyStage(Stage.INSTALLING, "Installing AimShot Engine...", 90);

        // Execute installation via PackageInstaller session
        boolean started = PackageInstallerHelper.installApkWithSession(activity != null ? activity : context, pendingVerifiedApk, this);
        if (!started) {
            notifyError("Failed to initiate Android PackageInstaller session.", true);
        }
    }

    /**
     * Called on Activity onResume to check if returning from permission screen
     * or if installation finished while app was backgrounded.
     */
    public void onResumeCheck(Activity activity) {
        // 1. Resuming after unknown app sources permission grant
        if (isWaitingForPermission && pendingVerifiedApk != null && pendingVerifiedApk.exists()) {
            if (PackageInstallerHelper.canInstallPackages(activity)) {
                Log.i(TAG, "Unknown apps permission granted by user. Resuming installation without re-downloading.");
                proceedToInstallation(activity);
                return;
            } else {
                Log.w(TAG, "User returned without granting unknown apps permission.");
                isWaitingForPermission = false;
                notifyError("Installation permission was denied. Please allow AimShot Engine to install updates.", true);
                return;
            }
        }

        // 2. Checking if package was upgraded while backgrounded
        int currentCode = getInstalledVersionCode();
        if (baselineVersionCode > 0 && currentCode > baselineVersionCode) {
            Log.i(TAG, "Detected version upgrade via onResume(): " + baselineVersionCode + " -> " + currentCode);
            onInstallSuccess("com.alpha10.aimshot");
        }
    }

    // =========================================================================
    // PackageInstallStatusReceiver.InstallStatusListener Callbacks
    // =========================================================================

    @Override
    public void onUserActionRequired(Intent confirmationIntent) {
        mainHandler.post(() -> {
            notifyStage(Stage.WAITING_CONFIRMATION, "Please confirm installation on the system prompt...", 88);
            if (attachedActivity != null) {
                attachedActivity.startActivity(confirmationIntent);
            } else {
                context.startActivity(confirmationIntent);
            }
        });
    }

    @Override
    public void onInstallSuccess(String packageName) {
        mainHandler.post(() -> {
            isRunning = false;
            // STAGE 8: Installation Successful
            notifyStage(Stage.INSTALL_SUCCESS, "AimShot Engine updated successfully!", 100);

            if (listener != null) {
                listener.onInstallationSuccess();
            }

            // Record success state
            context.getSharedPreferences("aimshot_update_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("last_update_success", true)
                    .putLong("last_update_timestamp", System.currentTimeMillis())
                    .apply();

            PackageInstallerHelper.setMainApkInstalled(context, true);

            // Clean up verified APK now that it has been installed
            if (pendingVerifiedApk != null && pendingVerifiedApk.exists()) {
                try { pendingVerifiedApk.delete(); } catch (Exception ignored) {}
                pendingVerifiedApk = null;
            }
        });
    }

    @Override
    public void onInstallFailed(int statusCode, String message) {
        mainHandler.post(() -> {
            isRunning = false;
            notifyError("Installation failed: " + message, true);
        });
    }

    @Override
    public void onInstallAborted() {
        mainHandler.post(() -> {
            isRunning = false;
            notifyStage(Stage.ERROR, "Installation cancelled by user.", 0);
            if (listener != null) {
                listener.onInstallationAborted();
            }
        });
    }

    /**
     * Executes STAGE 9: Opening AimShot Engine.
     */
    public boolean launchInstalledApp() {
        notifyStage(Stage.OPENING_APP, "Opening AimShot Engine...", 100);
        return PackageInstallerHelper.launchInstalledApp(context);
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

    public ReleaseManifest getAuthoritativeReleaseManifest() {
        return new ReleaseManifest(
                2,
                "1.0.1",
                "https://alpha10.freedev.app/api/aimshot-download.php",
                null,
                ReleaseManifest.PERMANENT_CERT_SHA256,
                "AimShot Engine 1.0.1: Authenticated single-use token streaming, package installer session dispatch, and auto-launch.",
                28500000L
        );
    }
}
