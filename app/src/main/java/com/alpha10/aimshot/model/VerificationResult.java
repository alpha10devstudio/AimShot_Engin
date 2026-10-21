package com.alpha10.aimshot.model;

/**
 * Standard server response wrapper for license key verification.
 */
public class VerificationResult {
    private final boolean success;
    private final String message;
    private final MembershipStatus membershipStatus;
    private final String errorCode;

    public VerificationResult(boolean success, String message, MembershipStatus membershipStatus, String errorCode) {
        this.success = success;
        this.message = message;
        this.membershipStatus = membershipStatus;
        this.errorCode = errorCode;
    }

    public static VerificationResult failure(String message, String errorCode) {
        return new VerificationResult(false, message, null, errorCode);
    }

    public static VerificationResult success(String message, MembershipStatus status) {
        return new VerificationResult(true, message, status, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public MembershipStatus getMembershipStatus() {
        return membershipStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
