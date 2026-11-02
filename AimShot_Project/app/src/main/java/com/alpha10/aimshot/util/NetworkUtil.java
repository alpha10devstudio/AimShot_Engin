package com.alpha10.aimshot.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles synchronous network capability checks, internet validation,
 * and asynchronous server reachability checks for AimShot Engine.
 */
public class NetworkUtil {

    public static final String ALPHA10_BASE_URL = "https://alpha10.freedev.app";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface ConnectivityCallback {
        void onResult(boolean isConnected);
    }

    /**
     * Fast synchronous check using ConnectivityManager and NetworkCapabilities.
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
     * Validates whether the network connection is actively validated (has real internet).
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
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    || Build.VERSION.SDK_INT < Build.VERSION_CODES.O);
        } else {
            NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }

    /**
     * Asynchronous reachability check that tests background DNS/socket.
     */
    public static void verifyInternetAccess(Context context, final ConnectivityCallback callback) {
        if (!isNetworkConnected(context)) {
            if (callback != null) callback.onResult(false);
            return;
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                boolean reachable = false;
                try {
                    Socket socket = new Socket();
                    SocketAddress socketAddress = new InetSocketAddress("8.8.8.8", 53);
                    socket.connect(socketAddress, 1800);
                    socket.close();
                    reachable = true;
                } catch (IOException e) {
                    reachable = false;
                }

                final boolean finalReachable = reachable;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onResult(finalReachable);
                        }
                    }
                });
            }
        });
    }

    /**
     * Asynchronously verifies HTTPS reachability of the Alpha10 server host.
     */
    public static void verifyServerReachability(Context context, final String targetUrl, final ConnectivityCallback callback) {
        if (!isNetworkConnected(context)) {
            if (callback != null) callback.onResult(false);
            return;
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                boolean reachable = false;
                HttpURLConnection conn = null;
                try {
                    String testUrl = (targetUrl != null && !targetUrl.isEmpty()) ? targetUrl : ALPHA10_BASE_URL;
                    URL url = new URL(testUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(3500);
                    conn.setReadTimeout(3500);
                    conn.setRequestProperty("User-Agent", "AimShotEngine/1.0 (Android)");
                    int code = conn.getResponseCode();
                    // Any valid HTTP response (including 401/403/200) proves server is alive and reachable
                    reachable = (code > 0);
                } catch (Exception e) {
                    // Try socket fallback to host
                    try {
                        Socket socket = new Socket();
                        socket.connect(new InetSocketAddress("alpha10.freedev.app", 443), 2000);
                        socket.close();
                        reachable = true;
                    } catch (Exception ex) {
                        reachable = false;
                    }
                } finally {
                    if (conn != null) {
                        try { conn.disconnect(); } catch (Exception ignored) {}
                    }
                }

                final boolean finalReachable = reachable;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onResult(finalReachable);
                        }
                    }
                });
            }
        });
    }
}
