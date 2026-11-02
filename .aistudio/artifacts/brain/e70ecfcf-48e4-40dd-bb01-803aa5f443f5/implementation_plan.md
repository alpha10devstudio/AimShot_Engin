# AimShot Engine — Secure APK Download, Verification, Update & Permanent Signing System

Comprehensive architectural blueprint and implementation plan for integrating an end-to-end, cryptographically verified APK download, storage, integrity verification, and package update system for **AimShot Engine** (`com.alpha10.aimshot`) while preserving 100% of existing tactical gaming features and branding.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following critical architectural decisions have been confirmed and will govern the implementation:

- **Confirmed Decision 1 — Permanent Release Keystore**: Rather than generating ephemeral debug keys on each GitHub Actions runner (which broke update compatibility), a permanent release keystore will be generated, configured with release build signing in Gradle, and integrated into GitHub Actions via encrypted GitHub Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
- **Confirmed Decision 2 — Expected SHA-256 Hash Source**: Because the server-side API at `alpha10.freedev.app` is frozen and streams raw APK binaries without metadata headers, the client will utilize an authoritative signed Release Manifest specification (`ReleaseManifest.java`) matching the GitHub Release asset and release build metadata.
- **Confirmed Decision 3 — UI Integration Entry Point**: A dedicated "Software Update & Security" card will be integrated into the existing Settings screen, alongside an unobtrusive tactical notification update banner on the Home dashboard when a newer compatible version is detected.

---

## 1. Overview & Core Concept

### What It Does
Provides AimShot Engine with a carrier-grade, cryptographically hardened in-app update pipeline. The system validates network reachability, authenticates with the Alpha10 server via `X-App-Key` to obtain a single-use 60-second Bearer token, downloads the production release APK directly into app-private internal storage, verifies both SHA-256 file integrity and the permanent signing certificate fingerprint, and triggers official Android Package Installer updates without requiring manual browser downloads.

### Target Audience & Persona
Competitive mobile gamers using AimShot Engine on Android 7.0+ (API 24 to 34) who require seamless, secure updates without losing their verified license keys, active game configurations, or device binding state.

### Key Value
- **Zero Update Incompatibility**: Eliminates `INSTALL_FAILED_UPDATE_INCOMPATIBLE` by enforcing a single, permanent release signing identity.
- **Anti-Tampering Assurance**: Two independent cryptographic gates (SHA-256 byte hash + X.509 certificate fingerprint) prevent corrupt or malicious APK installations.
- **App-Private Security**: APKs are stored in private internal storage (`filesDir/downloads`), shielded from ordinary file managers, and accessed via Android `FileProvider`.

---

## 2. User Experience & Visual Design

### Key User Flows

```
[Settings / Home Screen]
        │
        ▼ (Tap "Check for Updates" or Auto-Detect)
[Network & Server Reachability Gate]
        │
   (Connected & Verified)
        │
        ▼
[Tactical Update Dialog / HUD]
  - Displays Version Comparison (e.g., v1.0.0 -> v1.0.1)
  - Release Highlights & File Size
        │
        ▼ (User taps "Update Now")
[Authentication & Token Exchange]
  - Sends X-App-Key to /api/auth.php
  - Obtains single-use 60s temporary Bearer token
        │
        ▼
[Private Streamed Download]
  - Real-time progress bar (e.g., 14.2 MB / 28.5 MB · 50%)
  - Streamed into context.getFilesDir()/downloads/*.tmp
        │
        ▼
[Cryptographic Verification Gate]
  - Step 1: Calculate SHA-256 file hash -> Compare with expected
  - Step 2: Extract X.509 signing certificate -> Compare with trusted fingerprint
        │
   (Both Checks Pass)
        │
        ▼
[Official Android Package Installer]
  - Validates package name ("com.alpha10.aimshot")
  - Checks ACTION_MANAGE_UNKNOWN_APP_SOURCES on Android 8+
  - Launches PackageInstaller via FileProvider content:// URI
```

