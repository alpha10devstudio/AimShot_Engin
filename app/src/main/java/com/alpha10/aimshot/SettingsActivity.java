package com.alpha10.aimshot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.alpha10.aimshot.dialog.UpdateDialog;
import com.alpha10.aimshot.repository.MembershipRepository;
import com.alpha10.aimshot.util.DialogUiHelper;
import com.alpha10.aimshot.util.FontHelper;
import com.alpha10.aimshot.util.LocaleHelper;
import com.alpha10.aimshot.util.PackageInstallerHelper;

/**
 * Settings screen configuring theme, language localization, diagnostics, Private Vault updates,
 * and secure session termination with uniformly styled tactical popups.
 */
public class SettingsActivity extends BaseActivity {

    private static final String PREFS_NAME = "aimshot_settings_prefs";
    private static final String KEY_NOTIF = "notifications_enabled";
    private static final String KEY_HAPTIC = "haptic_enabled";
    private static final String KEY_LANG = "selected_language";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    private SwitchCompat switchTheme;
    private SwitchCompat switchNotifications;
    private SwitchCompat switchHaptic;
    private TextView tvThemeDesc;
    private TextView tvNotificationsStatus;
    private TextView tvHapticStatus;
    private TextView tvLanguageDesc;
    private TextView tvUpdateStatus;
    private SharedPreferences prefs;

    private boolean isNotificationsOn;
    private boolean isHapticOn;
    private UpdateDialog currentUpdateDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isNotificationsOn = prefs.getBoolean(KEY_NOTIF, true);
        isHapticOn = prefs.getBoolean(KEY_HAPTIC, true);

        initViews();
        setupListeners();

        if (findViewById(android.R.id.content) != null) {
            FontHelper.applyRegularFont(findViewById(android.R.id.content));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUpdateVersionRow();
        if (currentUpdateDialog != null && currentUpdateDialog.isShowing()) {
            currentUpdateDialog.onActivityResume();
        }
    }

    private void initViews() {
        View btnBack = findViewById(R.id.btn_settings_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        switchTheme = findViewById(R.id.switch_app_theme);
        switchNotifications = findViewById(R.id.switch_notifications);
        switchHaptic = findViewById(R.id.switch_haptic);
        tvThemeDesc = findViewById(R.id.tv_theme_desc);
        tvNotificationsStatus = findViewById(R.id.tv_notifications_status);
        tvHapticStatus = findViewById(R.id.tv_haptic_status);
        tvLanguageDesc = findViewById(R.id.tv_language_desc);
        tvUpdateStatus = findViewById(R.id.tv_setting_update_status);

        boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, true);
        if (switchTheme != null) {
            switchTheme.setChecked(isDarkMode);
        }
        if (tvThemeDesc != null) {
            tvThemeDesc.setText(isDarkMode ? R.string.setting_theme_desc : R.string.setting_theme_desc_light);
        }

        if (tvNotificationsStatus != null) {
            tvNotificationsStatus.setText(isNotificationsOn ? R.string.status_on : R.string.status_off);
        }
        if (switchNotifications != null) {
            switchNotifications.setChecked(isNotificationsOn);
        }

        if (tvHapticStatus != null) {
            tvHapticStatus.setText(isHapticOn ? R.string.status_on : R.string.status_off);
        }
        if (switchHaptic != null) {
            switchHaptic.setChecked(isHapticOn);
        }

        String currentCode = LocaleHelper.getPersistedData(this, "en");
        String displayLang = "English";
        if ("bn".equals(currentCode)) {
            displayLang = "বাংলা (Bengali)";
        } else if ("hi".equals(currentCode)) {
            displayLang = "हिन्दी (Hindi)";
        }
        if (tvLanguageDesc != null) {
            tvLanguageDesc.setText(displayLang);
        }

        refreshUpdateVersionRow();

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

    private void refreshUpdateVersionRow() {
        if (tvUpdateStatus == null) return;
        String versionLabel = getString(R.string.app_version);
        String vaultVer = PackageInstallerHelper.getInstalledEngineVersionName(this);
        if (vaultVer != null) {
            versionLabel = vaultVer.startsWith("v") ? vaultVer : ("v" + vaultVer);
        }
        tvUpdateStatus.setText(getString(R.string.setting_update_desc, versionLabel));
    }

    private void setupListeners() {
        View rowTheme = findViewById(R.id.row_setting_theme);
        if (rowTheme != null) {
            rowTheme.setOnClickListener(v -> {
                if (switchTheme != null) {
                    switchTheme.toggle();
                }
            });
        }
        if (switchTheme != null) {
            switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();
                if (tvThemeDesc != null) {
                    tvThemeDesc.setText(isChecked ? R.string.setting_theme_desc : R.string.setting_theme_desc_light);
                }
                triggerHapticFeedback();
                AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
                Toast.makeText(this, isChecked ? "Tactical Dark Mode active" : "Tactical Light Mode active", Toast.LENGTH_SHORT).show();
            });
        }

        View rowLanguage = findViewById(R.id.row_setting_language);
        if (rowLanguage != null) {
            rowLanguage.setOnClickListener(v -> {
                triggerHapticFeedback();
                showLanguageSelectionDialog();
            });
        }

