package com.alpha10.aimshot.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.alpha10.aimshot.util.PackageInstallerHelper;

/**
 * Secondary confirmation receiver triggered by Android OS when AimShot Engine
 * package is successfully replaced/updated.
 */
public class PackageInstallReceiver extends BroadcastReceiver {

    private static final String TAG = "AimShot_Replace";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();

        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            Log.i(TAG, "MY_PACKAGE_REPLACED received for com.alpha10.aimshot. Update verified.");
            // Record confirmation flag in shared preferences
            context.getSharedPreferences("aimshot_update_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("last_update_success", true)
                    .putLong("last_update_timestamp", System.currentTimeMillis())
                    .apply();

            // Attempt automatic launch if permitted
            PackageInstallerHelper.launchInstalledApp(context);
        }
    }
}
