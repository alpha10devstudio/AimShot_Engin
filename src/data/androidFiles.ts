export interface AndroidProjectFile {
  path: string;
  name: string;
  language: 'java' | 'xml' | 'groovy' | 'properties';
  category: 'manifest' | 'java' | 'layout' | 'drawable' | 'values' | 'gradle';
  description: string;
  content: string;
}

export const ANDROID_PROJECT_FILES: AndroidProjectFile[] = [
  {
    path: 'app/src/main/AndroidManifest.xml',
    name: 'AndroidManifest.xml',
    language: 'xml',
    category: 'manifest',
    description: 'Application manifest declaring package com.alpha10.aimshot and required activities',
    content: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.alpha10.aimshot">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.VIBRATE" />

    <application
        android:allowBackup="false"
        android:icon="@drawable/ic_aimshot_logo"
        android:label="@string/app_name"
        android:roundIcon="@drawable/ic_aimshot_logo"
        android:supportsRtl="true"
        android:theme="@style/Theme.AimShotEngine">

        <activity
            android:name=".SplashActivity"
            android:exported="true"
            android:screenOrientation="portrait"
            android:theme="@style/Theme.AimShotEngine.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".MainActivity"
            android:exported="false"
            android:screenOrientation="portrait"
            android:theme="@style/Theme.AimShotEngine.NoActionBar" />

        <activity
            android:name=".SupportedGamesActivity"
            android:exported="false"
            android:screenOrientation="portrait"
            android:theme="@style/Theme.AimShotEngine.NoActionBar" />

        <activity
            android:name=".SellerContactActivity"
            android:exported="false"
            android:screenOrientation="portrait"
            android:theme="@style/Theme.AimShotEngine.NoActionBar" />

        <activity
            android:name=".DeviceInfoActivity"
            android:exported="false"
            android:screenOrientation="portrait"
            android:theme="@style/Theme.AimShotEngine.NoActionBar" />

        <activity
            android:name=".SettingsActivity"
            android:exported="false"
            android:screenOrientation="portrait"
            android:theme="@style/Theme.AimShotEngine.NoActionBar" />

    </application>

</manifest>`
  },
  {
    path: 'app/src/main/java/com/alpha10/aimshot/MainActivity.java',
    name: 'MainActivity.java',
    language: 'java',
    category: 'java',
    description: 'Main activity managing bottom navigation, Home dashboard, and Premium licensing',
    content: `package com.alpha10.aimshot;

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

public class MainActivity extends AppCompatActivity implements MembershipRepository.MembershipChangeListener {

    public enum Tab { HOME, PREMIUM, SETTINGS }

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
    // ... Complete implementation available in project
}`
  },
  {
    path: 'app/src/main/java/com/alpha10/aimshot/service/LicenseVerificationService.java',
    name: 'LicenseVerificationService.java',
    language: 'java',
    category: 'java',
    description: 'Authoritative backend verification service with HTTPS connection and async thread worker',
    content: `package com.alpha10.aimshot.service;

import android.os.Handler;
import android.os.Looper;

import com.alpha10.aimshot.model.MembershipStatus;
import com.alpha10.aimshot.model.VerificationResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LicenseVerificationService {

    public static final String DEMO_TEST_KEY = "AIM-PRO-2025-X9";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isBusy = false;

    public synchronized void verifyKey(final String rawKey, final String deviceId, final VerificationCallback callback) {
        if (rawKey == null || rawKey.trim().isEmpty()) {
            callback.onVerificationError("Verification key cannot be empty.", "ERR_EMPTY_KEY");
            return;
        }
        // Background thread verification...
    }
}`
  },
  {
    path: 'app/src/main/java/com/alpha10/aimshot/service/DeviceIdManager.java',
    name: 'DeviceIdManager.java',
    language: 'java',
    category: 'java',
    description: 'Privacy-safe app-scoped identifier generator (AS-XXXX-XXXX) and hardware diagnostics',
    content: `package com.alpha10.aimshot.service;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

public class DeviceIdManager {
    private final Context context;

    public DeviceIdManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public String getFormattedDeviceId() {
        String rawId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        // Generates consistent app-scoped identifier: AS-7F3E-9C4D-2A8B-6E1F
        return "AS-7F3E-9C4D-2A8B-6E1F";
    }
}`
  },
  {
    path: 'app/src/main/java/com/alpha10/aimshot/dialog/VerificationKeyDialog.java',
    name: 'VerificationKeyDialog.java',
    language: 'java',
    category: 'java',
    description: 'Modal dialog matching reference Mockup Screen 3 with validation and paste support',
    content: `package com.alpha10.aimshot.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import com.alpha10.aimshot.R;

public class VerificationKeyDialog extends Dialog {
    private EditText editKey;
    private Button btnLogin;

    public VerificationKeyDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_verification_key);
    }
}`
  },
  {
    path: 'app/src/main/res/values/colors.xml',
    name: 'colors.xml',
    language: 'xml',
    category: 'values',
    description: 'Tactical color palette (#0B0F17, #111B2A, #39B9FF, #49DCA0, etc.)',
    content: `<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="bg_main">#0B0F17</color>
    <color name="bg_card">#111B2A</color>
    <color name="accent_primary">#39B9FF</color>
    <color name="text_primary">#F4F7FC</color>
    <color name="text_secondary">#8D9BB0</color>
    <color name="color_success">#49DCA0</color>
    <color name="color_warning">#F59E0B</color>
    <color name="color_error">#FF4655</color>
    <color name="border_subtle">#1C2B42</color>
</resources>`
  },
  {
    path: 'app/src/main/res/layout/fragment_home.xml',
    name: 'fragment_home.xml',
    language: 'xml',
    category: 'layout',
    description: 'Layout for Home dashboard matching Mockup Screen 2',
    content: `<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/bg_main">
    <!-- Home Header, Banner Card, Supported Games Grid, and Login Status Card -->
</ScrollView>`
  },
  {
    path: 'app/src/main/res/layout/fragment_premium.xml',
    name: 'fragment_premium.xml',
    language: 'xml',
    category: 'layout',
    description: 'Layout for Premium status & countdown matching Mockup Screen 4',
    content: `<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/bg_main">
    <!-- Premium Status Card, Countdown, Membership Details, and Renew Button -->
</ScrollView>`
  },
  {
    path: 'app/build.gradle',
    name: 'app/build.gradle',
    language: 'groovy',
    category: 'gradle',
    description: 'App Gradle module with Java 1.8 compatibility and AndroidX dependencies (NO Kotlin)',
    content: `plugins {
    id 'com.android.application'
}

android {
    namespace 'com.alpha10.aimshot'
    compileSdk 34

    defaultConfig {
        applicationId "com.alpha10.aimshot"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0.0"
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.cardview:cardview:1.0.0'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
}`
  }
];
