package com.alpha10.aimshot.dialog;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.alpha10.aimshot.R;
import com.alpha10.aimshot.model.VerificationResult;
import com.alpha10.aimshot.repository.MembershipRepository;
import com.alpha10.aimshot.service.DeviceIdManager;
import com.alpha10.aimshot.service.LicenseVerificationService;
import com.alpha10.aimshot.service.VerificationCallback;
import com.alpha10.aimshot.util.DialogUiHelper;
import com.alpha10.aimshot.util.FontHelper;

/**
 * Tactical verification key modal dialog with uniform 24dp screen margins,
 * rounded tactical icon badge, and right-aligned Paste button.
 */
public class VerificationKeyDialog extends Dialog {

    private EditText editKey;
    private View btnPaste;
    private Button btnLogin;
    private TextView tvClose;
    private TextView tvError;
    private ProgressBar progressBar;

    private final LicenseVerificationService verificationService;
    private final DeviceIdManager deviceIdManager;

    public interface OnLoginSuccessListener {
        void onLoginSuccess();
    }

    private OnLoginSuccessListener successListener;

    public VerificationKeyDialog(@NonNull Context context) {
        super(context, R.style.Theme_AimShotEngine_CustomDialog);
        this.verificationService = new LicenseVerificationService();
        this.deviceIdManager = new DeviceIdManager(context);
    }

    public void setOnLoginSuccessListener(OnLoginSuccessListener listener) {
        this.successListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_verification_key);
        DialogUiHelper.prepareDialogWindowBeforeShow(this);

        editKey = findViewById(R.id.edit_verification_key);
        btnPaste = findViewById(R.id.btn_paste_key);
        btnLogin = findViewById(R.id.btn_dialog_login);
        tvClose = findViewById(R.id.tv_dialog_close);
        tvError = findViewById(R.id.tv_dialog_error);
        progressBar = findViewById(R.id.progress_verification);

        if (findViewById(android.R.id.content) != null) {
            FontHelper.applyRegularFont(findViewById(android.R.id.content));
        }

        if (btnPaste != null) {
            btnPaste.setOnClickListener(v -> handlePasteFromClipboard());
        }

        btnLogin.setOnClickListener(v -> attemptVerification());

        tvClose.setOnClickListener(v -> dismiss());
    }

    @Override
    public void show() {
        DialogUiHelper.prepareDialogWindowBeforeShow(this);
        super.show();
        DialogUiHelper.applyPostShowWindowMetrics(this);
    }

    private void handlePasteFromClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData clipData = clipboard.getPrimaryClip();
                if (clipData != null && clipData.getItemCount() > 0) {
                    ClipData.Item item = clipData.getItemAt(0);
                    CharSequence clipText = (item != null) ? item.getText() : null;
                    if (clipText != null && clipText.length() > 0) {
                        String pastedKey = clipText.toString().trim();
                        editKey.setText(pastedKey);
                        editKey.setSelection(editKey.getText().length());
                        editKey.requestFocus();
                        Toast.makeText(getContext(), R.string.pasted_from_clipboard, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
            Toast.makeText(getContext(), R.string.clipboard_empty, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Unable to access clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void attemptVerification() {
        String key = editKey.getText().toString().trim();
        if (key.isEmpty()) {
            showError(getContext().getString(R.string.err_empty_key));
            return;
        }

        tvError.setVisibility(View.GONE);
        setLoadingState(true);

        String deviceId = deviceIdManager.getFormattedDeviceId();

        verificationService.verifyKey(key, deviceId, new VerificationCallback() {
            @Override
            public void onVerificationStarted() {
                setLoadingState(true);
            }

            @Override
            public void onVerificationSuccess(VerificationResult result) {
                setLoadingState(false);
                if (result.getMembershipStatus() != null) {
                    MembershipRepository.getInstance(getContext()).updateStatus(result.getMembershipStatus());
                    Toast.makeText(getContext(), result.getMessage(), Toast.LENGTH_SHORT).show();
                    if (successListener != null) {
                        successListener.onLoginSuccess();
                    }
                    dismiss();
                }
            }

            @Override
            public void onVerificationError(String errorMessage, String errorCode) {
                setLoadingState(false);
                showError(errorMessage);
            }
        });
    }

    private void setLoadingState(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (btnLogin != null) {
            btnLogin.setEnabled(!isLoading);
            btnLogin.setText(isLoading ? "Verifying..." : getContext().getString(R.string.btn_login));
            btnLogin.setTextColor(ContextCompat.getColor(
                    getContext(),
                    isLoading ? R.color.text_muted : R.color.text_on_accent
            ));
        }
        if (editKey != null) {
            editKey.setEnabled(!isLoading);
        }
        if (btnPaste != null) {
            btnPaste.setEnabled(!isLoading);
        }
    }

    private void showError(String message) {
        if (tvError != null) {
            tvError.setText(message);
            tvError.setVisibility(View.VISIBLE);
        }
    }
}
