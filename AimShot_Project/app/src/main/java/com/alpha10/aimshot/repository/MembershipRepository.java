package com.alpha10.aimshot.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.alpha10.aimshot.model.MembershipStatus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Single source of truth for the active user's license membership state.
 * Employs listeners to synchronize Home, Premium, and Settings screens.
 * Includes local memory caching, multi-key vault management, and auto-purge of expired keys.
 */
public class MembershipRepository {

    private static final String PREF_NAME = "aimshot_secure_license_prefs";
    private static final String KEY_STATE = "membership_state";
    private static final String KEY_PLAN = "plan_type";
    private static final String KEY_START_TS = "start_ts";
    private static final String KEY_EXPIRY_TS = "expiry_ts";
    private static final String KEY_UNLOCKED = "features_unlocked";
    private static final String KEY_DEVICE_ID = "bound_device_id";
    private static final String KEY_LICENSE_KEY = "saved_license_key";
    private static final String KEY_KEY_VAULT = "aimshot_cached_key_vault";

    private static MembershipRepository instance;
    private final SharedPreferences prefs;
    private MembershipStatus currentStatus;
    private final List<MembershipChangeListener> listeners = new ArrayList<>();

    public interface MembershipChangeListener {
        void onMembershipChanged(MembershipStatus newStatus);
    }

    /**
     * Immutable model representing a locally cached license key record in the vault.
     */
    public static class CachedKey implements Serializable {
        private final String key;
        private final String planType;
        private final long startTimestamp;
        private final long expiryTimestamp;
        private final String boundDeviceId;
        private final long lastUsedTimestamp;

        public CachedKey(String key, String planType, long startTimestamp, long expiryTimestamp, String boundDeviceId, long lastUsedTimestamp) {
            this.key = key != null ? key.trim() : "";
            this.planType = planType != null ? planType : "AimShot Pro License";
            this.startTimestamp = startTimestamp;
            this.expiryTimestamp = expiryTimestamp;
            this.boundDeviceId = boundDeviceId != null ? boundDeviceId : "";
            this.lastUsedTimestamp = lastUsedTimestamp;
        }

        public String getKey() { return key; }
        public String getPlanType() { return planType; }
        public long getStartTimestamp() { return startTimestamp; }
        public long getExpiryTimestamp() { return expiryTimestamp; }
        public String getBoundDeviceId() { return boundDeviceId; }
        public long getLastUsedTimestamp() { return lastUsedTimestamp; }

        public boolean isExpired() {
            return expiryTimestamp > 0 && System.currentTimeMillis() >= expiryTimestamp;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("key", key);
            obj.put("planType", planType);
            obj.put("startTimestamp", startTimestamp);
            obj.put("expiryTimestamp", expiryTimestamp);
            obj.put("boundDeviceId", boundDeviceId);
            obj.put("lastUsedTimestamp", lastUsedTimestamp);
            return obj;
        }

        public static CachedKey fromJson(JSONObject obj) {
            if (obj == null) return null;
            String key = obj.optString("key", "");
            String planType = obj.optString("planType", "AimShot Pro License");
            long start = obj.optLong("startTimestamp", 0);
            long expiry = obj.optLong("expiryTimestamp", 0);
            String devId = obj.optString("boundDeviceId", "");
            long lastUsed = obj.optLong("lastUsedTimestamp", System.currentTimeMillis());
            return new CachedKey(key, planType, start, expiry, devId, lastUsed);
        }
    }

    private MembershipRepository(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadFromPrefs();
        purgeExpiredKeysInternal();
    }

    public static synchronized MembershipRepository getInstance(Context context) {
        if (instance == null) {
            instance = new MembershipRepository(context);
        }
        return instance;
    }

    private void loadFromPrefs() {
        String stateStr = prefs.getString(KEY_STATE, MembershipStatus.State.FREE.name());
        MembershipStatus.State state;
        try {
            state = MembershipStatus.State.valueOf(stateStr);
        } catch (Exception e) {
            state = MembershipStatus.State.FREE;
        }

        String plan = prefs.getString(KEY_PLAN, "Free / Not Activated");
        long start = prefs.getLong(KEY_START_TS, 0);
        long expiry = prefs.getLong(KEY_EXPIRY_TS, 0);
        boolean unlocked = prefs.getBoolean(KEY_UNLOCKED, false);
        String deviceId = prefs.getString(KEY_DEVICE_ID, null);

        // If temporal expiry passed, degrade state to EXPIRED
        if (state == MembershipStatus.State.ACTIVE && expiry > 0 && System.currentTimeMillis() > expiry) {
            state = MembershipStatus.State.EXPIRED;
            unlocked = false;
        }

        currentStatus = new MembershipStatus(state, plan, start, expiry, unlocked, deviceId);
    }