### Visual Identity & Theme
- **Aesthetic Direction**: Tactical Gaming UI with Electric Blue accents (`#00E5FF` / `#00C2FF`), dark space background (`#0B0F17` / `#131926`), and subtle grid line textures.
- **Typography**: Brand headings styled with `versatylo.ttf`, and tactical labels, status messages, and progress figures styled with `roboto.ttf` using tabular numerals for live byte counters.
- **Micro-Interactions**: Smooth 60 FPS progress bar easing, electric blue pulse during verification phases, and tactile haptic vibration on successful validation.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Private Internal Storage vs. Public Downloads
- **Chosen Approach**: Store downloaded APKs in `context.getFilesDir() + "/downloads/"` and serve via `FileProvider`.
- **Why**: Protects the APK from external file-manager tampering, avoids broad `READ_EXTERNAL_STORAGE` / `MANAGE_EXTERNAL_STORAGE` permissions, and complies with Android 11+ scoped storage requirements.
- **Alternatives Considered**: Public `Environment.DIRECTORY_DOWNLOADS` was rejected because external apps can modify or delete the file prior to installation.

### Decision 2: Two-Tier Cryptographic Verification
- **Chosen Approach**: Mandatory SHA-256 hash match **plus** X.509 certificate fingerprint match before invoking the installer.
- **Why**: SHA-256 ensures bit-level download completeness and integrity, while certificate fingerprint verification guarantees the binary originated from the authentic developer signing key.
- **Alternatives Considered**: Relying solely on the Android OS installer was rejected because the OS check happens too late in the UX flow and gives vague error messages.

### Decision 3: Atomic Staging File Lifecycle
- **Chosen Approach**: Download stream writes to `AimShot_{version}.apk.tmp`. Only after complete stream reception and SHA-256 validation is the file atomically renamed to `AimShot_{version}.apk`. If verification fails or download is aborted, the `.tmp` file is immediately deleted.
- **Why**: Guarantees corrupt, partial, or interrupted downloads can never be executed or consume residual disk space.

---

## 4. Technical Architecture & Data Strategy

