package com.alpha10.aimshot.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Downloads the Main AimShot Engine APK binary from the existing unmodified Alpha10 endpoint:
 * GET https://alpha10.freedev.app/api/aimshot-download.php
 *
 * Enforces:
 * - Single-use Authorization: Bearer TEMPORARY_TOKEN header with zero auto-redirect token leakage
 * - Strict 60-second expiration window validation
 * - Private internal storage staging (context.getFilesDir()/downloads/ — invisible to File Managers)
 * - Binary content-type & ZIP/APK magic-byte verification
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
    private volatile boolean isCancelled = false;

    public void cancelDownload() {
        this.isCancelled = true;
    }

    /**
     * Purges any leftover temporary or previously staged APK files in private internal storage.
     */
    public static void cleanupPrivateDownloads(Context context) {
        if (context == null) return;
        File downloadsDir = new File(context.getFilesDir(), "downloads");
        if (!downloadsDir.exists()) return;
        File[] files = downloadsDir.listFiles();
        if (files != null) {
            for (File f : files) {
                try { f.delete(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Streams the protected APK into app-private internal storage: context.getFilesDir()/downloads/
     */
    public void downloadApk(final Context context,
                            final String bearerToken,
                            final long tokenIssuedAt,
                            final long tokenExpiresInSeconds,
                            final int targetVersionCode,
                            final DownloadCallback callback) {
        if (callback == null || context == null) return;

        if (bearerToken == null || bearerToken.trim().isEmpty()) {
            callback.onDownloadError("Missing temporary download token.", -100);
            return;
        }

        // Enforce 60-second single-use token expiration window before opening network stream
        long now = System.currentTimeMillis();
        long tokenLifespanMillis = tokenExpiresInSeconds > 0 ? (tokenExpiresInSeconds * 1000L) : 60000L;
        if (now > tokenIssuedAt + tokenLifespanMillis) {
            callback.onDownloadError("Temporary download token expired (60s limit exceeded). Please retry.", -101);
            return;
        }

        this.isCancelled = false;
        final Context appContext = context.getApplicationContext();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                InputStream is = null;
                FileOutputStream fos = null;
                File stagedFile = null;

                // Single-use token: copy locally and clear reference immediately after setting header
                String oneTimeToken = bearerToken.trim();

                try {
                    File downloadsDir = new File(appContext.getFilesDir(), "downloads");
                    if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
                        mainHandler.post(() -> callback.onDownloadError("Unable to initialize private internal storage directory.", -2));
                        return;
                    }

                    // Remove any stale .tmp files from prior interrupted attempts
                    File[] staleFiles = downloadsDir.listFiles((dir, name) -> name.endsWith(".tmp"));
                    if (staleFiles != null) {
                        for (File stale : staleFiles) {
                            try { stale.delete(); } catch (Exception ignored) {}
                        }
                    }

                    int safeVersion = Math.max(1, targetVersionCode);
                    stagedFile = new File(downloadsDir, "AimShot_Engine_v" + safeVersion + ".apk.tmp");
                    if (stagedFile.exists()) {
                        stagedFile.delete();
                    }

                    URL url = new URL(DOWNLOAD_ENDPOINT);
                    conn = (HttpURLConnection) url.openConnection();
                    // Prevent automatic redirects from dropping Authorization header or consuming single-use token twice
                    conn.setInstanceFollowRedirects(false);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", "AimShotLauncher/1.0 (Android)");
                    conn.setRequestProperty("Authorization", "Bearer " + oneTimeToken);
                    conn.setRequestProperty("Accept", "application/vnd.android.package-archive, application/octet-stream");
                    conn.setConnectTimeout(12000);
                    conn.setReadTimeout(20000);

                    // Immediately clear token from local variable
                    oneTimeToken = null;

                    final int responseCode = conn.getResponseCode();
                    if (responseCode != 200) {
                        String errSnippet = readShortErrorStream(conn.getErrorStream());
                        cleanupFile(stagedFile);
                        final String errMsg = errSnippet.isEmpty()
                                ? "APK download rejected by server (HTTP " + responseCode + ")"
                                : "APK download rejected (HTTP " + responseCode + "): " + errSnippet;
                        mainHandler.post(() -> callback.onDownloadError(errMsg, responseCode));
                        return;
                    }

                    String contentType = conn.getContentType();
                    if (contentType != null) {
                        String lowerType = contentType.toLowerCase();
                        if (lowerType.contains("text/html") || lowerType.contains("application/json")) {
                            cleanupFile(stagedFile);
                            mainHandler.post(() -> callback.onDownloadError("Server returned non-binary response (" + contentType + ") instead of APK.", -102));
                            return;
                        }
                    }

                    long contentLength = BuildCompatContentLength(conn);
                    long minRequiredSpace = contentLength > 0 ? (contentLength + (15L * 1024L * 1024L)) : (40L * 1024L * 1024L);
                    if (appContext.getFilesDir().getUsableSpace() < minRequiredSpace) {
                        cleanupFile(stagedFile);
                        mainHandler.post(() -> callback.onDownloadError("Insufficient internal storage space to stage Main AimShot Engine APK.", -2));
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
                            mainHandler.post(() -> callback.onDownloadError("Download cancelled by user.", -3));
                            return;
                        }

                        fos.write(buffer, 0, bytesRead);
                        bytesDownloaded += bytesRead;

                        if (contentLength > 0) {
                            int percent = (int) ((bytesDownloaded * 100L) / contentLength);
                            if (percent != lastReportedPercent) {
                                lastReportedPercent = percent;
                                final long currentBytes = bytesDownloaded;
                                mainHandler.post(() -> callback.onDownloadProgress(currentBytes, contentLength, percent));
                            }
                        }
                    }

                    fos.flush();
                    fos.getFD().sync();
                    fos.close();
                    fos = null;

                    if (stagedFile.length() == 0 || (contentLength > 0 && stagedFile.length() < contentLength)) {
                        cleanupFile(stagedFile);
                        mainHandler.post(() -> callback.onDownloadError("Incomplete APK binary received from server.", -4));
                        return;
                    }

                    if (!hasValidApkZipHeader(stagedFile)) {
                        cleanupFile(stagedFile);
                        mainHandler.post(() -> callback.onDownloadError("Downloaded payload is not a valid Android APK package.", -5));
                        return;
                    }

                    final File finalStagedFile = stagedFile;
                    mainHandler.post(() -> callback.onDownloadComplete(finalStagedFile));

                } catch (Exception e) {
                    cleanupFile(stagedFile);
                    final String msg = e.getMessage() != null ? e.getMessage() : "Stream interrupted";
                    mainHandler.post(() -> callback.onDownloadError("Download error: " + msg, -1));
                } finally {
                    oneTimeToken = null;
                    try { if (fos != null) fos.close(); } catch (Exception ignored) {}
                    try { if (is != null) is.close(); } catch (Exception ignored) {}
                    if (conn != null) {
                        try { conn.disconnect(); } catch (Exception ignored) {}
                    }
                }
            }
        });
    }

    private static long BuildCompatContentLength(HttpURLConnection conn) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return conn.getContentLengthLong();
        }
        return conn.getContentLength();
    }

    private static boolean hasValidApkZipHeader(File file) {
        if (file == null || file.length() < 4) return false;
        byte[] magic = new byte[4];
        try (FileInputStream fis = new FileInputStream(file)) {
            if (fis.read(magic) != 4) return false;
            return magic[0] == 0x50 && magic[1] == 0x4B && magic[2] == 0x03 && magic[3] == 0x04;
        } catch (Exception e) {
            return false;
        }
    }

    private static String readShortErrorStream(InputStream errStream) {
        if (errStream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(errStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null && sb.length() < 200) {
                sb.append(line);
            }
        } catch (Exception ignored) {
        }
        return sb.toString().trim();
    }

    private static void cleanupFile(File file) {
        if (file != null && file.exists()) {
            try { file.delete(); } catch (Exception ignored) {}
        }
    }
}
