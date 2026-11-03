package com.alpha10.aimshot.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.util.Log;

import com.alpha10.aimshot.util.PackageInstallerHelper;

/**
 * BroadcastReceiver for handling Android PackageInstaller session callbacks.
 * Receives status callbacks: STATUS_SUCCESS, STATUS_PENDING_USER_ACTION, STATUS_FAILURE, etc.
 */
public class PackageInstallStatusReceiver extends BroadcastReceiver {

    public static final String ACTION_INSTALL_STATUS = "com.alpha10.aimshot.INSTALL_STATUS";
    private static final String TAG = "AimShot_Installer";

    public interface InstallStatusListener {
        void onUserActionRequired(Intent confirmationIntent);
        void onInstallSuccess(String packageName);
        void onInstallFailed(int statusCode, String message);
        void onInstallAborted();
    }

    private static InstallStatusListener activeListener;

    public static void setStatusListener(InstallStatusListener listener) {
        activeListener = listener;
    }

    public static void clearStatusListener() {
        activeListener = null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);

        switch (status) {
            case PackageInstaller.STATUS_PENDING_USER_ACTION:
                // User must confirm in system prompt dialog
                Intent confirmIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (activeListener != null) {
                        activeListener.onUserActionRequired(confirmIntent);
                    } else {
                        context.startActivity(confirmIntent);
                    }
                }
                break;

            case PackageInstaller.STATUS_SUCCESS:
                Log.i(TAG, "Package installation succeeded officially.");
                PackageInstallerHelper.setMainApkInstalled(context, true);
                if (activeListener != null) {
                    activeListener.onInstallSuccess("com.alpha10.aimshot");
                }
                break;

            case PackageInstaller.STATUS_FAILURE_ABORTED:
                Log.w(TAG, "Package installation cancelled by user.");
                if (activeListener != null) {
                    activeListener.onInstallAborted();
                }
                break;

            case PackageInstaller.STATUS_FAILURE_BLOCKED:
            case PackageInstaller.STATUS_FAILURE_CONFLICT:
            case PackageInstaller.STATUS_FAILURE_INVALID:
            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
            case PackageInstaller.STATUS_FAILURE:
            default:
                Log.e(TAG, "Package installation failed with status code " + status + ": " + message);
                if (activeListener != null) {
                    activeListener.onInstallFailed(status, message != null ? message : "Installation failed (Code " + status + ")");
                }
                break;
        }
    }
}
