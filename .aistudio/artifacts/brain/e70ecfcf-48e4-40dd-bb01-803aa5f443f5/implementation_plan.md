# AimShot Engine — Dual-Application Package Architecture Implementation Plan

Architectural specification and execution plan to decouple the **AimShot Launcher App** and the **Main AimShot Engine APK** into distinct Android application packages, permanently resolving the self-launch loop and establishing a true carrier-grade Downloader, Installer, and Launcher system.

---

## 1. Problem Analysis & Root Cause

### The Self-Launch Loop
- **Current State**: The Launcher Android project had `applicationId "com.alpha10.aimshot"`.
- When the user tapped the Launcher Button to launch the downloaded Main Engine, the system called:
  `context.getPackageManager().getLaunchIntentForPackage("com.alpha10.aimshot")`
- Because the Launcher App itself was running under `com.alpha10.aimshot`, Android resolved the intent to the Launcher's own `SplashActivity` / `MainActivity`, causing the Launcher to re-launch itself in an infinite loop.
- Furthermore, on Android OS, two applications cannot share the same `applicationId` on a single device simultaneously; an APK with an identical package ID is treated as an update to the current app rather than a companion/main engine app.

---

## 2. Confirmed Package Naming Architecture

Based on user review and architectural requirements, the two applications are officially assigned distinct identities:

| Attribute | Application 1: Launcher App | Application 2: Main Engine APK |
|---|---|---|
| **Role** | Downloader, Verifier, License Manager & Launcher | Core 120 FPS Aim Assist & Touch Guideline Engine |
| **Package Name / Application ID** | `com.alpha10.aimshot.launcher` | `com.alpha10.aimshot` (Preserves server release ID) |
| **Android Display Name** | **AimShot Launcher** | **AimShot Engine** |
| **Storage & Origin** | Installed on device / Client project | Streamed from Alpha10 Server (`aimshot-download.php`) |
| **FileProvider Authority** | `com.alpha10.aimshot.launcher.fileprovider` | Internal |
| **Interaction** | Checks OS for `com.alpha10.aimshot`, installs & launches | Executed as target application |

---

## 3. Step-by-Step Implementation Roadmap

### Step 1: Android Gradle & Application ID Update
- **File**: `AimShot_Project/app/build.gradle`
  - Change `applicationId` from `"com.alpha10.aimshot"` to `"com.alpha10.aimshot.launcher"`.
  - Retain Java code package namespace: `namespace 'com.alpha10.aimshot'` (prevents breaking any existing Java class imports or R class references).
  - Retain release signing configs (`aimshot-release.p12` keystore).

### Step 2: AndroidManifest & FileProvider Configuration
- **File**: `AimShot_Project/app/src/main/AndroidManifest.xml`
  - Update FileProvider `android:authorities` to `"${applicationId}.fileprovider"` (resolves to `com.alpha10.aimshot.launcher.fileprovider`).
  - Maintain `<queries>` for `<package android:name="com.alpha10.aimshot" />` so Android 11+ (API 30+) Package Manager can authoritatively detect the Main Engine.
- **File**: `AimShot_Project/app/src/main/res/values/strings.xml` (plus `values-bn/strings.xml`, `values-hi/strings.xml`)
  - Update `app_name` to `"AimShot Launcher"`.
  - Update launcher action button strings to reflect the clean distinction between the Launcher and the Main AimShot Engine.

### Step 3: PackageInstallerHelper & Inter-App Launcher Routing
- **File**: `AimShot_Project/app/src/main/java/com/alpha10/aimshot/util/PackageInstallerHelper.java`
  - Explicitly define:
    ```java
    public static final String TARGET_MAIN_PACKAGE = "com.alpha10.aimshot";
    ```
  - Rewrite `isMainApkInstalled(Context context)`:
    - Authoritatively queries Android OS:
      ```java
      PackageManager pm = context.getPackageManager();
      PackageInfo info = pm.getPackageInfo(TARGET_MAIN_PACKAGE, 0);
      Intent launchIntent = pm.getLaunchIntentForPackage(TARGET_MAIN_PACKAGE);
      return info != null && launchIntent != null;
      ```
    - Since Launcher ID is `com.alpha10.aimshot.launcher` and Target ID is `com.alpha10.aimshot`, this check returns `true` ONLY when the actual external Main Engine APK is installed on the device!
  - Update `launchInstalledApp(Context context)`:
    - Resolves `getLaunchIntentForPackage("com.alpha10.aimshot")`.
    - Adds `Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED`.
    - Launches the Main AimShot Engine. Zero self-looping!
- **File**: `AimShot_Project/app/src/main/java/com/alpha10/aimshot/MainActivity.java`
  - Update `handleLauncherButtonClick()` to use the authoritative OS detection.
  - If installed: Directly launches `com.alpha10.aimshot`.
  - If not installed: Initiates Alpha10 server auth (`POST /api/auth.php`), downloads the APK, verifies SHA-256 + certificate, and triggers official `PackageInstaller`.

### Step 4: Web Preview UI & Session HUD Sync
- **Files**: `src/App.tsx`, `src/components/screens/HomeScreen.tsx`, `src/data/translations.ts`
  - Reflect `AimShot Launcher` (`com.alpha10.aimshot.launcher`) on header and app identity.
  - Reflect `AimShot Engine` (`com.alpha10.aimshot`) as the target engine.
  - When Main APK is installed: Launcher button launches the dedicated AimShot Engine session HUD (`com.alpha10.aimshot`), with zero looping back.

### Step 5: Testing & Verification
- Expand unit tests in `tests/aimshot-logic.test.mjs`:
  - Test 32: Dual-package distinction validation (`com.alpha10.aimshot.launcher` vs `com.alpha10.aimshot`).
  - Test 33: Self-launch prevention validation (verifies target package does not match launcher package).
  - Test 34: Authoritative external package status detection.
- Run `npm test`, `compile_applet`, and `lint_applet`.
