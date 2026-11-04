package com.alpha10.aimshot.runtime;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.alpha10.aimshot.service.ApkDownloadService;
import com.alpha10.aimshot.util.ApkSecurityVerifier;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Manages the hidden Private Internal Vault for the Main AimShot Engine APK ("com.alpha10.aimshot").
 *
 * - Stores verified APK strictly inside private app storage (files/engine_vault/) invisible to
 *   file managers and external apps (zero OS PackageInstaller installation, zero App Drawer icon).
 * - Enforces Android 14+ (API 34) Read-Only DEX security rules (setReadOnly) prior to class loading.
 * - Extracts CPU ABI native libraries (.so) for JNI compatibility.
 * - Parses entry Launcher Activity, Application class, Services, theme, and overlay permissions.
 */
public final class PrivateEngineVault {

    private static final String TAG = "AimShot_PrivateVault";
    public static final String TARGET_ENGINE_PACKAGE = "com.alpha10.aimshot";

    private static final String VAULT_DIR_NAME = "engine_vault";
    private static final String PREFS_VAULT = "aimshot_private_vault_prefs";

    private static final String KEY_APK_PATH = "vault_apk_path";
    private static final String KEY_NATIVE_LIB_DIR = "vault_native_lib_dir";
    private static final String KEY_VERSION_CODE = "vault_version_code";
    private static final String KEY_VERSION_NAME = "vault_version_name";
    private static final String KEY_SHA256 = "vault_sha256";
    private static final String KEY_ENTRY_ACTIVITY = "vault_entry_activity";
    private static final String KEY_APPLICATION_CLASS = "vault_application_class";
    private static final String KEY_THEME_RES_ID = "vault_theme_res_id";
    private static final String KEY_REQUIRES_OVERLAY = "vault_requires_overlay";
    private static final String KEY_ACTIVITY_CLASSES = "vault_activity_classes";
    private static final String KEY_SERVICE_CLASSES = "vault_service_classes";

    private PrivateEngineVault() {
    }

    public static final class VaultMetadata {
        public final File apkFile;
        public final File nativeLibDir;
        public final File odexDir;
        public final int versionCode;
        public final String versionName;
        public final String sha256;
        public final String entryActivityClass;
        public final String applicationClass;
        public final int themeResId;
        public final boolean requiresOverlayPermission;
        public final Set<String> activityClasses;
        public final Set<String> serviceClasses;

        public VaultMetadata(File apkFile,
                             File nativeLibDir,
                             File odexDir,
                             int versionCode,
                             String versionName,
                             String sha256,
                             String entryActivityClass,
                             String applicationClass,
                             int themeResId,
                             boolean requiresOverlayPermission,
                             Set<String> activityClasses,
                             Set<String> serviceClasses) {
            this.apkFile = apkFile;
            this.nativeLibDir = nativeLibDir;
            this.odexDir = odexDir;
            this.versionCode = versionCode;
            this.versionName = versionName;
            this.sha256 = sha256;
            this.entryActivityClass = entryActivityClass;
            this.applicationClass = applicationClass;
            this.themeResId = themeResId;
            this.requiresOverlayPermission = requiresOverlayPermission;
            this.activityClasses = activityClasses != null ? activityClasses : new HashSet<String>();
            this.serviceClasses = serviceClasses != null ? serviceClasses : new HashSet<String>();
        }
    }

