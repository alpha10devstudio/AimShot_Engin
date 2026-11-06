package com.alpha10.aimshot.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.alpha10.aimshot.R;
import com.alpha10.aimshot.controller.UpdateController;
import com.alpha10.aimshot.util.DialogUiHelper;
import com.alpha10.aimshot.util.FontHelper;
import com.alpha10.aimshot.util.PackageInstallerHelper;

/**
 * Tactical HUD Dialog displaying real-time progress across Private Vault download,
 * SHA-256 verification, native engine extraction, and in-app container launch.
 */
public class UpdateDialog extends Dialog implements UpdateController.UpdateListener {

    public interface OnInstallStateChangedListener {
        void onMainApkInstalled();
    }

    private final Activity activity;
    private final UpdateController controller;
    private final boolean autoStart;

    private TextView tvTitle;
    private TextView tvVersionInfo;
    private ProgressBar progressBar;
    private TextView tvProgressDetail;
    private TextView tvStatusMessage;
    private TextView tvErrorBox;
    private Button btnCancel;
    private Button btnPrimary;

    private boolean isUpdating = false;
    private boolean isInstallSuccess = false;
    private OnInstallStateChangedListener installStateChangedListener;

    public UpdateDialog(@NonNull Activity activity) {
        this(activity, true);
    }

    public UpdateDialog(@NonNull Activity activity, boolean autoStart) {
        super(activity, R.style.Theme_AimShotEngine_CustomDialog);
        this.activity = activity;
        this.controller = new UpdateController(activity);
        this.autoStart = autoStart;
    }

    public void setOnInstallStateChangedListener(OnInstallStateChangedListener listener) {
        this.installStateChangedListener = listener;
    }

    public UpdateController getController() {
        return controller;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_update);
        DialogUiHelper.prepareDialogWindowBeforeShow(this);

        setCancelable(false);
        setCanceledOnTouchOutside(false);

        tvTitle = findViewById(R.id.tv_update_title);
        tvVersionInfo = findViewById(R.id.tv_update_version_info);
        progressBar = findViewById(R.id.progress_update_bar);
        tvProgressDetail = findViewById(R.id.tv_update_progress_detail);
        tvStatusMessage = findViewById(R.id.tv_update_status_message);
        tvErrorBox = findViewById(R.id.tv_update_error_box);
        btnCancel = findViewById(R.id.btn_update_cancel);
        btnPrimary = findViewById(R.id.btn_update_primary);

        btnCancel.setOnClickListener(v -> {
            controller.cancel();
            dismiss();
        });

        bindPrimaryActionButton();

        if (findViewById(android.R.id.content) != null) {
            FontHelper.applyRegularFont(findViewById(android.R.id.content));
        }

