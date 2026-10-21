package com.alpha10.aimshot;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.alpha10.aimshot.repository.MembershipRepository;

/**
 * Settings screen configuring preferences, diagnostics, and secure session termination.
 * Features dedicated icons, solid background custom dialogs, physical vibration, and persistent bottom nav.
 */
public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "aimshot_settings_prefs";
    private static final String KEY_NOTIF = "notifications_enabled";
    private static final String KEY_HAPTIC = "haptic_enabled";
    private static final String KEY_LANG = "selected_language";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    private SwitchCompat switchTheme;
    private TextView tvNotificationsStatus;
    private TextView tvHapticStatus;
    private TextView tvLanguageDesc;
    private SharedPreferences prefs;

    private boolean isNotificationsOn;
    private boolean isHapticOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isNotificationsOn = prefs.getBoolean(KEY_NOTIF, true);
        isHapticOn = prefs.getBoolean(KEY_HAPTIC, true);

        initViews();
        setupListeners();
    }

    private void initViews() {
        View btnBack = findViewById(R.id.btn_settings_back);
        btnBack.setOnClickListener(v -> finish());

        switchTheme = findViewById(R.id.switch_app_theme);
        tvNotificationsStatus = findViewById(R.id.tv_notifications_status);
        tvHapticStatus = findViewById(R.id.tv_haptic_status);
        tvLanguageDesc = findViewById(R.id.tv_language_desc);

        // Populate saved states
        if (tvNotificationsStatus != null) {
            tvNotificationsStatus.setText(isNotificationsOn ? "On" : "Off");
        }
        if (tvHapticStatus != null) {
            tvHapticStatus.setText(isHapticOn ? "On" : "Off");
        }
        if (tvLanguageDesc != null) {
            tvLanguageDesc.setText(prefs.getString(KEY_LANG, "English"));
        }
        if (switchTheme != null) {
            switchTheme.setChecked(prefs.getBoolean(KEY_DARK_MODE, true));
        }

        // Bottom Navigation Bar
        View navHome = findViewById(R.id.settings_nav_home);
        if (navHome != null) {
            navHome.setOnClickListener(v -> finish());
        }

        View navPremium = findViewById(R.id.settings_nav_premium);
        if (navPremium != null) {
            navPremium.setOnClickListener(v -> {
                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("TARGET_TAB", "PREMIUM");
                startActivity(intent);
                finish();
            });
        }
    }

    private void setupListeners() {
        // App Theme Toggle (Dark / Light mode)
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            Toast.makeText(this, isChecked ? "Tactical Dark Mode active" : "Default Light Mode active", Toast.LENGTH_SHORT).show();
        });

        // Notifications row
        View rowNotifications = findViewById(R.id.row_setting_notifications);
        rowNotifications.setOnClickListener(v -> {
            isNotificationsOn = !isNotificationsOn;
            prefs.edit().putBoolean(KEY_NOTIF, isNotificationsOn).apply();
            tvNotificationsStatus.setText(isNotificationsOn ? "On" : "Off");
            Toast.makeText(SettingsActivity.this, "Notifications " + (isNotificationsOn ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
            triggerHapticFeedback();
        });

        // Haptic Feedback row
        View rowHaptic = findViewById(R.id.row_setting_haptic);
        rowHaptic.setOnClickListener(v -> {
            isHapticOn = !isHapticOn;
            prefs.edit().putBoolean(KEY_HAPTIC, isHapticOn).apply();
            tvHapticStatus.setText(isHapticOn ? "On" : "Off");
            Toast.makeText(SettingsActivity.this, "Haptic feedback " + (isHapticOn ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
            if (isHapticOn) {
                triggerHapticFeedback();
            }
        });

        // Language row
        View rowLanguage = findViewById(R.id.row_setting_language);
        rowLanguage.setOnClickListener(v -> showLanguageSelectionDialog());

        // Privacy & Security row (Solid Popup Fix)
        View rowPrivacy = findViewById(R.id.row_setting_privacy);
        rowPrivacy.setOnClickListener(v -> showCustomInfoDialog(
                "Privacy & Security",
                "AimShot Engine guarantees complete operational security:\n\n" +
                        "• Zero plaintext credential storage\n" +
                        "• No IMEI or SIM hardware harvesting\n" +
                        "• App-scoped cryptographic device fingerprint\n" +
                        "• 256-bit TLS encrypted cloud communications\n" +
                        "• Independent tactical server verification\n\n" +
                        "Your privacy and account safety are 100% protected.",
                R.drawable.ic_shield
        ));

        // Help & Support row
        View rowHelp = findViewById(R.id.row_setting_help);
        rowHelp.setOnClickListener(v -> startActivity(new Intent(SettingsActivity.this, SellerContactActivity.class)));

        // About App row (Solid Popup Fix)
        View rowAbout = findViewById(R.id.row_setting_about);
        rowAbout.setOnClickListener(v -> showCustomInfoDialog(
                "AimShot Engine v1.0.0",
                "Sharper Aim • Better Game • Higher You\n\n" +
                        "Package: com.alpha10.aimshot\n" +
                        "Engine Version: 1.0.0 (Build 16)\n" +
                        "Architecture: Native Android (Java + Modern XML)\n\n" +
                        "Designed for competitive accuracy, zero recoil simulation, and peak frame pacing.\n\n" +
                        "© 2026 AimShot Studio. All rights reserved.",
                R.drawable.ic_aimshot_logo
        ));

        // Logout action button
        Button btnLogout = findViewById(R.id.btn_settings_logout);
        btnLogout.setOnClickListener(v -> confirmLogout());
    }

    private void triggerHapticFeedback() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(50);
                }
            }
        } catch (Exception ignored) {}
    }

    private void showLanguageSelectionDialog() {
        final String[] languages = {"English", "বাংলা (Bengali)", "हिन्दी (Hindi)", "Español (Spanish)", "العربية (Arabic)"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_AimShotEngine_Dialog);
        builder.setTitle("Select Language");
        builder.setItems(languages, (dialog, which) -> {
            String selected = languages[which];
            prefs.edit().putString(KEY_LANG, selected).apply();
            if (tvLanguageDesc != null) {
                tvLanguageDesc.setText(selected);
            }
            Toast.makeText(SettingsActivity.this, "Language set to " + selected, Toast.LENGTH_SHORT).show();
            triggerHapticFeedback();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Renders high-fidelity solid card dialog with #111B2A background, preventing overlapping text.
     */
    private void showCustomInfoDialog(String title, String message, int iconRes) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_custom_info, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_info_title);
        TextView tvMsg = dialogView.findViewById(R.id.tv_dialog_info_message);
        ImageView ivIcon = dialogView.findViewById(R.id.iv_dialog_info_icon);
        Button btnOk = dialogView.findViewById(R.id.btn_dialog_info_positive);

        if (tvTitle != null) tvTitle.setText(title);
        if (tvMsg != null) tvMsg.setText(message);
        if (ivIcon != null && iconRes != 0) ivIcon.setImageResource(iconRes);
        if (btnOk != null) {
            btnOk.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void confirmLogout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_AimShotEngine_Dialog);
        builder.setTitle("Confirm Logout");
        builder.setMessage(R.string.logout_confirmation);
        builder.setPositiveButton("Log Out", (dialog, which) -> {
            MembershipRepository.getInstance(SettingsActivity.this).clearSession();
            Toast.makeText(SettingsActivity.this, "Session cleared. Logged out.", Toast.LENGTH_SHORT).show();
            finish();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
