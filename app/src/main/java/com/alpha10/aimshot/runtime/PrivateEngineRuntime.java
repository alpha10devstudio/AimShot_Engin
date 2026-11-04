package com.alpha10.aimshot.runtime;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

import com.alpha10.aimshot.model.MembershipStatus;
import com.alpha10.aimshot.repository.MembershipRepository;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Core dynamic container runtime that loads and executes the Main AimShot Engine APK
 * ("com.alpha10.aimshot") directly from AimShot Launcher's private internal vault.
 *
 * Enforces active license verification before every launch and isolates plugin code,
 * resources, themes, Activities, and Services inside the Launcher process.
 */
public final class PrivateEngineRuntime {

    private static final String TAG = "AimShot_EngineRuntime";

    public static final String EXTRA_PLUGIN_ACTIVITY_CLASS = "com.alpha10.aimshot.extra.PLUGIN_ACTIVITY_CLASS";
    public static final String EXTRA_PLUGIN_SERVICE_CLASS = "com.alpha10.aimshot.extra.PLUGIN_SERVICE_CLASS";
    public static final String EXTRA_PLUGIN_ORIGINAL_INTENT = "com.alpha10.aimshot.extra.PLUGIN_ORIGINAL_INTENT";
    public static final String EXTRA_INSTRUMENTATION_HANDLED = "com.alpha10.aimshot.extra.INSTRUMENTATION_HANDLED";

    private static volatile LoadedPlugin sLoadedPlugin;
    private static volatile boolean sInstrumentationHooked = false;

    private PrivateEngineRuntime() {
    }

    public static final class LoadedPlugin {
        private final PrivateEngineVault.VaultMetadata metadata;
        private final IsolatedPluginClassLoader classLoader;
        private final AssetManager assetManager;
        private final PluginContextWrapper.PluginResources resources;
        private final ApplicationInfo hybridAppInfo;
        private final Map<String, Integer> activityThemes;
        private final int defaultThemeResId;
        private Application pluginApplication;

        LoadedPlugin(PrivateEngineVault.VaultMetadata metadata,
                     IsolatedPluginClassLoader classLoader,
                     AssetManager assetManager,
                     PluginContextWrapper.PluginResources resources,
                     ApplicationInfo hybridAppInfo,
                     Map<String, Integer> activityThemes,
                     int defaultThemeResId) {
            this.metadata = metadata;
            this.classLoader = classLoader;
            this.assetManager = assetManager;
            this.resources = resources;
            this.hybridAppInfo = hybridAppInfo;
            this.activityThemes = activityThemes;
            this.defaultThemeResId = defaultThemeResId;
        }

        public PrivateEngineVault.VaultMetadata getMetadata() {
            return metadata;
        }

        public IsolatedPluginClassLoader getClassLoader() {
            return classLoader;
        }

        public AssetManager getAssetManager() {
            return assetManager;
        }

        public PluginContextWrapper.PluginResources getResources() {
            return resources;
        }

        public ApplicationInfo getHybridApplicationInfo() {
            return hybridAppInfo;
        }

        public int getDefaultThemeResId() {
            return defaultThemeResId;
        }

        public int resolveActivityTheme(String className) {
            if (className != null && activityThemes.containsKey(className)) {
                int t = activityThemes.get(className);
                if (t != 0) return t;
            }
            return defaultThemeResId;
        }

        public Application getPluginApplication() {
            return pluginApplication;
        }

        void setPluginApplication(Application app) {
            this.pluginApplication = app;
        }
    }

    public static synchronized void invalidateLoadedEngine() {
        sLoadedPlugin = null;
    }

    public static LoadedPlugin getCachedPlugin() {
        return sLoadedPlugin;
    }