    /**
     * Installs a verified staged APK into the Launcher's private internal vault without OS installation.
     */
    public static synchronized VaultMetadata installVerifiedApkIntoVault(
            Context context,
            File stagedApkFile,
            ApkSecurityVerifier.VerificationResult verifyResult
    ) throws Exception {
        if (context == null) {
            throw new IllegalArgumentException("Context unavailable for Private Vault setup.");
        }
        if (stagedApkFile == null || !stagedApkFile.exists() || stagedApkFile.length() == 0) {
            throw new IllegalStateException("Verified APK file is missing or empty.");
        }

        Context appCtx = context.getApplicationContext();

        // Unload any previously cached runtime before replacing vault files
        PrivateEngineRuntime.invalidateLoadedEngine();

        File vaultRoot = new File(appCtx.getFilesDir(), VAULT_DIR_NAME);
        if (!vaultRoot.exists() && !vaultRoot.mkdirs()) {
            throw new IllegalStateException("Unable to create private engine vault directory.");
        }

        // Remove older vault APKs, native libs, and optimized dex caches cleanly
        File[] existing = vaultRoot.listFiles();
        if (existing != null) {
            for (File child : existing) {
                deleteRecursivelyUnlockingReadOnly(child);
            }
        }

        File odexRoot = new File(appCtx.getCodeCacheDir(), "engine_dex_opt");
        if (odexRoot.exists()) {
            deleteRecursivelyUnlockingReadOnly(odexRoot);
        }
        odexRoot.mkdirs();

        int versionCode = Math.max(1, verifyResult.getParsedVersionCode());
        String versionName = verifyResult.getParsedVersionName() != null
                ? verifyResult.getParsedVersionName()
                : "1.0.0";

        File vaultApk = new File(vaultRoot, "aimshot_engine_v" + versionCode + ".apk");
        copyFileAtomically(stagedApkFile, vaultApk);

        // Clean up staged download file immediately so only the vault copy exists
        try {
            stagedApkFile.delete();
        } catch (Exception ignored) {
        }
        ApkDownloadService.cleanupPrivateDownloads(appCtx);

        // Extract CPU ABI native .so libraries before locking APK read-only
        File nativeLibDir = new File(vaultRoot, "native_lib");
        nativeLibDir.mkdirs();
        extractBestMatchingNativeLibraries(vaultApk, nativeLibDir);

        // CRITICAL for Android 14 (API 34+): Dynamically loaded DEX/APK files MUST be read-only!
        vaultApk.setReadable(true, true);
        vaultApk.setWritable(false, false);
        vaultApk.setReadOnly();

        // Parse archive components & entry launcher activity
        PackageManager pm = appCtx.getPackageManager();
        int flags = PackageManager.GET_ACTIVITIES
                | PackageManager.GET_SERVICES
                | PackageManager.GET_META_DATA
                | PackageManager.GET_PERMISSIONS;
        PackageInfo archiveInfo = pm.getPackageArchiveInfo(vaultApk.getAbsolutePath(), flags);
        if (archiveInfo == null || archiveInfo.applicationInfo == null) {
            throw new IllegalStateException("Unable to inspect Main AimShot Engine components.");
        }

        ApplicationInfo appInfo = archiveInfo.applicationInfo;
        appInfo.sourceDir = vaultApk.getAbsolutePath();
        appInfo.publicSourceDir = vaultApk.getAbsolutePath();
        appInfo.nativeLibraryDir = nativeLibDir.getAbsolutePath();

        Set<String> activitySet = new HashSet<>();
        if (archiveInfo.activities != null) {
            for (ActivityInfo ai : archiveInfo.activities) {
                if (ai != null && ai.name != null) {
                    activitySet.add(normalizeClassName(archiveInfo.packageName, ai.name));
                }
            }
        }

        Set<String> serviceSet = new HashSet<>();
        if (archiveInfo.services != null) {
            for (ServiceInfo si : archiveInfo.services) {
                if (si != null && si.name != null) {
                    serviceSet.add(normalizeClassName(archiveInfo.packageName, si.name));
                }
            }
        }

        String entryActivity = discoverLauncherEntryActivity(vaultApk, archiveInfo);
        if (entryActivity == null || entryActivity.isEmpty()) {
            throw new IllegalStateException("No launchable entry Activity found inside Main AimShot Engine APK.");
        }
        activitySet.add(entryActivity);

        String applicationClass = appInfo.className != null
                ? normalizeClassName(archiveInfo.packageName, appInfo.className)
                : null;

        int resolvedTheme = appInfo.theme;
        if (archiveInfo.activities != null) {
            for (ActivityInfo ai : archiveInfo.activities) {
                if (ai != null && entryActivity.equals(normalizeClassName(archiveInfo.packageName, ai.name))) {
                    if (ai.theme != 0) {
                        resolvedTheme = ai.theme;
                    }
                    break;
                }
            }
        }

        boolean requiresOverlay = false;
        if (archiveInfo.requestedPermissions != null) {
            for (String perm : archiveInfo.requestedPermissions) {
                if ("android.permission.SYSTEM_ALERT_WINDOW".equals(perm)) {
                    requiresOverlay = true;
                    break;
                }
            }
        }

        SharedPreferences prefs = appCtx.getSharedPreferences(PREFS_VAULT, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_APK_PATH, vaultApk.getAbsolutePath())
                .putString(KEY_NATIVE_LIB_DIR, nativeLibDir.getAbsolutePath())
                .putInt(KEY_VERSION_CODE, versionCode)
                .putString(KEY_VERSION_NAME, versionName)
                .putString(KEY_SHA256, verifyResult.getCalculatedSha256())
                .putString(KEY_ENTRY_ACTIVITY, entryActivity)
                .putString(KEY_APPLICATION_CLASS, applicationClass)
                .putInt(KEY_THEME_RES_ID, resolvedTheme)
                .putBoolean(KEY_REQUIRES_OVERLAY, requiresOverlay)
                .putStringSet(KEY_ACTIVITY_CLASSES, activitySet)
                .putStringSet(KEY_SERVICE_CLASSES, serviceSet)
                .apply();

        Log.i(TAG, "Installed Main AimShot Engine v" + versionName + " (" + versionCode
                + ") into Private Vault. Entry=" + entryActivity + ", Services=" + serviceSet.size());

        return new VaultMetadata(
                vaultApk,
                nativeLibDir,
                odexRoot,
                versionCode,
                versionName,
                verifyResult.getCalculatedSha256(),
                entryActivity,
                applicationClass,
                resolvedTheme,
                requiresOverlay,
                activitySet,
                serviceSet
        );
    }

