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
 * Coordinates the complete AimShot Engine download, verification, installation & user-consented launch flow:
 * 1. Checking Internet & Alpha10 Server Connection
 * 2. Zero-Secret Broker Authentication & 60s Single-Use Token Acquisition
 * 3. Private Internal Storage APK Binary Streaming
 * 4. Multi-Tier APK Structure, Package ID, Trusted SHA-256 & Release Certificate Verification
 * 5. Official Android PackageInstaller.Session Execution & Unknown Sources Permission Handling
 * 6. Reliable Installation Result Detection (Success / Failure / Cancellation)
 * 7. User-Consented Main AimShot Engine Launch ("com.alpha10.aimshot")
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

    /**
     * Executes the complete download, verification & official PackageInstaller pipeline.
     */
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

        // STAGE 1: Internet Connection Verification
        notifyStage(Stage.CHECKING_INTERNET, "Checking internet connection...", 5);
        if (!NetworkUtil.isNetworkConnected(context)) {
            isRunning = false;
            notifyError("No internet connection available. Please connect and try again.", true);
            return;
        }

        // STAGE 2: Alpha10 Server Reachability Check
        notifyStage(Stage.CONNECTING_SERVER, "Verifying connection to Alpha10 Server...", 10);
        NetworkUtil.verifyServerReachability(context, NetworkUtil.ALPHA10_BASE_URL, new NetworkUtil.ConnectivityCallback() {
            @Override
            public void onResult(boolean isReachable) {
                if (!isRunning) return;
                if (!isReachable) {
                    isRunning = false;
                    notifyError("Unable to reach Alpha10 Server (https://alpha10.freedev.app). Please check your connection.", true);
                    return;
                }

                // STAGE 3: Zero-Secret Server Broker Authentication
                notifyStage(Stage.AUTHENTICATING, "Authenticating securely with Alpha10 Server...", 15);
                authService.authenticate(context, new UpdateAuthService.AuthCallback() {
                    @Override
                    public void onAuthSuccess(UpdateAuthService.AuthSessionResult sessionResult) {
                        if (!isRunning) return;

                        // Update ReleaseManifest with authoritative server-supplied hash & cert metadata
                        activeManifest = mergeServerManifest(activeManifest, sessionResult);

                        // STAGE 4: Downloading Main AimShot Engine APK into Private Internal Storage
                        notifyStage(Stage.DOWNLOADING, "Downloading AimShot Engine v" + activeManifest.getVersionName() + "...", 20);

                        downloadService.downloadApk(
                                context,
                                sessionResult.getTemporaryToken(),
                                sessionResult.getIssuedAtTimestamp(),
                                sessionResult.getExpiresInSeconds(),
                                activeManifest.getVersionCode(),
                                new ApkDownloadService.DownloadCallback() {
                                    @Override
                                    public void onDownloadProgress(long bytesDownloaded, long totalBytes, int percent) {
                                        int mappedPercent = 20 + (int) (percent * 0.50f);
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

                                        // STAGE 5: Multi-Tier APK Security Verification (Structure, Package ID, SHA-256, Release Cert)
                                        notifyStage(Stage.VERIFYING_SECURITY,
                                                "Verifying Package ID, SHA-256 digest & Release Signing Certificate...", 75);

                                        ApkSecurityVerifier.VerificationResult verifyResult =
                                                ApkSecurityVerifier.verifyApk(context, stagedApkFile, activeManifest);

                                        if (!verifyResult.isSuccess()) {
                                            if (stagedApkFile.exists()) stagedApkFile.delete();
                                            isRunning = false;
                                            notifyError("Security verification failed: " + verifyResult.getErrorMessage(), false);
                                            return;
                                        }

                                        // Atomically promote verified .tmp file inside private internal storage
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

    /**
     * Executes Unknown App Installation permission gate and commits official PackageInstaller.Session.
     */
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
                    "Unknown App Installation permission required. Opening Settings...", 80);
            PackageInstallerHelper.requestInstallPermission(activity);
            return;
        }

        isWaitingForPermission = false;
        notifyStage(Stage.WAITING_CONFIRMATION, "Preparing Android PackageInstaller session...", 85);
        notifyStage(Stage.INSTALLING, "Streaming verified APK to Android PackageInstaller...", 90);

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

    /**
     * Handles returning from Unknown App Sources settings or system PackageInstaller confirmation UI.
     */
    public void onResumeCheck(Activity activity) {
        this.attachedActivity = activity;

        // 1. Resuming after Unknown App Sources permission prompt
        if (isWaitingForPermission) {
            if (pendingVerifiedApk != null && pendingVerifiedApk.exists()) {
                if (PackageInstallerHelper.canInstallPackages(activity)) {
                    Log.i(TAG, "Unknown apps permission granted. Resuming PackageInstaller session without re-downloading.");
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

        // 2. Secondary OS check ONLY if a PackageInstaller session was committed while backgrounded
        if (isSessionCommitted && isRunning) {
            boolean isNowInstalled = PackageInstallerHelper.isMainApkInstalled(context);
            int currentCode = getInstalledVersionCode();
            if (isNowInstalled && (baselineVersionCode == -1 || currentCode > baselineVersionCode)) {
                Log.i(TAG, "Confirmed Main AimShot Engine installation on OS resume (vCode " + currentCode + ").");
                onInstallSuccess(PackageInstallerHelper.TARGET_PACKAGE_NAME);
            }
        }
    }

    // =========================================================================
    // PackageInstallStatusReceiver.InstallStatusListener Callbacks
    // =========================================================================

    @Override
    public void onUserActionRequired(Intent confirmationIntent) {
        mainHandler.post(() -> {
            notifyStage(Stage.WAITING_CONFIRMATION, "Please confirm installation in the Android system dialog...", 88);
            try {
                if (attachedActivity != null && !attachedActivity.isFinishing()) {
                    attachedActivity.startActivity(confirmationIntent);
                } else {
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

    /**
     * Launches Main AimShot Engine ("com.alpha10.aimshot") strictly on explicit user permission/tap.
     */
    public boolean launchInstalledApp() {
        notifyStage(Stage.OPENING_APP, "Opening AimShot Engine...", 100);
        String trustedCert = activeManifest != null
                ? activeManifest.getTrustedCertFingerprint()
                : ReleaseManifest.PERMANENT_CERT_SHA256;
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
        String expectedSha256 = (sessionResult.getExpectedSha256() != null && !sessionResult.getExpectedSha256().isEmpty())
                ? sessionResult.getExpectedSha256()
                : base.getExpectedSha256();
        String trustedCert = (sessionResult.getTrustedCertSha256() != null && !sessionResult.getTrustedCertSha256().isEmpty())
                ? sessionResult.getTrustedCertSha256()
                : base.getTrustedCertFingerprint();

        return new ReleaseManifest(
                verCode,
                verName,
                base.getDownloadUrl(),
                expectedSha256,
                trustedCert,
                base.getReleaseNotes(),
                base.getFileSizeBytes()
        );
    }

    /**
     * Baseline manifest referencing the unmodified download endpoint and preserved Release Certificate.
     * Note: expectedSha256 is intentionally null until supplied authoritatively by the server broker
     * (zero fabricated/guessed SHA-256 hashes).
     */
    public ReleaseManifest createBaselineManifest() {
        return new ReleaseManifest(
                1,
                "1.0.0",
                ApkDownloadService.DOWNLOAD_ENDPOINT,
                null,
                ReleaseManifest.PERMANENT_CERT_SHA256,
                "Main AimShot Engine (com.alpha10.aimshot) verified installation package.",
                0L
        );
    }
}
