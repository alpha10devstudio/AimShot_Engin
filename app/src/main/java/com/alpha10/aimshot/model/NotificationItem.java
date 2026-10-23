package com.alpha10.aimshot.model;

/**
 * Model representing an official AimShot Engine announcement / website-to-app notification.
 */
public class NotificationItem {

    private String id;
    private String title;
    private String message;
    private String category; // "announcement", "update", "feature", "maintenance"
    private String timestamp;
    private boolean isRead;
    private String minVersion;
    private String actionUrl;
    private String priority; // "normal", "high", "urgent"
    private boolean isActive;

    public NotificationItem() {
    }

    public NotificationItem(String id, String title, String message, String category,
                            String timestamp, boolean isRead, String priority) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.category = category;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.priority = priority;
        this.isActive = true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCategory() {
        return category != null ? category : "announcement";
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTimestamp() {
        return timestamp != null ? timestamp : "Just now";
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getMinVersion() {
        return minVersion;
    }

    public void setMinVersion(String minVersion) {
        this.minVersion = minVersion;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    public String getPriority() {
        return priority != null ? priority : "normal";
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
