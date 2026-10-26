package com.alpha10.aimshot;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.alpha10.aimshot.util.FontHelper;
import com.alpha10.aimshot.util.LocaleHelper;

/**
 * Custom Application class ensuring theme persistence, font caching, and locale integrity.
 */
public class AimShotApp extends Application {

    private static final String PREFS_NAME = "aimshot_settings_prefs";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Restore saved theme before any activity renders
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, true);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        // 2. Pre-cache fonts
        FontHelper.getBrandFont(this);
        FontHelper.getRegularFont(this);
    }
}