        View rowNotifications = findViewById(R.id.row_setting_notifications);
        if (rowNotifications != null) {
            rowNotifications.setOnClickListener(v -> {
                if (switchNotifications != null) {
                    switchNotifications.toggle();
                }
            });
        }
        if (switchNotifications != null) {
            switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                isNotificationsOn = isChecked;
                prefs.edit().putBoolean(KEY_NOTIF, isNotificationsOn).apply();
                if (tvNotificationsStatus != null) {
                    tvNotificationsStatus.setText(isNotificationsOn ? R.string.status_on : R.string.status_off);
                }
                Toast.makeText(SettingsActivity.this, "Notifications " + (isNotificationsOn ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
                triggerHapticFeedback();
            });
        }

        View rowHaptic = findViewById(R.id.row_setting_haptic);
        if (rowHaptic != null) {
            rowHaptic.setOnClickListener(v -> {
                if (switchHaptic != null) {
                    switchHaptic.toggle();
                }
            });
        }
        if (switchHaptic != null) {
            switchHaptic.setOnCheckedChangeListener((buttonView, isChecked) -> {
                isHapticOn = isChecked;
                prefs.edit().putBoolean(KEY_HAPTIC, isHapticOn).apply();
                if (tvHapticStatus != null) {
                    tvHapticStatus.setText(isHapticOn ? R.string.status_on : R.string.status_off);
                }
                Toast.makeText(SettingsActivity.this, "Haptic feedback " + (isHapticOn ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
                if (isHapticOn) {
                    triggerHapticFeedback();
                }
            });
        }

        View rowDeviceInfo = findViewById(R.id.row_setting_device_info);
        if (rowDeviceInfo != null) {
            rowDeviceInfo.setOnClickListener(v -> {
                triggerHapticFeedback();
                startActivity(new Intent(SettingsActivity.this, DeviceInfoActivity.class));
            });
        }

        View rowPrivacy = findViewById(R.id.row_setting_privacy);
        if (rowPrivacy != null) {
            rowPrivacy.setOnClickListener(v -> {
                triggerHapticFeedback();
                showCustomInfoDialog(
                        getString(R.string.setting_privacy),
                        "AimShot Engine guarantees complete operational security:\n\n" +
                                "• Zero plaintext credential storage\n" +
                                "• Hidden Private Vault runtime execution\n" +
                                "• No IMEI or SIM hardware harvesting\n" +
                                "• App-scoped cryptographic device fingerprint\n" +
                                "• 256-bit TLS encrypted cloud communications\n" +
                                "• Independent tactical server verification\n\n" +
                                "Your privacy and account safety are 100% protected.",
                        R.drawable.ic_shield
                );
            });
        }

        View rowHelp = findViewById(R.id.row_setting_help);
        if (rowHelp != null) {
            rowHelp.setOnClickListener(v -> {
                triggerHapticFeedback();
                startActivity(new Intent(SettingsActivity.this, SellerContactActivity.class));
            });
        }

        View rowUpdate = findViewById(R.id.row_setting_update);
        if (rowUpdate != null) {
            rowUpdate.setOnClickListener(v -> {
                triggerHapticFeedback();
                currentUpdateDialog = new UpdateDialog(SettingsActivity.this);
                currentUpdateDialog.setOnInstallStateChangedListener(this::refreshUpdateVersionRow);
                currentUpdateDialog.show();
            });
        }

        View rowAbout = findViewById(R.id.row_setting_about);
        if (rowAbout != null) {
            rowAbout.setOnClickListener(v -> {
                triggerHapticFeedback();
                showCustomInfoDialog(
                        "AimShot Engine v1.0.0",
                        "Sharper Aim • Better Game • Higher You\n\n" +
                                "Launcher Host: " + getPackageName() + "\n" +
                                "Vault Engine: com.alpha10.aimshot (Private Container)\n" +
                                "Version: 1.0.0\n" +
                                "Architecture: Native Android Private Vault Runtime\n\n" +
                                "Designed for competitive accuracy, zero recoil simulation, and peak frame pacing.\n\n" +
                                "© 2026 AimShot Studio. All rights reserved.",
                        R.drawable.ic_aimshot_logo
                );
            });
        }

        Button btnLogout = findViewById(R.id.btn_settings_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                triggerHapticFeedback();
                confirmLogout();
            });
        }
    }

    private void triggerHapticFeedback() {
        if (!isHapticOn) {
            return;
        }
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(45);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void showLanguageSelectionDialog() {
        View dialogView = DialogUiHelper.inflateDialogView(this, R.layout.dialog_language_select);
        final AlertDialog dialog = DialogUiHelper.createTacticalAlertDialog(this, dialogView);

        String currentCode = LocaleHelper.getPersistedData(this, "en");

        View rowEn = dialogView.findViewById(R.id.row_lang_en);
        View rowBn = dialogView.findViewById(R.id.row_lang_bn);
        View rowHi = dialogView.findViewById(R.id.row_lang_hi);

        ImageView ivEn = dialogView.findViewById(R.id.iv_radio_en);
        ImageView ivBn = dialogView.findViewById(R.id.iv_radio_bn);
        ImageView ivHi = dialogView.findViewById(R.id.iv_radio_hi);

        TextView tvEn = dialogView.findViewById(R.id.tv_lang_en);
        TextView tvBn = dialogView.findViewById(R.id.tv_lang_bn);
        TextView tvHi = dialogView.findViewById(R.id.tv_lang_hi);

        Button btnClose = dialogView.findViewById(R.id.btn_lang_close);

        styleLanguageOptionRow(rowEn, ivEn, tvEn, "en".equals(currentCode));
        styleLanguageOptionRow(rowBn, ivBn, tvBn, "bn".equals(currentCode));
        styleLanguageOptionRow(rowHi, ivHi, tvHi, "hi".equals(currentCode));

        if (rowEn != null) {
            rowEn.setOnClickListener(v -> applySelectedLanguage(dialog, "en", "English"));
        }
        if (rowBn != null) {
            rowBn.setOnClickListener(v -> applySelectedLanguage(dialog, "bn", "বাংলা (Bengali)"));
        }
        if (rowHi != null) {
            rowHi.setOnClickListener(v -> applySelectedLanguage(dialog, "hi", "हिन्दी (Hindi)"));
        }
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        DialogUiHelper.showStyledDialog(dialog);
    }

    private void styleLanguageOptionRow(View row, ImageView radioIcon, TextView label, boolean isSelected) {
        if (row != null) {
            row.setBackgroundResource(isSelected ? R.drawable.bg_game_card_selected : R.drawable.bg_game_card_normal);
        }
        if (radioIcon != null) {
            radioIcon.setImageResource(isSelected ? R.drawable.ic_radio_checked : R.drawable.ic_radio_unchecked);
        }
        if (label != null) {
            label.setTextColor(ContextCompat.getColor(this,
                    isSelected ? R.color.accent_primary : R.color.text_primary));
        }
    }

    private void applySelectedLanguage(AlertDialog dialog, String langCode, String displayName) {
        prefs.edit().putString(KEY_LANG, displayName).apply();
        LocaleHelper.setLocale(SettingsActivity.this, langCode);

        if (tvLanguageDesc != null) {
            tvLanguageDesc.setText(displayName);
        }
        triggerHapticFeedback();
        Toast.makeText(SettingsActivity.this,
                getString(R.string.setting_language) + ": " + displayName,
                Toast.LENGTH_SHORT).show();
        dialog.dismiss();

        recreate();
    }

    private void showCustomInfoDialog(String title, String message, int iconRes) {
        View dialogView = DialogUiHelper.inflateDialogView(this, R.layout.dialog_custom_info);
        final AlertDialog dialog = DialogUiHelper.createTacticalAlertDialog(this, dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_info_title);
        TextView tvMsg = dialogView.findViewById(R.id.tv_dialog_info_message);
        ImageView ivIcon = dialogView.findViewById(R.id.iv_dialog_info_icon);
        Button btnOk = dialogView.findViewById(R.id.btn_dialog_info_positive);

        if (tvTitle != null) tvTitle.setText(title);
        if (tvMsg != null) tvMsg.setText(message);
        if (ivIcon != null && iconRes != 0) {
            ivIcon.setImageResource(iconRes);
            ivIcon.clearColorFilter();
        }
        if (btnOk != null) {
            btnOk.setText(R.string.btn_understood);
            btnOk.setOnClickListener(v -> dialog.dismiss());
        }

        DialogUiHelper.showStyledDialog(dialog);
    }

    private void confirmLogout() {
        View dialogView = DialogUiHelper.inflateDialogView(this, R.layout.dialog_tactical_confirm);
        final AlertDialog dialog = DialogUiHelper.createTacticalAlertDialog(this, dialogView);

        ImageView ivIcon = dialogView.findViewById(R.id.iv_confirm_icon);
        TextView tvTitle = dialogView.findViewById(R.id.tv_confirm_title);
        TextView tvMsg = dialogView.findViewById(R.id.tv_confirm_message);
        Button btnCancel = dialogView.findViewById(R.id.btn_confirm_negative);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm_positive);

        if (ivIcon != null) {
            ivIcon.setImageResource(R.drawable.ic_shield);
            ivIcon.clearColorFilter();
        }
        if (tvTitle != null) {
            tvTitle.setText(R.string.btn_logout);
        }
        if (tvMsg != null) {
            tvMsg.setText(R.string.logout_confirmation);
        }
        if (btnCancel != null) {
            btnCancel.setText(R.string.action_close);
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        if (btnConfirm != null) {
            btnConfirm.setText(R.string.btn_logout);
            btnConfirm.setBackgroundResource(R.drawable.bg_button_logout);
            btnConfirm.setTextColor(ContextCompat.getColor(this, R.color.color_error));
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();
                MembershipRepository.getInstance(SettingsActivity.this).clearSession();
                Toast.makeText(SettingsActivity.this, "Session cleared. Logged out.", Toast.LENGTH_SHORT).show();
                finish();
            });
        }

        DialogUiHelper.showStyledDialog(dialog);
    }
}
