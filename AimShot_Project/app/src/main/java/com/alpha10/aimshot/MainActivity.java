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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alpha10.aimshot.adapter.GameGridAdapter;
import com.alpha10.aimshot.dialog.VerificationKeyDialog;
import com.alpha10.aimshot.model.GameItem;
import com.alpha10.aimshot.model.MembershipStatus;
import com.alpha10.aimshot.repository.GameRepository;
import com.alpha10.aimshot.repository.MembershipRepository;

/**
 * Main dashboard hosting Home and Premium destinations with persistent Bottom Navigation.
 */
public class MainActivity extends AppCompatActivity implements MembershipRepository.MembershipChangeListener {

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
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent != null && "PREMIUM".equals(intent.getStringExtra("TARGET_TAB"))) {
            navigateToTab(Tab.PREMIUM);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
        tvNavSettings = findViewById(R.id.tv_nav_settings);

        navDotHome = findViewById(R.id.nav_dot_home);
        navDotPremium = findViewById(R.id.nav_dot_premium);
        navDotSettings = findViewById(R.id.nav_dot_settings);
    }

    private void setupNavigation() {
        navBtnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentTab != Tab.HOME) {
                    navigateToTab(Tab.HOME);
                }
            }
        });

        navBtnPremium.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentTab != Tab.PREMIUM) {
                    navigateToTab(Tab.PREMIUM);
                }
            }
        });

        navBtnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
    }

    public void navigateToTab(Tab tab) {
        currentTab = tab;
        updateNavUi();

        fragmentContainer.removeAllViews();
        if (tab == Tab.HOME) {
            renderHomeScreen();
        } else if (tab == Tab.PREMIUM) {
            renderPremiumScreen();
        }
    }

    private void updateNavUi() {
        int colorActive = ContextCompat.getColor(this, R.color.accent_primary);
        int colorInactive = ContextCompat.getColor(this, R.color.nav_inactive);

        // Home
        ivNavHome.setColorFilter(currentTab == Tab.HOME ? colorActive : colorInactive);
        tvNavHome.setTextColor(currentTab == Tab.HOME ? colorActive : colorInactive);
        if (navDotHome != null) navDotHome.setVisibility(currentTab == Tab.HOME ? View.VISIBLE : View.INVISIBLE);

        // Premium
        ivNavPremium.setColorFilter(currentTab == Tab.PREMIUM ? colorActive : colorInactive);
        tvNavPremium.setTextColor(currentTab == Tab.PREMIUM ? colorActive : colorInactive);
        if (navDotPremium != null) navDotPremium.setVisibility(currentTab == Tab.PREMIUM ? View.VISIBLE : View.INVISIBLE);

        // Settings
        ivNavSettings.setColorFilter(currentTab == Tab.SETTINGS ? colorActive : colorInactive);
        tvNavSettings.setTextColor(currentTab == Tab.SETTINGS ? colorActive : colorInactive);
        if (navDotSettings != null) navDotSettings.setVisibility(currentTab == Tab.SETTINGS ? View.VISIBLE : View.INVISIBLE);
    }

    private final android.os.Handler carouselHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable carouselRunnable;
    private android.widget.ViewFlipper currentFlipper;
    private ImageView currentDot1, currentDot2, currentDot3;

    @Override
    protected void onResume() {
        super.onResume();
        if (currentTab == Tab.HOME) {
            startCarouselTimer();
            updateNotificationBadge();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCarouselTimer();
    }

    private void startCarouselTimer() {
        stopCarouselTimer();
        if (currentFlipper == null) return;
        carouselRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentFlipper != null && currentTab == Tab.HOME) {
                    currentFlipper.showNext();
                    updateCarouselIndicators(currentFlipper.getDisplayedChild(), currentDot1, currentDot2, currentDot3);
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
            int unread = com.alpha10.aimshot.repository.NotificationsRepository.getInstance(this).getUnreadCount();
            View badge = findViewById(R.id.tv_header_notif_badge);
            if (badge instanceof TextView) {
                TextView tvBadge = (TextView) badge;
                if (unread > 0) {
                    tvBadge.setText(String.valueOf(unread));
                    tvBadge.setVisibility(View.VISIBLE);
                } else {
                    tvBadge.setVisibility(View.GONE);
                }
            }
        } catch (Exception ignored) {}
    }

    private void renderHomeScreen() {
        View homeView = LayoutInflater.from(this).inflate(R.layout.fragment_home, fragmentContainer, false);
        fragmentContainer.addView(homeView);

        // Header notifications - Open dedicated NotificationsActivity
        View btnNotifications = homeView.findViewById(R.id.btn_notifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, NotificationsActivity.class));
                }
            });
        }
        updateNotificationBadge();

        // View All Games
        View tvViewAll = homeView.findViewById(R.id.tv_view_all_games);
        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, SupportedGamesActivity.class));
                }
            });
        }

        // Home Carousel Setup with Navigation Arrows & Dynamic 3-Dot Indicators
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

            // Left / Right arrow navigation
            if (btnPrev != null) {
                btnPrev.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        flipper.showPrevious();
                        updateCarouselIndicators(flipper.getDisplayedChild(), dot1, dot2, dot3);
                        resetCarouselTimer();
                    }
                });
            }

            if (btnNext != null) {
                btnNext.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        flipper.showNext();
                        updateCarouselIndicators(flipper.getDisplayedChild(), dot1, dot2, dot3);
                        resetCarouselTimer();
                    }
                });
            }

            // Direct indicator dot clicks
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

            // Horizontal Swipe Gesture Support on Carousel
            final android.view.GestureDetector gestureDetector = new android.view.GestureDetector(this, new android.view.GestureDetector.SimpleOnGestureListener() {
                private static final int SWIPE_THRESHOLD = 50;
                private static final int SWIPE_VELOCITY_THRESHOLD = 80;

                @Override
                public boolean onFling(android.view.MotionEvent e1, android.view.MotionEvent e2, float velocityX, float velocityY) {
                    if (e1 == null || e2 == null) return false;
                    float diffX = e2.getX() - e1.getX();
                    float diffY = e2.getY() - e1.getY();
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
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
            });

            flipper.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

            // Start synchronized timer
            startCarouselTimer();
        }

        // Game Grid with persistent selection
        RecyclerView rvGrid = homeView.findViewById(R.id.rv_games_grid);
        if (rvGrid != null) {
            rvGrid.setLayoutManager(new GridLayoutManager(this, 4));
            final GameGridAdapter adapter = new GameGridAdapter(GameRepository.getInstance().getAllGames(), new GameGridAdapter.OnGameClickListener() {
                @Override
                public void onGameClick(GameItem item) {
                    if (item.isActive()) {
                        Toast.makeText(MainActivity.this, "Aim Assist Ready for " + item.getName(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, item.getName() + " is currently locked / Pro Key required", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            rvGrid.setAdapter(adapter);
        }

        // State-Dependent Login Card / Launch Button Setup
        updateHomeLoginCard(homeView);
    }

    private void updateCarouselIndicators(int index, ImageView d1, ImageView d2, ImageView d3) {
        if (d1 == null || d2 == null || d3 == null) return;
        float density = getResources().getDisplayMetrics().density;
        int activeWidth = (int) (20 * density);
        int inactiveWidth = (int) (6 * density);
        int height = (int) (6 * density);

        // Dot 1
        d1.setImageResource(index == 0 ? R.drawable.indicator_dot_active : R.drawable.indicator_dot_inactive);
        android.widget.LinearLayout.LayoutParams lp1 = new android.widget.LinearLayout.LayoutParams(index == 0 ? activeWidth : inactiveWidth, height);
        lp1.setMargins((int)(3*density), 0, (int)(3*density), 0);
        d1.setLayoutParams(lp1);

        // Dot 2
        d2.setImageResource(index == 1 ? R.drawable.indicator_dot_active : R.drawable.indicator_dot_inactive);
        android.widget.LinearLayout.LayoutParams lp2 = new android.widget.LinearLayout.LayoutParams(index == 1 ? activeWidth : inactiveWidth, height);
        lp2.setMargins((int)(3*density), 0, (int)(3*density), 0);
        d2.setLayoutParams(lp2);

        // Dot 3
        d3.setImageResource(index == 2 ? R.drawable.indicator_dot_active : R.drawable.indicator_dot_inactive);
        android.widget.LinearLayout.LayoutParams lp3 = new android.widget.LinearLayout.LayoutParams(index == 2 ? activeWidth : inactiveWidth, height);
        lp3.setMargins((int)(3*density), 0, (int)(3*density), 0);
        d3.setLayoutParams(lp3);
    }

    private void updateHomeLoginCard(View homeView) {
        final MembershipStatus status = membershipRepository.getCurrentStatus();
        View cardLoginStatus = homeView.findViewById(R.id.card_login_status);
        View cardLaunchGame = homeView.findViewById(R.id.card_launch_game);
        Button btnLaunchGame = homeView.findViewById(R.id.btn_launch_game);
        Button btnLogin = homeView.findViewById(R.id.btn_home_login);

        if (status.getState() == MembershipStatus.State.ACTIVE) {
            // Already logged in: Hide login card, display Launch The Game card
            if (cardLoginStatus != null) cardLoginStatus.setVisibility(View.GONE);
            if (cardLaunchGame != null) {
                cardLaunchGame.setVisibility(View.VISIBLE);
                TextView tvDays = homeView.findViewById(R.id.tv_active_days_remaining);
                if (tvDays != null) {
                    tvDays.setText(status.getRemainingDays() + " Days");
                }
                if (btnLaunchGame != null) {
                    btnLaunchGame.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Toast.makeText(MainActivity.this, "AimShot Engine Overlay Activated for Carrom Disc Pool (120 FPS)", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        } else {
            // Not logged in: Show login card, hide Launch The Game button
            if (cardLoginStatus != null) cardLoginStatus.setVisibility(View.VISIBLE);
            if (cardLaunchGame != null) cardLaunchGame.setVisibility(View.GONE);
            if (btnLogin != null) {
                btnLogin.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showVerificationDialog();
                    }
                });
            }
        }
    }

    private void renderPremiumScreen() {
        View premiumView = LayoutInflater.from(this).inflate(R.layout.fragment_premium, fragmentContainer, false);
        fragmentContainer.addView(premiumView);

        // Notification icon on top-right (opens NotificationsActivity)
        View btnNotifications = premiumView.findViewById(R.id.btn_premium_notifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, NotificationsActivity.class));
                }
            });
        }

        // Upgrade / Renew button
        View btnRenew = premiumView.findViewById(R.id.btn_upgrade_renew);
        if (btnRenew != null) {
            btnRenew.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, SellerContactActivity.class));
                }
            });
        }

        // Get Free Key button
        View btnFreeKey = premiumView.findViewById(R.id.btn_get_free_key);
        if (btnFreeKey != null) {
            btnFreeKey.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        String url = getString(R.string.free_key_url);
                        android.content.Intent browserIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url));
                        startActivity(browserIntent);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Opening Free Key Portal", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        // Get Paid Key button
        View btnPaidKey = premiumView.findViewById(R.id.btn_get_paid_key);
        if (btnPaidKey != null) {
            btnPaidKey.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, SellerContactActivity.class));
                }
            });
        }

        populatePremiumData(premiumView);
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

        if (status.getState() == MembershipStatus.State.ACTIVE) {
            if (tvBadge != null) {
                tvBadge.setText(R.string.status_active);
                tvBadge.setBackgroundResource(R.drawable.bg_badge_active);
            }

            int remainingDays = status.getRemainingDays();
            if (tvDaysRemaining != null) tvDaysRemaining.setText(remainingDays + " Days");
            if (tvExpirySub != null) tvExpirySub.setText("( " + status.getFormattedExpiryDate() + " )");
            if (tvRemainingDaysRow != null) tvRemainingDaysRow.setText(remainingDays + " Days");

            if (progressBar != null) progressBar.setProgress(Math.max(10, Math.min(100, remainingDays * 3)));
            if (layoutFeaturesUnlocked != null) layoutFeaturesUnlocked.setVisibility(View.VISIBLE);

            if (tvPlan != null) tvPlan.setText(status.getPlanType());
            if (tvStart != null) tvStart.setText(status.getFormattedStartDate());
            if (tvExpiry != null) tvExpiry.setText(status.getFormattedExpiryDate());
        } else {
            if (tvBadge != null) {
                tvBadge.setText(status.getState() == MembershipStatus.State.EXPIRED ? R.string.status_expired : R.string.status_free);
                tvBadge.setBackgroundColor(ContextCompat.getColor(this, R.color.border_subtle));
            }

            if (tvDaysRemaining != null) tvDaysRemaining.setText("0 Days");
            if (tvExpirySub != null) tvExpirySub.setText("( Not Activated )");
            if (tvRemainingDaysRow != null) tvRemainingDaysRow.setText("0 Days");

            if (progressBar != null) progressBar.setProgress(0);
            if (layoutFeaturesUnlocked != null) layoutFeaturesUnlocked.setVisibility(View.GONE);

            if (tvPlan != null) tvPlan.setText("Free / Not Activated");
            if (tvStart != null) tvStart.setText("--");
            if (tvExpiry != null) tvExpiry.setText("--");
        }
    }

    private void showVerificationDialog() {
        VerificationKeyDialog dialog = new VerificationKeyDialog(this);
        dialog.setOnLoginSuccessListener(new VerificationKeyDialog.OnLoginSuccessListener() {
            @Override
            public void onLoginSuccess() {
                if (currentTab == Tab.HOME) {
                    navigateToTab(Tab.HOME);
                } else if (currentTab == Tab.PREMIUM) {
                    navigateToTab(Tab.PREMIUM);
                }
            }
        });
        dialog.show();
    }

    @Override
    public void onMembershipChanged(MembershipStatus newStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (currentTab == Tab.HOME) {
                    renderHomeScreen();
                } else if (currentTab == Tab.PREMIUM) {
                    renderPremiumScreen();
                }
            }
        });
    }
}
