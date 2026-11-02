package com.alpha10.aimshot.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;

/**
 * Handles official Android package installation via FileProvider and the PackageInstaller API.
 */
public class PackageInstallerHelper {

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
                intent.setData(Uri.parse("package:" + contextPackageName(activity)));
                activity.startActivityForResult(intent, REQUEST_CODE_UNKNOWN_APPS);
            } catch (Exception e) {
                Intent fallback = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                activity.startActivity(fallback);
            }
        }
    }

    /**
     * Dispatches the validated APK file to the official Android Package Installer.
     */
    public static boolean installApk(Context context, File apkFile) {
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
            Toast.makeText(context, "Error initiating package installer: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private static String contextPackageName(Context context) {
        return context != null ? context.getPackageName() : "com.alpha10.aimshot";
    }
}