### System Component Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                        AIMSHOT ENGINE ANDROID CLIENT                   │
│                                                                        │
│   ┌───────────────────────────┐      ┌─────────────────────────────┐   │
│   │       MainActivity        │      │      SettingsActivity       │   │
│   │  (Home Update Banner HUD) │      │ (Update & Diagnostics Card) │   │
│   └─────────────┬─────────────┘      └──────────────┬──────────────┘   │
│                 │                                   │                  │
│                 └─────────────────┬─────────────────┘                  │
│                                   ▼                                    │
│                 ┌───────────────────────────────────┐                  │
│                 │      UpdateControllerDialog       │                  │
│                 │  (Tactical Progress & HUD States) │                  │
│                 └─────────────────┬─────────────────┘                  │
│                                   ▼                                    │
│                 ┌───────────────────────────────────┐                  │
│                 │       UpdateManagerService        │                  │
│                 │ - Network & Server Reachability   │                  │
│                 │ - Auth (POST /api/auth.php)       │                  │
│                 │ - Download (/api/aimshot-download)│                  │
│                 │ - Atomic File Storage Management  │                  │
│                 └─────────────────┬─────────────────┘                  │
│                                   ▼                                    │
│                 ┌───────────────────────────────────┐                  │
│                 │       SecurityVerification        │                  │
│                 │ 1. ApkSha256Validator             │                  │
│                 │ 2. CertificateFingerprintVerifier │                  │
│                 │ 3. PackageName & VersionValidator │                  │
│                 └─────────────────┬─────────────────┘                  │
│                                   ▼                                    │
│                 ┌───────────────────────────────────┐                  │
│                 │     Android PackageInstaller      │                  │
│                 │  (FileProvider content:// URI)    │                  │
│                 └───────────────────────────────────┘                  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ HTTPS (TLS 1.3)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                      ALPH10 FROZEN SERVER API                          │
│                                                                        │
│   POST /api/auth.php              GET /api/aimshot-download.php        │
│   Header: X-App-Key               Header: Authorization Bearer <token> │
│   Returns: {token, expires_in}    Streams: Application/octet-stream    │
└────────────────────────────────────────────────────────────────────────┘
```

### Component Implementation Scope

1. **`NetworkUtil.java` Extension**:
   - Enhances active network capability check (`NET_CAPABILITY_INTERNET` + `NET_CAPABILITY_VALIDATED`).
   - Adds lightweight asynchronous HTTPS server reachability ping to `https://alpha10.freedev.app`.

2. **`UpdateAuthService.java`**:
   - Executes `POST https://alpha10.freedev.app/api/auth.php` with `X-App-Key` header.
   - Parses JSON payload: extracts temporary token string and expiry timestamp.
   - Strictly manages token in volatile memory; guarantees zero reuse.

3. **`ApkDownloadService.java`**:
   - Streams APK via `GET https://alpha10.freedev.app/api/aimshot-download.php` with `Authorization: Bearer <token>`.
   - Writes to `context.getFilesDir()/downloads/AimShot_v{code}.apk.tmp` with live progress callback (bytes read, total bytes, percent).

4. **`ApkSecurityVerifier.java`**:
   - **SHA-256 Digest**: Computes message digest over byte stream using standard `java.security.MessageDigest`.
   - **Certificate Extraction**: Uses Android `PackageManager.getPackageArchiveInfo()` (with `GET_SIGNING_CERTIFICATES` for API 28+ and `GET_SIGNATURES` for API 24-27).
   - **Fingerprint Matching**: Extracts certificate SHA-256 hash and validates against established permanent identity.
   - **Package & Version Verification**: Confirms package name equals `com.alpha10.aimshot` and `versionCode > installedVersionCode`.

5. **`PackageInstallerHelper.java`**:
   - Manages Android 8.0+ `ACTION_MANAGE_UNKNOWN_APP_SOURCES` permission checks.
   - Generates secure `content://` URI via `androidx.core.content.FileProvider`.
   - Triggers `Intent(Intent.ACTION_VIEW)` with MIME `application/vnd.android.package-archive` and `FLAG_GRANT_READ_URI_PERMISSION`.

6. **Permanent Release Signing Setup**:
   - Updates `AimShot_Project/app/build.gradle` to define `signingConfigs.release` reading environment properties or keystore file.
   - Modifies `.github/workflows/build-apk.yml` to remove the ephemeral `keytool -genkey` step and use the persistent release keystore from GitHub Secrets.
   - Generates a permanent release keystore `aimshot-release.jks` and outputs exact base64 and fingerprint for GitHub Actions configuration.

7. **Tactical UI Integration**:
   - Updates `activity_settings.xml` and `SettingsActivity.java` with a dedicated "Software Update & Security" card.
   - Implements `UpdateDialog.java` providing real-time HUD progress, verification state animations, and retry/cancel controls.
   - Fully preserves Carrom Pool, 8 Ball Pool disabled state, License Verification, Device ID copy, and language/theme settings.

---

## 5. Verification & Testing Strategy

- **Test Suite 1: Connectivity & Network Error Handling**:
  - Offline condition: verifies immediate error prompt and cancellation.
  - Server unreachable: verifies descriptive toast and retry button.
- **Test Suite 2: Authentication & Single-Use Token**:
  - Valid token reception within 60s lifecycle.
  - Token memory sanitization upon download completion or failure.
- **Test Suite 3: Integrity & Certificate Security**:
  - Exact SHA-256 verification against release manifest.
  - Tampered APK byte injection test: must immediately quarantine and delete file.
  - Mismatched certificate test: must abort prior to Android installer.
- **Test Suite 4: FileProvider & Package Installer**:
  - Valid `content://` URI generation without file manager exposure.
  - Clean update transition without license key or device ID invalidation.
- **Regression Suite**:
  - Complete verification of existing 15 unit tests and web companion app compilation.
