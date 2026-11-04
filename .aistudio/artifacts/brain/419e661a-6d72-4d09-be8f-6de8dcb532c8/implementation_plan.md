# AimShot Engine — Full App Localization, Smooth Navigation Transitions, 2-Game Card Layout & Direct Package Installation

Comprehensive implementation plan to resolve four core functional and UI requirements:
1. **Universal App Localization**: Synchronize language selection (English, Bengali, Hindi) across all app screens and activities, so returning from Settings re-renders the Home, Premium, and all sub-views in the chosen language immediately without remaining in English.
2. **Smooth Slide Page Transitions**: Eliminate jarring window flicker/jolt ("ঝটকা") when opening and closing Settings and all secondary screens by implementing fluid right-to-left slide enter/exit animations (`slide_in_right`, `slide_out_left`, `slide_in_left`, `slide_out_right`).
3. **Streamlined 2-Game Side-by-Side Card Layout**: Limit the Supported Games section to exclusively **Carrom Disc Pool** (Active & Unlocked) and **8 Ball Pool** (Locked), rendered as two beautifully balanced tactical cards side-by-side.
4. **Standard Direct OS Package Installation**: Restore direct Android `PackageInstaller` APK installation for `com.alpha10.aimshot`, allowing normal system-level package presence while launching and managing the engine securely via `AimShot Launcher` (`package="com.alpha10.aimshot"`).

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions have been confirmed by the user in Phase 1:

- **Confirmed Decision 1: Direct OS Package Installation**: Rather than storing and running the APK through an in-memory/isolated classloader vault, the downloaded `com.alpha10.aimshot` APK will be installed directly via the Android OS `PackageInstaller`. The app will be visible in device `Settings -> Apps` and launched directly from `AimShot Launcher` via `com.alpha10.aimshot` launch intent.
- **Confirmed Decision 2: Universal Smooth Slide Navigation**: All activity transitions (Settings, Device Info, Seller Contact, Notifications, Supported Games) will use smooth right-to-left slide-in and slide-out transitions, completely eliminating the system animation jolt ("ঝটকা").
- **Confirmed Decision 3: 2-Game Balanced Layout**: Only **Carrom Disc Pool** and **8 Ball Pool** will be shown on the home screen, displayed side-by-side in a 2-column balanced grid with tactical badges and active/locked status indicators.

---

## 1. Overview & Core Concept

### What It Does
- **App-wide Multilingual Refresh**: When a user selects Bengali (`বাংলা`) or Hindi (`हिन्दी`) in Settings, the language preference is persisted to `SharedPreferences` and applied to `LocaleHelper`. Every activity in the back stack (including `MainActivity`) automatically recreates its layout and typography upon `onResume()` if the active locale differs from the saved locale.
- **Buttery-Smooth Navigation**: Navigating between `MainActivity` and `SettingsActivity` (or any sub-activity) triggers fluid hardware-accelerated slide transitions. Closing the page slides it gracefully back to the right.
- **Focused 2-Card Game Grid**: Eliminates the 8 unused locked game cards. Carrom Disc Pool occupies the left slot with an active green tactical badge, and 8 Ball Pool occupies the right slot with a sleek locked indicator and 0.45 alpha.
- **Reliable Direct APK Installation**: Downloads the verified `com.alpha10.aimshot` APK from `https://alpha10.freedev.app/api/aimshot-download.php`, checks SHA-256 integrity, invokes the Android `PackageInstaller` session, handles installation success/failure intents cleanly, and updates the launcher button to "LAUNCH AIMSHOT ENGINE" to start `package="com.alpha10.aimshot"`.

### Key Value
- Seamless user experience with zero language mismatch between screens.
- Elimination of visual jarring or window stuttering on modern Android versions (Android 10 through 14).
- Clean, focused tactical interface showing only the two games relevant to the user.
- Direct, native Android execution for peak performance, overlay guidelines, and system service compatibility.

---

## 2. User Experience & Visual Design

### Key User Flows

