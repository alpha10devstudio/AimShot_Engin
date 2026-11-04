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
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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
 * Tactical Launch Dialog orchestrating strict 4-step internet & Alpha10 Server verification,
 * HUD status updates, and game package launch intent execution with solid opaque dialogs.
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
        super(context, R.style.Theme_AimShotEngine_CustomDialog);
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
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.dimAmount = 0.68f;
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            getWindow().setAttributes(lp);
        }

        setCancelable(false);
        setCanceledOnTouchOutside(false);

        ImageView ivIcon = findViewById(R.id.iv_launch_game_icon);
        TextView tvName = findViewById(R.id.tv_launch_game_name);
        tvStatusMessage = findViewById(R.id.tv_launch_status_message);
        btnAbort = findViewById(R.id.btn_launch_abort);

        if (gameItem != null) {
            if (ivIcon != null && gameItem.getIconResId() != 0) {
                ivIcon.setImageResource(gameItem.getIconResId());
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
        isAborted = false;
        if (tvStatusMessage != null) {
            tvStatusMessage.setText(R.string.verifying_network_msg);
        }

        // Enforce full 4-step active internet + Alpha10 server verification before launching
        NetworkUtil.verifyFullLaunchConnectivity(getContext(), status -> {
            if (isAborted || !isShowing()) return;

            if (status != NetworkUtil.ConnectivityStatus.CONNECTED) {
                isAborted = true;
                cleanupHandlers();
                dismiss();
                showConnectivityFailureDialog(status);
                if (listener != null) {
                    listener.onLaunchComplete(false);
                }
                return;
            }

            if (tvStatusMessage != null) {
                tvStatusMessage.setText(R.string.launching_status_init);
            }

            // Step 2: Sync overlay state at 600ms
            step2Runnable = new Runnable() {
                @Override
                public void run() {
                    if (isAborted || !isShowing()) return;

                    if (!NetworkUtil.isNetworkConnected(getContext())) {
                        abortLaunch(getContext().getString(R.string.connection_lost));
                        return;
                    }

                    if (tvStatusMessage != null) {
                        tvStatusMessage.setText(R.string.launching_status_sync);
                    }

                    // Step 3: Trigger game package launch at 1200ms
                    step3Runnable = new Runnable() {
                        @Override
                        public void run() {
                            if (isAborted || !isShowing()) return;
                            executeGameLaunch();
                        }
                    };
                    handler.postDelayed(step3Runnable, 600);
                }
            };
            handler.postDelayed(step2Runnable, 600);
        });
    }

    private void executeGameLaunch() {
        if (!NetworkUtil.isNetworkConnected(getContext())) {
            abortLaunch(getContext().getString(R.string.connection_lost));
            return;
        }

        if (tvStatusMessage != null) {
            tvStatusMessage.setText(getContext().getString(R.string.launching_game, gameItem.getName()));
        }

        String packageName = gameItem.getLaunchPackage();
        Intent launchIntent = null;
        try {
            launchIntent = getContext().getPackageManager().getLaunchIntentForPackage(packageName);
        } catch (Exception ignored) {
        }

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

    private void showConnectivityFailureDialog(NetworkUtil.ConnectivityStatus status) {
        Context ctx = getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx, R.style.Theme_AimShotEngine_CustomDialog);
        View dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_tactical_confirm, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();
        configureSolidWindow(dialog);

        ImageView ivIcon = dialogView.findViewById(R.id.iv_confirm_icon);
        TextView tvTitle = dialogView.findViewById(R.id.tv_confirm_title);
        TextView tvMsg = dialogView.findViewById(R.id.tv_confirm_message);
        Button btnClose = dialogView.findViewById(R.id.btn_confirm_negative);
        Button btnRetry = dialogView.findViewById(R.id.btn_confirm_positive);

        if (ivIcon != null) ivIcon.setImageResource(R.drawable.ic_globe);
        if (status == NetworkUtil.ConnectivityStatus.SERVER_UNREACHABLE) {
            if (tvTitle != null) tvTitle.setText(R.string.server_connection_failed_title);
            if (tvMsg != null) tvMsg.setText(R.string.server_connection_failed_msg);
        } else if (status == NetworkUtil.ConnectivityStatus.NO_INTERNET_ACCESS) {
            if (tvTitle != null) tvTitle.setText(R.string.no_internet_title);
            if (tvMsg != null) tvMsg.setText(R.string.no_internet_access_msg);
        } else {
            if (tvTitle != null) tvTitle.setText(R.string.no_internet_title);
            if (tvMsg != null) tvMsg.setText(R.string.no_internet_msg);
        }

        if (btnClose != null) {
            btnClose.setText(R.string.action_close);
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
        if (btnRetry != null) {
            btnRetry.setText(R.string.btn_retry);
            btnRetry.setOnClickListener(v -> {
                dialog.dismiss();
                new GameLaunchDialog(ctx, gameItem, listener).show();
            });
        }

        FontHelper.applyRegularFont(dialogView);
        dialog.show();
        applySolidDim(dialog);
    }

    /**
     * Solid Tactical Dialog shown when targeted game is not found on device.
     */
    private void showNotInstalledDialog() {
        Context ctx = getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx, R.style.Theme_AimShotEngine_CustomDialog);
        View dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_tactical_confirm, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();
        configureSolidWindow(dialog);

        TextView tvTitle = dialogView.findViewById(R.id.tv_confirm_title);
        TextView tvMsg = dialogView.findViewById(R.id.tv_confirm_message);
        Button btnClose = dialogView.findViewById(R.id.btn_confirm_negative);
        Button btnInstall = dialogView.findViewById(R.id.btn_confirm_positive);

        if (tvTitle != null) tvTitle.setText(R.string.game_not_installed_title);
        if (tvMsg != null) tvMsg.setText(ctx.getString(R.string.game_not_installed_msg, gameItem.getName()));

        if (btnClose != null) {
            btnClose.setText(R.string.action_close);
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
        if (btnInstall != null) {
            btnInstall.setText(R.string.btn_install_game);
            btnInstall.setOnClickListener(v -> {
                dialog.dismiss();
                try {
                    Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + gameItem.getLaunchPackage()));
                    marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(marketIntent);
                } catch (ActivityNotFoundException e) {
                    Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + gameItem.getLaunchPackage()));
                    webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(webIntent);
                }
            });
        }

        FontHelper.applyRegularFont(dialogView);
        dialog.show();
        applySolidDim(dialog);
    }

    private static void configureSolidWindow(AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private static void applySolidDim(AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.dimAmount = 0.68f;
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setAttributes(lp);
        }
    }
}
