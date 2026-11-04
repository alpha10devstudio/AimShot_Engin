package com.alpha10.aimshot.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles strict 4-step network capability checks, real HTTP 204 internet validation
 * (detecting zero-balance SIMs, expired data packs, dead router WAN, and captive portals),
 * and genuine HTTPS reachability checks against Alpha10 Server without raw socket false-positives.
 */
public class NetworkUtil {

    public static final String ALPHA10_BASE_URL = "https://alpha10.freedev.app";
    public static final String ALPHA10_SERVER_CHECK_URL = "https://alpha10.freedev.app/api/auth.php";

    private static final String INTERNET_PROBE_PRIMARY = "https://clients3.google.com/generate_204";
    private static final String INTERNET_PROBE_FALLBACK = "https://connectivitycheck.gstatic.com/generate_204";

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public enum ConnectivityStatus {
        CONNECTED,
        NO_NETWORK,
        NO_INTERNET_ACCESS,
        SERVER_UNREACHABLE
    }

    public interface ConnectivityCallback {
        void onResult(boolean isConnected);
    }

    public interface DetailedConnectivityCallback {
        void onConnectivityEvaluated(ConnectivityStatus status);
    }

    /**
     * Step 1A: Synchronous check that Wi-Fi / Mobile Data / VPN transport is active.
     */
    public static boolean isNetworkConnected(Context context) {
        if (context == null) return false;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork == null) return false;
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
            if (capabilities == null) return false;
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } else {
            NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }

    /**
     * Step 1B: Synchronous check whether Android OS flagged the active network as a Captive Portal.
     */
    public static boolean hasCaptivePortal(Context context) {
        if (context == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork == null) return false;
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
            return capabilities != null
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL);
        }
        return false;
    }

    /**
     * Checks whether the OS currently marks the active network as validated.
     * Note: OS validation can lag or be stale on zero-balance mobile data, so launch/download flows
     * must also call verifyFullLaunchConnectivity() or verifyInternetAccess().
     */
    public static boolean isNetworkValidated(Context context) {
        if (context == null) return false;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork == null) return false;
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
            if (capabilities == null) return false;
            if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)) {
                return false;
            }
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    || Build.VERSION.SDK_INT < Build.VERSION_CODES.O);
        } else {
            NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }

    /**
     * Step 2: Live HTTPS HTTP 204 probe to verify real internet data routing.
     * Detects SIMs with 0 MB / expired data packs, ISP captive portals (which return 200 or 302),
     * and Wi-Fi routers with unplugged/offline WAN links.
     */
    public static void verifyInternetAccess(final Context context, final ConnectivityCallback callback) {
        if (!isNetworkConnected(context) || hasCaptivePortal(context)) {
            if (callback != null) {
                mainHandler.post(() -> callback.onResult(false));
            }
            return;
        }

        executor.execute(() -> {
            final boolean hasRealInternet = probeRealInternetBlocking();
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onResult(hasRealInternet);
                }
            });
        });
    }

    /**
     * Step 3: Live HTTPS reachability check against the Alpha10 Server API endpoint.
     * Never falls back to raw TCP port 443 sockets (which can produce false positives via carrier proxies).
     */
    public static void verifyServerReachability(final Context context,
                                                final String targetUrl,
                                                final ConnectivityCallback callback) {
        if (!isNetworkConnected(context) || hasCaptivePortal(context)) {
            if (callback != null) {
                mainHandler.post(() -> callback.onResult(false));
            }
            return;
        }

        executor.execute(() -> {
            final boolean reachable = probeAlpha10ServerBlocking(targetUrl);
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onResult(reachable);
                }
            });
        });
    }

    /**
     * Executes the complete 4-Step Launch & Download Gate:
     * 1. Active Transport Check (Wi-Fi / Cellular / VPN + no Captive Portal)
     * 2. Real Internet HTTP 204 Probe (blocks 0-balance SIM, expired pack, dead WAN, captive redirect)
     * 3. Alpha10 Server HTTPS Reachability Check (https://alpha10.freedev.app/api/auth.php)
     * 4. Delivers exact ConnectivityStatus on the Main UI thread
     */
    public static void verifyFullLaunchConnectivity(final Context context,
                                                    final DetailedConnectivityCallback callback) {
        if (callback == null) return;

        if (!isNetworkConnected(context)) {
            mainHandler.post(() -> callback.onConnectivityEvaluated(ConnectivityStatus.NO_NETWORK));
            return;
        }

        if (hasCaptivePortal(context)) {
            mainHandler.post(() -> callback.onConnectivityEvaluated(ConnectivityStatus.NO_INTERNET_ACCESS));
            return;
        }

        executor.execute(() -> {
            if (!probeRealInternetBlocking()) {
                mainHandler.post(() -> callback.onConnectivityEvaluated(ConnectivityStatus.NO_INTERNET_ACCESS));
                return;
            }

            if (!probeAlpha10ServerBlocking(ALPHA10_SERVER_CHECK_URL)) {
                mainHandler.post(() -> callback.onConnectivityEvaluated(ConnectivityStatus.SERVER_UNREACHABLE));
                return;
            }

            mainHandler.post(() -> callback.onConnectivityEvaluated(ConnectivityStatus.CONNECTED));
        });
    }

    private static boolean probeRealInternetBlocking() {
        if (checkHttp204Endpoint(INTERNET_PROBE_PRIMARY)) {
            return true;
        }
        return checkHttp204Endpoint(INTERNET_PROBE_FALLBACK);
    }

    private static boolean checkHttp204Endpoint(String probeUrl) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(probeUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setUseCaches(false);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestProperty("Cache-Control", "no-cache, no-store");
            conn.setRequestProperty("User-Agent", "AimShotLauncher/1.0 (Android)");
            int responseCode = conn.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_NO_CONTENT;
        } catch (Exception e) {
            return false;
        } finally {
            if (conn != null) {
                try { conn.disconnect(); } catch (Exception ignored) {}
            }
        }
    }

    private static boolean probeAlpha10ServerBlocking(String targetUrl) {
        HttpURLConnection conn = null;
        try {
            String checkUrl = (targetUrl != null && !targetUrl.trim().isEmpty())
                    ? targetUrl.trim()
                    : ALPHA10_SERVER_CHECK_URL;
            URL url = new URL(checkUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setUseCaches(false);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4500);
            conn.setReadTimeout(4500);
            conn.setRequestProperty("Cache-Control", "no-cache");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");
            int code = conn.getResponseCode();
            // Any non-5xx HTTP response (e.g., 200, 400, 401, 403, 405) confirms TLS + Alpha10 HTTP server is live
            return code > 0 && code < 500;
        } catch (Exception e) {
            return false;
        } finally {
            if (conn != null) {
                try { conn.disconnect(); } catch (Exception ignored) {}
            }
        }
    }
}