    public static boolean isEngineInstalledInVault(Context context) {
        return getVaultMetadata(context) != null;
    }

    public static int getInstalledVersionCode(Context context) {
        VaultMetadata meta = getVaultMetadata(context);
        return meta != null ? meta.versionCode : -1;
    }

    public static String getInstalledVersionName(Context context) {
        VaultMetadata meta = getVaultMetadata(context);
        return meta != null ? meta.versionName : null;
    }

    public static synchronized VaultMetadata getVaultMetadata(Context context) {
        if (context == null) return null;
        Context appCtx = context.getApplicationContext();
        SharedPreferences prefs = appCtx.getSharedPreferences(PREFS_VAULT, Context.MODE_PRIVATE);

        String apkPath = prefs.getString(KEY_APK_PATH, null);
        String entryActivity = prefs.getString(KEY_ENTRY_ACTIVITY, null);
        if (apkPath == null || entryActivity == null || entryActivity.trim().isEmpty()) {
            return null;
        }

        File apkFile = new File(apkPath);
        if (!apkFile.exists() || apkFile.length() == 0) {
            return null;
        }

        // Enforce Android 14+ read-only requirement on every access
        if (apkFile.canWrite()) {
            apkFile.setReadable(true, true);
            apkFile.setWritable(false, false);
            apkFile.setReadOnly();
        }

        String nativePath = prefs.getString(KEY_NATIVE_LIB_DIR, null);
        File nativeDir = nativePath != null ? new File(nativePath) : new File(apkFile.getParentFile(), "native_lib");
        if (!nativeDir.exists()) {
            nativeDir.mkdirs();
        }

        File odexDir = new File(appCtx.getCodeCacheDir(), "engine_dex_opt");
        if (!odexDir.exists()) {
            odexDir.mkdirs();
        }

        Set<String> savedActivities = prefs.getStringSet(KEY_ACTIVITY_CLASSES, null);
        Set<String> savedServices = prefs.getStringSet(KEY_SERVICE_CLASSES, null);

        return new VaultMetadata(
                apkFile,
                nativeDir,
                odexDir,
                prefs.getInt(KEY_VERSION_CODE, 1),
                prefs.getString(KEY_VERSION_NAME, "1.0.0"),
                prefs.getString(KEY_SHA256, ""),
                entryActivity,
                prefs.getString(KEY_APPLICATION_CLASS, null),
                prefs.getInt(KEY_THEME_RES_ID, 0),
                prefs.getBoolean(KEY_REQUIRES_OVERLAY, false),
                savedActivities != null ? new HashSet<>(savedActivities) : new HashSet<String>(),
                savedServices != null ? new HashSet<>(savedServices) : new HashSet<String>()
        );
    }

