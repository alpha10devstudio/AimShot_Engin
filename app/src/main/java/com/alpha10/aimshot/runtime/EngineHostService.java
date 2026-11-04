package com.alpha10.aimshot.runtime;

import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Non-exported proxy Service hosting floating overlay / guideline background Services declared
 * inside the private Main AimShot Engine APK ("com.alpha10.aimshot").
 *
 * Shares this Service's valid OS Binder token (mToken / mActivityManager) with hosted plugin
 * Services so startForeground(), stopForeground(), stopSelf(), and WindowManager overlays
 * execute natively under AimShot Launcher's process UID.
 */
public class EngineHostService extends Service {

    private static final String TAG = "AimShot_EngineHostSvc";
    private static final String ACTION_STOP_PLUGIN_SERVICE = "com.alpha10.aimshot.action.STOP_PLUGIN_SERVICE";

    private final Map<String, Service> activePluginServices = new ConcurrentHashMap<>();

    public static boolean stopHostedPluginService(Context context, String pluginServiceClass) {
        if (context == null) return false;
        Intent stopIntent = new Intent(context, EngineHostService.class);
        stopIntent.setAction(ACTION_STOP_PLUGIN_SERVICE);
        if (pluginServiceClass != null) {
            stopIntent.putExtra(PrivateEngineRuntime.EXTRA_PLUGIN_SERVICE_CLASS, pluginServiceClass);
        }
        context.startService(stopIntent);
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!PrivateEngineRuntime.isLicenseActive(this)) {
            stopAllPluginServices();
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent == null) {
            return START_STICKY;
        }

        if (ACTION_STOP_PLUGIN_SERVICE.equals(intent.getAction())) {
            String stopTarget = intent.getStringExtra(PrivateEngineRuntime.EXTRA_PLUGIN_SERVICE_CLASS);
            if (stopTarget != null && activePluginServices.containsKey(stopTarget)) {
                Service removed = activePluginServices.remove(stopTarget);
                if (removed != null) {
                    try { removed.onDestroy(); } catch (Throwable ignored) {}
                }
            } else {
                stopAllPluginServices();
            }
            if (activePluginServices.isEmpty()) {
                stopSelf();
            }
            return START_NOT_STICKY;
        }

        String serviceClass = intent.getStringExtra(PrivateEngineRuntime.EXTRA_PLUGIN_SERVICE_CLASS);
        if (serviceClass == null || serviceClass.isEmpty()) {
            return START_NOT_STICKY;
        }

        try {
            Service pluginService = getOrCreatePluginService(serviceClass);
            Intent original = intent.getParcelableExtra(PrivateEngineRuntime.EXTRA_PLUGIN_ORIGINAL_INTENT);
            Intent forwardIntent = original != null ? original : intent;
            return pluginService.onStartCommand(forwardIntent, flags, startId);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to start hosted plugin Service " + serviceClass + ": " + t.getMessage(), t);
            return START_NOT_STICKY;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (intent == null || !PrivateEngineRuntime.isLicenseActive(this)) {
            return null;
        }
        String serviceClass = intent.getStringExtra(PrivateEngineRuntime.EXTRA_PLUGIN_SERVICE_CLASS);
        if (serviceClass == null || serviceClass.isEmpty()) {
            return null;
        }
        try {
            Service pluginService = getOrCreatePluginService(serviceClass);
            Intent original = intent.getParcelableExtra(PrivateEngineRuntime.EXTRA_PLUGIN_ORIGINAL_INTENT);
            return pluginService.onBind(original != null ? original : intent);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to bind hosted plugin Service " + serviceClass + ": " + t.getMessage(), t);
            return null;
        }
    }

    @Override
    public void onDestroy() {
        stopAllPluginServices();
        super.onDestroy();
    }

    private synchronized Service getOrCreatePluginService(String serviceClass) throws Exception {
        Service existing = activePluginServices.get(serviceClass);
        if (existing != null) {
            return existing;
        }

        PrivateEngineRuntime.LoadedPlugin plugin = PrivateEngineRuntime.ensureLoaded(this);
        Class<?> cls = plugin.getClassLoader().loadClass(serviceClass);
        Service created = (Service) cls.newInstance();

        PluginContextWrapper serviceContext = new PluginContextWrapper(
                getBaseContext(),
                plugin,
                plugin.getDefaultThemeResId()
        );

        // Attach host Service's valid OS token & ActivityManager so startForeground / stopSelf work natively
        Field mBaseField = ContextWrapper.class.getDeclaredField("mBase");
        mBaseField.setAccessible(true);
        mBaseField.set(created, serviceContext);

        copyServiceField("mThread", this, created);
        copyServiceField("mClassName", this, created);
        copyServiceField("mToken", this, created);
        copyServiceField("mActivityManager", this, created);

        if (plugin.getPluginApplication() != null) {
            try {
                Field mAppField = Service.class.getDeclaredField("mApplication");
                mAppField.setAccessible(true);
                mAppField.set(created, plugin.getPluginApplication());
            } catch (Throwable ignored) {
            }
        } else {
            copyServiceField("mApplication", this, created);
        }

        created.onCreate();
        activePluginServices.put(serviceClass, created);
        return created;
    }

    private void stopAllPluginServices() {
        for (Service svc : activePluginServices.values()) {
            try {
                svc.onDestroy();
            } catch (Throwable ignored) {
            }
        }
        activePluginServices.clear();
    }

    private static void copyServiceField(String name, Service from, Service to) {
        try {
            Field f = Service.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(to, f.get(from));
        } catch (Throwable ignored) {
        }
    }
}
