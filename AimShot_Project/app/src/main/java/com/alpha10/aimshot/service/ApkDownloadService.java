package com.alpha10.aimshot.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Downloads the binary release APK from GET https://alpha10.freedev.app/api/aimshot-download.php
 * using the single-use Bearer token, strictly enforcing the 60-second expiration window
 * and writing directly into app-private internal storage.
 */
public class ApkDownloadService {

    public static final String DOWNLOAD_ENDPOINT = "https://alpha10.freedev.app/api/aimshot-download.php";
    private static final int BUFFER_SIZE = 8192;

    public interface DownloadCallback {
        void onDownloadProgress(long bytesDownloaded, long totalBytes, int percent);
        void onDownloadComplete(File stagedApkFile);
        void onDownloadError(String errorMessage, int httpCode);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isCancelled = false;

    public void cancelDownload() {
        this.isCancelled = true;
    }

    /**
     * Downloads the APK file to app-private storage context.getFilesDir()/downloads/
     */
    public void downloadApk(final Context context,
                            final String bearerToken,
                            final long tokenIssuedAt,
                            final long tokenExpiresInSeconds,
                            final int targetVersionCode,
                            final DownloadCallback callback) {
        if (callback == null || context == null) return;

        // Verify token is not expired before initiating connection
        long now = System.currentTimeMillis();
        long tokenLifespanMillis = tokenExpiresInSeconds > 0 ? (tokenExpiresInSeconds * 1000) : 60000;
        if (now > tokenIssuedAt + tokenLifespanMillis) {
            callback.onDownloadError("Temporary token expired (exceeded 60s window). Please re-authenticate.", -101);
            return;
        }

        this.isCancelled = false;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                InputStream is = null;
                FileOutputStream fos = null;
                File stagedFile = null;

                // Token is single-use: copy locally and advise immediate caller-side disposal
                String activeToken = bearerToken;

                try {
                    // Ensure private downloads directory exists
                    File downloadsDir = new File(context.getFilesDir(), "downloads");
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs();
                    }

                    // Clean up any stale partial files
                    File[] staleFiles = downloadsDir.listFiles((dir, name) -> name.endsWith(".tmp"));
                    if (staleFiles != null) {
                        for (File stale : staleFiles) {
                            try { stale.delete(); } catch (Exception ignored) {}
                        }
                    }

                    // Staging file with .tmp extension
                    stagedFile = new File(downloadsDir, "AimShot_v" + targetVersionCode + ".apk.tmp");
                    if (stagedFile.exists()) {
                        stagedFile.delete();
                    }

                    URL url = new URL(DOWNLOAD_ENDPOINT);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", "AimShotEngine/1.0 (Android)");
                    conn.setRequestProperty("Authorization", "Bearer " + activeToken);
                    conn.setRequestProperty("Accept", "application/octet-stream");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);

                    // Clear token reference immediately
                    activeToken = null;

                    int responseCode = conn.getResponseCode();
                    if (responseCode != 200) {
                        cleanupFile(stagedFile);
                        mainHandler.post(() -> callback.onDownloadError("Download request failed (HTTP " + responseCode + ")", responseCode));
                        return;
                    }

                    String contentType = conn.getContentType();
                    if (contentType != null && (contentType.contains("text/html") || contentType.contains("application/json"))) {
                        cleanupFile(stagedFile);
                        mainHandler.post(() -> callback.onDownloadError("Server returned non-binary error page instead of APK", -102));
                        return;
                    }

                    long contentLength = conn.getContentLength();
                    // Storage check: ensure sufficient internal space
                    long minRequiredSpace = contentLength > 0 ? (contentLength + (10 * 1024 * 1024)) : (30 * 1024 * 1024);
                    if (context.getFilesDir().getUsableSpace() < minRequiredSpace) {
                        cleanupFile(stagedFile);
                        mainHandler.post(() -> callback.onDownloadError("Insufficient internal storage to store update", -2));
                        return;
                    }

                    is = conn.getInputStream();
                    fos = new FileOutputStream(stagedFile);

                    byte[] buffer = new byte[BUFFER_SIZE];
                    long bytesDownloaded = 0;
                    int bytesRead;
                    int lastReportedPercent = -1;

                    while ((bytesRead = is.read(buffer)) != -1) {
                        if (isCancelled) {
                            cleanupFile(stagedFile);
                            mainHandler.post(() -> callback.onDownloadError("Download cancelled by user", -3));
                            return;
                        }

                        fos.write(buffer, 0, bytesRead);
                        bytesDownloaded += bytesRead;

                        if (contentLength > 0) {
                            int percent = (int) ((bytesDownloaded * 100) / contentLength);
                            if (percent != lastReportedPercent) {
                                lastReportedPercent = percent;
                                final long currentBytes = bytesDownloaded;
                                mainHandler.post(() -> callback.onDownloadProgress(currentBytes, contentLength, percent));
                            }
                        }
                    }

                    fos.flush();

                    // Verification that download is complete
                    if (stagedFile.length() == 0 || (contentLength > 0 && stagedFile.length() < contentLength)) {
                        cleanupFile(stagedFile);
                        mainHandler.post(() -> callback.onDownloadError("Incomplete download received from server", -4));
                        return;
                    }

                    final File finalStagedFile = stagedFile;
                    mainHandler.post(() -> callback.onDownloadComplete(finalStagedFile));

                } catch (Exception e) {
                    cleanupFile(stagedFile);
                    mainHandler.post(() -> callback.onDownloadError("Download error: " + e.getMessage(), -1));
                } finally {
                    try { if (fos != null) fos.close(); } catch (Exception ignored) {}
                    try { if (is != null) is.close(); } catch (Exception ignored) {}
                    if (conn != null) {
                        try { conn.disconnect(); } catch (Exception ignored) {}
                    }
                }
            }
        });
    }

    private static void cleanupFile(File file) {
        if (file != null && file.exists()) {
            try { file.delete(); } catch (Exception ignored) {}
        }
    }
}