    /**
     * Checks whether an older standalone OS-installed copy of com.alpha10.aimshot is exposed
     * in the phone's App Drawer from earlier testing so the Launcher can prompt cleanup.
     */
    public static boolean isLegacyStandaloneAppInstalled(Context context) {
        if (context == null) return false;
        if (TARGET_ENGINE_PACKAGE.equals(context.getPackageName())) return false;
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(TARGET_ENGINE_PACKAGE, 0);
            return info != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Launches the standard Android OS uninstall prompt to remove any exposed standalone copy
     * of com.alpha10.aimshot from the device's App Drawer.
     */
    public static void requestUninstallLegacyStandaloneApp(Activity activity) {
        if (activity == null) return;
        try {
            Intent uninstallIntent = new Intent(Intent.ACTION_DELETE);
            uninstallIntent.setData(Uri.parse("package:" + TARGET_ENGINE_PACKAGE));
            activity.startActivity(uninstallIntent);
        } catch (Exception e) {
            Log.w(TAG, "Unable to launch uninstall prompt for legacy package: " + e.getMessage());
        }
    }

    private static String discoverLauncherEntryActivity(File apkFile, PackageInfo archiveInfo) {
        String pkgName = archiveInfo.packageName != null ? archiveInfo.packageName : TARGET_ENGINE_PACKAGE;

        // 1. Parse binary AndroidManifest.xml inside the APK to find exact MAIN + LAUNCHER component
        AssetManager assetManager = null;
        XmlResourceParser parser = null;
        try {
            assetManager = AssetManager.class.newInstance();
            Method addAssetPath = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            addAssetPath.setAccessible(true);
            int cookie = (Integer) addAssetPath.invoke(assetManager, apkFile.getAbsolutePath());
            if (cookie != 0) {
                parser = assetManager.openXmlResourceParser(cookie, "AndroidManifest.xml");
                String foundLauncher = parseManifestForLauncherActivity(parser, pkgName);
                if (foundLauncher != null && !foundLauncher.isEmpty()) {
                    return foundLauncher;
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Manifest XML inspection fallback: " + t.getMessage());
        } finally {
            if (parser != null) {
                try { parser.close(); } catch (Exception ignored) {}
            }
            if (assetManager != null) {
                try { assetManager.close(); } catch (Exception ignored) {}
            }
        }

        // 2. Fallback to PackageInfo activities list
        if (archiveInfo.activities != null && archiveInfo.activities.length > 0) {
            for (ActivityInfo ai : archiveInfo.activities) {
                if (ai != null && ai.name != null) {
                    String normalized = normalizeClassName(pkgName, ai.name);
                    if (normalized.endsWith("SplashActivity") || normalized.endsWith("MainActivity")) {
                        return normalized;
                    }
                }
            }
            if (archiveInfo.activities[0] != null && archiveInfo.activities[0].name != null) {
                return normalizeClassName(pkgName, archiveInfo.activities[0].name);
            }
        }
        return null;
    }

    private static String parseManifestForLauncherActivity(XmlResourceParser parser, String defaultPkg) throws Exception {
        String androidNs = "http://schemas.android.com/apk/res/android";
        String manifestPkg = defaultPkg;

        String currentComponent = null;
        String currentAliasTarget = null;
        boolean inActivityOrAlias = false;
        boolean inIntentFilter = false;
        boolean hasMainAction = false;
        boolean hasLauncherCategory = false;

        Map<String, String> aliasTargets = new HashMap<>();
        List<String> launcherCandidates = new ArrayList<>();

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tag = parser.getName();
                if ("manifest".equals(tag)) {
                    String p = parser.getAttributeValue(null, "package");
                    if (p != null && !p.isEmpty()) {
                        manifestPkg = p;
                    }
                } else if ("activity".equals(tag) || "activity-alias".equals(tag)) {
                    inActivityOrAlias = true;
                    String rawName = parser.getAttributeValue(androidNs, "name");
                    currentComponent = normalizeClassName(manifestPkg, rawName);
                    if ("activity-alias".equals(tag)) {
                        String rawTarget = parser.getAttributeValue(androidNs, "targetActivity");
                        currentAliasTarget = normalizeClassName(manifestPkg, rawTarget);
                        if (currentComponent != null && currentAliasTarget != null) {
                            aliasTargets.put(currentComponent, currentAliasTarget);
                        }
                    } else {
                        currentAliasTarget = null;
                    }
                } else if (inActivityOrAlias && "intent-filter".equals(tag)) {
                    inIntentFilter = true;
                    hasMainAction = false;
                    hasLauncherCategory = false;
                } else if (inIntentFilter && "action".equals(tag)) {
                    String actionName = parser.getAttributeValue(androidNs, "name");
                    if ("android.intent.action.MAIN".equals(actionName)) {
                        hasMainAction = true;
                    }
                } else if (inIntentFilter && "category".equals(tag)) {
                    String catName = parser.getAttributeValue(androidNs, "name");
                    if ("android.intent.category.LAUNCHER".equals(catName)) {
                        hasLauncherCategory = true;
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                String tag = parser.getName();
                if (inIntentFilter && "intent-filter".equals(tag)) {
                    if (hasMainAction && hasLauncherCategory && currentComponent != null) {
                        String resolved = aliasTargets.containsKey(currentComponent)
                                ? aliasTargets.get(currentComponent)
                                : currentComponent;
                        launcherCandidates.add(resolved);
                    }
                    inIntentFilter = false;
                } else if ("activity".equals(tag) || "activity-alias".equals(tag)) {
                    inActivityOrAlias = false;
                    currentComponent = null;
                    currentAliasTarget = null;
                }
            }
            eventType = parser.next();
        }

        return launcherCandidates.isEmpty() ? null : launcherCandidates.get(0);
    }

    private static void extractBestMatchingNativeLibraries(File apkFile, File destNativeDir) {
        String[] supportedAbis = Build.SUPPORTED_ABIS;
        if (supportedAbis == null || supportedAbis.length == 0) return;

        try (ZipFile zipFile = new ZipFile(apkFile)) {
            for (String abi : supportedAbis) {
                String prefix = "lib/" + abi + "/";
                List<ZipEntry> matchingEntries = new ArrayList<>();
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory() && entry.getName().startsWith(prefix) && entry.getName().endsWith(".so")) {
                        matchingEntries.add(entry);
                    }
                }

                if (!matchingEntries.isEmpty()) {
                    byte[] buffer = new byte[16384];
                    for (ZipEntry soEntry : matchingEntries) {
                        String fileName = new File(soEntry.getName()).getName();
                        File outSo = new File(destNativeDir, fileName);
                        try (InputStream is = zipFile.getInputStream(soEntry);
                             FileOutputStream fos = new FileOutputStream(outSo)) {
                            int len;
                            while ((len = is.read(buffer)) != -1) {
                                fos.write(buffer, 0, len);
                            }
                            fos.flush();
                            fos.getFD().sync();
                        }
                        outSo.setReadable(true, false);
                        outSo.setExecutable(true, false);
                    }
                    Log.i(TAG, "Extracted " + matchingEntries.size() + " native .so libraries for ABI: " + abi);
                    return;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Native library extraction skipped/warning: " + e.getMessage());
        }
    }

    private static void copyFileAtomically(File src, File dst) throws Exception {
        byte[] buffer = new byte[32768];
        try (FileInputStream fis = new FileInputStream(src);
             FileOutputStream fos = new FileOutputStream(dst)) {
            int read;
            while ((read = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            fos.flush();
            fos.getFD().sync();
        }
    }

    private static void deleteRecursivelyUnlockingReadOnly(File target) {
        if (target == null || !target.exists()) return;
        try {
            target.setWritable(true, true);
        } catch (Exception ignored) {
        }
        if (target.isDirectory()) {
            File[] children = target.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursivelyUnlockingReadOnly(child);
                }
            }
        }
        try {
            target.delete();
        } catch (Exception ignored) {
        }
    }

    public static String normalizeClassName(String pkg, String rawClassName) {
        if (rawClassName == null || rawClassName.trim().isEmpty()) return null;
        String trimmed = rawClassName.trim();
        if (trimmed.startsWith(".")) {
            return pkg + trimmed;
        }
        if (!trimmed.contains(".")) {
            return pkg + "." + trimmed;
        }
        return trimmed;
    }
}
