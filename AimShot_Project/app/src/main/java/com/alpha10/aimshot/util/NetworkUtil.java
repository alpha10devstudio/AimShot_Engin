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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles synchronous network capability checks and asynchronous internet reachability validation.
 */
public class NetworkUtil {

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
     * Asynchronous reachability check that tests a background socket connection.
     * Does not block the main UI thread.
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
                    socket.connect(socketAddress, 1500);
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
}
