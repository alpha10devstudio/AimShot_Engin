package com.alpha10.aimshot.model;

/**
 * Model representing a supported game item with verified tactical tooling.
 */
public class GameItem {
    private final String id;
    private final String name;
    private final String featureSummary;
    private final int iconResId;
    private final boolean isVerified;
    private final String launchPackage;

    public GameItem(String id, String name, String featureSummary, int iconResId, boolean isVerified, String launchPackage) {
        this.id = id;
        this.name = name;
        this.featureSummary = featureSummary;
        this.iconResId = iconResId;
        this.isVerified = isVerified;
        this.launchPackage = launchPackage;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFeatureSummary() {
        return featureSummary;
    }

    public int getIconResId() {
        return iconResId;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public String getLaunchPackage() {
        return launchPackage;
    }
}
