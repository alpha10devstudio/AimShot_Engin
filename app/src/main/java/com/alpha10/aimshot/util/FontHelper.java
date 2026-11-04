package com.alpha10.aimshot.util;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.alpha10.aimshot.R;

/**
 * Caches and applies custom typography across the application:
 * - Brand Font: versatylo.ttf (AimShot Engine headers & identity)
 * - Regular UI Font: roboto.ttf (tactical buttons, labels, inputs, settings)
 */
public class FontHelper {

    private static Typeface brandFont;
    private static Typeface regularFont;

    public static Typeface getBrandFont(Context context) {
        if (brandFont == null && context != null) {
            try {
                brandFont = Typeface.createFromAsset(context.getAssets(), "fonts/versatylo.ttf");
            } catch (Exception e1) {
                try {
                    brandFont = ResourcesCompat.getFont(context.getApplicationContext(), R.font.versatylo);
                } catch (Exception ignored) {}
            }
        }
        return brandFont;
    }

    public static Typeface getRegularFont(Context context) {
        if (regularFont == null && context != null) {
            try {
                regularFont = Typeface.createFromAsset(context.getAssets(), "fonts/roboto.ttf");
            } catch (Exception e1) {
                try {
                    regularFont = ResourcesCompat.getFont(context.getApplicationContext(), R.font.roboto);
                } catch (Exception ignored) {}
            }
        }
        return regularFont;
    }

    public static void applyBrandFont(TextView... views) {
        if (views == null) return;
        for (TextView v : views) {
            if (v != null) {
                Typeface tf = getBrandFont(v.getContext());
                if (tf != null) {
                    v.setTypeface(tf);
                }
            }
        }
    }

    public static void applyRegularFont(View root) {
        if (root == null) return;
        Typeface tf = getRegularFont(root.getContext());
        if (tf == null) return;

        if (root instanceof TextView) {
            ((TextView) root).setTypeface(tf);
        } else if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyRegularFont(vg.getChildAt(i));
            }
        }
    }
}
