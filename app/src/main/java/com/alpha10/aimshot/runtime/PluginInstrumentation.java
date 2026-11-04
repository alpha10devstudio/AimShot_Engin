package com.alpha10.aimshot.runtime;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Window;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Custom Instrumentation bridge enabling unregistered Activities inside the private vault APK
 * ("com.alpha10.aimshot") to run as genuine first-class Android Activities hosted through
 * the non-exported EngineHostActivity manifest slot.
 */
public class PluginInstrumentation extends Instrumentation {

    private static final String TAG = "AimShot_PluginInstr";

    private final Context hostAppContext;
    private final Instrumentation base;

    public PluginInstrumentation(Context hostAppContext, Instrumentation base) {
        this.hostAppContext = hostAppContext;
        this.base = base;
    }

    /**
     * Intercepts Activity.startActivityForResult calls originating inside plugin Activities
     * so internal navigation between multiple screens inside com.alpha10.aimshot routes cleanly
     * through EngineHostActivity.
     *
     * Note: Omitting @Override intentionally because execStartActivity is hidden in compile-time
     * android.jar stubs while present as a public virtual method on runtime android.app.Instrumentation.
     */
    public ActivityResult execStartActivity(Context who,
                                            IBinder contextThread,
                                            IBinder token,
                                            Activity target,
                                            Intent intent,
                                            int requestCode,
                                            Bundle options) {
        Intent effectiveIntent = intent;
        try {
            if (isFromPlugin(who, target)) {
                effectiveIntent = PrivateEngineRuntime.rewriteActivityIntentIfPlugin(hostAppContext, intent);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Intent rewrite warning: " + t.getMessage());
        }

        try {
            Method execMethod = Instrumentation.class.getDeclaredMethod(
                    "execStartActivity",
                    Context.class,
                    IBinder.class,
                    IBinder.class,
                    Activity.class,
                    Intent.class,
                    int.class,
                    Bundle.class
            );
            execMethod.setAccessible(true);
            return (ActivityResult) execMethod.invoke(
                    base,
                    who,
                    contextThread,
                    token,
                    target,
                    effectiveIntent,
                    requestCode,
                    options
            );
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getTargetException();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(cause);
        } catch (Throwable t) {
            throw new RuntimeException("Unable to delegate execStartActivity", t);
        }
    }

    @Override
    public Activity newActivity(ClassLoader cl, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (EngineHostActivity.class.getName().equals(className)
                && intent != null
                && intent.hasExtra(PrivateEngineRuntime.EXTRA_PLUGIN_ACTIVITY_CLASS)) {

            // Enforce active Launcher license check before instantiating any plugin Activity
            if (!PrivateEngineRuntime.isLicenseActive(hostAppContext)) {
                Log.w(TAG, "Blocked unauthorized EngineHostActivity instantiation without active license.");
                return base.newActivity(cl, className, intent);
            }

            String targetPluginClass = intent.getStringExtra(PrivateEngineRuntime.EXTRA_PLUGIN_ACTIVITY_CLASS);
            if (targetPluginClass != null && !targetPluginClass.isEmpty()) {
                try {
                    PrivateEngineRuntime.LoadedPlugin plugin = PrivateEngineRuntime.ensureLoaded(hostAppContext);
                    Class<?> pluginActivityClass = plugin.getClassLoader().loadClass(targetPluginClass);
                    Activity pluginActivity = (Activity) pluginActivityClass.newInstance();
                    intent.putExtra(PrivateEngineRuntime.EXTRA_INSTRUMENTATION_HANDLED, true);

                    Intent original = intent.getParcelableExtra(PrivateEngineRuntime.EXTRA_PLUGIN_ORIGINAL_INTENT);
                    if (original != null && original.getExtras() != null) {
                        intent.putExtras(original.getExtras());
                    }
                    return pluginActivity;
                } catch (Throwable t) {
                    Log.e(TAG, "Failed to instantiate plugin Activity " + targetPluginClass + ": " + t.getMessage(), t);
                }
            }
        }
        return base.newActivity(cl, className, intent);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        preparePluginActivityIfNeeded(activity);
        base.callActivityOnCreate(activity, icicle);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle, PersistableBundle persistentState) {
        preparePluginActivityIfNeeded(activity);
        base.callActivityOnCreate(activity, icicle, persistentState);
    }

    private boolean isFromPlugin(Context who, Activity target) {
        PrivateEngineRuntime.LoadedPlugin plugin = PrivateEngineRuntime.getCachedPlugin();
        if (plugin == null) return false;
        ClassLoader pluginCl = plugin.getClassLoader();
        if (target != null && target.getClass().getClassLoader() == pluginCl) {
            return true;
        }
        if (who instanceof PluginContextWrapper) {
            return true;
        }
        return who != null && who.getClassLoader() == pluginCl;
    }

    /**
     * Injects isolated PluginResources, PluginContextWrapper, PluginApplication, theme, and
     * PhoneWindow LayoutInflater onto a newly created plugin Activity right before onCreate().
     */
    private void preparePluginActivityIfNeeded(Activity activity) {
        if (activity == null) return;
        PrivateEngineRuntime.LoadedPlugin plugin = PrivateEngineRuntime.getCachedPlugin();
        if (plugin == null) return;

        if (activity.getClass().getClassLoader() != plugin.getClassLoader()) {
            return;
        }

        try {
            String actClassName = activity.getClass().getName();
            int themeRes = plugin.resolveActivityTheme(actClassName);

            Context rawBase = activity.getBaseContext();
            PluginContextWrapper wrappedBase = new PluginContextWrapper(rawBase, plugin, themeRes);

            // 1. Replace ContextWrapper.mBase with PluginContextWrapper
            Field mBaseField = ContextWrapper.class.getDeclaredField("mBase");
            mBaseField.setAccessible(true);
            mBaseField.set(activity, wrappedBase);

            // 2. Replace ContextThemeWrapper.mResources & reset mTheme so plugin theme resolves cleanly
            try {
                Field mResourcesField = ContextThemeWrapper.class.getDeclaredField("mResources");
                mResourcesField.setAccessible(true);
                mResourcesField.set(activity, plugin.getResources());
            } catch (Throwable ignored) {
            }

            try {
                Field mThemeField = ContextThemeWrapper.class.getDeclaredField("mTheme");
                mThemeField.setAccessible(true);
                mThemeField.set(activity, null);
            } catch (Throwable ignored) {
            }

            // 3. Inject plugin Application & ActivityInfo metadata
            if (plugin.getPluginApplication() != null) {
                try {
                    Field mAppField = Activity.class.getDeclaredField("mApplication");
                    mAppField.setAccessible(true);
                    mAppField.set(activity, plugin.getPluginApplication());
                } catch (Throwable ignored) {
                }
            }

            try {
                Field mActivityInfoField = Activity.class.getDeclaredField("mActivityInfo");
                mActivityInfoField.setAccessible(true);
                ActivityInfo info = (ActivityInfo) mActivityInfoField.get(activity);
                if (info != null) {
                    if (themeRes != 0) {
                        info.theme = themeRes;
                    }
                    info.applicationInfo = plugin.getHybridApplicationInfo();
                }
            } catch (Throwable ignored) {
            }

            if (themeRes != 0) {
                activity.setTheme(themeRes);
            }

            // 4. Rebind PhoneWindow's LayoutInflater to PluginContextWrapper so setContentView()
            //    inflates layouts using the plugin's Resources and IsolatedPluginClassLoader
            Window window = activity.getWindow();
            if (window != null) {
                try {
                    LayoutInflater cloned = window.getLayoutInflater().cloneInContext(wrappedBase);
                    Field winInflaterField = window.getClass().getDeclaredField("mLayoutInflater");
                    winInflaterField.setAccessible(true);
                    winInflaterField.set(window, cloned);
                } catch (Throwable ignored) {
                }
            }

        } catch (Throwable t) {
            Log.e(TAG, "Error preparing plugin Activity environment: " + t.getMessage(), t);
        }
    }
}
