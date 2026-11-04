package com.alpha10.aimshot.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.alpha10.aimshot.R;
import com.alpha10.aimshot.controller.UpdateController;
import com.alpha10.aimshot.util.FontHelper;

/**
 * Tactical Gaming HUD Dialog providing real-time visual feedback across all 9 update stages:
 * 1. Checking Internet Connection
 * 2. Connecting to Alpha10 Server
 * 3. Authenticating
 * 4. Downloading AimShot Engine
 * 5. Verifying APK Security
 * 6. Waiting for Installation Confirmation
 * 7. Installing AimShot Engine
 * 8. Installation Successful
 * 9. Opening AimShot Engine
 */
public class UpdateDialog extends Dialog implements UpdateController.UpdateListener {

    private final Activity activity;
    private final UpdateController controller;
    private final Handler handler = new Handler(Looper.getMainLooper());

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
    private int countdownSeconds = 3;
    private Runnable countdownRunnable;
    private final boolean autoStart;

    public UpdateDialog(@NonNull Activity activity) {
        this(activity, true);
    }

    public UpdateDialog(@NonNull Activity activity, boolean autoStart) {
        super(activity);
        this.activity = activity;
        this.controller = new UpdateController(activity);
        this.autoStart = autoStart;
    }

    public UpdateController getController() {
        return controller;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_update);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

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
            stopCountdown();
            controller.cancel();
            dismiss();
        });

        btnPrimary.setOnClickListener(v -> {
            if (isInstallSuccess) {
                stopCountdown();
                controller.launchInstalledApp();
                dismiss();
                return;
            }
            if (isUpdating) return;
            startUpdateProcess();
        });

        if (findViewById(android.R.id.content) != null) {
            FontHelper.applyRegularFont(findViewById(android.R.id.content));
        }

        boolean isInstalled = com.alpha10.aimshot.util.PackageInstallerHelper.isMainApkInstalled(activity);
        if (!isInstalled || autoStart) {
            startUpdateProcess();
        } else {
            checkUpdateStatus();
        }
    }

    public void onActivityResume() {
        if (controller != null && isShowing()) {
            controller.onResumeCheck(activity);
            if (com.alpha10.aimshot.util.PackageInstallerHelper.isMainApkInstalled(activity)) {
                onInstallationSuccess();
            }
        }
    }

    private void checkUpdateStatus() {
        tvStatusMessage.setText(R.string.update_checking);
        progressBar.setIndeterminate(true);
        tvErrorBox.setVisibility(View.GONE);
        btnPrimary.setText(R.string.update_btn_check);
        btnPrimary.setEnabled(false);

        controller.checkForUpdates(null, this);
    }

    private void startUpdateProcess() {
        isUpdating = true;
        isInstallSuccess = false;
        tvErrorBox.setVisibility(View.GONE);
        progressBar.setIndeterminate(false);
        progressBar.setProgress(0);
        btnPrimary.setEnabled(false);
        btnCancel.setEnabled(true);

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
                    tvVersionInfo.setText("AIMSHOT UPDATE HUD");
                    break;
            }
        }
    }

    @Override
    public void onDownloadProgress(long bytesDownloaded, long totalBytes, int percent) {
        if (tvProgressDetail != null) {
            if (percent > 0 && percent < 100) {
                float mbDownloaded = bytesDownloaded / (1024f * 1024f);
                float mbTotal = totalBytes / (1024f * 1024f);
                tvProgressDetail.setVisibility(View.VISIBLE);
                tvProgressDetail.setText(String.format("%.1f MB / %.1f MB (%d%%)", mbDownloaded, mbTotal, percent));
            } else {
                tvProgressDetail.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onInstallationSuccess() {
        isUpdating = false;
        isInstallSuccess = true;
        stopCountdown();

        if (progressBar != null) {
            progressBar.setIndeterminate(false);
            progressBar.setProgress(100);
        }

        if (tvStatusMessage != null) {
            tvStatusMessage.setText(getContext().getString(R.string.auto_launching_countdown, 3));
        }

        if (btnPrimary != null) {
            btnPrimary.setText(R.string.btn_open_app);
            btnPrimary.setEnabled(true);
            btnPrimary.setTextColor(Color.parseColor("#49DCA0"));
        }

        if (btnCancel != null) {
            btnCancel.setText(R.string.action_close);
        }

        // Start 3-second automatic launch countdown
        countdownSeconds = 3;
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                countdownSeconds--;
                if (countdownSeconds > 0) {
                    if (tvStatusMessage != null && isShowing()) {
                        tvStatusMessage.setText(getContext().getString(R.string.auto_launching_countdown, countdownSeconds));
                    }
                    handler.postDelayed(this, 1000);
                } else {
                    if (tvStatusMessage != null && isShowing()) {
                        tvStatusMessage.setText(R.string.stage_9_open);
                    }
                    // Attempt launch; if background restrictions block it, button remains active
                    controller.launchInstalledApp();
                }
            }
        };
        handler.postDelayed(countdownRunnable, 1000);
    }

    @Override
    public void onInstallationAborted() {
        isUpdating = false;
        isInstallSuccess = false;
        stopCountdown();

        if (progressBar != null) {
            progressBar.setIndeterminate(false);
        }

        if (tvStatusMessage != null) {
            tvStatusMessage.setText("Installation was cancelled.");
        }

        if (btnPrimary != null) {
            btnPrimary.setEnabled(true);
            btnPrimary.setText(R.string.btn_retry);
            btnPrimary.setOnClickListener(v -> startUpdateProcess());
        }
    }

    @Override
    public void onUpdateUpToDate(int currentVersionCode) {
        isUpdating = false;
        isInstallSuccess = false;
        stopCountdown();

        if (progressBar != null) {
            progressBar.setIndeterminate(false);
            progressBar.setProgress(100);
        }

        if (tvStatusMessage != null) {
            tvStatusMessage.setText(getContext().getString(R.string.update_up_to_date, controller.getInstalledVersionName()));
        }

        if (btnPrimary != null) {
            btnPrimary.setText(R.string.action_close);
            btnPrimary.setEnabled(true);
            btnPrimary.setOnClickListener(v -> dismiss());
        }
    }

    @Override
    public void onError(String errorMessage, boolean canRetry) {
        isUpdating = false;
        isInstallSuccess = false;
        stopCountdown();

        if (progressBar != null) {
            progressBar.setIndeterminate(false);
        }

        if (tvErrorBox != null) {
            tvErrorBox.setText(errorMessage);
            tvErrorBox.setVisibility(View.VISIBLE);
        }

        if (tvStatusMessage != null) {
            tvStatusMessage.setText("Operation Halted");
        }

        if (btnPrimary != null) {
            btnPrimary.setEnabled(true);
            btnPrimary.setText(canRetry ? R.string.btn_retry : R.string.action_close);
            btnPrimary.setOnClickListener(v -> {
                if (canRetry) {
                    startUpdateProcess();
                } else {
                    dismiss();
                }
            });
        }
    }

    private void stopCountdown() {
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
            countdownRunnable = null;
        }
    }

    @Override
    public void dismiss() {
        stopCountdown();
        super.dismiss();
    }
}
