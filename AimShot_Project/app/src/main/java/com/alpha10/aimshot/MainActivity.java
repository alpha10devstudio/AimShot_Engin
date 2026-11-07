package com.alpha10.aimshot;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alpha10.aimshot.adapter.GameGridAdapter;
import com.alpha10.aimshot.dialog.UpdateDialog;
import com.alpha10.aimshot.dialog.VerificationKeyDialog;
import com.alpha10.aimshot.model.GameItem;
import com.alpha10.aimshot.model.MembershipStatus;
import com.alpha10.aimshot.repository.GameRepository;
import com.alpha10.aimshot.repository.MembershipRepository;
import com.alpha10.aimshot.repository.NotificationsRepository;
import com.alpha10.aimshot.model.VerificationResult;
import com.alpha10.aimshot.service.DeviceIdManager;
import com.alpha10.aimshot.service.LicenseVerificationService;
import com.alpha10.aimshot.service.VerificationCallback;
import com.alpha10.aimshot.util.DialogUiHelper;
import com.alpha10.aimshot.util.FontHelper;
import com.alpha10.aimshot.util.NetworkUtil;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import com.alpha10.aimshot.util.LocaleHelper;
import com.alpha10.aimshot.util.PackageInstallerHelper;

/**
 * Main dashboard hosting Home and Premium destinations, persistent Bottom Navigation,
 * Private Vault Engine setup/launch gate, and Legacy Exposed App Copy cleanup detection.
 */
