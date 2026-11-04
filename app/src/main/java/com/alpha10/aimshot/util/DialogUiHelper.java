package com.alpha10.aimshot.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.appcompat.app.AlertDialog;

import com.alpha10.aimshot.R;

/**
 * Centralizes tactical dialog inflation, window sizing, horizontal screen margins,
 * transparent outer window frame, and 72% backdrop dim across all Dialog and AlertDialog instances.
 */
public final class DialogUiHelper {

    private static final float DEFAULT_DIM_AMOUNT = 0.72f;

    private DialogUiHelper() {
    }

    /**
     * Inflates a custom dialog layout inside a parent FrameLayout so root layout parameters and
     * outer padding insets are strictly preserved when passed to AlertDialog.Builder.setView().
     */
    public static View inflateDialogView(Context context, int layoutResId) {
        FrameLayout tempParent = new FrameLayout(context);
        View view = LayoutInflater.from(context).inflate(layoutResId, tempParent, false);
        FontHelper.applyRegularFont(view);
        return view;
    }

    /**
     * Creates a pre-styled AlertDialog using Theme_AimShotEngine_CustomDialog.
     */
    public static AlertDialog createTacticalAlertDialog(Context context, View dialogView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                context,
                R.style.Theme_AimShotEngine_CustomDialog
        );
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        prepareDialogWindowBeforeShow(dialog);
        return dialog;
    }

    /**
     * Prepares window background transparency prior to Dialog.show().
     */
    public static void prepareDialogWindowBeforeShow(Dialog dialog) {
        if (dialog == null) return;
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.getDecorView().setPadding(0, 0, 0, 0);
        }
    }

    /**
     * Shows the dialog and enforces full-width container sizing (so inner 24dp XML padding insets
     * apply uniformly) and consistent dim backdrop after Android's internal AlertDialog setup.
     */
    public static void showStyledDialog(Dialog dialog) {
        if (dialog == null) return;
        prepareDialogWindowBeforeShow(dialog);
        dialog.show();
        applyPostShowWindowMetrics(dialog);
    }

    /**
     * Applies post-show window dimensions, center gravity, and dim backdrop to any Dialog/AlertDialog.
     */
    public static void applyPostShowWindowMetrics(Dialog dialog) {
        if (dialog == null) return;
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.getDecorView().setPadding(0, 0, 0, 0);
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            WindowManager.LayoutParams lp = window.getAttributes();
            if (lp != null) {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                lp.gravity = Gravity.CENTER;
                lp.dimAmount = DEFAULT_DIM_AMOUNT;
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                window.setAttributes(lp);
            }
        }
    }
}
