package com.alpha10.aimshot.service;

import com.alpha10.aimshot.model.VerificationResult;

/**
 * Asynchronous callback interface for license verification operations.
 */
public interface VerificationCallback {
    void onVerificationStarted();
    void onVerificationSuccess(VerificationResult result);
    void onVerificationError(String errorMessage, String errorCode);
}