```
[Home Screen (MainActivity)]
        │
        ├── User taps "Settings" in Bottom Navigation Bar
        │         └── [Smooth Slide-in from Right Animation (280ms cubic-bezier)]
        ▼
[Settings Screen (SettingsActivity)]
        │
        ├── User taps "Language" -> Tactical Dialog opens
        │         └── User selects "বাংলা (Bengali)"
        │         └── Dialog closes, SettingsActivity updates language immediately
        │
        ├── User taps Back Button (or system back)
        │         └── [Smooth Slide-out to Right Animation (250ms)]
        ▼
[Home Screen (MainActivity)]
        │
        └── onResume() detects locale change (EN -> BN)
        └── Recreates view with Bengali strings ("হোম", "প্রিমিয়াম", "সেটিংস", "সাপোর্টেড গেমস")
        └── Supported Games shows 2 balanced cards: "ক্যারম ডিস্ক পুল" & "৮ বল পুল"
```

### Visual Identity & Layout Refinements
- **Supported Games 2-Card Grid**:
  - Left card: **Carrom Disc Pool** — Active cyan/green glow, tactical crosshair icon, "সক্রিয়" (Active) badge.
  - Right card: **8 Ball Pool** — Dimmed tactical navy background (`alpha 0.65`), locked padlock icon, "লক করা" (Locked) badge.
  - Two equal-width columns (`GridLayoutManager(context, 2)` or a tailored 2-column flex container) with `12dp` central gutter and `16dp` outer margins.
- **Slide Animations**:
  - `slide_in_right.xml`: Translate X from `100%` to `0%` with interpolator `@android:anim/decelerate_interpolator` (duration `260ms`).
  - `slide_out_left.xml`: Translate X from `0%` to `-25%` with alpha from `1.0` to `0.85`.
  - `slide_in_left.xml`: Translate X from `-25%` to `0%` with alpha from `0.85` to `1.0`.
  - `slide_out_right.xml`: Translate X from `0%` to `100%` with interpolator `@android:anim/accelerate_interpolator` (duration `240ms`).

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Language Propagation via BaseActivity Lifecycle
- **Approach**: In `BaseActivity.onResume()`, compare the current active configuration language with `LocaleHelper.getPersistedData(this, "en")`. If they mismatch, invoke `recreate()` on the activity.
- **Why**: Android maintains suspended activity stacks. When `SettingsActivity` is popped from the back stack, `MainActivity.onCreate()` or `attachBaseContext()` is not invoked again; only `onResume()` is called. Checking in `onResume()` guarantees instantaneous, flicker-free language updates across all screens.

### Decision 2: Smooth Window & Activity Transitions
- **Approach**: Define custom animation resources (`slide_in_right`, `slide_out_left`, `slide_in_left`, `slide_out_right`) and apply them universally:
  1. Programmatically via `Activity.overridePendingTransition()` immediately following `startActivity()` and inside `finish()`.
  2. In `styles.xml` / `Theme.AimShotEngine` under `android:windowAnimationStyle` so system back gestures and navigation events automatically slide smoothly.
- **Why**: Default Android window transitions on Android 12–14 often execute abrupt vertical slides or zoom-in morphs that clash with custom tactical layouts, causing perceived "ঝটকা" (jerk). Consistent horizontal slides feel native, snappy, and premium.

### Decision 3: Game Repository Truncation to 2 Titles
- **Approach**: In `GameRepository.initializeGames()`, keep strictly **Carrom Disc Pool** (`com.miniclip.carrom`) and **8 Ball Pool** (`com.miniclip.eightballpool`). Remove PUBG, Free Fire, BGMI, COD, Valorant, GTA V, eFootball, and Minecraft.
- **Layout Adjustment**: Update `rv_games_grid` layout manager from 4 columns to 2 columns so the cards fill the grid symmetrically without awkward blank gaps.

### Decision 4: Direct OS Package Installation & Launcher Execution
- **Approach**:
  1. Restore the verified APK download and direct Android `PackageInstaller` pipeline in `UpdateController` and `PackageInstallerHelper`.
  2. Once installed by Android OS, `com.alpha10.aimshot` exists as a registered application package.
  3. `PackageInstallerHelper.isMainApkInstalled(context)` checks `packageManager.getPackageInfo("com.alpha10.aimshot", 0)`.
  4. `PackageInstallerHelper.launchInstalledApp(context)` retrieves `packageManager.getLaunchIntentForPackage("com.alpha10.aimshot")` and starts the activity with `FLAG_ACTIVITY_NEW_TASK`.
  5. Remove the legacy uninstaller prompt that asked users to remove `com.alpha10.aimshot`, because this is now the intended installation format.