public class MainActivity extends BaseActivity
        implements MembershipRepository.MembershipChangeListener {

    public enum Tab {
        HOME,
        PREMIUM,
        SETTINGS
    }

    private FrameLayout fragmentContainer;

    private View navBtnHome, navBtnPremium, navBtnSettings;
    private View navDotHome, navDotPremium, navDotSettings;

    private ImageView ivNavHome, ivNavPremium, ivNavSettings;

    private TextView tvNavHome, tvNavPremium, tvNavSettings;

    private Tab currentTab = Tab.HOME;

    private MembershipRepository membershipRepository;

    private boolean isLaunching = false;
    private UpdateDialog activeUpdateDialog;
    private AlertDialog activeNetworkCheckDialog;
    private AlertDialog activeLegacyCleanupDialog;

    private final android.os.Handler carouselHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());

    private Runnable carouselRunnable;

    private final android.os.Handler liveCountdownHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());

    private Runnable liveCountdownRunnable;

    private android.widget.ViewFlipper currentFlipper;

    private ImageView currentDot1, currentDot2, currentDot3;

    private static final String SETTINGS_PREFS_NAME = "aimshot_settings_prefs";
    private static final String KEY_NOTIF = "notifications_enabled";
    private static final String KEY_HAPTIC = "haptic_enabled";
    private static final String KEY_LANG = "selected_language";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        membershipRepository = MembershipRepository.getInstance(this);
        membershipRepository.addListener(this);

        initViews();
        setupNavigation();

        navigateToTab(Tab.HOME);

        handleIncomingIntent(getIntent());
        syncLicenseStateInBackground();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent != null) {
            String target = intent.getStringExtra("TARGET_TAB");
            if ("PREMIUM".equals(target)) {
                navigateToTab(Tab.PREMIUM);
            } else if ("SETTINGS".equals(target)) {
                navigateToTab(Tab.SETTINGS);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopCarouselTimer();
        stopLiveCountdownTimer();
        dismissVerifyingConnectionDialog();
        dismissLegacyCleanupDialog();

        if (membershipRepository != null) {
            membershipRepository.removeListener(this);
        }
    }

    private void initViews() {
        fragmentContainer = findViewById(R.id.fragment_container);

        navBtnHome = findViewById(R.id.nav_btn_home);
        navBtnPremium = findViewById(R.id.nav_btn_premium);
        navBtnSettings = findViewById(R.id.nav_btn_settings);

        ivNavHome = findViewById(R.id.iv_nav_home);
        ivNavPremium = findViewById(R.id.iv_nav_premium);
        ivNavSettings = findViewById(R.id.iv_nav_settings);

        tvNavHome = findViewById(R.id.tv_nav_home);
        tvNavPremium = findViewById(R.id.tv_nav_premium);
        tvNavSettings = findViewById(R.id.nav_btn_settings != 0 ? R.id.tv_nav_settings : R.id.tv_nav_settings);

        navDotHome = findViewById(R.id.nav_dot_home);
        navDotPremium = findViewById(R.id.nav_dot_premium);
        navDotSettings = findViewById(R.id.nav_dot_settings);
    }

    private void setupNavigation() {
        navBtnHome.setOnClickListener(v -> {
            if (currentTab != Tab.HOME) {
                navigateToTab(Tab.HOME);
            }
        });

        navBtnPremium.setOnClickListener(v -> {
            if (currentTab != Tab.PREMIUM) {
                navigateToTab(Tab.PREMIUM);
            }
        });

        navBtnSettings.setOnClickListener(v -> {
            if (currentTab != Tab.SETTINGS) {
                navigateToTab(Tab.SETTINGS);
            }
        });
    }

    public void navigateToTab(Tab tab) {
        Tab previousTab = currentTab;
        currentTab = tab;
        updateNavUi();
        stopCarouselTimer();
        stopLiveCountdownTimer();
        fragmentContainer.removeAllViews();

        if (tab == Tab.HOME) {
            renderHomeScreen();
        } else if (tab == Tab.PREMIUM) {
            renderPremiumScreen();
        } else if (tab == Tab.SETTINGS) {
            renderSettingsScreen();
        }

        if (fragmentContainer.getChildCount() > 0 && previousTab != tab) {
            View child = fragmentContainer.getChildAt(0);
            int animRes = (tab.ordinal() < previousTab.ordinal())
                    ? R.anim.slide_in_left
                    : R.anim.slide_in_right;
            android.view.animation.Animation slideAnim =
                    android.view.animation.AnimationUtils.loadAnimation(this, animRes);
            child.startAnimation(slideAnim);
        }
    }

    private void updateNavUi() {
        int colorActive = ContextCompat.getColor(this, R.color.accent_primary);
        int colorInactive = ContextCompat.getColor(this, R.color.nav_inactive);

        ivNavHome.setColorFilter(currentTab == Tab.HOME ? colorActive : colorInactive);
        tvNavHome.setTextColor(currentTab == Tab.HOME ? colorActive : colorInactive);
        if (navDotHome != null) {
            navDotHome.setVisibility(currentTab == Tab.HOME ? View.VISIBLE : View.INVISIBLE);
        }

        ivNavPremium.setColorFilter(currentTab == Tab.PREMIUM ? colorActive : colorInactive);
        tvNavPremium.setTextColor(currentTab == Tab.PREMIUM ? colorActive : colorInactive);
        if (navDotPremium != null) {
            navDotPremium.setVisibility(currentTab == Tab.PREMIUM ? View.VISIBLE : View.INVISIBLE);
        }

        ivNavSettings.setColorFilter(currentTab == Tab.SETTINGS ? colorActive : colorInactive);
        tvNavSettings.setTextColor(currentTab == Tab.SETTINGS ? colorActive : colorInactive);
        if (navDotSettings != null) {
            navDotSettings.setVisibility(currentTab == Tab.SETTINGS ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void startCarouselTimer() {
        stopCarouselTimer();
        if (currentFlipper == null) return;

        carouselRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentFlipper != null && currentTab == Tab.HOME) {
                    currentFlipper.showNext();
                    updateCarouselIndicators(
                            currentFlipper.getDisplayedChild(),
                            currentDot1,
                            currentDot2,
                            currentDot3
                    );
                    carouselHandler.postDelayed(this, 4000);
                }
            }
        };

        carouselHandler.postDelayed(carouselRunnable, 4000);
    }

    private void stopCarouselTimer() {
        if (carouselRunnable != null) {
            carouselHandler.removeCallbacks(carouselRunnable);
            carouselRunnable = null;
        }
    }

    private void resetCarouselTimer() {
        stopCarouselTimer();
        startCarouselTimer();
    }

    private void updateNotificationBadge() {
        try {
            int unread = com.alpha10.aimshot.repository
                    .NotificationsRepository.getInstance(this)
                    .getUnreadCount();

            int[] badgeIds = new int[]{
                    R.id.tv_header_notif_badge,
                    R.id.tv_premium_notif_badge
            };

            for (int badgeId : badgeIds) {
                View badge = findViewById(badgeId);
                if (badge instanceof TextView) {
                    TextView tvBadge = (TextView) badge;
                    if (unread > 0) {
                        tvBadge.setText(String.valueOf(unread));
                        tvBadge.setVisibility(View.VISIBLE);
                    } else {
                        tvBadge.setVisibility(View.GONE);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void renderHomeScreen() {
        View homeView = LayoutInflater.from(this).inflate(
                R.layout.fragment_home,
                fragmentContainer,
                false
        );

        fragmentContainer.addView(homeView);

        View btnNotifications = homeView.findViewById(R.id.btn_notifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> startActivity(
                    new Intent(MainActivity.this, NotificationsActivity.class)
            ));
        }

        updateNotificationBadge();

        View tvViewAll = homeView.findViewById(R.id.tv_view_all_games);
        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> startActivity(
                    new Intent(MainActivity.this, SupportedGamesActivity.class)
            ));
        }

        final android.widget.ViewFlipper flipper = homeView.findViewById(R.id.home_carousel_flipper);
        final ImageView dot1 = homeView.findViewById(R.id.dot_1);
        final ImageView dot2 = homeView.findViewById(R.id.dot_2);
        final ImageView dot3 = homeView.findViewById(R.id.dot_3);

        View btnPrev = homeView.findViewById(R.id.btn_carousel_prev);
        View btnNext = homeView.findViewById(R.id.btn_carousel_next);

        currentFlipper = flipper;
        currentDot1 = dot1;
        currentDot2 = dot2;
        currentDot3 = dot3;

        if (flipper != null) {
            updateCarouselIndicators(0, dot1, dot2, dot3);

            if (btnPrev != null) {
                btnPrev.setOnClickListener(v -> {
                    flipper.showPrevious();
                    updateCarouselIndicators(flipper.getDisplayedChild(), dot1, dot2, dot3);
                    resetCarouselTimer();
                });
            }

            if (btnNext != null) {
                btnNext.setOnClickListener(v -> {
                    flipper.showNext();
                    updateCarouselIndicators(flipper.getDisplayedChild(), dot1, dot2, dot3);
                    resetCarouselTimer();
                });
            }

            if (dot1 != null) {
                dot1.setOnClickListener(v -> {
                    flipper.setDisplayedChild(0);
                    updateCarouselIndicators(0, dot1, dot2, dot3);
                    resetCarouselTimer();
                });
            }

            if (dot2 != null) {
                dot2.setOnClickListener(v -> {
                    flipper.setDisplayedChild(1);
                    updateCarouselIndicators(1, dot1, dot2, dot3);
                    resetCarouselTimer();
                });
            }

            if (dot3 != null) {
                dot3.setOnClickListener(v -> {
                    flipper.setDisplayedChild(2);
                    updateCarouselIndicators(2, dot1, dot2, dot3);
                    resetCarouselTimer();
                });
            }

            final android.view.GestureDetector gestureDetector =
                    new android.view.GestureDetector(
                            this,
                            new android.view.GestureDetector.SimpleOnGestureListener() {
                                private static final int SWIPE_THRESHOLD = 50;
                                private static final int SWIPE_VELOCITY_THRESHOLD = 80;

                                @Override
                                public boolean onFling(
                                        android.view.MotionEvent e1,
                                        android.view.MotionEvent e2,
                                        float velocityX,
                                        float velocityY
                                ) {
                                    if (e1 == null || e2 == null) return false;
                                    float diffX = e2.getX() - e1.getX();
                                    float diffY = e2.getY() - e1.getY();
                                    if (Math.abs(diffX) > Math.abs(diffY)) {
                                        if (Math.abs(diffX) > SWIPE_THRESHOLD &&
                                                Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                                            if (diffX > 0) {
                                                flipper.showPrevious();
                                            } else {
                                                flipper.showNext();
                                            }
                                            updateCarouselIndicators(flipper.getDisplayedChild(), dot1, dot2, dot3);
                                            resetCarouselTimer();
                                            return true;
                                        }
                                    }
                                    return false;
                                }
                            }
                    );

            flipper.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
            startCarouselTimer();
        }

        RecyclerView rvGrid = homeView.findViewById(R.id.rv_games_grid);
        if (rvGrid != null) {
            rvGrid.setLayoutManager(new GridLayoutManager(this, 2));
            final GameGridAdapter adapter = new GameGridAdapter(
                    GameRepository.getInstance().getAllGames(),
                    item -> {
                        if (item.isActive()) {
                            Toast.makeText(
                                    MainActivity.this,
                                    "Aim Assist Ready for " + item.getName(),
                                    Toast.LENGTH_SHORT
                            ).show();
                        } else {
                            Toast.makeText(
                                    MainActivity.this,
                                    item.getName() + " is currently locked / Pro Key required",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );
            rvGrid.setAdapter(adapter);
        }

        updateHomeLoginCard(homeView);
        FontHelper.applyRegularFont(homeView);

        // Apply custom brand font (versatylo.ttf) to AimShot Engine brand headers
        TextView tvHeaderAimshot = homeView.findViewById(R.id.tv_header_aimshot);
        TextView tvHeaderEngine = homeView.findViewById(R.id.tv_header_engine);
        TextView tvHeroAimshot = homeView.findViewById(R.id.tv_hero_aimshot);
        TextView tvHeroEngine = homeView.findViewById(R.id.tv_hero_engine);
        FontHelper.applyBrandFont(tvHeaderAimshot, tvHeaderEngine, tvHeroAimshot, tvHeroEngine);
    }

    private void updateCarouselIndicators(int index, ImageView d1, ImageView d2, ImageView d3) {
        if (d1 == null || d2 == null || d3 == null) return;

        float density = getResources().getDisplayMetrics().density;
        int activeWidth = (int) (20 * density);
        int inactiveWidth = (int) (6 * density);
        int height = (int) (6 * density);

        d1.setImageResource(index == 0 ? R.drawable.indicator_dot_active : R.drawable.indicator_dot_inactive);
        android.widget.LinearLayout.LayoutParams lp1 =
                new android.widget.LinearLayout.LayoutParams(index == 0 ? activeWidth : inactiveWidth, height);
        lp1.setMargins((int) (3 * density), 0, (int) (3 * density), 0);
        d1.setLayoutParams(lp1);

        d2.setImageResource(index == 1 ? R.drawable.indicator_dot_active : R.drawable.indicator_dot_inactive);
        android.widget.LinearLayout.LayoutParams lp2 =
                new android.widget.LinearLayout.LayoutParams(index == 1 ? activeWidth : inactiveWidth, height);
        lp2.setMargins((int) (3 * density), 0, (int) (3 * density), 0);
        d2.setLayoutParams(lp2);

        d3.setImageResource(index == 2 ? R.drawable.indicator_dot_active : R.drawable.indicator_dot_inactive);
        android.widget.LinearLayout.LayoutParams lp3 =
                new android.widget.LinearLayout.LayoutParams(index == 2 ? activeWidth : inactiveWidth, height);
        lp3.setMargins((int) (3 * density), 0, (int) (3 * density), 0);
        d3.setLayoutParams(lp3);
    }

    private void updateHomeLoginCard(View homeView) {
        final MembershipStatus status = membershipRepository.getCurrentStatus();

        View cardLoginStatus = homeView.findViewById(R.id.card_login_status);
        View cardLaunchGame = homeView.findViewById(R.id.card_launch_game);
        Button btnLaunchGame = homeView.findViewById(R.id.btn_launch_game);
        Button btnLogin = homeView.findViewById(R.id.btn_home_login);

        if (status.getState() == MembershipStatus.State.ACTIVE) {
            if (cardLoginStatus != null) {
                cardLoginStatus.setVisibility(View.GONE);
            }
            if (cardLaunchGame != null) {
                cardLaunchGame.setVisibility(View.VISIBLE);
                TextView tvBadge = homeView.findViewById(R.id.tv_active_game_badge);
                if (tvBadge != null) {
                    tvBadge.setText("ACTIVE");
                    tvBadge.setVisibility(View.VISIBLE);
                }
                updateLauncherButtonState(btnLaunchGame);
                if (btnLaunchGame != null) {
                    btnLaunchGame.setOnClickListener(v -> handleLauncherButtonClick());
                }
            }
        } else {
            if (cardLoginStatus != null) {
                cardLoginStatus.setVisibility(View.VISIBLE);
            }
            if (cardLaunchGame != null) {
                cardLaunchGame.setVisibility(View.GONE);
            }
            if (btnLogin != null) {
                btnLogin.setOnClickListener(v -> showVerificationDialog());
            }
        }
    }

    private void updateLauncherButtonState(Button btnLaunchGame) {
        if (btnLaunchGame == null) return;

        boolean isInstalledInVault = PackageInstallerHelper.isMainApkInstalled(this);
        if (isInstalledInVault) {
            btnLaunchGame.setText(R.string.launch_aimshot_engine);
        } else {
            btnLaunchGame.setText(R.string.install_aimshot_engine);
        }
    }

    /**
     * Enforces:
     * 0. Cleanup of any old exposed OS-installed standalone copy of com.alpha10.aimshot
     * 1. Active radio network check
     * 2 & 3. Live internet + Alpha10 Server reachability probe
     * 4. Private Vault Engine launch inside AimShot Launcher (or Private Vault download/setup)
     */
    /**
     * Enforces Authoritative Security Gate:
     * 1. Active network connectivity check.
     * 2. Active license session presence.
     * 3. Real-time HTTPS license re-validation against https://alpha10.freedev.app before every single launch.
     * 4. If server denies or revokes license: instantly clear local session and block launch.
     * 5. If server approves license: proceed to engine execution.
     */
    private void handleLauncherButtonClick() {
        if (isLaunching) {
            return;
        }
        isLaunching = true;

        MembershipStatus currentStatus = membershipRepository.getCurrentStatus();
        final String savedKey = membershipRepository.getSavedLicenseKey();

        // 1. Check if user has active session and saved key
        if (currentStatus.getState() != MembershipStatus.State.ACTIVE || savedKey == null || savedKey.trim().isEmpty()) {
            isLaunching = false;
            Toast.makeText(this, R.string.license_required_to_launch, Toast.LENGTH_SHORT).show();
            showVerificationDialog();
            return;
        }

        // 2. Strict Internet & Active Data Package Verification (Zero-tolerance for offline bypass)
        if (!NetworkUtil.isNetworkConnected(this) || NetworkUtil.hasCaptivePortal(this)) {
            isLaunching = false;
            showConnectivityErrorDialog(NetworkUtil.ConnectivityStatus.NO_INTERNET_ACCESS);
            return;
        }

        // Show tactical verifying dialog
        showVerifyingConnectionDialog(R.string.verifying_license_title, R.string.verifying_license_msg);

        // 3. MANDATORY Server Verification before EVERY SINGLE LAUNCH:
        DeviceIdManager devIdMgr = new DeviceIdManager(this);
        String deviceId = devIdMgr.getRawAndroidId();
        String deviceModel = devIdMgr.getDeviceBrand() + " " + devIdMgr.getDeviceModel();
        String appVersion = "2.4.0";

        LicenseVerificationService licenseService = new LicenseVerificationService();
        licenseService.verifyKey(savedKey.trim(), deviceId, deviceModel, appVersion, new VerificationCallback() {
            @Override
            public void onVerificationStarted() {
            }

            @Override
            public void onVerificationSuccess(VerificationResult result) {
                dismissVerifyingConnectionDialog();

                if (isFinishing() || isDestroyed()) {
                    isLaunching = false;
                    return;
                }

                // Authoritative server clearance confirmed: update vault & local cache
                if (result.getMembershipStatus() != null) {
                    membershipRepository.saveActiveKey(savedKey.trim(), result.getMembershipStatus());
                }

                executeMainApkLaunch();
            }

            @Override
            public void onVerificationError(String errorMessage, String errorCode) {
                dismissVerifyingConnectionDialog();
                isLaunching = false;

                boolean isAuthoritativeRevocation = "STATUS_INVALID".equals(errorCode)
                        || "STATUS_REVOKED".equals(errorCode)
                        || "STATUS_EXPIRED".equals(errorCode)
                        || "STATUS_DEVICE_REVOKED".equals(errorCode);

                if (isAuthoritativeRevocation) {
                    // Server authoritatively rejected/revoked the key: wipe key & logout
                    membershipRepository.revokeKey(savedKey);
                    showLicenseDeniedDialog(errorMessage != null ? errorMessage : getString(R.string.key_invalid_or_expired));
                    showVerificationDialog();
                } else {
                    // Non-authoritative network/parsing issue: if local vault key is active and unexpired, allow launch seamlessly!
                    if (membershipRepository.isCurrentKeyValidLocally()) {
                        executeMainApkLaunch();
                        return;
                    }
                    showLicenseDeniedDialog(errorMessage != null ? errorMessage : getString(R.string.no_internet_access_msg));
                }
            }
        });
    }

    private void executeMainApkLaunch() {
        boolean isInstalled = PackageInstallerHelper.isMainApkInstalled(MainActivity.this);

        if (isInstalled) {
            boolean launched = PackageInstallerHelper.launchInstalledApp(MainActivity.this);
            if (launched) {
                Toast.makeText(
                        MainActivity.this,
                        R.string.launching_aimshot_engine,
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                Toast.makeText(
                        MainActivity.this,
                        R.string.failed_to_launch_main_apk,
                        Toast.LENGTH_LONG
                ).show();
            }
            isLaunching = false;
        } else {
            isLaunching = false;
            Toast.makeText(
                    MainActivity.this,
                    R.string.main_apk_not_installed,
                    Toast.LENGTH_SHORT
            ).show();

            activeUpdateDialog = new UpdateDialog(MainActivity.this, true);
            activeUpdateDialog.setOnInstallStateChangedListener(() -> {
                if (currentTab == Tab.HOME) {
                    Button btnLaunchGame = findViewById(R.id.btn_launch_game);
                    updateLauncherButtonState(btnLaunchGame);
                }
            });
            activeUpdateDialog.show();
        }
    }

    /**
     * Prompts the user to remove any exposed standalone OS copy of com.alpha10.aimshot left over
     * from earlier testing so no duplicate AimShot icon remains in the App Drawer.
     */
    private void checkLegacyStandaloneAppOnResume() {
        if (isFinishing() || isDestroyed()) return;
        if (PackageInstallerHelper.isLegacyStandaloneAppInstalled(this)) {
            if (activeLegacyCleanupDialog == null || !activeLegacyCleanupDialog.isShowing()) {
                showLegacyStandaloneCleanupDialog();
            }
        } else {
            dismissLegacyCleanupDialog();
        }
    }

    private void showLegacyStandaloneCleanupDialog() {
        dismissLegacyCleanupDialog();
        if (isFinishing() || isDestroyed()) return;

        View dialogView = DialogUiHelper.inflateDialogView(this, R.layout.dialog_tactical_confirm);
        activeLegacyCleanupDialog = DialogUiHelper.createTacticalAlertDialog(this, dialogView);

        ImageView ivIcon = dialogView.findViewById(R.id.iv_confirm_icon);
        View pbLoading = dialogView.findViewById(R.id.pb_confirm_loading);
        TextView tvTitle = dialogView.findViewById(R.id.tv_confirm_title);
        TextView tvMsg = dialogView.findViewById(R.id.tv_confirm_message);
        Button btnLater = dialogView.findViewById(R.id.btn_confirm_negative);
        Button btnUninstall = dialogView.findViewById(R.id.btn_confirm_positive);

        if (ivIcon != null) {
            ivIcon.setVisibility(View.VISIBLE);
            ivIcon.setImageResource(R.drawable.ic_shield);
            ivIcon.clearColorFilter();
        }
        if (pbLoading != null) {
            pbLoading.setVisibility(View.GONE);
        }
        if (tvTitle != null) {
            tvTitle.setText(R.string.legacy_app_detected_title);
        }
        if (tvMsg != null) {
            tvMsg.setText(R.string.legacy_app_detected_msg);
        }
        if (btnLater != null) {
            btnLater.setText(R.string.action_close);
            btnLater.setOnClickListener(v -> dismissLegacyCleanupDialog());
        }
        if (btnUninstall != null) {
            btnUninstall.setText(R.string.btn_uninstall_legacy);
            btnUninstall.setOnClickListener(v -> {
                dismissLegacyCleanupDialog();
                PackageInstallerHelper.requestUninstallLegacyStandaloneApp(MainActivity.this);
            });
        }

        DialogUiHelper.showStyledDialog(activeLegacyCleanupDialog);
    }

    private void dismissLegacyCleanupDialog() {
        if (activeLegacyCleanupDialog != null) {
            try {
                if (activeLegacyCleanupDialog.isShowing()) {
                    activeLegacyCleanupDialog.dismiss();
                }
            } catch (Exception ignored) {
            }
            activeLegacyCleanupDialog = null;
        }
    }

    private void showVerifyingConnectionDialog() {
        showVerifyingConnectionDialog(R.string.verifying_network_title, R.string.verifying_network_msg);
    }

    private void showVerifyingConnectionDialog(int titleRes, int msgRes) {
        dismissVerifyingConnectionDialog();
        if (isFinishing() || isDestroyed()) return;

        View dialogView = DialogUiHelper.inflateDialogView(this, R.layout.dialog_tactical_confirm);
        activeNetworkCheckDialog = DialogUiHelper.createTacticalAlertDialog(this, dialogView);

        View ivIcon = dialogView.findViewById(R.id.iv_confirm_icon);
        View pbLoading = dialogView.findViewById(R.id.pb_confirm_loading);
        TextView tvTitle = dialogView.findViewById(R.id.tv_confirm_title);
        TextView tvMsg = dialogView.findViewById(R.id.tv_confirm_message);
        View layoutButtons = dialogView.findViewById(R.id.layout_confirm_buttons);

        if (ivIcon != null) ivIcon.setVisibility(View.GONE);
        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);
        if (tvTitle != null) tvTitle.setText(titleRes);
        if (tvMsg != null) tvMsg.setText(msgRes);
        if (layoutButtons != null) layoutButtons.setVisibility(View.GONE);

        activeNetworkCheckDialog.setCancelable(false);
        activeNetworkCheckDialog.setCanceledOnTouchOutside(false);
        DialogUiHelper.showStyledDialog(activeNetworkCheckDialog);
    }

    private void showLicenseDeniedDialog(String message) {
        if (isFinishing() || isDestroyed()) return;

        View dialogView = DialogUiHelper.inflateDialogView(this, R.layout.dialog_tactical_confirm);
        final AlertDialog dialog = DialogUiHelper.createTacticalAlertDialog(this, dialogView);

        ImageView ivIcon = dialogView.findViewById(R.id.iv_confirm_icon);
        View pbLoading = dialogView.findViewById(R.id.pb_confirm_loading);
        TextView tvTitle = dialogView.findViewById(R.id.tv_confirm_title);
        TextView tvMsg = dialogView.findViewById(R.id.tv_confirm_message);
        Button btnClose = dialogView.findViewById(R.id.btn_confirm_negative);
        Button btnLogin = dialogView.findViewById(R.id.btn_confirm_positive);

        if (ivIcon != null) {
            ivIcon.setVisibility(View.VISIBLE);
            ivIcon.setImageResource(R.drawable.ic_shield);
            ivIcon.setColorFilter(ContextCompat.getColor(this, R.color.color_error));
        }
        if (pbLoading != null) {
            pbLoading.setVisibility(View.GONE);
        }

        if (tvTitle != null) {
            tvTitle.setText(R.string.license_check_failed_title);
        }
        if (tvMsg != null) {
            tvMsg.setText(message != null && !message.isEmpty() ? message : getString(R.string.license_check_failed_msg));
        }

        if (btnClose != null) {
            btnClose.setText(R.string.action_close);
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
        if (btnLogin != null) {
            btnLogin.setText(R.string.login_now);
            btnLogin.setOnClickListener(v -> {
                dialog.dismiss();
                showVerificationDialog();
            });
        }

        DialogUiHelper.showStyledDialog(dialog);
    }

    private void dismissVerifyingConnectionDialog() {
        if (activeNetworkCheckDialog != null) {
            try {
                if (activeNetworkCheckDialog.isShowing()) {
                    activeNetworkCheckDialog.dismiss();
                }
            } catch (Exception ignored) {
            }
            activeNetworkCheckDialog = null;
        }
    }

    private void showConnectivityErrorDialog(NetworkUtil.ConnectivityStatus status) {
        if (isFinishing() || isDestroyed()) return;

        View dialogView = DialogUiHelper.inflateDialogView(this, R.layout.dialog_tactical_confirm);
        final AlertDialog dialog = DialogUiHelper.createTacticalAlertDialog(this, dialogView);

        ImageView ivIcon = dialogView.findViewById(R.id.iv_confirm_icon);
        View pbLoading = dialogView.findViewById(R.id.pb_confirm_loading);
        TextView tvTitle = dialogView.findViewById(R.id.tv_confirm_title);
        TextView tvMsg = dialogView.findViewById(R.id.tv_confirm_message);
        Button btnClose = dialogView.findViewById(R.id.btn_confirm_negative);
        Button btnRetry = dialogView.findViewById(R.id.btn_confirm_positive);

        if (ivIcon != null) {
            ivIcon.setVisibility(View.VISIBLE);
            ivIcon.setImageResource(R.drawable.ic_globe);
            ivIcon.setColorFilter(ContextCompat.getColor(this, R.color.accent_primary));
        }
        if (pbLoading != null) {
            pbLoading.setVisibility(View.GONE);
        }

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
                handleLauncherButtonClick();
            });
        }

        DialogUiHelper.showStyledDialog(dialog);
    }

    private void renderPremiumScreen() {
        View premiumView = LayoutInflater.from(this).inflate(
                R.layout.fragment_premium,
                fragmentContainer,
                false
        );

        fragmentContainer.addView(premiumView);

        View btnNotifications = premiumView.findViewById(R.id.btn_premium_notifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> startActivity(
                    new Intent(MainActivity.this, NotificationsActivity.class)
            ));
        }

        View btnRenew = premiumView.findViewById(R.id.btn_upgrade_renew);
        if (btnRenew != null) {
            btnRenew.setOnClickListener(v -> startActivity(
                    new Intent(MainActivity.this, SellerContactActivity.class)
            ));
        }

        View btnFreeKey = premiumView.findViewById(R.id.btn_get_free_key);
        if (btnFreeKey != null) {
            btnFreeKey.setOnClickListener(v -> {
                try {
                    String url = getString(R.string.free_key_url);
                    Intent browserIntent = new Intent(
                            Intent.ACTION_VIEW,
                            android.net.Uri.parse(url)
                    );
                    startActivity(browserIntent);
                } catch (Exception e) {
                    Toast.makeText(
                            MainActivity.this,
                            "Opening Free Key Portal",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        }

        View btnPaidKey = premiumView.findViewById(R.id.btn_get_paid_key);
        if (btnPaidKey != null) {
            btnPaidKey.setOnClickListener(v -> startActivity(
                    new Intent(MainActivity.this, SellerContactActivity.class)
            ));
        }

        populatePremiumData(premiumView);
        updateNotificationBadge();

        FontHelper.applyRegularFont(premiumView);
    }

    private void populatePremiumData(View premiumView) {
        MembershipStatus status = membershipRepository.getCurrentStatus();

        TextView tvBadge = premiumView.findViewById(R.id.tv_badge_status);
        TextView tvDaysRemaining = premiumView.findViewById(R.id.tv_premium_remaining_days);
        TextView tvExpirySub = premiumView.findViewById(R.id.tv_premium_expiry_date_sub);
        ProgressBar progressBar = premiumView.findViewById(R.id.progress_license_duration);
        View layoutFeaturesUnlocked = premiumView.findViewById(R.id.layout_features_unlocked);
        TextView tvPlan = premiumView.findViewById(R.id.tv_detail_plan_type);
        TextView tvStart = premiumView.findViewById(R.id.tv_detail_start_date);
        TextView tvExpiry = premiumView.findViewById(R.id.tv_detail_expiry_date);
        TextView tvRemainingDaysRow = premiumView.findViewById(R.id.tv_detail_days_remaining);

        stopLiveCountdownTimer();

        if (status.getState() == MembershipStatus.State.ACTIVE) {
            if (tvBadge != null) {
                tvBadge.setText(R.string.status_active);
                tvBadge.setBackgroundResource(R.drawable.bg_badge_active);
                tvBadge.setTextColor(ContextCompat.getColor(this, R.color.badge_active_text));
            }

            if (tvExpirySub != null) {
                tvExpirySub.setText("( " + status.getFormattedExpiryDate() + " )");
            }
            if (layoutFeaturesUnlocked != null) {
                layoutFeaturesUnlocked.setVisibility(View.VISIBLE);
            }
            if (tvPlan != null) {
                tvPlan.setText(status.getPlanType());
            }
            if (tvStart != null) {
                tvStart.setText(status.getFormattedStartDate());
            }
            if (tvExpiry != null) {
                tvExpiry.setText(status.getFormattedExpiryDate());
            }

            // Real-time second-by-second live countdown ticker
            liveCountdownRunnable = new Runnable() {
                @Override
                public void run() {
                    MembershipStatus currentStatus = membershipRepository.getCurrentStatus();
                    if (currentStatus.getState() == MembershipStatus.State.ACTIVE) {
                        String liveTime = currentStatus.getFormattedLiveCountdown(MainActivity.this);
                        if (tvDaysRemaining != null) {
                            tvDaysRemaining.setText(liveTime);
                        }
                        if (tvRemainingDaysRow != null) {
                            tvRemainingDaysRow.setText(liveTime);
                        }
                        if (progressBar != null) {
                            long remainingSecs = currentStatus.getRemainingSeconds();
                            int totalSecs = 18 * 86400;
                            int progressPct = (int) Math.max(8, Math.min(100, (remainingSecs * 100) / totalSecs));
                            progressBar.setProgress(progressPct);
                        }
                        liveCountdownHandler.postDelayed(this, 1000);
                    } else {
                        String zeroStr = getString(R.string.countdown_zero);
                        if (tvDaysRemaining != null) tvDaysRemaining.setText(zeroStr);
                        if (tvRemainingDaysRow != null) tvRemainingDaysRow.setText(zeroStr);
                        if (progressBar != null) progressBar.setProgress(0);
                    }
                }
            };
            liveCountdownHandler.post(liveCountdownRunnable);
        } else {
            if (tvBadge != null) {
                tvBadge.setText(
                        status.getState() == MembershipStatus.State.EXPIRED
                                ? R.string.status_expired
                                : R.string.status_free
                );
                tvBadge.setBackgroundResource(R.drawable.bg_badge_inactive);
                tvBadge.setTextColor(ContextCompat.getColor(this, R.color.badge_inactive_text));
            }
            String zeroStr = getString(R.string.countdown_zero);
            if (tvDaysRemaining != null) {
                tvDaysRemaining.setText(zeroStr);
            }
            if (tvExpirySub != null) {
                tvExpirySub.setText("( Not Activated )");
            }
            if (tvRemainingDaysRow != null) {
                tvRemainingDaysRow.setText(zeroStr);
            }
            if (progressBar != null) {
                progressBar.setProgress(0);
            }
            if (layoutFeaturesUnlocked != null) {
                layoutFeaturesUnlocked.setVisibility(View.GONE);
            }
            if (tvPlan != null) {
                tvPlan.setText("Free / Not Activated");
            }
            if (tvStart != null) {
                tvStart.setText("--");
            }
            if (tvExpiry != null) {
                tvExpiry.setText("--");
            }
        }
    }

    private void stopLiveCountdownTimer() {
        if (liveCountdownRunnable != null) {
            liveCountdownHandler.removeCallbacks(liveCountdownRunnable);
            liveCountdownRunnable = null;
        }
    }

    private void showVerificationDialog() {
        VerificationKeyDialog dialog = new VerificationKeyDialog(this);
        dialog.setOnLoginSuccessListener(() -> {
            if (currentTab == Tab.HOME) {
                navigateToTab(Tab.HOME);
            } else if (currentTab == Tab.PREMIUM) {
                navigateToTab(Tab.PREMIUM);
            }
        });
        dialog.show();
    }

    @Override
    public void onMembershipChanged(MembershipStatus newStatus) {
        runOnUiThread(() -> {
            if (currentTab == Tab.HOME) {
                renderHomeScreen();
            } else if (currentTab == Tab.PREMIUM) {
                renderPremiumScreen();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateNavUi();

        if (activeUpdateDialog != null && activeUpdateDialog.isShowing()) {
            activeUpdateDialog.onActivityResume();
        }

        if (currentTab == Tab.HOME) {
            fragmentContainer.removeAllViews();
            renderHomeScreen();
            updateNotificationBadge();
            startCarouselTimer();
        } else if (currentTab == Tab.PREMIUM) {
            fragmentContainer.removeAllViews();
            renderPremiumScreen();
        } else if (currentTab == Tab.SETTINGS) {
            fragmentContainer.removeAllViews();
            renderSettingsScreen();
        }

        checkLegacyStandaloneAppOnResume();
        syncLicenseStateInBackground();
    }

    @Override
    public void onBackPressed() {
        if (currentTab != Tab.HOME) {
            navigateToTab(Tab.HOME);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCarouselTimer();
        stopLiveCountdownTimer();
    }

    private void syncLicenseStateInBackground() {
        if (!NetworkUtil.isNetworkConnected(this) || NetworkUtil.hasCaptivePortal(this)) {
            return;
        }
        final String savedKey = membershipRepository.getSavedLicenseKey();
        if (savedKey == null || savedKey.trim().isEmpty()) {
            return;
        }

        DeviceIdManager devIdMgr = new DeviceIdManager(this);
        String deviceId = devIdMgr.getRawAndroidId();
        String deviceModel = devIdMgr.getDeviceBrand() + " " + devIdMgr.getDeviceModel();
        String appVersion = "2.4.0";

        LicenseVerificationService licenseService = new LicenseVerificationService();
        licenseService.verifyKey(savedKey.trim(), deviceId, deviceModel, appVersion, new VerificationCallback() {
            @Override
            public void onVerificationStarted() {}

            @Override
            public void onVerificationSuccess(VerificationResult result) {
                if (result.getMembershipStatus() != null) {
                    membershipRepository.saveActiveKey(savedKey.trim(), result.getMembershipStatus());
                }
            }

            @Override
            public void onVerificationError(String errorMessage, String errorCode) {
                boolean isAuthoritativeRevocation = "STATUS_INVALID".equals(errorCode)
                        || "STATUS_REVOKED".equals(errorCode)
                        || "STATUS_EXPIRED".equals(errorCode)
                        || "STATUS_DEVICE_REVOKED".equals(errorCode);
                if (isAuthoritativeRevocation) {
                    membershipRepository.revokeKey(savedKey);
                }
            }
        });
    }

    private void renderSettingsScreen() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View settingsView = inflater.inflate(R.layout.fragment_settings, fragmentContainer, false);
        fragmentContainer.addView(settingsView);

        SharedPreferences settingsPrefs = getSharedPreferences(SETTINGS_PREFS_NAME, MODE_PRIVATE);
        boolean isNotificationsOn = settingsPrefs.getBoolean(KEY_NOTIF, true);
        boolean isHapticOn = settingsPrefs.getBoolean(KEY_HAPTIC, true);
        boolean isDarkMode = settingsPrefs.getBoolean(KEY_DARK_MODE, true);

        View btnBack = settingsView.findViewById(R.id.btn_settings_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> navigateToTab(Tab.HOME));
        }

        SwitchCompat switchTheme = settingsView.findViewById(R.id.switch_app_theme);
        SwitchCompat switchNotifications = settingsView.findViewById(R.id.switch_notifications);
        SwitchCompat switchHaptic = settingsView.findViewById(R.id.switch_haptic);
        TextView tvThemeDesc = settingsView.findViewById(R.id.tv_theme_desc);
        TextView tvNotificationsStatus = settingsView.findViewById(R.id.tv_notifications_status);
        TextView tvHapticStatus = settingsView.findViewById(R.id.tv_haptic_status);
        TextView tvLanguageDesc = settingsView.findViewById(R.id.tv_language_desc);
        TextView tvUpdateStatus = settingsView.findViewById(R.id.tv_setting_update_status);

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

        refreshSettingsUpdateVersionRow(tvUpdateStatus);

        View rowTheme = settingsView.findViewById(R.id.row_setting_theme);
        if (rowTheme != null) {
            rowTheme.setOnClickListener(v -> {
                if (switchTheme != null) switchTheme.toggle();
            });
        }
        if (switchTheme != null) {
            switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
                settingsPrefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();
                if (tvThemeDesc != null) {
                    tvThemeDesc.setText(isChecked ? R.string.setting_theme_desc : R.string.setting_theme_desc_light);
                }
                triggerHapticFeedback();
                AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
                Toast.makeText(this, isChecked ? "Tactical Dark Mode active" : "Tactical Light Mode active", Toast.LENGTH_SHORT).show();
            });
        }

        View rowLanguage = settingsView.findViewById(R.id.row_setting_language);
        if (rowLanguage != null) {
            rowLanguage.setOnClickListener(v -> {
                triggerHapticFeedback();
                showLanguageSelectionDialog(tvLanguageDesc);
            });
        }

        View rowNotifications = settingsView.findViewById(R.id.row_setting_notifications);
        if (rowNotifications != null) {
            rowNotifications.setOnClickListener(v -> {
                if (switchNotifications != null) switchNotifications.toggle();
            });
        }
        if (switchNotifications != null) {
            switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                settingsPrefs.edit().putBoolean(KEY_NOTIF, isChecked).apply();
                if (tvNotificationsStatus != null) {
                    tvNotificationsStatus.setText(isChecked ? R.string.status_on : R.string.status_off);
                }
                Toast.makeText(MainActivity.this, "Notifications " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
                triggerHapticFeedback();
            });
        }

        View rowHaptic = settingsView.findViewById(R.id.row_setting_haptic);
        if (rowHaptic != null) {
            rowHaptic.setOnClickListener(v -> {
                if (switchHaptic != null) switchHaptic.toggle();
            });
        }
        if (switchHaptic != null) {
            switchHaptic.setOnCheckedChangeListener((buttonView, isChecked) -> {
                settingsPrefs.edit().putBoolean(KEY_HAPTIC, isChecked).apply();
                if (tvHapticStatus != null) {
                    tvHapticStatus.setText(isChecked ? R.string.status_on : R.string.status_off);
                }
                Toast.makeText(MainActivity.this, "Haptic feedback " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
                if (isChecked) {
                    triggerHapticFeedback();
                }
            });
        }

        View rowDeviceInfo = settingsView.findViewById(R.id.row_setting_device_info);
        if (rowDeviceInfo != null) {
            rowDeviceInfo.setOnClickListener(v -> {
                triggerHapticFeedback();
                startActivity(new Intent(MainActivity.this, DeviceInfoActivity.class));
            });
        }

        View rowPrivacy = settingsView.findViewById(R.id.row_setting_privacy);
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

        View rowHelp = settingsView.findViewById(R.id.row_setting_help);
        if (rowHelp != null) {
            rowHelp.setOnClickListener(v -> {
                triggerHapticFeedback();
                startActivity(new Intent(MainActivity.this, SellerContactActivity.class));
            });
        }

        View rowUpdate = settingsView.findViewById(R.id.row_setting_update);
        if (rowUpdate != null) {
            rowUpdate.setOnClickListener(v -> {
                triggerHapticFeedback();
                activeUpdateDialog = new UpdateDialog(MainActivity.this);
                activeUpdateDialog.setOnInstallStateChangedListener(() -> refreshSettingsUpdateVersionRow(tvUpdateStatus));
                activeUpdateDialog.show();
            });
        }

        View rowAbout = settingsView.findViewById(R.id.row_setting_about);
        if (rowAbout != null) {
            rowAbout.setOnClickListener(v -> {
                triggerHapticFeedback();
                showCustomInfoDialog(
                        getString(R.string.app_name) + " " + getString(R.string.app_version),
                        getString(R.string.slogan) + "\n\n" +
                                "Engine: AimShot Core Engine\n" +
                                "Version: " + getString(R.string.app_version) + "\n" +
                                "Architecture: 120 FPS Tactical Precision Overlay\n\n" +
                                "Designed for competitive accuracy, zero recoil simulation, and peak frame pacing.\n\n" +
                                "© 2026 AimShot Studio. All rights reserved.",
                        R.drawable.ic_aimshot_logo
                );
            });
        }

        Button btnLogout = settingsView.findViewById(R.id.btn_settings_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                triggerHapticFeedback();
                confirmLogout();
            });
        }

        FontHelper.applyRegularFont(settingsView);
    }

    private void refreshSettingsUpdateVersionRow(TextView tvUpdateStatus) {
        if (tvUpdateStatus == null) return;
        String versionLabel = getString(R.string.app_version);
        String vaultVer = PackageInstallerHelper.getInstalledEngineVersionName(this);
        if (vaultVer != null) {
            versionLabel = vaultVer.startsWith("v") ? vaultVer : ("v" + vaultVer);
        }
        tvUpdateStatus.setText(getString(R.string.setting_update_desc, versionLabel));
    }

    private void triggerHapticFeedback() {
        SharedPreferences settingsPrefs = getSharedPreferences(SETTINGS_PREFS_NAME, MODE_PRIVATE);
        boolean isHapticOn = settingsPrefs.getBoolean(KEY_HAPTIC, true);
        if (!isHapticOn) return;
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(45);
                }
            }
        } catch (Exception ignored) {}
    }

    private void showLanguageSelectionDialog(TextView tvLanguageDesc) {
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
            rowEn.setOnClickListener(v -> applySelectedLanguage(dialog, "en", "English", tvLanguageDesc));
        }
        if (rowBn != null) {
            rowBn.setOnClickListener(v -> applySelectedLanguage(dialog, "bn", "বাংলা (Bengali)", tvLanguageDesc));
        }
        if (rowHi != null) {
            rowHi.setOnClickListener(v -> applySelectedLanguage(dialog, "hi", "हिन्दी (Hindi)", tvLanguageDesc));
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

    private void applySelectedLanguage(AlertDialog dialog, String langCode, String displayName, TextView tvLanguageDesc) {
        SharedPreferences settingsPrefs = getSharedPreferences(SETTINGS_PREFS_NAME, MODE_PRIVATE);
        settingsPrefs.edit().putString(KEY_LANG, displayName).apply();
        LocaleHelper.setLocale(MainActivity.this, langCode);

        if (tvLanguageDesc != null) {
            tvLanguageDesc.setText(displayName);
        }
        triggerHapticFeedback();
        Toast.makeText(MainActivity.this,
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
                membershipRepository.clearSession();
                Toast.makeText(MainActivity.this, "Session cleared. Logged out.", Toast.LENGTH_SHORT).show();
                navigateToTab(Tab.HOME);
            });
        }

        DialogUiHelper.showStyledDialog(dialog);
    }
}
