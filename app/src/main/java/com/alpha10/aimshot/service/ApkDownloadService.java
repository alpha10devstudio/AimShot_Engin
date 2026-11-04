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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Downloads the Main AimShot Engine APK binary directly from Alpha10 Website:
 * GET https://alpha10.freedev.app/api/aimshot-download.php
 *
 * - Sends Authorization: Bearer <token>, X-App-Key, and solved __test cookie
 * - Automatically solves aes.js challenge or falls back to ?token= query parameter if needed
 * - Streams into private internal storage (context.getFilesDir()/downloads/)
 * - Verifies valid ZIP/APK magic header (PK\x03\x04)
 */
public class ApkDownloadService {

    public static final String DOWNLOAD_ENDPOINT = "https://alpha10.freedev.app/api/aimshot-download.php";
    private static final int BUFFER_SIZE = 16384;
    private static final long ESTIMATED_APK_BYTES = 9500000L;

    public interface DownloadCallback {
        void onDownloadProgress(long bytesDownloaded, long totalBytes, int percent);
        void onDownloadComplete(File stagedApkFile);
        void onDownloadError(String errorMessage, int httpCode);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean isCancelled = false;
    private volatile String lastServerSha256Header = null;

    public void cancelDownload() {
        this.isCancelled = true;
    }

    public String getLastServerSha256Header() {
        return lastServerSha256Header;
    }

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

    public void downloadApk(final Context context,
                            final String bearerToken,
                            final long tokenIssuedAt,
                            final long tokenExpiresInSeconds,
                            final int targetVersionCode,
                            final DownloadCallback callback) {
        downloadApk(
                context,
                DOWNLOAD_ENDPOINT,
                bearerToken,
                UpdateAuthService.getCachedSessionCookie(),
                tokenIssuedAt,
                tokenExpiresInSeconds,
                targetVersionCode,
                callback
        );
    }