---

## 4. Technical Architecture & Component Mapping

### System Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                          AimShot Launcher                              │
│                                                                        │
│   ┌──────────────────────────────────────────────────────────────┐     │
│   │                        BaseActivity                          │     │
│   │  • attachBaseContext(LocaleHelper.onAttach(context))         │     │
│   │  • onResume() -> checkLocaleChanged() -> recreate()          │     │
│   │  • applySmoothSlideTransitions()                             │     │
│   └───────────────┬──────────────────────────────┬───────────────┘     │
│                   │                              │                     │
│                   ▼                              ▼                     │
│         ┌───────────────────┐          ┌───────────────────┐           │
│         │   MainActivity    │          │ SettingsActivity  │           │
│         │  (Home & Premium) │◄────────►│ (Theme, Language) │           │
│         └─────────┬─────────┘          └─────────┬─────────┘           │
│                   │                              │                     │
│                   ▼                              ▼                     │
│         ┌───────────────────┐          ┌───────────────────┐           │
│         │  GameRepository   │          │   LocaleHelper    │           │
│         │ • Carrom (Active) │          │ • Persist code    │           │
│         │ • 8 Ball (Locked) │          │ • updateResources │           │
│         └───────────────────┘          └───────────────────┘           │
│                   │                                                    │
│                   ▼                                                    │
│         ┌─────────────────────────────────────────────────┐            │
│         │     UpdateController & PackageInstallerHelper   │            │
│         │ • Stream APK from https://alpha10.freedev.app   │            │
│         │ • Verify SHA-256                                │            │
│         │ • OS PackageInstaller.Session                   │            │
│         │ • Launch package "com.alpha10.aimshot"          │            │
│         └────────────────────────┬────────────────────────┘            │
└──────────────────────────────────┼─────────────────────────────────────┘
                                   │
                                   ▼
             ┌──────────────────────────────────────────┐
             │       Android Operating System           │
             │                                          │
             │   Package: com.alpha10.aimshot           │
             │   Location: Phone Settings -> Apps       │
             │   Launched exclusively via AimShot       │
             └──────────────────────────────────────────┘
```

---

## 5. Implementation Steps & Verification Plan

### Phase A: Language Synchronization Across All Activities
- Update `LocaleHelper.java` to track the last attached language code.
- In `BaseActivity.java`, record the attached language code. In `onResume()`, if `getPersistedData()` differs from the current attached code, call `recreate()`.
- Ensure string resources in `values/strings.xml`, `values-bn/strings.xml`, and `values-hi/strings.xml` contain complete translations for all home titles, banners, game cards, and launcher buttons.

### Phase B: Smooth Navigation Slide Animations
- Create animation XML resources in `res/anim/`:
  - `slide_in_right.xml`
  - `slide_out_left.xml`
  - `slide_in_left.xml`
  - `slide_out_right.xml`
- Update `styles.xml` / `themes.xml` to include `WindowSlideAnimation` under `android:windowAnimationStyle`.
- Add transition overrides in `BaseActivity`, `MainActivity`, and `SettingsActivity` when starting or finishing activities.

### Phase C: 2-Game Card Layout
- Modify `GameRepository.java` to only register Carrom Disc Pool and 8 Ball Pool.
- Update `MainActivity.java` `rvGrid` column count from `4` to `2` (`new GridLayoutManager(this, 2)`).
- Ensure the cards fill the horizontal space harmoniously with `12dp` spacing and rounded tactical borders.

### Phase D: Direct OS Installation & Launcher Button Flow
- Revert `UpdateController.java` to install via `PackageInstallerHelper.installPackage(...)`.
- Update `PackageInstallerHelper.java` to verify OS installation of `com.alpha10.aimshot` via `PackageManager`.
- Remove the legacy standalone uninstaller prompt from `MainActivity` so `com.alpha10.aimshot` is recognized as the valid installed engine.
- Wire `handleLauncherButtonClick()` to launch `com.alpha10.aimshot` directly when installed.

### Phase E: Verification
- Verify compilation with `compile_applet` and `lint_applet`.
- Verify Java and XML syntax with static AST check.
- Sync changes between `AimShot_Project/app/` and root `app/`.
