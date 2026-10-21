package com.alpha10.aimshot.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.alpha10.aimshot.model.MembershipStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Single source of truth for the active user's license membership state.
 * Employs listeners to synchronize Home, Premium, and Settings screens.
 */
public class MembershipRepository {

    private static final String PREF_NAME = "aimshot_secure_license_prefs";
    private static final String KEY_STATE = "membership_state";
    private static final String KEY_PLAN = "plan_type";
    private static final String KEY_START_TS = "start_ts";
    private static final String KEY_EXPIRY_TS = "expiry_ts";
    private static final String KEY_UNLOCKED = "features_unlocked";
    private static final String KEY_DEVICE_ID = "bound_device_id";

    private static MembershipRepository instance;
    private final SharedPreferences prefs;
    private MembershipStatus currentStatus;
    private final List<MembershipChangeListener> listeners = new ArrayList<>();

    public interface MembershipChangeListener {
        void onMembershipChanged(MembershipStatus newStatus);
    }

    private MembershipRepository(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadFromPrefs();
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

    public synchronized void clearSession() {
        prefs.edit().clear().apply();
        currentStatus = MembershipStatus.createDefaultLoggedOut();
        notifyListeners(currentStatus);
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
