package com.alpha10.aimshot.dialog;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.alpha10.aimshot.R;
import com.alpha10.aimshot.model.GameItem;
import com.alpha10.aimshot.util.FontHelper;
import com.alpha10.aimshot.util.NetworkUtil;

/**
 * Tactical Launch Dialog orchestrating internet verification, HUD status updates,
 * and game package launch intent execution.
 */
public class GameLaunchDialog extends Dialog {

    public interface OnLaunchListener {
        void onLaunchComplete(boolean success);
    }

    private final GameItem gameItem;
    private final OnLaunchListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView tvStatusMessage;
    private Button btnAbort;
    private boolean isAborted = false;

    private Runnable step2Runnable;
    private Runnable step3Runnable;

    public GameLaunchDialog(@NonNull Context context, @NonNull GameItem gameItem, OnLaunchListener listener) {
        super(context);
        this.gameItem = gameItem;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_game_launch);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        setCancelable(false);
        setCanceledOnTouchOutside(false);

        ImageView ivIcon = findViewById(R.id.iv_launch_game_icon);
        TextView tvName = findViewById(R.id.tv_launch_game_name);
        tvStatusMessage = findViewById(R.id.tv_launch_status_message);
        btnAbort = findViewById(R.id.btn_launch_abort);

        if (gameItem != null) {
            if (ivIcon != null && gameItem.getIconRes() != 0) {
                ivIcon.setImageResource(gameItem.getIconRes());
            }
            if (tvName != null) {
                tvName.setText(gameItem.getName());
            }
        }

        if (btnAbort != null) {
            btnAbort.setOnClickListener(v -> abortLaunch("Launch aborted by user"));
        }

        if (findViewById(android.R.id.content) != null) {
            FontHelper.applyRegularFont(findViewById(android.R.id.content));
        }

        startLaunchSequence();
    }

    private void startLaunchSequence() {
        // Step 1: Check initial network connectivity
        if (!NetworkUtil.isNetworkConnected(getContext())) {
            abortLaunch(getContext().getString(R.string.no_internet_msg));
            return;
        }

        tvStatusMessage.setText(R.string.launching_status_init);

        // Step 2: Sync overlay state at 700ms
        step2Runnable = new Runnable() {
            @Override
            public void run() {
                if (isAborted || !isShowing()) return;

                // Validate network is still alive
                if (!NetworkUtil.isNetworkConnected(getContext())) {
                    abortLaunch(getContext().getString(R.string.connection_lost));
                    return;
                }

                tvStatusMessage.setText(R.string.launching_status_sync);

                // Step 3: Trigger game package launch at 1400ms
                step3Runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (isAborted || !isShowing()) return;
                        executeGameLaunch();
                    }
                };
                handler.postDelayed(step3Runnable, 700);
            }
        };
        handler.postDelayed(step2Runnable, 700);
    }

    private void executeGameLaunch() {
        // Final internet safety check
        if (!NetworkUtil.isNetworkConnected(getContext())) {
            abortLaunch(getContext().getString(R.string.connection_lost));
            return;
        }

        tvStatusMessage.setText(getContext().getString(R.string.launching_game, gameItem.getName()));

        String packageName = gameItem.getPackageName();
        Intent launchIntent = null;
        try {
            launchIntent = getContext().getPackageManager().getLaunchIntentForPackage(packageName);
        } catch (Exception ignored) {}

        if (launchIntent != null) {
            try {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(launchIntent);
                Toast.makeText(getContext(), "AimShot Engine active: " + gameItem.getName(), Toast.LENGTH_SHORT).show();
                dismiss();
                if (listener != null) {
                    listener.onLaunchComplete(true);
                }
            } catch (Exception e) {
                dismiss();
                showNotInstalledDialog();
                if (listener != null) {
                    listener.onLaunchComplete(false);
                }
            }
        } else {
            dismiss();
            showNotInstalledDialog();
            if (listener != null) {
                listener.onLaunchComplete(false);
            }
        }
    }

    private void abortLaunch(String reason) {
        isAborted = true;
        cleanupHandlers();
        dismiss();
        if (reason != null && !reason.isEmpty()) {
            Toast.makeText(getContext(), reason, Toast.LENGTH_SHORT).show();
        }
        if (listener != null) {
            listener.onLaunchComplete(false);
        }
    }

    private void cleanupHandlers() {
        if (step2Runnable != null) handler.removeCallbacks(step2Runnable);
        if (step3Runnable != null) handler.removeCallbacks(step3Runnable);
    }

    @Override
    public void dismiss() {
        cleanupHandlers();
        super.dismiss();
    }

    /**
     * Tactical Dialog shown when targeted game is not found on device.
     */
    private void showNotInstalledDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.Theme_AimShotEngine_Dialog);
        builder.setTitle(R.string.game_not_installed_title);
        builder.setMessage(getContext().getString(R.string.game_not_installed_msg, gameItem.getName()));
        builder.setPositiveButton(R.string.btn_install_game, (dialog, which) -> {
            try {
                Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + gameItem.getPackageName()));
                marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(marketIntent);
            } catch (ActivityNotFoundException e) {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + gameItem.getPackageName()));
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(webIntent);
            }
        });
        builder.setNegativeButton(R.string.action_close, null);
        builder.show();
    }
}
