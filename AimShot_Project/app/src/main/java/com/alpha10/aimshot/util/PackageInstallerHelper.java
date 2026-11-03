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
     * Preference keys for tracking installed Main APK state.
     */
    public static final String PREF_NAME = "AimShot_Launcher_State";
    public static final String KEY_MAIN_APK_INSTALLED = "main_apk_installed";

    /**
     * Sets whether the Main AimShot Engine APK has been verified and installed.
     */
    public static void setMainApkInstalled(Context context, boolean installed) {
        if (context == null) return;
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_MAIN_APK_INSTALLED, installed)
                .apply();
        Log.i(TAG, "Main APK installed state updated to: " + installed);
    }

    /**
     * Checks if the Main AimShot Engine APK (com.alpha10.aimshot) is installed.
     * When running as a separate launcher, inspects the OS PackageManager.
     * When running in unified launcher environment, checks verified installation state.
     */
    public static boolean isMainApkInstalled(Context context) {
        if (context == null) return false;

        // Situation A: Check if package is installed in the Android OS
        if (!context.getPackageName().equals(TARGET_PACKAGE_NAME)) {
            try {
                PackageManager pm = context.getPackageManager();
                Intent launchIntent = pm.getLaunchIntentForPackage(TARGET_PACKAGE_NAME);
                if (launchIntent != null) {
                    return true;
                }
                PackageInfo info = pm.getPackageInfo(TARGET_PACKAGE_NAME, 0);
                return info != null;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            } catch (Exception e) {
                return false;
            }
        }

        // Check persistent launcher installation flag
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_MAIN_APK_INSTALLED, false);
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
     * Launches the installed Main AimShot Engine application using its official launcher intent.
     * Under NO circumstances will this launch Carrom Pool or any unrelated package.
     */
    public static boolean launchInstalledApp(Context context) {
        if (context == null) return false;

        try {
            PackageManager pm = context.getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(TARGET_PACKAGE_NAME);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(launchIntent);
                Log.i(TAG, "Successfully started launch intent for: " + TARGET_PACKAGE_NAME);
                return true;
            }

            // Fallback explicit action intent for Main AimShot Engine
            Intent mainIntent = new Intent(Intent.ACTION_MAIN);
            mainIntent.setPackage(TARGET_PACKAGE_NAME);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (mainIntent.resolveActivity(pm) != null) {
                context.startActivity(mainIntent);
                Log.i(TAG, "Started explicit ACTION_MAIN intent for: " + TARGET_PACKAGE_NAME);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch installed app: " + e.getMessage(), e);
        }
        return false;
    }
}