    /**
     * Installs the PluginInstrumentation hook on ActivityThread so registered stub EngineHostActivity
     * seamlessly instantiates and runs plugin Activities with full Android lifecycle support.
     */
    public static synchronized boolean installInstrumentationHook(Context context) {
        if (sInstrumentationHooked) {
            return true;
        }
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
            currentActivityThreadMethod.setAccessible(true);
            Object activityThread = currentActivityThreadMethod.invoke(null);

            if (activityThread == null) {
                Field sCurrentField = activityThreadClass.getDeclaredField("sCurrentActivityThread");
                sCurrentField.setAccessible(true);
                activityThread = sCurrentField.get(null);
            }

            if (activityThread == null) {
                return false;
            }

            Field mInstrumentationField = activityThreadClass.getDeclaredField("mInstrumentation");
            mInstrumentationField.setAccessible(true);
            Instrumentation current = (Instrumentation) mInstrumentationField.get(activityThread);

            if (!(current instanceof PluginInstrumentation)) {
                PluginInstrumentation wrapped = new PluginInstrumentation(context.getApplicationContext(), current);
                mInstrumentationField.set(activityThread, wrapped);
            }
            sInstrumentationHooked = true;
            Log.i(TAG, "PluginInstrumentation hook active.");
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "Instrumentation hook unavailable, proxy fallback will be used: " + t.getMessage());
            return false;
        }
    }

    /**
     * Loads or returns the cached Main AimShot Engine container from the Private Vault.
     */
    public static synchronized LoadedPlugin ensureLoaded(Context context) throws Exception {
        if (context == null) {
            throw new IllegalArgumentException("Context is null");
        }
        Context appCtx = context.getApplicationContext();
        installInstrumentationHook(appCtx);

        PrivateEngineVault.VaultMetadata metadata = PrivateEngineVault.getVaultMetadata(appCtx);
        if (metadata == null) {
            sLoadedPlugin = null;
            throw new IllegalStateException("Main AimShot Engine is not installed in the private vault.");
        }

        if (sLoadedPlugin != null
                && sLoadedPlugin.getMetadata().apkFile.getAbsolutePath().equals(metadata.apkFile.getAbsolutePath())
                && sLoadedPlugin.getMetadata().versionCode == metadata.versionCode) {
            sLoadedPlugin.getResources().syncConfiguration(
                    appCtx.getResources().getConfiguration(),
                    appCtx.getResources().getDisplayMetrics()
            );
            return sLoadedPlugin;
        }

        // Enforce Android 14+ read-only rule right before creating DexClassLoader
        if (metadata.apkFile.canWrite()) {
            metadata.apkFile.setReadable(true, true);
            metadata.apkFile.setWritable(false, false);
            metadata.apkFile.setReadOnly();
        }

        String apkPath = metadata.apkFile.getAbsolutePath();
        PackageManager pm = appCtx.getPackageManager();
        PackageInfo archiveInfo = pm.getPackageArchiveInfo(
                apkPath,
                PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES | PackageManager.GET_META_DATA
        );
        if (archiveInfo == null || archiveInfo.applicationInfo == null) {
            throw new IllegalStateException("Private Engine APK archive could not be parsed.");
        }

        ApplicationInfo hostAppInfo = appCtx.getApplicationInfo();
        ApplicationInfo hybridAppInfo = new ApplicationInfo(archiveInfo.applicationInfo);
        hybridAppInfo.sourceDir = apkPath;
        hybridAppInfo.publicSourceDir = apkPath;
        hybridAppInfo.nativeLibraryDir = metadata.nativeLibDir.getAbsolutePath();
        hybridAppInfo.dataDir = hostAppInfo.dataDir;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            hybridAppInfo.deviceProtectedDataDir = hostAppInfo.deviceProtectedDataDir;
        }
        hybridAppInfo.uid = hostAppInfo.uid;
        hybridAppInfo.packageName = hostAppInfo.packageName;

        // Build isolated plugin Resources & AssetManager
        Resources rawPluginRes = createPluginResources(appCtx, pm, archiveInfo.applicationInfo, apkPath);
        PluginContextWrapper.PluginResources pluginResources = new PluginContextWrapper.PluginResources(
                rawPluginRes,
                appCtx.getPackageName(),
                PrivateEngineVault.TARGET_ENGINE_PACKAGE
        );

        Map<String, Integer> activityThemes = new HashMap<>();
        if (archiveInfo.activities != null) {
            for (ActivityInfo ai : archiveInfo.activities) {
                if (ai != null && ai.name != null && ai.theme != 0) {
                    String norm = PrivateEngineVault.normalizeClassName(archiveInfo.packageName, ai.name);
                    activityThemes.put(norm, ai.theme);
                }
            }
        }

        int defaultTheme = metadata.themeResId != 0
                ? metadata.themeResId
                : archiveInfo.applicationInfo.theme;

        IsolatedPluginClassLoader classLoader = new IsolatedPluginClassLoader(
                apkPath,
                metadata.odexDir.getAbsolutePath(),
                metadata.nativeLibDir.getAbsolutePath(),
                appCtx.getClassLoader()
        );

        LoadedPlugin loaded = new LoadedPlugin(
                metadata,
                classLoader,
                rawPluginRes.getAssets(),
                pluginResources,
                hybridAppInfo,
                activityThemes,
                defaultTheme
        );

        // Instantiate & initialize plugin Application inside isolated PluginContextWrapper
        Application pluginApp = instantiatePluginApplication(appCtx, loaded, metadata.applicationClass);
        loaded.setPluginApplication(pluginApp);

        sLoadedPlugin = loaded;
        Log.i(TAG, "Loaded Main AimShot Engine v" + metadata.versionName + " inside Private Container.");
        return loaded;
    }

    /**
     * Launches the hidden Main AimShot Engine inside AimShot Launcher's private container,
     * strictly gated behind an active verified license session.
     */
    public static boolean launchEngineFromVault(Context context) {
        if (context == null) return false;

        MembershipStatus status = MembershipRepository.getInstance(context).getCurrentStatus();
        if (status == null || status.getState() != MembershipStatus.State.ACTIVE) {
            Log.e(TAG, "Blocked engine launch: active license verification required.");
            return false;
        }

        try {
            LoadedPlugin loaded = ensureLoaded(context);
            String entryClass = loaded.getMetadata().entryActivityClass;
            if (entryClass == null || entryClass.isEmpty()) {
                return false;
            }

            Intent hostIntent = new Intent(context, EngineHostActivity.class);
            hostIntent.putExtra(EXTRA_PLUGIN_ACTIVITY_CLASS, entryClass);
            if (!(context instanceof Activity)) {
                hostIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(hostIntent);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to launch Main AimShot Engine from vault: " + t.getMessage(), t);
            return false;
        }
    }

    public static boolean isLicenseActive(Context context) {
        if (context == null) return false;
        MembershipStatus status = MembershipRepository.getInstance(context).getCurrentStatus();
        return status != null && status.getState() == MembershipStatus.State.ACTIVE;
    }

    /**
     * Rewrites explicit internal plugin Activity intents to EngineHostActivity while leaving
     * external intents (games, browsers, Settings permissions) untouched.
     */
    public static Intent rewriteActivityIntentIfPlugin(Context hostContext, Intent intent) {
        if (hostContext == null || intent == null) return intent;
        if (intent.hasExtra(EXTRA_PLUGIN_ACTIVITY_CLASS)) return intent;

        ComponentName comp = intent.getComponent();
        if (comp == null) return intent;

        String className = comp.getClassName();
        if (className == null || className.isEmpty()) return intent;

        // Never rewrite AimShot Launcher's own manifest activities
        if (isHostLauncherComponent(hostContext, className)) {
            return intent;
        }

        LoadedPlugin loaded = sLoadedPlugin;
        if (loaded == null) {
            try {
                loaded = ensureLoaded(hostContext);
            } catch (Exception ignored) {
                return intent;
            }
        }

        boolean isKnownPluginActivity = loaded.getMetadata().activityClasses.contains(className);
        if (!isKnownPluginActivity
                && (PrivateEngineVault.TARGET_ENGINE_PACKAGE.equals(comp.getPackageName())
                || hostContext.getPackageName().equals(comp.getPackageName()))) {
            try {
                Class<?> candidate = Class.forName(className, false, loaded.getClassLoader());
                isKnownPluginActivity = Activity.class.isAssignableFrom(candidate);
            } catch (Throwable ignored) {
            }
        }

        if (isKnownPluginActivity) {
            Intent proxyIntent = new Intent(intent);
            proxyIntent.setComponent(new ComponentName(hostContext.getPackageName(), EngineHostActivity.class.getName()));
            proxyIntent.putExtra(EXTRA_PLUGIN_ACTIVITY_CLASS, className);
            proxyIntent.putExtra(EXTRA_PLUGIN_ORIGINAL_INTENT, new Intent(intent));
            return proxyIntent;
        }
        return intent;
    }

    /**
     * Rewrites explicit internal plugin Service intents (such as floating guideline/overlay services)
     * to EngineHostService.
     */
    public static Intent rewriteServiceIntentIfPlugin(Context hostContext, Intent serviceIntent) {
        if (hostContext == null || serviceIntent == null) return serviceIntent;
        if (serviceIntent.hasExtra(EXTRA_PLUGIN_SERVICE_CLASS)) return serviceIntent;

        ComponentName comp = serviceIntent.getComponent();
        if (comp == null) return serviceIntent;

        String className = comp.getClassName();
        if (className == null || className.isEmpty() || EngineHostService.class.getName().equals(className)) {
            return serviceIntent;
        }

        LoadedPlugin loaded = sLoadedPlugin;
        if (loaded == null) {
            try {
                loaded = ensureLoaded(hostContext);
            } catch (Exception ignored) {
                return serviceIntent;
            }
        }

        boolean isPluginService = loaded.getMetadata().serviceClasses.contains(className);
        if (!isPluginService) {
            try {
                Class<?> candidate = Class.forName(className, false, loaded.getClassLoader());
                isPluginService = android.app.Service.class.isAssignableFrom(candidate);
            } catch (Throwable ignored) {
            }
        }

        if (isPluginService) {
            Intent proxyService = new Intent(hostContext, EngineHostService.class);
            if (serviceIntent.getExtras() != null) {
                proxyService.putExtras(serviceIntent.getExtras());
            }
            proxyService.setAction(serviceIntent.getAction());
            proxyService.setData(serviceIntent.getData());
            proxyService.putExtra(EXTRA_PLUGIN_SERVICE_CLASS, className);
            proxyService.putExtra(EXTRA_PLUGIN_ORIGINAL_INTENT, new Intent(serviceIntent));
            return proxyService;
        }
        return serviceIntent;
    }

    public static boolean isPluginServiceIntent(Intent intent) {
        if (intent == null || intent.getComponent() == null) return false;
        String cls = intent.getComponent().getClassName();
        LoadedPlugin loaded = sLoadedPlugin;
        if (loaded != null && loaded.getMetadata().serviceClasses.contains(cls)) {
            return true;
        }
        if (loaded != null) {
            try {
                Class<?> c = Class.forName(cls, false, loaded.getClassLoader());
                return android.app.Service.class.isAssignableFrom(c);
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private static boolean isHostLauncherComponent(Context hostContext, String className) {
        try {
            PackageInfo hostInfo = hostContext.getPackageManager().getPackageInfo(
                    hostContext.getPackageName(),
                    PackageManager.GET_ACTIVITIES
            );
            if (hostInfo != null && hostInfo.activities != null) {
                for (ActivityInfo ai : hostInfo.activities) {
                    if (ai != null && className.equals(ai.name)
                            && !EngineHostActivity.class.getName().equals(ai.name)) {
                        // Beware: if both Launcher and Plugin have "com.alpha10.aimshot.MainActivity",
                        // check caller context: rewriteActivityIntentIfPlugin is only invoked from
                        // PluginContextWrapper or PluginInstrumentation when source is plugin!
                        return false;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static Resources createPluginResources(Context hostContext,
                                                   PackageManager pm,
                                                   ApplicationInfo rawPluginAppInfo,
                                                   String apkPath) throws Exception {
        try {
            ApplicationInfo copy = new ApplicationInfo(rawPluginAppInfo);
            copy.sourceDir = apkPath;
            copy.publicSourceDir = apkPath;
            if (copy.uid == 0) {
                copy.uid = hostContext.getApplicationInfo().uid;
            }
            Resources res = pm.getResourcesForApplication(copy);
            if (res != null) {
                return res;
            }
        } catch (Throwable ignored) {
        }

        AssetManager assetManager = AssetManager.class.newInstance();
        Method addAssetPath = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
        addAssetPath.setAccessible(true);
        int cookie = (Integer) addAssetPath.invoke(assetManager, apkPath);
        if (cookie == 0) {
            throw new IllegalStateException("Failed to attach private APK assets.");
        }
        Resources hostRes = hostContext.getResources();
        return new Resources(assetManager, hostRes.getDisplayMetrics(), hostRes.getConfiguration());
    }

    private static Application instantiatePluginApplication(Context hostAppCtx,
                                                            LoadedPlugin loaded,
                                                            String appClassName) {
        try {
            Application app;
            if (appClassName != null
                    && !appClassName.isEmpty()
                    && !"android.app.Application".equals(appClassName)) {
                Class<?> cls = Class.forName(appClassName, true, loaded.getClassLoader());
                app = (Application) cls.newInstance();
            } else {
                app = new Application();
            }

            PluginContextWrapper baseWrapper = new PluginContextWrapper(
                    hostAppCtx,
                    loaded,
                    loaded.getDefaultThemeResId()
            );

            Method attachMethod = Application.class.getDeclaredMethod("attach", Context.class);
            attachMethod.setAccessible(true);
            attachMethod.invoke(app, baseWrapper);

            loaded.setPluginApplication(app);
            app.onCreate();
            return app;
        } catch (Throwable t) {
            Log.w(TAG, "Fallback base Application used for plugin: " + t.getMessage());
            try {
                Application fallback = new Application();
                PluginContextWrapper baseWrapper = new PluginContextWrapper(
                        hostAppCtx,
                        loaded,
                        loaded.getDefaultThemeResId()
                );
                Method attachMethod = Application.class.getDeclaredMethod("attach", Context.class);
                attachMethod.setAccessible(true);
                attachMethod.invoke(fallback, baseWrapper);
                return fallback;
            } catch (Throwable ignored) {
                return null;
            }
        }
    }
}
