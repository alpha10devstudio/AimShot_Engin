package com.alpha10.aimshot.model;

/**
 * Encapsulates official seller contact configuration.
 */
public class SellerInfo {
    private final String sellerName;
    private final boolean isOnline;
    private final boolean isVerified;
    private final String whatsappNumber;
    private final String telegramHandle;
    private final String emailAddress;
    private final String trustRating;

    public SellerInfo(String sellerName, boolean isOnline, boolean isVerified, String whatsappNumber, String telegramHandle, String emailAddress, String trustRating) {
        this.sellerName = sellerName;
        this.isOnline = isOnline;
        this.isVerified = isVerified;
        this.whatsappNumber = whatsappNumber;
        this.telegramHandle = telegramHandle;
        this.emailAddress = emailAddress;
        this.trustRating = trustRating;
    }

    public static SellerInfo getDefaultSeller() {
        return new SellerInfo(
            "AimShot Shop",
            true,
            true,
            "+8801700111222",
            "@aimshot_support",
            "aimshot@support.com",
            "100% Trusted"
        );
    }

    public String getSellerName() {
        return sellerName;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public String getWhatsappNumber() {
        return whatsappNumber;
    }

    public String getTelegramHandle() {
        return telegramHandle;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getTrustRating() {
        return trustRating;
    }
}
