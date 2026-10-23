package com.alpha10.aimshot.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.alpha10.aimshot.model.NotificationItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Repository managing website-to-app notification data flow, offline persistence,
 * server synchronization, and read state tracking.
 */
public class NotificationsRepository {

    private static final String PREFS_NAME = "aimshot_notifications_store";
    private static final String KEY_READ_IDS = "read_notification_ids";
    private static final String KEY_CACHED_NOTIFS = "cached_notifications_json";

    private static NotificationsRepository instance;
    private final Context appContext;
    private final SharedPreferences prefs;
    private final List<NotificationItem> memoryList = new ArrayList<>();

    public interface SyncCallback {
        void onSuccess(List<NotificationItem> items, int unreadCount);
        void onError(String error, List<NotificationItem> fallbackItems);
    }

    public static synchronized NotificationsRepository getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationsRepository(context.getApplicationContext());
        }
        return instance;
    }

    private NotificationsRepository(Context context) {
        this.appContext = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadFromCacheOrDefaults();
    }

    private void loadFromCacheOrDefaults() {
        String cachedJson = prefs.getString(KEY_CACHED_NOTIFS, null);
        memoryList.clear();

        if (cachedJson != null && !cachedJson.trim().isEmpty()) {
            try {
                JSONArray arr = new JSONArray(cachedJson);
                Set<String> readIds = getReadIds();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    NotificationItem item = parseJsonItem(obj, readIds);
                    if (item != null) memoryList.add(item);
                }
            } catch (Exception ignored) {
                populateSeedAnnouncements();
            }
        }

        if (memoryList.isEmpty()) {
            populateSeedAnnouncements();
        }
    }

    private void populateSeedAnnouncements() {
        Set<String> readIds = getReadIds();
        memoryList.add(new NotificationItem(
                "notif-101",
                "AimShot Engine v1.0.0 Global Release",
                "Welcome to AimShot Engine! Enhanced auto-aim guidelines, millimeter precision physics, and 120 FPS frame pacing are active.",
                "announcement",
                "Today, 10:24 AM",
                readIds.contains("notif-101"),
                "high"
        ));

        memoryList.add(new NotificationItem(
                "notif-102",
                "Carrom Disc Pool Guideline Calibration",
                "New millimeter precision laser physics engine calibrated for smooth bank shots and double rebound angles.",
                "update",
                "Yesterday",
                readIds.contains("notif-102"),
                "normal"
        ));

        memoryList.add(new NotificationItem(
                "notif-103",
                "Anti-Ban Shield v2.4 Active",
                "Kernel touch hook isolation verified against latest mobile anti-cheat updates. 100% undetectable safe overlay kernel.",
                "feature",
                "2 days ago",
                readIds.contains("notif-103"),
                "high"
        ));

        memoryList.add(new NotificationItem(
                "notif-104",
                "Cloud Server Optimization Notice",
                "Routine cloud verification server maintenance scheduled for 03:00 UTC. In-app offline license verification remains 100% functional.",
                "maintenance",
                "3 days ago",
                readIds.contains("notif-104"),
                "normal"
        ));
    }

    private NotificationItem parseJsonItem(JSONObject obj, Set<String> readIds) {
        try {
            String id = obj.optString("id", "notif-" + System.currentTimeMillis());
            String title = obj.optString("title", "Announcement");
            String message = obj.optString("message", "");
            String category = obj.optString("category", "announcement");
            String timestamp = obj.optString("timestamp", "Just now");
            String priority = obj.optString("priority", "normal");
            boolean isRead = readIds.contains(id) || obj.optBoolean("isRead", false);

            NotificationItem item = new NotificationItem(id, title, message, category, timestamp, isRead, priority);
            item.setMinVersion(obj.optString("minVersion", "1.0.0"));
            item.setActionUrl(obj.optString("actionUrl", ""));
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    public synchronized List<NotificationItem> getAllNotifications() {
        return new ArrayList<>(memoryList);
    }

    public synchronized int getUnreadCount() {
        int count = 0;
        for (NotificationItem item : memoryList) {
            if (!item.isRead()) {
                count++;
            }
        }
        return count;
    }

    public synchronized void markAsRead(String id) {
        Set<String> readIds = new HashSet<>(getReadIds());
        readIds.add(id);
        saveReadIds(readIds);

        for (NotificationItem item : memoryList) {
            if (item.getId().equals(id)) {
                item.setRead(true);
                break;
            }
        }
    }

    public synchronized void markAllAsRead() {
        Set<String> readIds = new HashSet<>(getReadIds());
        for (NotificationItem item : memoryList) {
            item.setRead(true);
            readIds.add(item.getId());
        }
        saveReadIds(readIds);
    }

    private Set<String> getReadIds() {
        return prefs.getStringSet(KEY_READ_IDS, new HashSet<>());
    }

    private void saveReadIds(Set<String> readIds) {
        prefs.edit().putStringSet(KEY_READ_IDS, readIds).apply();
    }

    /**
     * Asynchronously synchronizes notifications from the web server endpoint.
     * Uses background thread to prevent blocking main UI thread.
     */
    public void syncNotifications(SyncCallback callback) {
        new Thread(() -> {
            boolean success = false;
            String errorMsg = null;
            List<NotificationItem> syncedItems = new ArrayList<>();

            // Endpoints to try (Android emulator loopback & direct localhost)
            String[] endpoints = new String[]{
                    "http://10.0.2.2:3000/api/notifications",
                    "http://127.0.0.1:3000/api/notifications"
            };

            for (String endpointUrl : endpoints) {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(endpointUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);
                    conn.setRequestProperty("Accept", "application/json");

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();

                        String jsonResponse = sb.toString();
                        JSONArray array = new JSONArray(jsonResponse);
                        Set<String> readIds = getReadIds();

                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            NotificationItem item = parseJsonItem(obj, readIds);
                            if (item != null) {
                                syncedItems.add(item);
                            }
                        }

                        if (!syncedItems.isEmpty()) {
                            // Cache to SharedPreferences
                            prefs.edit().putString(KEY_CACHED_NOTIFS, jsonResponse).apply();
                            synchronized (NotificationsRepository.this) {
                                memoryList.clear();
                                memoryList.addAll(syncedItems);
                            }
                            success = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    errorMsg = e.getMessage();
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }

            if (success) {
                callback.onSuccess(getAllNotifications(), getUnreadCount());
            } else {
                callback.onError(errorMsg != null ? errorMsg : "Offline mode active", getAllNotifications());
            }
        }).start();
    }
}