    public synchronized MembershipStatus getCurrentStatus() {
        // Double check for temporal expiration
        if (currentStatus.getState() == MembershipStatus.State.EXPIRED &&
                prefs.getString(KEY_STATE, "").equals(MembershipStatus.State.ACTIVE.name())) {
            updateStatus(new MembershipStatus(
                    MembershipStatus.State.EXPIRED,
                    currentStatus.getPlanType(),
                    currentStatus.getStartTimestamp(),
                    currentStatus.getExpiryTimestamp(),
                    false,
                    currentStatus.getBoundDeviceId()
            ));
        }
        return currentStatus;
    }

    public synchronized String getSavedLicenseKey() {
        return prefs.getString(KEY_LICENSE_KEY, "");
    }

    public synchronized void saveLicenseKey(String key) {
        if (key != null) {
            prefs.edit().putString(KEY_LICENSE_KEY, key.trim()).apply();
        }
    }

    /**
     * Checks if the active license key is currently valid locally (unexpired).
     */
    public synchronized boolean isCurrentKeyValidLocally() {
        MembershipStatus status = getCurrentStatus();
        String savedKey = getSavedLicenseKey();
        return status != null
                && status.getState() == MembershipStatus.State.ACTIVE
                && status.getExpiryTimestamp() > System.currentTimeMillis()
                && savedKey != null
                && !savedKey.trim().isEmpty();
    }

    /**
     * Saves an active key, updates current membership, stores it in the multi-key vault,
     * and automatically purges expired keys.
     */
    public synchronized void saveActiveKey(String key, MembershipStatus status) {
        if (key == null || key.trim().isEmpty() || status == null) return;
        String cleanKey = key.trim();

        this.currentStatus = status;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_LICENSE_KEY, cleanKey);
        editor.putString(KEY_STATE, status.getState().name());
        editor.putString(KEY_PLAN, status.getPlanType());
        editor.putLong(KEY_START_TS, status.getStartTimestamp());
        editor.putLong(KEY_EXPIRY_TS, status.getExpiryTimestamp());
        editor.putBoolean(KEY_UNLOCKED, status.isFeaturesUnlocked());
        editor.putString(KEY_DEVICE_ID, status.getBoundDeviceId());

        // Upsert into multi-key vault
        List<CachedKey> vault = loadVaultFromPrefs();
        boolean found = false;
        long now = System.currentTimeMillis();

        for (int i = 0; i < vault.size(); i++) {
            CachedKey existing = vault.get(i);
            if (existing.getKey().equalsIgnoreCase(cleanKey)) {
                vault.set(i, new CachedKey(
                        cleanKey,
                        status.getPlanType(),
                        status.getStartTimestamp(),
                        status.getExpiryTimestamp(),
                        status.getBoundDeviceId(),
                        now
                ));
                found = true;
                break;
            }
        }

        if (!found) {
            vault.add(new CachedKey(
                    cleanKey,
                    status.getPlanType(),
                    status.getStartTimestamp(),
                    status.getExpiryTimestamp(),
                    status.getBoundDeviceId(),
                    now
            ));
        }

        saveVaultToEditor(editor, vault);
        editor.apply();

