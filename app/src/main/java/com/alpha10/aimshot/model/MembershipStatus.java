package com.alpha10.aimshot.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Encapsulates the user's license membership state.
 * Prevents unauthorized local elevation of privileges.
 */
public class MembershipStatus implements Serializable {

    public enum State {
        FREE,
        ACTIVE,
        EXPIRED,
        PENDING_VERIFICATION
    }

    private final State state;
    private final String planType;
    private final long startTimestamp;
    private final long expiryTimestamp;
    private final boolean featuresUnlocked;
    private final String boundDeviceId;

    public MembershipStatus(State state, String planType, long startTimestamp, long expiryTimestamp, boolean featuresUnlocked, String boundDeviceId) {
        this.state = state;
        this.planType = planType;
        this.startTimestamp = startTimestamp;
        this.expiryTimestamp = expiryTimestamp;
        this.featuresUnlocked = featuresUnlocked;
        this.boundDeviceId = boundDeviceId;
    }

    public static MembershipStatus createDefaultLoggedOut() {
        return new MembershipStatus(State.FREE, "Free / Not Activated", 0, 0, false, null);
    }

    public State getState() {
        // Enforce expiration check strictly against current system time
        if (state == State.ACTIVE && expiryTimestamp > 0 && System.currentTimeMillis() > expiryTimestamp) {
            return State.EXPIRED;
        }
        return state;
    }

    public String getPlanType() {
        return planType != null ? planType : "None";
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public long getExpiryTimestamp() {
        return expiryTimestamp;
    }

    public boolean isFeaturesUnlocked() {
        return getState() == State.ACTIVE && featuresUnlocked;
    }

    public String getBoundDeviceId() {
        return boundDeviceId;
    }

    public int getRemainingDays() {
        if (getState() != State.ACTIVE || expiryTimestamp <= 0) {
            return 0;
        }
        long diffMillis = expiryTimestamp - System.currentTimeMillis();
        if (diffMillis <= 0) {
            return 0;
        }
        return (int) TimeUnit.MILLISECONDS.toDays(diffMillis);
    }

    public String getFormattedStartDate() {
        if (startTimestamp <= 0) return "--";
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
        return sdf.format(new Date(startTimestamp));
    }

    public String getFormattedExpiryDate() {
        if (expiryTimestamp <= 0) return "--";
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
        return sdf.format(new Date(expiryTimestamp));
    }
}
