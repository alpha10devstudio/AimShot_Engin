package com.alpha10.aimshot.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.util.Log;

import com.alpha10.aimshot.util.PackageInstallerHelper;

/**
 * Receives official Android PackageInstaller.Session callbacks and validates active session ID,
 * target package identity, and OS-level installation completion.
 */
public class PackageInstallStatusReceiver extends BroadcastReceiver {

    public static final String ACTION_INSTALL_STATUS = "com.alpha10.aimshot.launcher.INSTALL_STATUS";
    private static final String TAG = "AimShot_Installer";

    public interface InstallStatusListener {
        void onUserActionRequired(Intent confirmationIntent);
        void onInstallSuccess(String packageName);
        void onInstallFailed(int statusCode, String message);
        void onInstallAborted();
    }

    private static volatile InstallStatusListener activeListener;
    private static volatile int activeSessionId = -1;

    public static void registerActiveSession(int sessionId, InstallStatusListener listener) {
        activeSessionId = sessionId;
        activeListener = listener;
    }

    public static void setStatusListener(InstallStatusListener listener) {
        activeListener = listener;
    }

    public static void clearStatusListener() {
        activeListener = null;
        activeSessionId = -1;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        if (!ACTION_INSTALL_STATUS.equals(intent.getAction())) return;

        int incomingSessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1);
        if (activeSessionId != -1 && incomingSessionId != -1 && incomingSessionId != activeSessionId) {
            Log.w(TAG, "Ignoring callback for inactive PackageInstaller session: " + incomingSessionId);
            return;
        }

        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        String rawMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        String targetPkg = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME);

        InstallStatusListener listener = activeListener;

        switch (status) {
            case PackageInstaller.STATUS_PENDING_USER_ACTION:
                Intent confirmIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (listener != null) {
                        listener.onUserActionRequired(confirmIntent);
                    } else {
                        context.startActivity(confirmIntent);
                    }
                } else if (listener != null) {
                    clearStatusListener();
                    listener.onInstallFailed(status, "System installer confirmation prompt could not be opened.");
                }
                break;

            case PackageInstaller.STATUS_SUCCESS:
                clearStatusListener();
                String resolvedPkg = (targetPkg != null && !targetPkg.isEmpty())
                        ? targetPkg
                        : PackageInstallerHelper.TARGET_PACKAGE_NAME;

                // Positively confirm OS-level presence of com.alpha10.aimshot
                if (PackageInstallerHelper.isMainApkInstalled(context)) {
                    Log.i(TAG, "Verified Main AimShot Engine installation on OS: " + resolvedPkg);
                    if (listener != null) {
                        listener.onInstallSuccess(resolvedPkg);
                    }
                } else {
                    Log.e(TAG, "STATUS_SUCCESS reported but target package is not resolvable on OS.");
                    if (listener != null) {
                        listener.onInstallFailed(PackageInstaller.STATUS_FAILURE,
                                "Package installation was not confirmed by Android PackageManager.");
                    }
                }
                break;

            case PackageInstaller.STATUS_FAILURE_ABORTED:
                clearStatusListener();
                Log.w(TAG, "Package installation cancelled by user.");
                if (listener != null) {
                    listener.onInstallAborted();
                }
                break;

            case PackageInstaller.STATUS_FAILURE_BLOCKED:
            case PackageInstaller.STATUS_FAILURE_CONFLICT:
            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
            case PackageInstaller.STATUS_FAILURE_INVALID:
            case PackageInstaller.STATUS_FAILURE_STORAGE:
            case PackageInstaller.STATUS_FAILURE:
            default:
                clearStatusListener();
                String friendlyReason = formatFailureReason(status, rawMessage);
                Log.e(TAG, "Package installation failed (" + status + "): " + friendlyReason);
                if (listener != null) {
                    listener.onInstallFailed(status, friendlyReason);
                }
                break;
        }
    }

    private static String formatFailureReason(int status, String rawMessage) {
        switch (status) {
            case PackageInstaller.STATUS_FAILURE_BLOCKED:
                return "Installation blocked by device security policy or Play Protect.";
            case PackageInstaller.STATUS_FAILURE_CONFLICT:
                return "Signature conflict with an existing installation of AimShot Engine. Uninstall the conflicting copy first.";
            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                return "APK is incompatible with this Android version or CPU architecture.";
            case PackageInstaller.STATUS_FAILURE_INVALID:
                return "APK archive was rejected as invalid by Android PackageInstaller.";
            case PackageInstaller.STATUS_FAILURE_STORAGE:
                return "Insufficient device storage to complete installation.";
            default:
                return (rawMessage != null && !rawMessage.trim().isEmpty())
                        ? rawMessage.trim()
                        : "Installation failed (Error code " + status + ").";
        }
    }
}
