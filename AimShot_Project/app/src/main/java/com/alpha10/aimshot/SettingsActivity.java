package com.alpha10.aimshot;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.alpha10.aimshot.repository.MembershipRepository;

/**
 * Settings screen configuring preferences, diagnostics, and secure session termination.
 */
public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchTheme;
    private TextView tvNotificationsStatus;
    private TextView tvHapticStatus;
    private boolean isNotificationsOn = true;
    private boolean isHapticOn = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        setupListeners();
    }

    private void initViews() {
        View btnBack = findViewById(R.id.btn_settings_back);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        switchTheme = findViewById(R.id.switch_app_theme);
        tvNotificationsStatus = findViewById(R.id.tv_notifications_status);
        tvHapticStatus = findViewById(R.id.tv_haptic_status);
    }

    private void setupListeners() {
        // App Theme Toggle
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this, isChecked ? "Tactical Dark Mode active" : "Default Dark Mode", Toast.LENGTH_SHORT).show();
        });

        // Notifications row
        View rowNotifications = findViewById(R.id.row_setting_notifications);
        rowNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isNotificationsOn = !isNotificationsOn;
                tvNotificationsStatus.setText(isNotificationsOn ? "On" : "Off");
                Toast.makeText(SettingsActivity.this, "Notifications " + (isNotificationsOn ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
            }
        });

        // Haptic Feedback row
        View rowHaptic = findViewById(R.id.row_setting_haptic);
        rowHaptic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isHapticOn = !isHapticOn;
                tvHapticStatus.setText(isHapticOn ? "On" : "Off");
                Toast.makeText(SettingsActivity.this, "Haptic feedback " + (isHapticOn ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
            }
        });

        // Language row
        View rowLanguage = findViewById(R.id.row_setting_language);
        rowLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SettingsActivity.this, "Language: English (US)", Toast.LENGTH_SHORT).show();
            }
        });

        // Privacy & Security row
        View rowPrivacy = findViewById(R.id.row_setting_privacy);
        rowPrivacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPrivacyDialog();
            }
        });

        // Help & Support row
        View rowHelp = findViewById(R.id.row_setting_help);
        rowHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, SellerContactActivity.class));
            }
        });

        // About App row
        View rowAbout = findViewById(R.id.row_setting_about);
        rowAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAboutDialog();
            }
        });

        // Logout action button
        Button btnLogout = findViewById(R.id.btn_settings_logout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmLogout();
            }
        });
    }

    private void showPrivacyDialog() {
        new AlertDialog.Builder(this, R.style.Theme_AimShotEngine_Dialog)
                .setTitle("Privacy & Security")
                .setMessage("AimShot Engine guarantees:\n\n• No plaintext credential storage\n• No IMEI or SIM harvesting\n• App-scoped device identification\n• Encrypted HTTPS communication\n• Independent server verification")
                .setPositiveButton("Understood", null)
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this, R.style.Theme_AimShotEngine_Dialog)
                .setTitle("AimShot Engine v1.0.0")
                .setMessage("Sharper Aim • Better Game • Higher You\n\nPackage: com.alpha10.aimshot\nArchitecture: Native Android (Java + XML)\n\nAll rights reserved.")
                .setPositiveButton("Close", null)
                .show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this, R.style.Theme_AimShotEngine_Dialog)
                .setTitle("Confirm Logout")
                .setMessage(R.string.logout_confirmation)
                .setPositiveButton("Log Out", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MembershipRepository.getInstance(SettingsActivity.this).clearSession();
                        Toast.makeText(SettingsActivity.this, "Session cleared. Logged out.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
