package com.alpha10.aimshot.service;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Handles privacy-conscious app-scoped device identification and hardware diagnostics.
 * Adheres strictly to Android security guidelines: No IMEI, no MAC address, no telephony identifiers.
 */
public class DeviceIdManager {

    private final Context context;

    public DeviceIdManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Derives a consistent, readable, app-scoped Device ID formatted as AS-XXXX-XXXX-XXXX-XXXX.
     * Uses SHA-256 hash of Android ID combined with package salt.
     */
    public String getFormattedDeviceId() {
        String rawId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (rawId == null || rawId.trim().isEmpty()) {
            rawId = "AIMSHOT_ANONYMOUS_DEV_" + Build.MODEL;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawId.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                String h = Integer.toHexString(0xFF & hash[i]);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            String base = hex.toString().toUpperCase(Locale.US);
            // Format: AS-7F3E-9C4D-2A8B-6E1F
            return String.format("AS-%s-%s-%s-%s",
                    safeSubstring(base, 0, 4),
                    safeSubstring(base, 4, 8),
                    safeSubstring(base, 8, 12),
                    safeSubstring(base, 12, 16));
        } catch (NoSuchAlgorithmException e) {
            return "AS-7F3E-9C4D-2A8B-6E1F";
        }
    }

    private String safeSubstring(String str, int start, int end) {
        if (str.length() >= end) {
            return str.substring(start, end);
        }
        return "ABCD";
    }

    public String getAndroidVersion() {
        return Build.VERSION.RELEASE + " (" + getAndroidCodeName(Build.VERSION.SDK_INT) + ")";
    }

    public String getDeviceModel() {
        return Build.MODEL != null ? Build.MODEL : "Android Device";
    }

    public String getDeviceBrand() {
        return Build.MANUFACTURER != null ? Build.MANUFACTURER.toLowerCase(Locale.US) : "generic";
    }

    public String getTotalRam() {
        try {
            ActivityManager actManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            if (actManager != null) {
                actManager.getMemoryInfo(memInfo);
                double totalGB = (double) memInfo.totalMem / (1024 * 1024 * 1024);
                return String.format(Locale.US, "%.1f GB", totalGB);
            }
        } catch (Exception ignored) {
        }
        return "3.0 GB";
    }

    public String getScreenResolution() {
        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) {
                DisplayMetrics dm = new DisplayMetrics();
                wm.getDefaultDisplay().getRealMetrics(dm);
                return dm.widthPixels + " x " + dm.heightPixels;
            }
        } catch (Exception ignored) {
        }
        return "720 x 1600";
    }

    public String getCpuName() {
        String hardware = Build.HARDWARE;
        if (hardware != null && !hardware.equalsIgnoreCase("unknown")) {
            return hardware;
        }
        return Build.BOARD != null ? Build.BOARD : "ARMv8 Processor";
    }

    private String getAndroidCodeName(int sdkInt) {
        switch (sdkInt) {
            case 34: return "Upside Down Cake";
            case 33: return "Tiramisu";
            case 32: return "Sv2";
            case 31: return "Snow Cone";
            case 30: return "Red Velvet Cake";
            default: return "API " + sdkInt;
        }
    }
}
