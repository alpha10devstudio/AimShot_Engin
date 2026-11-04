package com.alpha10.aimshot.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;

/**
 * Handles package management and launch execution for the Main AimShot Engine ("com.alpha10.aimshot")
 * installed as an official Android OS package.
 */
public final class PackageInstallerHelper {

    private static final String TAG = "AimShot_PkgInstaller";
    public static final String TARGET_PACKAGE_NAME = "com.alpha10.aimshot";
    public static final String FILE_PROVIDER_AUTHORITY = "com.alpha10.aimshot.launcher.fileprovider";

    private PackageInstallerHelper() {
    }

    /**
     * Checks if the Main AimShot Engine is installed on the Android operating system.
     */
    public static boolean isMainApkInstalled(Context context) {
        if (context == null) return false;
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo(TARGET_PACKAGE_NAME, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking package installation", e);
            return false;
        }
    }

    /**
     * Retrieves the installed version code of the target engine package.
     */
    public static int getInstalledPackageVersionCode(Context context, String packageName) {
        if (context == null) return -1;
        String pkg = (packageName != null && !packageName.isEmpty()) ? packageName : TARGET_PACKAGE_NAME;
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(pkg, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return (int) pi.getLongVersionCode();
            } else {
                @SuppressWarnings("deprecation")
                int vc = pi.versionCode;
                return vc;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Retrieves the installed version name (e.g., "1.0.0").
     */
    public static String getInstalledEngineVersionName(Context context) {
        if (context == null) return null;
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(TARGET_PACKAGE_NAME, 0);
            return pi.versionName;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Checks if the app has permission to request package installations on Android 8+ (Oreo+).
     */
    public static boolean canRequestPackageInstalls(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return context != null && context.getPackageManager().canRequestPackageInstalls();
        }
        return true;
    }

    /**
     * Directs user to the system Settings page to allow installing unknown apps from AimShot Launcher.
     */
    public static void requestInstallPermission(Activity activity, int requestCode) {
        if (activity == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent, requestCode);
        }
    }

    /**
     * Triggers the Android OS PackageInstaller to install the downloaded verified APK.
     */
    public static boolean installPackage(Activity activity, File apkFile) {
        if (activity == null || apkFile == null || !apkFile.exists()) {
            Log.e(TAG, "Cannot install: activity or apk file is invalid");
            return false;
        }

        try {
            Uri apkUri = FileProvider.getUriForFile(
                    activity,
                    FILE_PROVIDER_AUTHORITY,
                    apkFile
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            activity.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch OS PackageInstaller intent", e);
            return false;
        }
    }

    /**
     * Launches the installed AimShot Engine package directly via Intent.
     */
    public static boolean launchInstalledApp(Context context) {
        if (context == null) return false;
        try {
            PackageManager pm = context.getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(TARGET_PACKAGE_NAME);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                context.startActivity(launchIntent);
                return true;
            }

            // Fallback for packages without direct LAUNCHER category filter
            PackageInfo pi = pm.getPackageInfo(TARGET_PACKAGE_NAME, PackageManager.GET_ACTIVITIES);
            if (pi.activities != null && pi.activities.length > 0) {
                Intent explicitIntent = new Intent();
                explicitIntent.setClassName(TARGET_PACKAGE_NAME, pi.activities[0].name);
                explicitIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                context.startActivity(explicitIntent);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch installed package: " + TARGET_PACKAGE_NAME, e);
        }
        return false;
    }

    /**
     * No-op legacy check retained for binary compatibility.
     */
    public static boolean isLegacyStandaloneAppInstalled(Context context) {
        return false;
    }

    /**
     * No-op legacy uninstaller retained for binary compatibility.
     */
    public static void requestUninstallLegacyStandaloneApp(Activity activity) {
    }
}
