package com.alpha10.aimshot;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

/**
 * Splash screen presenting the tactical reticle branding and initial initialization.
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends BaseActivity {

    private static final int SPLASH_DELAY_MS = 1600;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        }, SPLASH_DELAY_MS);
    }
}