        boolean isInstalled = PackageInstallerHelper.isMainApkInstalled(activity);
        if (!isInstalled || autoStart) {
            startUpdateProcess();
        } else {
            checkUpdateStatus();
        }
    }

    @Override
    public void show() {
        DialogUiHelper.prepareDialogWindowBeforeShow(this);
        super.show();
        DialogUiHelper.applyPostShowWindowMetrics(this);
    }

    private void bindPrimaryActionButton() {
        if (btnPrimary == null) return;
        btnPrimary.setOnClickListener(v -> {
            if (isInstallSuccess) {
                boolean launched = controller.launchInstalledApp(activity);
                if (launched) {
                    dismiss();
                } else {
                    Toast.makeText(activity, R.string.failed_to_launch_main_apk, Toast.LENGTH_LONG).show();
                }
                return;
            }
            if (isUpdating) return;

            if (controller.getPendingVerifiedApk() != null && controller.getPendingVerifiedApk().exists()) {
                isUpdating = true;
                tvErrorBox.setVisibility(View.GONE);
                setPrimaryButtonState(false, R.string.update_btn_download);
                controller.proceedToInstallation(activity);
                return;
            }

            startUpdateProcess();
        });
    }

    private void setPrimaryButtonState(boolean enabled, int textResId) {
        if (btnPrimary == null) return;
        btnPrimary.setEnabled(enabled);
        btnPrimary.setText(textResId);
        btnPrimary.setTextColor(ContextCompat.getColor(
                getContext(),
                enabled ? R.color.text_on_accent : R.color.text_muted
        ));
    }

    public void onActivityResume() {
        if (controller != null && isShowing()) {
            controller.onResumeCheck(activity);
        }
    }

    private void checkUpdateStatus() {
        tvStatusMessage.setText(R.string.update_checking);
        progressBar.setIndeterminate(true);
        tvErrorBox.setVisibility(View.GONE);
        setPrimaryButtonState(false, R.string.update_btn_check);

        controller.checkForUpdates(null, this);
    }

    private void startUpdateProcess() {
        isUpdating = true;
        isInstallSuccess = false;
        tvErrorBox.setVisibility(View.GONE);
        progressBar.setIndeterminate(false);
        progressBar.setProgress(0);
        setPrimaryButtonState(false, R.string.update_btn_download);
        btnCancel.setEnabled(true);
        btnCancel.setText(R.string.btn_cancel);
        btnCancel.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        bindPrimaryActionButton();

        controller.startUpdatePipeline(activity, this);
    }

    @Override
    public void onStageChanged(UpdateController.Stage stage, String message, int progressPercent) {
        if (tvStatusMessage != null) {
            tvStatusMessage.setText(message);
        }

        if (progressBar != null) {
            if (progressPercent > 0) {
                progressBar.setIndeterminate(false);
                progressBar.setProgress(progressPercent);
            } else {
                progressBar.setIndeterminate(true);
            }
        }

        if (tvVersionInfo != null) {
            switch (stage) {
                case CHECKING_INTERNET:
                    tvVersionInfo.setText(R.string.stage_1_internet);
                    break;
                case CONNECTING_SERVER:
                    tvVersionInfo.setText(R.string.stage_2_server);
                    break;
                case AUTHENTICATING:
                    tvVersionInfo.setText(R.string.stage_3_auth);
                    break;
                case DOWNLOADING:
                    tvVersionInfo.setText(R.string.stage_4_download);
                    break;
                case VERIFYING_SECURITY:
                    tvVersionInfo.setText(R.string.stage_5_verify);
                    break;
                case WAITING_CONFIRMATION:
                    tvVersionInfo.setText(R.string.stage_6_confirm);
                    break;
                case INSTALLING:
                    tvVersionInfo.setText(R.string.stage_7_install);
                    break;
                case INSTALL_SUCCESS:
                    tvVersionInfo.setText(R.string.stage_8_success);
                    break;
                case OPENING_APP:
                    tvVersionInfo.setText(R.string.stage_9_open);
                    break;
                default:
                    tvVersionInfo.setText("AIMSHOT CORE ENGINE");
                    break;
            }
        }
    }

    @Override
    public void onDownloadProgress(long bytesDownloaded, long totalBytes, int percent) {
        if (tvProgressDetail != null) {
            if (percent > 0 && percent < 100 && totalBytes > 0) {
                float mbDownloaded = bytesDownloaded / (1024f * 1024f);
                float mbTotal = totalBytes / (1024f * 1024f);
                tvProgressDetail.setVisibility(View.VISIBLE);
                tvProgressDetail.setText(String.format(Locale.US, "%.1f MB / %.1f MB (%d%%)", mbDownloaded, mbTotal, percent));
            } else {
                tvProgressDetail.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onInstallationSuccess() {
        isUpdating = false;
        isInstallSuccess = true;

        if (tvProgressDetail != null) {
            tvProgressDetail.setVisibility(View.GONE);
        }
        if (tvErrorBox != null) {
            tvErrorBox.setVisibility(View.GONE);
        }
        if (progressBar != null) {
            progressBar.setIndeterminate(false);
            progressBar.setProgress(100);
        }
        if (tvStatusMessage != null) {
            tvStatusMessage.setText(R.string.stage_8_success);
        }

        setPrimaryButtonState(true, R.string.btn_open_app);
        bindPrimaryActionButton();

        if (btnCancel != null) {
            btnCancel.setText(R.string.action_close);
            btnCancel.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        }

        if (installStateChangedListener != null) {
            installStateChangedListener.onMainApkInstalled();
        }
    }

    @Override
    public void onInstallationAborted() {
        isUpdating = false;
        isInstallSuccess = false;

        if (progressBar != null) {
            progressBar.setIndeterminate(false);
            progressBar.setProgress(0);
        }
        if (tvStatusMessage != null) {
            tvStatusMessage.setText("Setup was cancelled by user.");
        }
        setPrimaryButtonState(true, R.string.btn_retry);
        bindPrimaryActionButton();
    }

    @Override
    public void onUpdateUpToDate(int currentVersionCode) {
        isUpdating = false;
        isInstallSuccess = true;

        if (progressBar != null) {
            progressBar.setIndeterminate(false);
            progressBar.setProgress(100);
        }
        if (tvStatusMessage != null) {
            tvStatusMessage.setText(getContext().getString(R.string.update_up_to_date, controller.getInstalledVersionName()));
        }
        setPrimaryButtonState(true, R.string.btn_open_app);
        bindPrimaryActionButton();

        if (btnCancel != null) {
            btnCancel.setText(R.string.action_close);
            btnCancel.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        }
    }

    @Override
    public void onError(String errorMessage, boolean canRetry) {
        isUpdating = false;
        isInstallSuccess = false;

        if (progressBar != null) {
            progressBar.setIndeterminate(false);
        }
        if (tvErrorBox != null) {
            tvErrorBox.setText(errorMessage);
            tvErrorBox.setVisibility(View.VISIBLE);
        }
        if (tvStatusMessage != null) {
            tvStatusMessage.setText("Verification / Vault Setup Halted");
        }
        if (btnPrimary != null) {
            setPrimaryButtonState(true, canRetry ? R.string.btn_retry : R.string.action_close);
            if (canRetry) {
                bindPrimaryActionButton();
            } else {
                btnPrimary.setOnClickListener(v -> dismiss());
            }
        }
    }
}
