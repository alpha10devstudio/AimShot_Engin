package com.alpha10.aimshot.runtime;

import android.content.Context;

import dalvik.system.DexClassLoader;

/**
 * Isolated child-first DexClassLoader for the Private Engine APK ("com.alpha10.aimshot").
 *
 * Uses Android's BootClassLoader (Context.class.getClassLoader()) as its direct parent so that:
 * 1. Core Android OS classes (android.app.Activity, android.app.Service, android.view.View, etc.)
 *    are shared seamlessly with the Android OS runtime.
 * 2. App-level classes inside "com.alpha10.aimshot" (including its own com.alpha10.aimshot.MainActivity,
 *    com.alpha10.aimshot.R, androidx.*, and com.google.android.material.*) NEVER collide with
 *    AimShot Launcher's identically-named Java namespace classes or R resource IDs.
 */
public final class IsolatedPluginClassLoader extends DexClassLoader {

    private final ClassLoader hostFallbackLoader;

    public IsolatedPluginClassLoader(String dexPath,
                                     String optimizedDirectory,
                                     String librarySearchPath,
                                     ClassLoader hostClassLoader) {
        super(
                dexPath,
                optimizedDirectory,
                librarySearchPath,
                Context.class.getClassLoader()
        );
        this.hostFallbackLoader = hostClassLoader;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name == null) {
            throw new ClassNotFoundException("null");
        }

        Class<?> loaded = findLoadedClass(name);
        if (loaded != null) {
            if (resolve) resolveClass(loaded);
            return loaded;
        }

        // 1. Always delegate core Android OS / JDK boot classes to BootClassLoader
        if (isFrameworkBootClass(name)) {
            try {
                Class<?> bootClass = getParent().loadClass(name);
                if (resolve) resolveClass(bootClass);
                return bootClass;
            } catch (ClassNotFoundException ignored) {
            }
        }

        // 2. Load plugin's own classes (com.alpha10.aimshot.*, androidx.*, material.*, etc.) from private APK first
        try {
            Class<?> pluginClass = findClass(name);
            if (resolve) resolveClass(pluginClass);
            return pluginClass;
        } catch (ClassNotFoundException ignored) {
        }

        // 3. Fallback to BootClassLoader (e.g. org.apache.http.legacy or vendor framework classes)
        try {
            Class<?> parentClass = super.loadClass(name, resolve);
            if (parentClass != null) {
                return parentClass;
            }
        } catch (ClassNotFoundException ignored) {
        }

        // 4. Last-resort fallback to host only for non-conflicting utility classes
        if (hostFallbackLoader != null
                && !name.startsWith("com.alpha10.aimshot.")
                && !name.startsWith("androidx.")
                && !name.startsWith("com.google.android.material.")) {
            return hostFallbackLoader.loadClass(name);
        }

        throw new ClassNotFoundException(name);
    }

    private static boolean isFrameworkBootClass(String name) {
        if (name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("dalvik.")
                || name.startsWith("sun.")
                || name.startsWith("org.json.")
                || name.startsWith("org.xml.")
                || name.startsWith("org.w3c.")
                || name.startsWith("org.xmlpull.")) {
            return true;
        }
        if (name.startsWith("android.")) {
            // Keep android.support.* isolated to the plugin APK if used
            return !name.startsWith("android.support.");
        }
        return false;
    }
}
