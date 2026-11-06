package com.alpha10.aimshot.model;

import android.content.Context;
import com.alpha10.aimshot.R;
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

    public long getRemainingMillis() {
        if (getState() != State.ACTIVE || expiryTimestamp <= 0) {
            return 0;
        }
        long diffMillis = expiryTimestamp - System.currentTimeMillis();
        return Math.max(0, diffMillis);
    }

    public long getRemainingSeconds() {
        return getRemainingMillis() / 1000;
    }

    public int getRemainingDaysPart() {
        return (int) (getRemainingSeconds() / 86400);
    }

    public int getRemainingHoursPart() {
        return (int) ((getRemainingSeconds() % 86400) / 3600);
    }

    public int getRemainingMinutesPart() {
        return (int) ((getRemainingSeconds() % 3600) / 60);
    }

    public int getRemainingSecondsPart() {
        return (int) (getRemainingSeconds() % 60);
    }

    /**
     * Formats real-time live countdown in the exact required pattern:
     * [X] দিন [HH]:[MM]:[SS] (e.g. "০ দিন ১২:৪৫:৩০সে" or "18 Days 14:22:15s")
     */
    public String getFormattedLiveCountdown(Context context) {
        if (context == null || getState() != State.ACTIVE || expiryTimestamp <= 0) {
            return context != null ? context.getString(R.string.countdown_zero) : "0 Days 00:00:00s";
        }
        long totalSecs = getRemainingSeconds();
        if (totalSecs <= 0) {
            return context.getString(R.string.countdown_zero);
        }
        int d = getRemainingDaysPart();
        int h = getRemainingHoursPart();
        int m = getRemainingMinutesPart();
        int s = getRemainingSecondsPart();

        Locale currentLocale = context.getResources().getConfiguration().locale;
        String lang = currentLocale != null ? currentLocale.getLanguage() : "en";

        if ("bn".equals(lang)) {
            String dStr = toBengaliDigits(String.valueOf(d));
            String hStr = toBengaliDigits(String.format(Locale.US, "%02d", h));
            String mStr = toBengaliDigits(String.format(Locale.US, "%02d", m));
            String sStr = toBengaliDigits(String.format(Locale.US, "%02d", s));
            return context.getString(R.string.countdown_format, dStr, hStr, mStr, sStr);
        } else if ("hi".equals(lang)) {
            String dStr = String.valueOf(d);
            String hStr = String.format(Locale.US, "%02d", h);
            String mStr = String.format(Locale.US, "%02d", m);
            String sStr = String.format(Locale.US, "%02d", s);
            return context.getString(R.string.countdown_format, dStr, hStr, mStr, sStr);
        } else {
            return context.getString(R.string.countdown_format, d, h, m, s);
        }
    }

    private static String toBengaliDigits(String input) {
        if (input == null) return "";
        char[] bnDigits = {'০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'};
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= '0' && c <= '9') {
                sb.append(bnDigits[c - '0']);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