    public void downloadApk(final Context context,
                            final String customDownloadUrl,
                            final String bearerToken,
                            final String sessionCookie,
                            final long tokenIssuedAt,
                            final long tokenExpiresInSeconds,
                            final int targetVersionCode,
                            final DownloadCallback callback) {
        if (callback == null || context == null) return;

        if (bearerToken == null || bearerToken.trim().isEmpty()) {
            callback.onDownloadError("Missing download token from server.", -100);
            return;
        }

        long now = System.currentTimeMillis();
        long tokenLifespanMillis = tokenExpiresInSeconds > 0 ? (tokenExpiresInSeconds * 1000L) : 60000L;
        if (now > tokenIssuedAt + tokenLifespanMillis + 5000L) {
            callback.onDownloadError("Download token expired. Please tap Retry.", -101);
            return;
        }

        this.isCancelled = false;
        this.lastServerSha256Header = null;
        final Context appContext = context.getApplicationContext();
        final String resolvedEndpoint = (customDownloadUrl != null && !customDownloadUrl.trim().isEmpty())
                ? customDownloadUrl.trim()
                : DOWNLOAD_ENDPOINT;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                InputStream is = null;
                FileOutputStream fos = null;
                File stagedFile = null;

                String token = bearerToken.trim();
                String activeCookie = (sessionCookie != null && !sessionCookie.trim().isEmpty())
                        ? sessionCookie.trim()
                        : UpdateAuthService.getCachedSessionCookie();

                try {
                    File downloadsDir = new File(appContext.getFilesDir(), "downloads");
                    if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
                        mainHandler.post(() -> callback.onDownloadError("Unable to initialize private internal storage directory.", -2));
                        return;
                    }

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

                    conn = openDownloadConnection(resolvedEndpoint, token, activeCookie, false);
                    int responseCode = conn.getResponseCode();

                    // Follow up to 3 redirects manually so Authorization & Cookie headers are preserved
                    int redirectCount = 0;
                    while ((responseCode == 301 || responseCode == 302 || responseCode == 303
                            || responseCode == 307 || responseCode == 308) && redirectCount < 3) {
                        String location = conn.getHeaderField("Location");
                        conn.disconnect();
                        if (location == null || location.trim().isEmpty()) break;
                        String nextUrl = new URL(new URL(resolvedEndpoint), location.trim()).toString();
                        conn = openDownloadConnection(nextUrl, token, activeCookie, false);
                        responseCode = conn.getResponseCode();
                        redirectCount++;
                    }

                    // Fallback if Apache stripped Authorization header (401/403): retry with ?token=
                    if (responseCode == 401 || responseCode == 403) {
                        conn.disconnect();
                        conn = openDownloadConnection(resolvedEndpoint, token, activeCookie, true);
                        responseCode = conn.getResponseCode();
                    }

                    // If InfinityFree aes.js intercepted before PHP ran, solve cookie & retry immediately
                    String contentType = conn.getContentType();
                    if (responseCode == 200 && contentType != null
                            && contentType.toLowerCase(Locale.US).contains("text/html")) {
                        String htmlBody = readFullTextStream(conn.getInputStream());
                        conn.disconnect();
                        if (UpdateAuthService.isAntiBotChallenge(htmlBody)) {
                            String solvedCookie = UpdateAuthService.solveAntiBotCookie(htmlBody);
                            if (solvedCookie != null && !solvedCookie.isEmpty()) {
                                activeCookie = solvedCookie;
                                UpdateAuthService.setCachedSessionCookie(solvedCookie);
                                conn = openDownloadConnection(resolvedEndpoint, token, activeCookie, false);
                                responseCode = conn.getResponseCode();
                                contentType = conn.getContentType();
                            } else {
                                mainHandler.post(() -> callback.onDownloadError("Hosting security check blocked APK stream. Please retry.", -102));
                                return;
                            }
                        } else {
                            mainHandler.post(() -> callback.onDownloadError("Server returned HTML instead of APK file. Please retry.", -102));
                            return;
                        }
                    }

                    final int finalHttpCode = responseCode;
                    if (finalHttpCode != 200) {
                        String errSnippet = readShortErrorStream(conn.getErrorStream());
                        cleanupFile(stagedFile);
                        final String errMsg = errSnippet.isEmpty()
                                ? "APK download rejected by server (HTTP " + finalHttpCode + ")"
                                : "APK download rejected (HTTP " + finalHttpCode + "): " + errSnippet;
                        mainHandler.post(() -> callback.onDownloadError(errMsg, finalHttpCode));
                        return;
                    }

                    if (contentType != null && contentType.toLowerCase(Locale.US).contains("application/json")) {
                        String jsonErr = readShortErrorStream(conn.getInputStream());
                        cleanupFile(stagedFile);
                        final String errMsg = jsonErr.isEmpty()
                                ? "Server returned JSON instead of APK binary."
                                : "Server download error: " + jsonErr;
                        mainHandler.post(() -> callback.onDownloadError(errMsg, -102));
                        return;
                    }

                    String shaHeader = conn.getHeaderField("X-APK-SHA256");
                    if (shaHeader == null || shaHeader.trim().isEmpty()) {
                        shaHeader = conn.getHeaderField("X-Expected-Sha256");
                    }
                    if (shaHeader == null || shaHeader.trim().isEmpty()) {
                        shaHeader = conn.getHeaderField("X-Content-SHA256");
                    }
                    if (shaHeader != null && shaHeader.trim().matches("^[0-9a-fA-F]{64}$")) {
                        lastServerSha256Header = shaHeader.trim();
                    }

                    final long contentLength = BuildCompatContentLength(conn);
                    long minRequiredSpace = contentLength > 0 ? (contentLength + (10L * 1024L * 1024L)) : (30L * 1024L * 1024L);
                    if (appContext.getFilesDir().getUsableSpace() < minRequiredSpace) {
                        cleanupFile(stagedFile);
                        mainHandler.post(() -> callback.onDownloadError("Insufficient phone storage space to download AimShot Engine APK.", -2));
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

                        final int percent;
                        final long totalForUi;
                        if (contentLength > 0) {
                            percent = (int) Math.min(100L, (bytesDownloaded * 100L) / contentLength);
                            totalForUi = contentLength;
                        } else {
                            percent = (int) Math.min(95L, (bytesDownloaded * 95L) / ESTIMATED_APK_BYTES);
                            totalForUi = Math.max(bytesDownloaded, ESTIMATED_APK_BYTES);
                        }

                        if (percent != lastReportedPercent) {
                            lastReportedPercent = percent;
                            final long currentBytes = bytesDownloaded;
                            mainHandler.post(() -> callback.onDownloadProgress(currentBytes, totalForUi, percent));
                        }
                    }

                    fos.flush();
                    fos.getFD().sync();
                    fos.close();
                    fos = null;

                    if (stagedFile.length() == 0 || (contentLength > 0 && stagedFile.length() < contentLength)) {
                        cleanupFile(stagedFile);
                        mainHandler.post(() -> callback.onDownloadError("Incomplete APK received from server. Please retry.", -4));
                        return;
                    }

                    if (!hasValidApkZipHeader(stagedFile)) {
                        cleanupFile(stagedFile);
                        mainHandler.post(() -> callback.onDownloadError("Downloaded file is not a valid Android APK package.", -5));
                        return;
                    }

                    final File finalStagedFile = stagedFile;
                    mainHandler.post(() -> callback.onDownloadComplete(finalStagedFile));

                } catch (Exception e) {
                    cleanupFile(stagedFile);
                    final String msg = e.getMessage() != null ? e.getMessage() : "Stream interrupted";
                    mainHandler.post(() -> callback.onDownloadError("Download error: " + msg, -1));
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

    private static HttpURLConnection openDownloadConnection(String baseUrl,
                                                            String token,
                                                            String cookieHeader,
                                                            boolean appendQueryToken) throws Exception {
        String finalUrl = baseUrl;
        if (appendQueryToken) {
            String sep = baseUrl.contains("?") ? "&" : "?";
            finalUrl = baseUrl + sep + "token=" + URLEncoder.encode(token, "UTF-8");
        }
        URL url = new URL(finalUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(false);
        conn.setUseCaches(false);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", UpdateAuthService.BROWSER_USER_AGENT);
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("X-App-Key", UpdateAuthService.SECRET_APP_KEY);
        conn.setRequestProperty("Accept", "application/vnd.android.package-archive, application/octet-stream, */*");
        if (cookieHeader != null && !cookieHeader.trim().isEmpty()) {
            conn.setRequestProperty("Cookie", cookieHeader.trim());
        }
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        return conn;
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

    private static String readFullTextStream(InputStream stream) {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null && sb.length() < 16384) {
                sb.append(line).append('\n');
            }
        } catch (Exception ignored) {
        }
        return sb.toString();
    }

    private static String readShortErrorStream(InputStream errStream) {
        if (errStream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(errStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null && sb.length() < 240) {
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
