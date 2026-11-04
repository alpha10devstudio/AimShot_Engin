package com.alpha10.aimshot.util;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.alpha10.aimshot.model.ReleaseManifest;
import com.alpha10.aimshot.receiver.PackageInstallStatusReceiver;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Handles official Android PackageInstaller Session installation, Unknown App Sources permission
 * redirection, authoritative OS package presence checks, and verified Main AimShot Engine launching.
 */
public class PackageInstallerHelper {

    private static final String TAG = "AimShot_InstallerHelper";
    public static final String TARGET_PACKAGE_NAME = "com.alpha10.aimshot";
    public static final int REQUEST_CODE_UNKNOWN_APPS = 1001;

    /**
     * Checks if the Launcher app has permission to request APK package installations on Android 8.0+.
     */
    public static boolean canInstallPackages(Context context) {
        if (context == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return context.getPackageManager().canRequestPackageInstalls();
        }
        return true;
    }

    /**
     * Opens system Settings to grant Unknown App Installation permission for AimShot Launcher.
     */
    public static void requestInstallPermission(Activity activity) {
        if (activity == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivityForResult(intent, REQUEST_CODE_UNKNOWN_APPS);
            } catch (Exception e) {
                Intent fallback = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                activity.startActivity(fallback);
            }
        }
    }

    /**
     * Streams the verified APK from private internal storage directly into Android's official
     * PackageInstaller.Session API and registers deterministic status callbacks.
     * Does NOT fall back to untracked ACTION_VIEW intents.
     */
    public static int installApkWithSession(Context context,
                                            File apkFile,
                                            PackageInstallStatusReceiver.InstallStatusListener listener) {
        if (context == null || apkFile == null || !apkFile.exists() || apkFile.length() == 0) {
            return -1;
        }

        PackageInstaller packageInstaller = null;
        int sessionId = -1;

        try {
            PackageManager pm = context.getPackageManager();
            packageInstaller = pm.getPackageInstaller();

            PackageInstaller.SessionParams params =
                    new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            params.setAppPackageName(TARGET_PACKAGE_NAME);
            params.setSize(apkFile.length());

            sessionId = packageInstaller.createSession(params);
            PackageInstallStatusReceiver.registerActiveSession(sessionId, listener);

            try (PackageInstaller.Session session = packageInstaller.openSession(sessionId)) {
                try (OutputStream out = session.openWrite("AimShot_Main_Engine", 0, apkFile.length());
                     InputStream in = new FileInputStream(apkFile)) {
                    byte[] buffer = new byte[65536];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    session.fsync(out);
                }

                Intent broadcastIntent = new Intent(context, PackageInstallStatusReceiver.class);
                broadcastIntent.setAction(PackageInstallStatusReceiver.ACTION_INSTALL_STATUS);
                broadcastIntent.setPackage(context.getPackageName());

                int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    flags |= PendingIntent.FLAG_MUTABLE;
                }
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, sessionId, broadcastIntent, flags);

                session.commit(pendingIntent.getIntentSender());
            }

            return sessionId;

        } catch (Exception e) {
            Log.e(TAG, "Official PackageInstaller.Session failed: " + e.getMessage(), e);
            PackageInstallStatusReceiver.clearStatusListener();
            if (packageInstaller != null && sessionId != -1) {
                try { packageInstaller.abandonSession(sessionId); } catch (Exception ignored) {}
            }
            return -1;
        }
    }

    /**
     * Authoritatively checks Android OS PackageManager to determine if Main AimShot Engine
     * ("com.alpha10.aimshot") is currently installed on the device.
     *
     * Does NOT rely on SharedPreferences flags so manual uninstallation by the user is
     * immediately detected.
     */
    public static boolean isMainApkInstalled(Context context) {
        if (context == null) return false;
        if (context.getPackageName().equals(TARGET_PACKAGE_NAME)) {
            return false;
        }

        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(TARGET_PACKAGE_NAME, 0);
            if (info == null) return false;

            Intent launchIntent = pm.getLaunchIntentForPackage(TARGET_PACKAGE_NAME);
            return launchIntent != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        } catch (Exception e) {
            Log.w(TAG, "Error querying OS PackageManager for " + TARGET_PACKAGE_NAME + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Queries OS PackageManager for the installed versionCode of the given package, or -1 if absent.
     */
    public static int getInstalledPackageVersionCode(Context context, String packageName) {
        if (context == null || packageName == null) return -1;
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            return info != null ? info.versionCode : -1;
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Launches the installed Main AimShot Engine ("com.alpha10.aimshot") after verifying:
     * 1. Target package is distinct from Launcher ("com.alpha10.aimshot.launcher")
     * 2. Target package is genuinely installed on the OS
     * 3. Target package signing certificate matches the trusted Release Certificate
     *
     * Under NO circumstances launches Carrom Pool or re-launches AimShot Launcher.
     */
    public static boolean launchInstalledApp(Context context) {
        return launchInstalledApp(context, ReleaseManifest.PERMANENT_CERT_SHA256);
    }

    public static boolean launchInstalledApp(Context context, String trustedCertFingerprint) {
        if (context == null) return false;

        // 1. Prevent self-launch loop
        if (context.getPackageName().equals(TARGET_PACKAGE_NAME)) {
            Log.e(TAG, "Blocked self-launch: Launcher and target share identical package ID.");
            return false;
        }

        // 2. Verify Main AimShot Engine is installed on OS
        if (!isMainApkInstalled(context)) {
            Log.w(TAG, "Main AimShot Engine (" + TARGET_PACKAGE_NAME + ") is not installed.");
            return false;
        }

        // 3. Verify installed package signing certificate when expected fingerprint is provided
        if (trustedCertFingerprint != null && !trustedCertFingerprint.trim().isEmpty()) {
            boolean certValid = ApkSecurityVerifier.verifyInstalledPackageCertificate(
                    context,
                    TARGET_PACKAGE_NAME,
                    trustedCertFingerprint
            );
            if (!certValid) {
                Log.e(TAG, "Blocked launch: installed " + TARGET_PACKAGE_NAME + " failed signing certificate verification.");
                return false;
            }
        }

        try {
            PackageManager pm = context.getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(TARGET_PACKAGE_NAME);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(launchIntent);
                Log.i(TAG, "Launched Main AimShot Engine: " + TARGET_PACKAGE_NAME);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch Main AimShot Engine: " + e.getMessage(), e);
        }
        return false;
    }
}
