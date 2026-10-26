package com.alpha10.aimshot.dialog;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.alpha10.aimshot.R;
import com.alpha10.aimshot.model.VerificationResult;
import com.alpha10.aimshot.repository.MembershipRepository;
import com.alpha10.aimshot.service.DeviceIdManager;
import com.alpha10.aimshot.service.LicenseVerificationService;
import com.alpha10.aimshot.service.VerificationCallback;
import com.alpha10.aimshot.util.FontHelper;

/**
 * Tactical verification key modal dialog with right-aligned Paste button.
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
        super(context);
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

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        editKey = findViewById(R.id.edit_verification_key);
        btnPaste = findViewById(R.id.btn_paste_key);
        btnLogin = findViewById(R.id.btn_dialog_login);
        tvClose = findViewById(R.id.tv_dialog_close);
        tvError = findViewById(R.id.tv_dialog_error);
        progressBar = findViewById(R.id.progress_verification);

        // Apply regular typography
        if (findViewById(android.R.id.content) != null) {
            FontHelper.applyRegularFont(findViewById(android.R.id.content));
        }

        // Paste button listener
        if (btnPaste != null) {
            btnPaste.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handlePasteFromClipboard();
                }
            });
        }

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptVerification();
            }
        });

        tvClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    /**
     * Reads clipboard text, replaces verification key field content,
     * positions cursor at the end, and preserves editability without auto-submitting.
     */
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