        notifyListeners(status);
    }

    public synchronized void updateStatus(MembershipStatus newStatus) {
        this.currentStatus = newStatus;
        prefs.edit()
                .putString(KEY_STATE, newStatus.getState().name())
                .putString(KEY_PLAN, newStatus.getPlanType())
                .putLong(KEY_START_TS, newStatus.getStartTimestamp())
                .putLong(KEY_EXPIRY_TS, newStatus.getExpiryTimestamp())
                .putBoolean(KEY_UNLOCKED, newStatus.isFeaturesUnlocked())
                .putString(KEY_DEVICE_ID, newStatus.getBoundDeviceId())
                .apply();

        notifyListeners(newStatus);
    }

    /**
     * Checks if a key exists in the local vault and is still valid (unexpired).
     * If valid, activates it immediately without requiring redundant server roundtrips.
     */
    public synchronized boolean checkAndActivateKeyFromVault(String key) {
        if (key == null || key.trim().isEmpty()) return false;
        String cleanKey = key.trim();

        purgeExpiredKeysInternal();
        List<CachedKey> vault = loadVaultFromPrefs();

        for (CachedKey cached : vault) {
            if (cached.getKey().equalsIgnoreCase(cleanKey) && !cached.isExpired()) {
                MembershipStatus newStatus = new MembershipStatus(
                        MembershipStatus.State.ACTIVE,
                        cached.getPlanType(),
                        cached.getStartTimestamp(),
                        cached.getExpiryTimestamp(),
                        true,
                        cached.getBoundDeviceId()
                );
                saveActiveKey(cleanKey, newStatus);
                return true;
            }
        }
        return false;
    }

    /**
     * Switches the active license to another key stored in the vault if it is still valid.
     */
    public synchronized boolean switchKey(String targetKey) {
        return checkAndActivateKeyFromVault(targetKey);
    }

    /**
     * Retrieves all valid (unexpired) keys currently stored in the multi-key vault,
     * sorted by latest used first.
     */
    public synchronized List<CachedKey> getValidVaultKeys() {
        purgeExpiredKeysInternal();
        List<CachedKey> list = loadVaultFromPrefs();
        Collections.sort(list, new Comparator<CachedKey>() {
            @Override
            public int compare(CachedKey a, CachedKey b) {
                return Long.compare(b.getLastUsedTimestamp(), a.getLastUsedTimestamp());
            }
        });
        return list;
    }

    /**
     * Authoritatively revokes a key from the local vault (e.g. when server responds
     * with revoked, invalid, or expired).
     */
    public synchronized void revokeKey(String key) {
        if (key == null || key.trim().isEmpty()) return;
        String cleanKey = key.trim();

        List<CachedKey> vault = loadVaultFromPrefs();
        Iterator<CachedKey> iterator = vault.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getKey().equalsIgnoreCase(cleanKey)) {
                iterator.remove();
            }
        }

        SharedPreferences.Editor editor = prefs.edit();
        saveVaultToEditor(editor, vault);

        if (cleanKey.equalsIgnoreCase(getSavedLicenseKey())) {
            editor.putString(KEY_LICENSE_KEY, "");
            editor.putString(KEY_STATE, MembershipStatus.State.FREE.name());
            editor.putString(KEY_PLAN, "Free / Not Activated");
            editor.putLong(KEY_START_TS, 0);
            editor.putLong(KEY_EXPIRY_TS, 0);
            editor.putBoolean(KEY_UNLOCKED, false);
            currentStatus = MembershipStatus.createDefaultLoggedOut();
        }

        editor.apply();
        notifyListeners(currentStatus);
    }

    /**
     * Resets active session to Free/Logged Out, without wiping other valid keys in the vault.
     */
    public synchronized void clearSession() {
        prefs.edit()
                .putString(KEY_LICENSE_KEY, "")
                .putString(KEY_STATE, MembershipStatus.State.FREE.name())
                .putString(KEY_PLAN, "Free / Not Activated")
                .putLong(KEY_START_TS, 0)
                .putLong(KEY_EXPIRY_TS, 0)
                .putBoolean(KEY_UNLOCKED, false)
                .apply();

        currentStatus = MembershipStatus.createDefaultLoggedOut();
        notifyListeners(currentStatus);
    }

    /**
     * Completely wipes all preferences and the multi-key vault.
     */
    public synchronized void clearAllData() {
        prefs.edit().clear().apply();
        currentStatus = MembershipStatus.createDefaultLoggedOut();
        notifyListeners(currentStatus);
    }

    /**
     * Purges expired keys from the vault.
     */
    public synchronized void purgeExpiredKeys() {
        purgeExpiredKeysInternal();
    }

    private void purgeExpiredKeysInternal() {
        List<CachedKey> vault = loadVaultFromPrefs();
        boolean changed = false;
        long now = System.currentTimeMillis();

        Iterator<CachedKey> it = vault.iterator();
        while (it.hasNext()) {
            CachedKey k = it.next();
            if (k.getExpiryTimestamp() > 0 && now >= k.getExpiryTimestamp()) {
                it.remove();
                changed = true;
            }
        }

        if (changed) {
            SharedPreferences.Editor editor = prefs.edit();
            saveVaultToEditor(editor, vault);
            editor.apply();
        }
    }

    private List<CachedKey> loadVaultFromPrefs() {
        List<CachedKey> list = new ArrayList<>();
        String jsonStr = prefs.getString(KEY_KEY_VAULT, "[]");
        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                CachedKey k = CachedKey.fromJson(obj);
                if (k != null && !k.getKey().isEmpty()) {
                    list.add(k);
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    private void saveVaultToEditor(SharedPreferences.Editor editor, List<CachedKey> vault) {
        JSONArray arr = new JSONArray();
        for (CachedKey k : vault) {
            try {
                arr.put(k.toJson());
            } catch (Exception ignored) {}
        }
        editor.putString(KEY_KEY_VAULT, arr.toString());
    }

    public synchronized void addListener(MembershipChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void removeListener(MembershipChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(MembershipStatus status) {
        for (MembershipChangeListener l : listeners) {
            l.onMembershipChanged(status);
        }
    }
}
