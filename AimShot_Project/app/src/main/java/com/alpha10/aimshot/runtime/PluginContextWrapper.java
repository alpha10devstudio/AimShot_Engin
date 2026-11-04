package com.alpha10.aimshot.runtime;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;

import java.lang.reflect.Constructor;

/**
 * Hybrid Context wrapper injected into dynamically loaded Main AimShot Engine Activities, Services,
 * and Application instances.
 *
 * - Supplies isolated Resources, AssetManager, Theme, and IsolatedPluginClassLoader from the private vault APK.
 * - Preserves host Launcher package identity ("com.alpha10.aimshot.launcher") on OS Binder calls
 *   (WindowManager overlays, Settings.canDrawOverlays, Toast, PendingIntent, SharedPreferences)
 *   so Android 10–14 UID verification passes without SecurityException.
 * - Transparently intercepts internal plugin Activity & Service intents and routes them through
 *   EngineHostActivity and EngineHostService.
 */
public class PluginContextWrapper extends ContextThemeWrapper {

    private final Context hostBaseContext;
    private final PrivateEngineRuntime.LoadedPlugin plugin;
    private Resources.Theme pluginTheme;
    private LayoutInflater pluginLayoutInflater;

    public PluginContextWrapper(Context base,
                                PrivateEngineRuntime.LoadedPlugin plugin,
                                int themeResId) {
        super(base, 0);
        this.hostBaseContext = base;
        this.plugin = plugin;

        int effectiveTheme = themeResId != 0 ? themeResId : plugin.getDefaultThemeResId();
        this.pluginTheme = plugin.getResources().newTheme();
        if (effectiveTheme != 0) {
            this.pluginTheme.applyStyle(effectiveTheme, true);
        }
    }

    @Override
    public AssetManager getAssets() {
        return plugin.getAssetManager();
    }

    @Override
    public Resources getResources() {
        return plugin.getResources();
    }

    @Override
    public Resources.Theme getTheme() {
        if (pluginTheme == null) {
            pluginTheme = plugin.getResources().newTheme();
            if (plugin.getDefaultThemeResId() != 0) {
                pluginTheme.applyStyle(plugin.getDefaultThemeResId(), true);
            }
        }
        return pluginTheme;
    }

    @Override
    public void setTheme(int resid) {
        if (resid != 0) {
            getTheme().applyStyle(resid, true);
        }
    }

    @Override
    public ClassLoader getClassLoader() {
        return plugin.getClassLoader();
    }

    @Override
    public Context getApplicationContext() {
        if (plugin.getPluginApplication() != null) {
            return plugin.getPluginApplication();
        }
        return this;
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        return plugin.getHybridApplicationInfo();
    }

    @Override
    public String getPackageName() {
        return hostBaseContext.getPackageName();
    }

    @Override
    public String getOpPackageName() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return hostBaseContext.getOpPackageName();
        }
        return hostBaseContext.getPackageName();
    }

    @Override
    public Object getSystemService(String name) {
        if (Context.LAYOUT_INFLATER_SERVICE.equals(name)) {
            if (pluginLayoutInflater == null) {
                LayoutInflater baseInflater = (LayoutInflater) super.getSystemService(name);
                pluginLayoutInflater = baseInflater.cloneInContext(this);
            }
            return pluginLayoutInflater;
        }
        return super.getSystemService(name);
    }

    @Override
    public void startActivity(Intent intent) {
        Intent routed = PrivateEngineRuntime.rewriteActivityIntentIfPlugin(hostBaseContext, intent);
        super.startActivity(routed);
    }

    @Override
    public void startActivity(Intent intent, Bundle options) {
        Intent routed = PrivateEngineRuntime.rewriteActivityIntentIfPlugin(hostBaseContext, intent);
        super.startActivity(routed, options);
    }

    @Override
    public ComponentName startService(Intent service) {
        Intent routed = PrivateEngineRuntime.rewriteServiceIntentIfPlugin(hostBaseContext, service);
        return super.startService(routed);
    }

    @Override
    public ComponentName startForegroundService(Intent service) {
        Intent routed = PrivateEngineRuntime.rewriteServiceIntentIfPlugin(hostBaseContext, service);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return super.startForegroundService(routed);
        }
        return super.startService(routed);
    }

    @Override
    public boolean stopService(Intent name) {
        if (PrivateEngineRuntime.isPluginServiceIntent(name)) {
            String targetService = name.getComponent() != null ? name.getComponent().getClassName() : null;
            return EngineHostService.stopHostedPluginService(hostBaseContext, targetService);
        }
        return super.stopService(name);
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        Intent routed = PrivateEngineRuntime.rewriteServiceIntentIfPlugin(hostBaseContext, service);
        return super.bindService(routed, conn, flags);
    }

    /**
     * Installs a custom Factory2 on the cloned LayoutInflater so all custom/AndroidX/Material views
     * inside plugin XML layouts are instantiated from IsolatedPluginClassLoader (avoiding Android's
     * static LayoutInflater.sConstructorMap ClassLoader cache collisions).
     */
    public static void installIsolatedInflaterFactory(LayoutInflater inflater, final ClassLoader pluginLoader) {
        if (inflater == null || pluginLoader == null) return;
        if (inflater.getFactory2() != null) return;

        inflater.setFactory2(new LayoutInflater.Factory2() {
            @Override
            public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
                return instantiatePluginView(name, context, attrs, pluginLoader);
            }

            @Override
            public View onCreateView(String name, Context context, AttributeSet attrs) {
                return instantiatePluginView(name, context, attrs, pluginLoader);
            }
        });
    }

    private static View instantiatePluginView(String name,
                                              Context context,
                                              AttributeSet attrs,
                                              ClassLoader pluginLoader) {
        if (name == null || name.indexOf('.') == -1) {
            return null; // Standard android.widget.* / android.view.* handled by PhoneLayoutInflater
        }
        if (name.startsWith("android.widget.")
                || name.startsWith("android.view.")
                || name.startsWith("android.webkit.")) {
            return null;
        }
        try {
            Class<? extends View> viewClass = Class.forName(name, false, pluginLoader).asSubclass(View.class);
            Constructor<? extends View> constructor = viewClass.getConstructor(Context.class, AttributeSet.class);
            constructor.setAccessible(true);
            return constructor.newInstance(context, attrs);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Resources wrapper mapping host package resource lookups seamlessly to "com.alpha10.aimshot".
     */
    @SuppressWarnings("deprecation")
    public static final class PluginResources extends Resources {
        private final Resources delegate;
        private final String hostPackageName;
        private final String pluginPackageName;

        public PluginResources(Resources delegate, String hostPackageName, String pluginPackageName) {
            super(delegate.getAssets(), delegate.getDisplayMetrics(), delegate.getConfiguration());
            this.delegate = delegate;
            this.hostPackageName = hostPackageName;
            this.pluginPackageName = pluginPackageName;
        }

        @Override
        public int getIdentifier(String name, String defType, String defPackage) {
            if (hostPackageName != null && hostPackageName.equals(defPackage)) {
                int pluginId = delegate.getIdentifier(name, defType, pluginPackageName);
                if (pluginId != 0) {
                    return pluginId;
                }
            }
            int id = delegate.getIdentifier(name, defType, defPackage);
            if (id == 0 && defPackage == null) {
                id = delegate.getIdentifier(name, defType, pluginPackageName);
            }
            return id;
        }

        public void syncConfiguration(Configuration config, android.util.DisplayMetrics metrics) {
            try {
                delegate.updateConfiguration(config, metrics);
                super.updateConfiguration(config, metrics);
            } catch (Exception ignored) {
            }
        }
    }
}
