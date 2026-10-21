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

        // Premium
        ivNavPremium.setColorFilter(currentTab == Tab.PREMIUM ? colorActive : colorInactive);
        tvNavPremium.setTextColor(currentTab == Tab.PREMIUM ? colorActive : colorInactive);

        // Settings
        ivNavSettings.setColorFilter(colorInactive);
        tvNavSettings.setTextColor(colorInactive);
    }

    private void renderHomeScreen() {
        View homeView = LayoutInflater.from(this).inflate(R.layout.fragment_home, fragmentContainer, false);
        fragmentContainer.addView(homeView);

        // Header notifications
        View btnNotifications = homeView.findViewById(R.id.btn_notifications);
        btnNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SellerContactActivity.class));
            }
        });

        // View All Games
        View tvViewAll = homeView.findViewById(R.id.tv_view_all_games);
        tvViewAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SupportedGamesActivity.class));
            }
        });

        // Home Carousel Setup with Indicators
        final android.widget.ViewFlipper flipper = homeView.findViewById(R.id.home_carousel_flipper);
        final ImageView dot1 = homeView.findViewById(R.id.dot_1);
        final ImageView dot2 = homeView.findViewById(R.id.dot_2);
        if (flipper != null && dot1 != null && dot2 != null) {
            int c1 = getResources().getIdentifier("carousel_img_1", "drawable", getPackageName());
            if (c1 != 0) {
                ImageView iv1 = homeView.findViewById(R.id.iv_carousel_1);
                if (iv1 != null) iv1.setImageResource(c1);
            }
            int c2 = getResources().getIdentifier("carousel_img_2", "drawable", getPackageName());
            if (c2 != 0) {
                ImageView iv2 = homeView.findViewById(R.id.iv_carousel_2);
                if (iv2 != null) iv2.setImageResource(c2);
            }
        }

        // Game Grid
        RecyclerView rvGrid = homeView.findViewById(R.id.rv_games_grid);
        rvGrid.setLayoutManager(new GridLayoutManager(this, 4));
        GameGridAdapter adapter = new GameGridAdapter(GameRepository.getInstance().getAllGames(), new GameGridAdapter.OnGameClickListener() {
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

        // Login Card Setup
        updateHomeLoginCard(homeView);
    }

    private void updateHomeLoginCard(View homeView) {
        final MembershipStatus status = membershipRepository.getCurrentStatus();
        TextView tvLoginTitle = homeView.findViewById(R.id.tv_login_title);
        TextView tvLoginSubtitle = homeView.findViewById(R.id.tv_login_subtitle);
        Button btnLogin = homeView.findViewById(R.id.btn_home_login);
        ImageView ivLoginIcon = homeView.findViewById(R.id.iv_login_icon);

        if (status.getState() == MembershipStatus.State.ACTIVE) {
            ivLoginIcon.setImageResource(R.drawable.ic_check_circle);
            tvLoginTitle.setText("License Active (" + status.getRemainingDays() + " Days Left)");
            tvLoginSubtitle.setText("Pro gaming features unlocked. Plan: " + status.getPlanType());
            btnLogin.setText("View License Details");
            btnLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigateToTab(Tab.PREMIUM);
                }
            });
        } else {
            ivLoginIcon.setImageResource(R.drawable.ic_lock);
            tvLoginTitle.setText(R.string.you_are_not_logged_in);
            tvLoginSubtitle.setText(R.string.login_desc);
            btnLogin.setText(R.string.login_now);
            btnLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showVerificationDialog();
                }
            });
        }
    }

    private void renderPremiumScreen() {
        View premiumView = LayoutInflater.from(this).inflate(R.layout.fragment_premium, fragmentContainer, false);
        fragmentContainer.addView(premiumView);

        // Settings icon on top-right
        View btnSettings = premiumView.findViewById(R.id.btn_premium_settings);
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        // Upgrade / Renew button
        View btnRenew = premiumView.findViewById(R.id.btn_upgrade_renew);
        btnRenew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SellerContactActivity.class));
            }
        });

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
            tvBadge.setText(R.string.status_active);
            tvBadge.setBackgroundResource(R.drawable.bg_badge_active);

            int remainingDays = status.getRemainingDays();
            tvDaysRemaining.setText(remainingDays + " Days");
            tvExpirySub.setText("( " + status.getFormattedExpiryDate() + " )");
            tvRemainingDaysRow.setText(remainingDays + " Days");

            progressBar.setProgress(Math.max(10, Math.min(100, remainingDays * 3)));
            layoutFeaturesUnlocked.setVisibility(View.VISIBLE);

            tvPlan.setText(status.getPlanType());
            tvStart.setText(status.getFormattedStartDate());
            tvExpiry.setText(status.getFormattedExpiryDate());
        } else {
            tvBadge.setText(status.getState() == MembershipStatus.State.EXPIRED ? R.string.status_expired : R.string.status_free);
            tvBadge.setBackgroundColor(ContextCompat.getColor(this, R.color.border_subtle));

            tvDaysRemaining.setText("0 Days");
            tvExpirySub.setText("( Not Activated )");
            tvRemainingDaysRow.setText("0 Days");

            progressBar.setProgress(0);
            layoutFeaturesUnlocked.setVisibility(View.GONE);

            tvPlan.setText("Free / Not Activated");
            tvStart.setText("--");
            tvExpiry.setText("--");
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
