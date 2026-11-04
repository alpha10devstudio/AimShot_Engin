package com.alpha10.aimshot.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.alpha10.aimshot.util.PackageInstallerHelper;

/**
 * Secondary OS package broadcast observer for com.alpha10.aimshot installation events.
 * Does NOT auto-launch Main AimShot Engine without explicit user interaction.
 */
public class PackageInstallReceiver extends BroadcastReceiver {

    private static final String TAG = "AimShot_PkgObserver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        String action = intent.getAction();

        if (Intent.ACTION_PACKAGE_ADDED.equals(action) || Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            Uri data = intent.getData();
            String pkg = data != null ? data.getSchemeSpecificPart() : null;
            if (PackageInstallerHelper.TARGET_PACKAGE_NAME.equals(pkg)) {
                Log.i(TAG, "OS confirmed Main AimShot Engine package event (" + action + ") for " + pkg);
                context.getSharedPreferences("aimshot_update_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("last_update_success", true)
                        .putLong("last_update_timestamp", System.currentTimeMillis())
                        .apply();
            }
        }
    }
}
