package com.alpha10.aimshot.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.alpha10.aimshot.R;
import com.alpha10.aimshot.controller.UpdateController;
import com.alpha10.aimshot.model.ReleaseManifest;
import com.alpha10.aimshot.util.FontHelper;

import java.io.File;

/**
 * Tactical Gaming HUD Dialog providing real-time update check, authentication,
 * progress tracking, and installer invocation feedback.
 */
public class UpdateDialog extends Dialog implements UpdateController.UpdateListener {

    private final Activity activity;
    private final UpdateController controller;

    private TextView tvTitle;
    private TextView tvVersionInfo;
    private ProgressBar progressBar;
    private TextView tvProgressDetail;
    private TextView tvStatusMessage;
    private TextView tvErrorBox;
    private Button btnCancel;
    private Button btnPrimary;

    private boolean isUpdating = false;

    public UpdateDialog(@NonNull Activity activity) {
        super(activity);
        this.activity = activity;
        this.controller = new UpdateController(activity);
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
            controller.cancel();
            dismiss();
        });

        btnPrimary.setOnClickListener(v -> {
            if (isUpdating) return;
            startUpdateProcess();
        });

        if (findViewById(android.R.id.content) != null) {
            FontHelper.applyRegularFont(findViewById(android.R.id.content));
        }

        // Auto-check on display
        checkUpdateStatus();
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
        tvErrorBox.setVisibility(View.GONE);
        progressBar.setIndeterminate(false);
        progressBar.setProgress(0);
        btnPrimary.setEnabled(false);
        btnCancel.setEnabled(true);

        controller.startUpdatePipeline(activity, this);
    }

    @Override
    public void onStatusUpdate(String statusMessage, int progressPercent) {
        if (tvStatusMessage != null) {
            tvStatusMessage.setText(statusMessage);
        }
        if (progressBar != null) {
            if (progressPercent > 0) {
                progressBar.setIndeterminate(false);
                progressBar.setProgress(progressPercent);
            } else {
                progressBar.setIndeterminate(true);
            }
        }
        if (tvProgressDetail != null) {
            if (progressPercent > 0 && progressPercent < 100) {
                tvProgressDetail.setVisibility(View.VISIBLE);
                tvProgressDetail.setText(progressPercent + "% Completed");
            } else {
                tvProgressDetail.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onUpdateReadyToInstall(File verifiedApk) {
        isUpdating = false;
        if (tvStatusMessage != null) {
            tvStatusMessage.setText(R.string.update_status_installing);
        }
        if (btnPrimary != null) {
            btnPrimary.setText(R.string.action_close);
            btnPrimary.setEnabled(true);
            btnPrimary.setOnClickListener(v -> dismiss());
        }
    }

    @Override
    public void onUpdateComplete() {
        isUpdating = false;
        dismiss();
    }

    @Override
    public void onUpdateUpToDate(int currentVersionCode) {
        isUpdating = false;
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
    public void onUpdateError(String errorMessage, boolean canRetry) {
        isUpdating = false;
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
}
