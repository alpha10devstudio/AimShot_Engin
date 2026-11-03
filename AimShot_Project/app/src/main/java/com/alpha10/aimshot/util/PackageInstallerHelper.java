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

import androidx.core.content.FileProvider;

import com.alpha10.aimshot.receiver.PackageInstallStatusReceiver;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Handles Android package installation via PackageInstaller sessions and status callbacks,
 * unknown app permission redirects, and application launch orchestration.
 */
public class PackageInstallerHelper {

    private static final String TAG = "AimShot_InstallerHelper";
    public static final String TARGET_PACKAGE_NAME = "com.alpha10.aimshot";
    public static final int REQUEST_CODE_UNKNOWN_APPS = 1001;

    /**
     * Checks if the app currently has permission to install unknown APK packages on Android 8+.
     */
    public static boolean canInstallPackages(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return context.getPackageManager().canRequestPackageInstalls();
        }
        return true;
    }

    /**
     * Launches the system settings screen to allow installing unknown apps.
     */
    public static void requestInstallPermission(Activity activity) {
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
     * Installs the verified APK using Android's PackageInstaller Session API.
     * Communicates with PackageInstallStatusReceiver for actual status callbacks.
     */
    public static boolean installApkWithSession(Context context, File apkFile, PackageInstallStatusReceiver.InstallStatusListener listener) {
        if (context == null || apkFile == null || !apkFile.exists() || apkFile.length() == 0) {
            return false;
        }

        PackageInstallStatusReceiver.setStatusListener(listener);

        try {
            PackageManager pm = context.getPackageManager();
            PackageInstaller packageInstaller = pm.getPackageInstaller();

            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            params.setAppPackageName(TARGET_PACKAGE_NAME);

            int sessionId = packageInstaller.createSession(params);
            PackageInstaller.Session session = packageInstaller.openSession(sessionId);

            try (OutputStream out = session.openWrite("AimShot_Package", 0, apkFile.length());
                 InputStream in = new FileInputStream(apkFile)) {
                byte[] buffer = new byte[65536];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                session.fsync(out);
            }

            // Create IntentSender for PackageInstallStatusReceiver
            Intent broadcastIntent = new Intent(context, PackageInstallStatusReceiver.class);
            broadcastIntent.setAction(PackageInstallStatusReceiver.ACTION_INSTALL_STATUS);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags |= PendingIntent.FLAG_MUTABLE;
            }
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, sessionId, broadcastIntent, flags);

            session.commit(pendingIntent.getIntentSender());
            session.close();
            return true;

        } catch (Exception e) {
            Log.e(TAG, "PackageInstaller session creation failed: " + e.getMessage(), e);
            // Fallback to FileProvider Intent.ACTION_VIEW
            return installApkViaIntent(context, apkFile);
        }
    }

    /**
     * Fallback standard Android PackageInstaller intent via FileProvider.
     */
    public static boolean installApkViaIntent(Context context, File apkFile) {
        if (context == null || apkFile == null || !apkFile.exists()) {
            return false;
        }

        try {
            String authority = context.getPackageName() + ".fileprovider";
            Uri apkUri = FileProvider.getUriForFile(context, authority, apkFile);

            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(installIntent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "FileProvider installation intent failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Positively checks if the package is installed and returns its versionCode.
     */
    public static int getInstalledPackageVersionCode(Context context, String packageName) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return -1; // Not installed
        }
    }

    /**
     * Launches the installed AimShot Engine application using its official launcher intent.
     */
    public static boolean launchInstalledApp(Context context) {
        if (context == null) return false;

        try {
            PackageManager pm = context.getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(TARGET_PACKAGE_NAME);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(launchIntent);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch installed app: " + e.getMessage());
        }
        return false;
    }
}
