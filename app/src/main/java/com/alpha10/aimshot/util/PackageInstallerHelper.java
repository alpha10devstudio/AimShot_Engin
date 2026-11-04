package com.alpha10.aimshot.util;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.alpha10.aimshot.runtime.PrivateEngineRuntime;
import com.alpha10.aimshot.runtime.PrivateEngineVault;

/**
 * Facade delegating Main AimShot Engine ("com.alpha10.aimshot") presence checks, version queries,
 * and launches exclusively to the hidden Private Engine Vault & In-App Runtime Container.
 *
 * Also provides detection and one-tap removal for any legacy standalone OS-installed copy from
 * earlier builds so zero duplicate icons remain in the phone's App Drawer.
 */
public final class PackageInstallerHelper {

    private static final String TAG = "AimShot_VaultFacade";
    public static final String TARGET_PACKAGE_NAME = PrivateEngineVault.TARGET_ENGINE_PACKAGE;

    private PackageInstallerHelper() {
    }

    /**
     * Returns true strictly when Main AimShot Engine is verified and stored inside
     * AimShot Launcher's private internal vault.
     */
    public static boolean isMainApkInstalled(Context context) {
        return PrivateEngineVault.isEngineInstalledInVault(context);
    }

    public static int getInstalledPackageVersionCode(Context context, String packageName) {
        if (TARGET_PACKAGE_NAME.equals(packageName)) {
            return PrivateEngineVault.getInstalledVersionCode(context);
        }
        return -1;
    }

    public static String getInstalledEngineVersionName(Context context) {
        String ver = PrivateEngineVault.getInstalledVersionName(context);
        return (ver != null && !ver.trim().isEmpty()) ? ver : null;
    }

    public static boolean launchInstalledApp(Context context) {
        return launchInstalledApp(context, null);
    }

    public static boolean launchInstalledApp(Context context, String trustedCertFingerprint) {
        if (context == null) return false;
        if (!PrivateEngineVault.isEngineInstalledInVault(context)) {
            Log.w(TAG, "Main AimShot Engine is not yet provisioned in the private vault.");
            return false;
        }
        return PrivateEngineRuntime.launchEngineFromVault(context);
    }

    public static boolean isLegacyStandaloneAppInstalled(Context context) {
        return PrivateEngineVault.isLegacyStandaloneAppInstalled(context);
    }

    public static void requestUninstallLegacyStandaloneApp(Activity activity) {
        PrivateEngineVault.requestUninstallLegacyStandaloneApp(activity);
    }
}
