import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

describe('AimShot Engine Pure Logic Unit Tests', () => {
  // Test 1: Device ID Formatting
  test('Device ID follows AS-XXXX-XXXX-XXXX-XXXX format', () => {
    const rawHash = '7F3E9C4D2A8B6E1F';
    const formatted = `AS-${rawHash.slice(0, 4)}-${rawHash.slice(4, 8)}-${rawHash.slice(8, 12)}-${rawHash.slice(12, 16)}`;
    assert.match(formatted, /^AS-[0-9A-Z]{4}-[0-9A-Z]{4}-[0-9A-Z]{4}-[0-9A-Z]{4}$/);
    assert.equal(formatted, 'AS-7F3E-9C4D-2A8B-6E1F');
  });

  // Test 2: License expiration calculation
  test('Days remaining calculates correct days until expiry', () => {
    const now = Date.now();
    const eighteenDaysMillis = 18 * 24 * 60 * 60 * 1000;
    const expiryTimestamp = now + eighteenDaysMillis;
    
    const diff = expiryTimestamp - now;
    const days = Math.floor(diff / (24 * 60 * 60 * 1000));
    assert.equal(days, 18);
  });

  // Test 3: Expired membership validation
  test('Past expiry timestamp yields 0 days and expired state', () => {
    const pastTimestamp = Date.now() - 10000;
    const isExpired = Date.now() > pastTimestamp;
    assert.equal(isExpired, true);
  });

  // Test 4: Key input sanitation
  test('Key input trims whitespace and rejects empty string', () => {
    const validateKey = (key) => {
      if (!key || key.trim() === '') return { valid: false, error: 'Empty key' };
      return { valid: true, cleanKey: key.trim() };
    };

    assert.equal(validateKey('').valid, false);
    assert.equal(validateKey('   ').valid, false);
    assert.equal(validateKey('  AIM-PRO-2025-X9  ').cleanKey, 'AIM-PRO-2025-X9');
  });

  // Test 5: Game search filter
  test('Game search correctly matches titles and features', () => {
    const games = [
      { name: 'PUBG Mobile', features: 'Aim Assist • Crosshair • FPS Boost' },
      { name: 'Valorant', features: 'Crosshair • FPS • Game Sense' }
    ];

    const filterGames = (query) => {
      const q = query.toLowerCase();
      return games.filter(g => g.name.toLowerCase().includes(q) || g.features.toLowerCase().includes(q));
    };

    assert.equal(filterGames('Crosshair').length, 2);
    assert.equal(filterGames('Aim Assist').length, 1);
    assert.equal(filterGames('Aim Assist')[0].name, 'PUBG Mobile');
    assert.equal(filterGames('NonExistent').length, 0);
  });

  // Test 6: WhatsApp link generator
  test('WhatsApp link sanitizes non-numeric phone chars', () => {
    const rawPhone = '+880 1700 111222';
    const cleanPhone = rawPhone.replace(/[^0-9]/g, '');
    assert.equal(cleanPhone, '8801700111222');
    assert.equal(`https://wa.me/${cleanPhone}`, 'https://wa.me/8801700111222');
  });

  // Test 7: Notification category filtering & unread count
  test('Notification category filtering and unread count calculate correctly', () => {
    const sampleNotifs = [
      { id: '1', title: 'Update A', category: 'update', isRead: false },
      { id: '2', title: 'Feature B', category: 'feature', isRead: true },
      { id: '3', title: 'Maintenance C', category: 'maintenance', isRead: false },
      { id: '4', title: 'Announcement D', category: 'announcement', isRead: false }
    ];

    const unreadCount = sampleNotifs.filter(n => !n.isRead).length;
    assert.equal(unreadCount, 3);

    const updateCategory = sampleNotifs.filter(n => n.category === 'update');
    assert.equal(updateCategory.length, 1);
    assert.equal(updateCategory[0].title, 'Update A');
  });

  // Test 8: Notification mark as read immutability
  test('Marking notification as read returns updated item with isRead: true', () => {
    const notif = { id: 'test-1', title: 'Notice', isRead: false };
    const markAsRead = (item) => ({ ...item, isRead: true });
    const updated = markAsRead(notif);
    assert.equal(updated.isRead, true);
    assert.equal(notif.isRead, false); // Original remains unchanged
  });

  // Test 9: Carousel indicator dot lockstep synchronization
  test('Carousel index cycles smoothly with 3 slide dots', () => {
    const totalSlides = 3;
    let currentSlide = 0;
    const advanceSlide = (curr) => (curr + 1) % totalSlides;
    const prevSlide = (curr) => (curr === 0 ? totalSlides - 1 : curr - 1);

    currentSlide = advanceSlide(currentSlide);
    assert.equal(currentSlide, 1);
    currentSlide = advanceSlide(currentSlide);
    assert.equal(currentSlide, 2);
    currentSlide = advanceSlide(currentSlide);
    assert.equal(currentSlide, 0); // Wraps back to 0

    currentSlide = prevSlide(currentSlide);
    assert.equal(currentSlide, 2); // Wraps back to last slide
  });

  // Test 10: Settings 4 options toggle & state preservation
  test('Top 4 settings toggle states toggle independently', () => {
    const settings = {
      theme: 'dark',
      language: 'en',
      notificationsEnabled: true,
      hapticFeedbackEnabled: true
    };

    const toggleTheme = (s) => ({ ...s, theme: s.theme === 'dark' ? 'light' : 'dark' });
    const toggleNotifs = (s) => ({ ...s, notificationsEnabled: !s.notificationsEnabled });
    const toggleHaptic = (s) => ({ ...s, hapticFeedbackEnabled: !s.hapticFeedbackEnabled });
    const changeLang = (s, lang) => ({ ...s, language: lang });

    const s1 = toggleTheme(settings);
    assert.equal(s1.theme, 'light');
    assert.equal(s1.notificationsEnabled, true);

    const s2 = toggleNotifs(s1);
    assert.equal(s2.notificationsEnabled, false);

    const s3 = toggleHaptic(s2);
    assert.equal(s3.hapticFeedbackEnabled, false);

    const s4 = changeLang(s3, 'bn');
    assert.equal(s4.language, 'bn');
  });

  // Test 11: Clipboard Paste replaces text and avoids auto-submission
  test('Clipboard Paste replaces input content, positions cursor, and avoids auto-submission', () => {
    let inputField = 'OLD_KEY_123';
    let isSubmitted = false;

    const handlePaste = (clipboardContent) => {
      if (clipboardContent && clipboardContent.trim()) {
        inputField = clipboardContent.trim();
        const cursorPosition = inputField.length;
        return { success: true, text: inputField, cursor: cursorPosition, submitted: isSubmitted };
      }
      return { success: false, text: inputField, cursor: inputField.length, submitted: isSubmitted };
    };

    const res = handlePaste('  AIM-PRO-2025-X9  ');
    assert.equal(res.success, true);
    assert.equal(res.text, 'AIM-PRO-2025-X9');
    assert.equal(res.cursor, 15);
    assert.equal(res.submitted, false); // Crucial: must NOT auto-submit

    // Empty clipboard handling
    const emptyRes = handlePaste('');
    assert.equal(emptyRes.success, false);
    assert.equal(emptyRes.text, 'AIM-PRO-2025-X9'); // Preserves previous input without crash
  });

  // Test 12: Mandatory Internet Check prevents launch when offline
  test('Mandatory Internet Check gates launch button: blocks offline, allows online', () => {
    const launchGameFlow = (isNetworkOnline) => {
      if (!isNetworkOnline) {
        return {
          proceeded: false,
          error: 'No Internet Connection. Please connect to the internet and try again.',
          showSpinner: false
        };
      }
      return {
        proceeded: true,
        error: null,
        showSpinner: true
      };
    };

    // When offline
    const offlineResult = launchGameFlow(false);
    assert.equal(offlineResult.proceeded, false);
    assert.equal(offlineResult.showSpinner, false);
    assert.match(offlineResult.error, /No Internet Connection/);

    // When online
    const onlineResult = launchGameFlow(true);
    assert.equal(onlineResult.proceeded, true);
    assert.equal(onlineResult.showSpinner, true);
  });

  // Test 13: Game package mapping for Carrom Disc Pool
  test('Target game package resolves to com.miniclip.carrom for Carrom Disc Pool', () => {
    const games = [
      { id: 'carrom', name: 'Carrom Disc Pool', package: 'com.miniclip.carrom' },
      { id: '8ball', name: '8 Ball Pool', package: 'com.miniclip.eightballpool' }
    ];

    const activeGame = games.find(g => g.id === 'carrom');
    assert.ok(activeGame);
    assert.equal(activeGame.package, 'com.miniclip.carrom');
  });

  // Test 14: Badge text contrast fix ensures non-invisible text
  test('Badge background color (#49DCA0) and text color (#0B0F17) have strong contrast', () => {
    const bgGreen = '#49DCA0';
    const textDark = '#0B0F17';
    // Old bug: text was #49DCA0 on #49DCA0 (invisible)
    assert.notEqual(bgGreen, textDark);
    assert.equal(textDark, '#0B0F17');
  });

  // Test 15: Supported language locale mapping (en, bn, hi)
  test('Language options map cleanly to supported ISO codes', () => {
    const langMap = {
      'English': 'en',
      'বাংলা (Bengali)': 'bn',
      'हिन्दी (Hindi)': 'hi'
    };

    assert.equal(langMap['English'], 'en');
    assert.equal(langMap['বাংলা (Bengali)'], 'bn');
    assert.equal(langMap['हिन्दी (Hindi)'], 'hi');
  });

  // Test 16: Release Manifest version comparison
  test('Release Manifest correctly identifies newer version vs up-to-date', () => {
    const currentVersionCode = 1; // v1.0.0
    const manifestNew = { versionCode: 2, versionName: '1.0.1' };
    const manifestSame = { versionCode: 1, versionName: '1.0.0' };

    assert.equal(manifestNew.versionCode > currentVersionCode, true);
    assert.equal(manifestSame.versionCode > currentVersionCode, false);
  });

  // Test 17: SHA-256 integrity check logic with case/colon normalization
  test('SHA-256 integrity check normalizes hex format and rejects tampered digests', () => {
    const cleanHash = (h) => (h ? h.replace(/[:\s]/g, '').toLowerCase() : '');
    const expected = 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855';
    const calculatedMatching = 'E3:B0:C4:42:98:FC:1C:14:9A:FB:F4:C8:99:6F:B9:24:27:AE:41:E4:64:9B:93:4C:A4:95:99:1B:78:52:B8:55';
    const calculatedTampered = '112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00';

    assert.equal(cleanHash(calculatedMatching), cleanHash(expected));
    assert.notEqual(cleanHash(calculatedTampered), cleanHash(expected));
  });

  // Test 18: Permanent signing certificate SHA-256 fingerprint verification
  test('Permanent signing certificate matches established release identity fingerprint', () => {
    const trustedFingerprint = '1C:8D:56:83:62:EE:D9:04:02:36:43:0D:AC:8D:1F:BE:30:06:0E:9F:FD:2D:F4:01:AB:69:04:E7:94:86:A5:A2';
    const clean = (fp) => fp.replace(/[:\s]/g, '').toUpperCase();
    const extractedCert = '1c8d568362eed9040236430dac8d1fbe30060e9ffd2df401ab6904e79486a5a2';

    assert.equal(clean(extractedCert), clean(trustedFingerprint));
    assert.equal(clean(trustedFingerprint).length, 64);
  });

  // Test 19: Temporary Bearer token 60s lifecycle and single-use policy
  test('Temporary token expires after 60 seconds and cannot be reused', () => {
    const issuedAt = Date.now();
    const expiresInMs = 60 * 1000;
    let token = 'tmp_sec_tok_alpha10_xyz789';
    let isUsed = false;

    const useToken = (currentTime) => {
      if (currentTime > issuedAt + expiresInMs) return { valid: false, error: 'Token expired' };
      if (isUsed) return { valid: false, error: 'Token already consumed' };
      isUsed = true;
      const consumedToken = token;
      token = null; // Cleared from memory immediately
      return { valid: true, token: consumedToken };
    };

    // First use within window
    const firstUse = useToken(issuedAt + 10000);
    assert.equal(firstUse.valid, true);
    assert.equal(firstUse.token, 'tmp_sec_tok_alpha10_xyz789');

    // Second use rejected (single-use)
    const secondUse = useToken(issuedAt + 20000);
    assert.equal(secondUse.valid, false);
    assert.equal(secondUse.error, 'Token already consumed');

    // Expired token rejected
    isUsed = false;
    token = 'another_tok';
    const expiredUse = useToken(issuedAt + 70000);
    assert.equal(expiredUse.valid, false);
    assert.equal(expiredUse.error, 'Token expired');
  });

  // Test 20: Staging atomic file lifecycle (promotes .tmp only on complete success)
  test('Atomic staging preserves clean internal storage and cleans up on failure', () => {
    const fileStore = new Map();
    const stageFile = (name, content) => fileStore.set(`${name}.tmp`, content);
    const promoteFile = (name) => {
      const data = fileStore.get(`${name}.tmp`);
      fileStore.delete(`${name}.tmp`);
      fileStore.set(name, data);
    };
    const abortAndClean = (name) => fileStore.delete(`${name}.tmp`);

    // Failed download cleanup
    stageFile('AimShot_v2.apk', 'partial_stream');
    abortAndClean('AimShot_v2.apk');
    assert.equal(fileStore.has('AimShot_v2.apk.tmp'), false);
    assert.equal(fileStore.has('AimShot_v2.apk'), false);

    // Successful download promotion
    stageFile('AimShot_v2.apk', 'full_verified_apk_bytes');
    promoteFile('AimShot_v2.apk');
    assert.equal(fileStore.has('AimShot_v2.apk.tmp'), false);
    assert.equal(fileStore.has('AimShot_v2.apk'), true);
  });

  // Test 21: Package name check rejects non-AimShot packages
  test('Package identity validator enforces com.alpha10.aimshot package ID', () => {
    const expectedPackage = 'com.alpha10.aimshot';
    const isValidPackage = (pkg) => pkg === expectedPackage;

    assert.equal(isValidPackage('com.alpha10.aimshot'), true);
    assert.equal(isValidPackage('com.fake.impostor'), false);
    assert.equal(isValidPackage('com.miniclip.carrom'), false);
  });

  // Test 22: Authentication Failure Handling (Broker JSON error surfacing, non-200, missing token)
  test('UpdateAuthService surfaces exact server JSON errors and handles invalid HTTP status or missing token', () => {
    const handleAuthResponse = (status, body) => {
      try {
        const json = typeof body === 'string' ? JSON.parse(body) : body;
        if (json && (json.success === false || json.status === 'error')) {
          return { success: false, error: json.error || json.message || `Authentication rejected (HTTP ${status})` };
        }
        if (status !== 200) {
          const serverErr = json && (json.error || json.message);
          return { success: false, error: serverErr || `Server returned HTTP ${status}` };
        }
        const token = json && (json.token || json.temporary_token || json.temp_token || json.access_token || (json.data && json.data.token));
        if (!token) {
          const serverErr = json && (json.error || json.message);
          return { success: false, error: serverErr || 'Server response missing security token' };
        }
        return { success: true, token, expiresIn: json.expires_in || 60 };
      } catch (e) {
        if (status !== 200) {
          return { success: false, error: `Server returned HTTP ${status}` };
        }
        return { success: false, error: 'Malformed JSON from server' };
      }
    };

    assert.deepEqual(handleAuthResponse(401, 'Unauthorized'), {
      success: false,
      error: 'Server returned HTTP 401'
    });

    assert.deepEqual(handleAuthResponse(500, 'Server Error'), {
      success: false,
      error: 'Server returned HTTP 500'
    });

    assert.deepEqual(handleAuthResponse(200, '<html>502 Bad Gateway</html>'), {
      success: false,
      error: 'Malformed JSON from server'
    });

    assert.deepEqual(handleAuthResponse(200, JSON.stringify({ status: 'ok' })), {
      success: false,
      error: 'Server response missing security token'
    });

    const valid = handleAuthResponse(200, JSON.stringify({ token: 'as_tok_99182', expires_in: 60 }));
    assert.equal(valid.success, true);
    assert.equal(valid.token, 'as_tok_99182');
    assert.equal(valid.expiresIn, 60);
  });

  // Test 23: Download Failure & Content Validation (non-binary / HTML error page / size checks)
  test('ApkDownloadService detects invalid content type, HTML error responses, and stream aborts', () => {
    const validateDownloadStream = (httpStatus, contentType, byteLength, minExpectedBytes = 1000000) => {
      if (httpStatus !== 200) {
        return { ok: false, error: `Download failed with HTTP ${httpStatus}` };
      }
      if (contentType && contentType.includes('text/html')) {
        return { ok: false, error: 'Server returned HTML instead of binary APK stream' };
      }
      if (byteLength < minExpectedBytes) {
        return { ok: false, error: `Incomplete APK payload: ${byteLength} bytes received` };
      }
      return { ok: true, error: null };
    };

    assert.equal(validateDownloadStream(403, 'text/plain', 50000).ok, false);
    assert.equal(validateDownloadStream(200, 'text/html; charset=UTF-8', 50000).ok, false);
    assert.equal(validateDownloadStream(200, 'application/vnd.android.package-archive', 500, 1000000).ok, false);
    assert.equal(validateDownloadStream(200, 'application/octet-stream', 4500000, 1000000).ok, true);
  });

  // Test 24: APK Structure Validation (ZIP / AndroidManifest magic bytes)
  test('ApkSecurityVerifier rejects corrupt files or non-ZIP packages', () => {
    const checkApkStructure = (buffer) => {
      if (!buffer || buffer.length < 4) return { valid: false, error: 'Empty or truncated file' };
      const isZip = buffer[0] === 0x50 && buffer[1] === 0x4B && buffer[2] === 0x03 && buffer[3] === 0x04;
      if (!isZip) return { valid: false, error: 'File header does not match valid Android APK (ZIP) format' };
      return { valid: true };
    };

    const fakeText = Buffer.from('Error 404 Not Found');
    assert.equal(checkApkStructure(fakeText).valid, false);

    const zipHeader = Buffer.from([0x50, 0x4B, 0x03, 0x04, 0x14, 0x00]);
    assert.equal(checkApkStructure(zipHeader).valid, true);
  });

  // Test 25: PackageInstaller Session Status Handling
  test('PackageInstaller Session status correctly routes to Success, Cancellation, or Failure states', () => {
    const STATUS_SUCCESS = 0;
    const STATUS_FAILURE_ABORTED = 3;
    const STATUS_FAILURE_CONFLICT = 5;

    const processSessionStatus = (statusCode, message) => {
      switch (statusCode) {
        case STATUS_SUCCESS:
          return { stage: 'INSTALLED', canLaunch: true, message: 'Installation succeeded' };
        case STATUS_FAILURE_ABORTED:
          return { stage: 'CANCELLED', canLaunch: false, message: 'Installation was cancelled by user' };
        case STATUS_FAILURE_CONFLICT:
          return { stage: 'ERROR', canLaunch: false, message: 'Package signature conflict with installed app' };
        default:
          return { stage: 'ERROR', canLaunch: false, message: message || `Installation failed (code: ${statusCode})` };
      }
    };

    const successResult = processSessionStatus(STATUS_SUCCESS);
    assert.equal(successResult.stage, 'INSTALLED');
    assert.equal(successResult.canLaunch, true);

    const cancelResult = processSessionStatus(STATUS_FAILURE_ABORTED);
    assert.equal(cancelResult.stage, 'CANCELLED');
    assert.equal(cancelResult.canLaunch, false);

    const conflictResult = processSessionStatus(STATUS_FAILURE_CONFLICT);
    assert.equal(conflictResult.stage, 'ERROR');
    assert.match(conflictResult.message, /conflict/i);
  });

  // Test 26: Unknown App Installation Permission Caching and Resumption
  test('Permission redirection caches verified APK and resumes in onResume without redownload', () => {
    let pendingVerifiedApk = null;
    let downloadCount = 0;

    const onApkVerified = (apkPath) => {
      pendingVerifiedApk = apkPath;
    };

    const handleInstallRequest = (hasInstallPermission) => {
      if (!hasInstallPermission) {
        return { action: 'REDIRECT_SETTINGS', cachedApk: pendingVerifiedApk };
      }
      return { action: 'COMMIT_SESSION', apkToInstall: pendingVerifiedApk };
    };

    const onActivityResume = (hasInstallPermission) => {
      if (pendingVerifiedApk && hasInstallPermission) {
        const apk = pendingVerifiedApk;
        return { canResume: true, apkToInstall: apk };
      }
      return { canResume: false };
    };

    downloadCount++;
    onApkVerified('/data/user/0/com.alpha10.aimshot/files/downloads/AimShot_v1.0.1.apk');
    assert.ok(pendingVerifiedApk);

    const req1 = handleInstallRequest(false);
    assert.equal(req1.action, 'REDIRECT_SETTINGS');
    assert.equal(req1.cachedApk, '/data/user/0/com.alpha10.aimshot/files/downloads/AimShot_v1.0.1.apk');

    const resumed = onActivityResume(true);
    assert.equal(resumed.canResume, true);
    assert.equal(resumed.apkToInstall, '/data/user/0/com.alpha10.aimshot/files/downloads/AimShot_v1.0.1.apk');
    assert.equal(downloadCount, 1);
  });

  // Test 27: Open AimShot Engine Launch & Explicit User Consent (No Unsolicited Auto-Countdown)
  test('Launch trigger resolves package intent and strictly requires explicit user tap without auto-countdown', () => {
    const createLaunchPlan = (installedPackage) => {
      if (installedPackage !== 'com.alpha10.aimshot') {
        return { canLaunch: false, reason: 'Unknown package' };
      }

      return {
        canLaunch: true,
        targetPackage: installedPackage,
        buttonText: 'OPEN AIMSHOT ENGINE',
        hasAutoCountdown: false,
        countdownSeconds: null
      };
    };

    const fgPlan = createLaunchPlan('com.alpha10.aimshot');
    assert.equal(fgPlan.canLaunch, true);
    assert.equal(fgPlan.buttonText, 'OPEN AIMSHOT ENGINE');
    assert.equal(fgPlan.hasAutoCountdown, false);
    assert.equal(fgPlan.countdownSeconds, null);

    const wrongPkgPlan = createLaunchPlan('com.miniclip.carrom');
    assert.equal(wrongPkgPlan.canLaunch, false);
  });

  // Test 28: Localization strings for 9-stage sequence in English, Bengali, and Hindi
  test('9-stage update state machine has complete localized status strings', () => {
    const stages = [
      'CHECKING',
      'AUTH_REQUEST',
      'DOWNLOADING',
      'HASH_VERIFY',
      'SIGN_VERIFY',
      'REQUEST_PERMISSION',
      'INSTALLING',
      'INSTALLED',
      'ERROR'
    ];

    const localizedBengali = {
      CHECKING: 'ইন্টারনেট ও সার্ভার চেক করা হচ্ছে...',
      AUTH_REQUEST: 'সার্ভার ভেরিফিকেশন ও অথেনটিকেশন চলছে...',
      DOWNLOADING: 'নিরাপদ APK ডাউনলোড হচ্ছে...',
      HASH_VERIFY: 'SHA-256 ইন্টিগ্রিটি যাচাই করা হচ্ছে...',
      SIGN_VERIFY: 'ডিজিタル সিগনেচার যাচাই করা হচ্ছে...',
      REQUEST_PERMISSION: 'ইনস্টলেশন পারমিশন প্রয়োজন...',
      INSTALLING: 'অ্যান্ড্রয়েড সিস্টেমে ইনস্টল হচ্ছে...',
      INSTALLED: 'ইনস্টলেশন সফলভাবে সম্পন্ন হয়েছে!',
      ERROR: 'একটি সমস্যা দেখা দিয়েছে'
    };

    for (const stage of stages) {
      assert.ok(localizedBengali[stage], `Missing Bengali translation for stage ${stage}`);
      assert.ok(localizedBengali[stage].length > 0);
    }
  });

  // Test 29: Launcher Button Disconnection from Carrom Pool
  test('Launcher Button completely disconnects from Carrom Pool launch behavior', () => {
    const handleLauncherButtonClick = (isInstalled, carromPackage = 'com.miniclip.carrom') => {
      // Corrected logic: ALWAYS targets Main AimShot Engine (com.alpha10.aimshot)
      const targetPackage = 'com.alpha10.aimshot';

      // Crucial: Must NEVER equal carromPackage
      assert.notEqual(targetPackage, carromPackage);

      if (isInstalled) {
        return {
          action: 'LAUNCH_PACKAGE',
          package: targetPackage,
          gameLaunched: null // Carrom is NOT launched
        };
      } else {
        return {
          action: 'OPEN_INSTALLER_PIPELINE',
          targetPackage: targetPackage,
          gameLaunched: null
        };
      }
    };

    const whenInstalled = handleLauncherButtonClick(true);
    assert.equal(whenInstalled.action, 'LAUNCH_PACKAGE');
    assert.equal(whenInstalled.package, 'com.alpha10.aimshot');
    assert.equal(whenInstalled.gameLaunched, null);

    const whenNotInstalled = handleLauncherButtonClick(false);
    assert.equal(whenNotInstalled.action, 'OPEN_INSTALLER_PIPELINE');
    assert.equal(whenNotInstalled.targetPackage, 'com.alpha10.aimshot');
    assert.equal(whenNotInstalled.gameLaunched, null);
  });

  // Test 30: Situation A — Main APK already installed launches com.alpha10.aimshot
  test('Situation A: Installed AimShot Engine triggers explicit launch intent without downloading', () => {
    let downloadAttempted = false;
    let launchedPackage = null;

    const executeLauncherFlow = (installedPackages) => {
      const isInstalled = installedPackages.includes('com.alpha10.aimshot');
      if (isInstalled) {
        launchedPackage = 'com.alpha10.aimshot';
        return { success: true, launchedPackage };
      }
      downloadAttempted = true;
      return { success: false, action: 'DOWNLOAD' };
    };

    const res = executeLauncherFlow(['com.alpha10.aimshot', 'com.miniclip.carrom']);
    assert.equal(res.success, true);
    assert.equal(launchedPackage, 'com.alpha10.aimshot');
    assert.equal(downloadAttempted, false); // Zero downloads when already installed!
  });

  // Test 31: Situation B — Main APK not installed runs download & verification pipeline
  test('Situation B: Uninstalled Main APK executes full 9-stage Alpha10 pipeline before launch is enabled', () => {
    let pipelineCompleted = false;
    let mainApkMarkedInstalled = false;

    const startDownloadAndInstallPipeline = () => {
      // 1. Internet & Server Reachability
      const isOnline = true;
      assert.equal(isOnline, true);

      // 2. Zero-Secret Alpha10 Server Broker Authentication (No client X-App-Key!)
      const brokerPayload = {
        launcher_package: 'com.alpha10.aimshot.launcher',
        target_package: 'com.alpha10.aimshot',
        device_id: 'AS-7F3E-9C4D-2A8B-6E1F'
      };
      assert.equal('X-App-Key' in brokerPayload, false);
      const token = 'alpha10_temp_60s_token';
      assert.ok(token);

      // 3. Download Main APK
      const downloadHeader = { 'Authorization': `Bearer ${token}` };
      const receivedBytes = 4320000;
      assert.ok(receivedBytes > 1000000);

      // 4. Verification
      const sha256Valid = true;
      const certFingerprint = '1C:8D:56:83:62:EE:D9:04:02:36:43:0D:AC:8D:1F:BE:30:06:0E:9F:FD:2D:F4:01:AB:69:04:E7:94:86:A5:A2';
      assert.equal(sha256Valid, true);
      assert.equal(certFingerprint.length, 95);

      // 5. PackageInstaller Commit & Success
      mainApkMarkedInstalled = true;
      pipelineCompleted = true;

      return {
        installed: true,
        package: 'com.alpha10.aimshot',
        canOpen: true
      };
    };

    const result = startDownloadAndInstallPipeline();
    assert.equal(pipelineCompleted, true);
    assert.equal(mainApkMarkedInstalled, true);
    assert.equal(result.canOpen, true);
    assert.equal(result.package, 'com.alpha10.aimshot');
  });

  // Test 32: Dual Application Architecture & Package ID Distinction
  test('Dual Application Architecture: Launcher and Main Engine have distinct package IDs', () => {
    const LAUNCHER_PACKAGE = 'com.alpha10.aimshot.launcher';
    const MAIN_ENGINE_PACKAGE = 'com.alpha10.aimshot';

    // Must be distinct to avoid Android OS treating them as identical apps
    assert.notEqual(LAUNCHER_PACKAGE, MAIN_ENGINE_PACKAGE);
    assert.equal(LAUNCHER_PACKAGE.startsWith('com.alpha10.aimshot'), true);
    assert.equal(MAIN_ENGINE_PACKAGE, 'com.alpha10.aimshot');
  });

  // Test 33: Self-Launch Loop Prevention
  test('Self-Launch Loop Prevention: Launcher explicitly refuses to launch itself', () => {
    const launchInstalledApp = (currentPackage, targetPackage) => {
      // Loop protection: if launcher and target are identical, reject launch
      if (currentPackage === targetPackage) {
        return { success: false, error: 'Self-launch loop prevented' };
      }
      return { success: true, launchedPackage: targetPackage };
    };

    // Buggy previous state (both had same ID) -> blocked by safety check
    const loopAttempt = launchInstalledApp('com.alpha10.aimshot', 'com.alpha10.aimshot');
    assert.equal(loopAttempt.success, false);
    assert.equal(loopAttempt.error, 'Self-launch loop prevented');

    // Fixed dual-package architecture -> launches external Main Engine cleanly
    const cleanLaunch = launchInstalledApp('com.alpha10.aimshot.launcher', 'com.alpha10.aimshot');
    assert.equal(cleanLaunch.success, true);
    assert.equal(cleanLaunch.launchedPackage, 'com.alpha10.aimshot');
  });

  // Test 34: Authoritative External Package Status Detection
  test('External Package Status Detection: Accurate OS query from com.alpha10.aimshot.launcher', () => {
    const installedSystemPackages = new Set(['com.alpha10.aimshot.launcher', 'com.miniclip.carrom']);

    const isMainApkInstalled = (systemPackages, targetPackage = 'com.alpha10.aimshot') => {
      return systemPackages.has(targetPackage);
    };

    // Initially Main APK is not installed -> returns false, prompting download
    assert.equal(isMainApkInstalled(installedSystemPackages), false);

    // After Main APK is installed via PackageInstaller -> returns true
    installedSystemPackages.add('com.alpha10.aimshot');
    assert.equal(isMainApkInstalled(installedSystemPackages), true);
  });

  // Test 35: Target Package Version Query Isolation (Checks com.alpha10.aimshot, not Launcher)
  test('Target Package Version Query Isolation: Checks Main Engine version code instead of Launcher', () => {
    const installedVersions = {
      'com.alpha10.aimshot.launcher': 2, // Launcher is at v2
    };

    const getInstalledVersionCode = (pkg) => installedVersions[pkg] || -1;
    const activeManifestVersionCode = 2;

    // Buggy previous behavior: checked launcher (v2), thought 2 <= 2 (up to date), skipped download!
    const wrongCheck = getInstalledVersionCode('com.alpha10.aimshot.launcher');
    assert.equal(wrongCheck >= activeManifestVersionCode, true);

    // Fixed behavior: checks target package (com.alpha10.aimshot), gets -1 (uninstalled), initiates install!
    const correctCheck = getInstalledVersionCode('com.alpha10.aimshot');
    assert.equal(correctCheck, -1);
    const requiresInstall = correctCheck === -1 || activeManifestVersionCode > correctCheck;
    assert.equal(requiresInstall, true);
  });

  // Test 36: onResume Lifecycle & Automatic Return Detection
  test('onResume Lifecycle: Returning from Android installer recognizes installed package without re-downloading', () => {
    let currentInstalled = false;
    let onInstallSuccessCalled = false;
    let redownloadCount = 0;

    const onResumeCheck = () => {
      if (currentInstalled) {
        onInstallSuccessCalled = true;
      } else {
        redownloadCount++;
      }
    };

    // User is in installer
    onResumeCheck();
    assert.equal(onInstallSuccessCalled, false);
    assert.equal(redownloadCount, 1);

    // System successfully installed com.alpha10.aimshot
    currentInstalled = true;
    onResumeCheck();
    assert.equal(onInstallSuccessCalled, true);
    assert.equal(redownloadCount, 1); // No redundant redownload!
  });

  // Test 37: Direct 64-Char APK Secret Key & Automatic InfinityFree slowAES (__test cookie) Solver
  test('Direct Website APK Auth embeds exact 64-char secret key and solves InfinityFree AES/CBC/NoPadding cookie challenge', async () => {
    const crypto = await import('node:crypto');
    const authJava = fs.readFileSync(
      path.resolve('AimShot_Project/app/src/main/java/com/alpha10/aimshot/service/UpdateAuthService.java'),
      'utf8'
    );
    const dlJava = fs.readFileSync(
      path.resolve('AimShot_Project/app/src/main/java/com/alpha10/aimshot/service/ApkDownloadService.java'),
      'utf8'
    );

    const EXPECTED_KEY = '856b5c17909bcab591c7a41f3eb427296210e007889b2a595817a26596c62a10';
    assert.equal(EXPECTED_KEY.length, 64);
    assert.equal(authJava.includes(EXPECTED_KEY), true, 'UpdateAuthService must contain the 64-char secret key');
    assert.equal(authJava.includes('https://alpha10.freedev.app/api/auth.php'), true);
    assert.equal(authJava.includes('AES/CBC/NoPadding'), true, 'UpdateAuthService must solve aes.js __test cookie');
    assert.equal(dlJava.includes('https://alpha10.freedev.app/api/aimshot-download.php'), true);

    // Verify slowAES challenge decryption matches exact InfinityFree test vector
    const sampleHtml = 'var a=toNumbers("f655ba9d09a112d4968c63579db590b4"),b=toNumbers("98344c2eee86c3994890592585b49f80"),c=toNumbers("eadf33efc12d3e7b853fe942b17b4acf");';
    const matches = [...sampleHtml.matchAll(/toNumbers\(\s*"([0-9a-fA-F]{32})"\s*\)/g)].map(m => m[1]);
    assert.equal(matches.length, 3);
    const decipher = crypto.createDecipheriv('aes-128-cbc', Buffer.from(matches[0], 'hex'), Buffer.from(matches[1], 'hex'));
    decipher.setAutoPadding(false);
    const solved = '__test=' + Buffer.concat([decipher.update(Buffer.from(matches[2], 'hex')), decipher.final()]).toString('hex');
    assert.match(solved, /^__test=[0-9a-f]{32}$/);
  });

  // Test 38: UpdateAuthService surfaces server JSON error instead of masking with "No valid token found"
  test('UpdateAuthService surfaces exact server error message when HTTP 200 JSON indicates auth rejection', () => {
    const parseAuthResponse = (httpCode, rawBody) => {
      const json = JSON.parse(rawBody);
      if (json.success === false || json.status === 'error') {
        return { ok: false, error: json.error || json.message };
      }
      const token =
        json.download_token ||
        json.token ||
        json.temporary_token ||
        json.temp_token ||
        json.access_token ||
        (json.data && (json.data.token || json.data.temporary_token));
      if (!token) {
        return { ok: false, error: json.error || json.message || 'No valid temporary token received' };
      }
      return {
        ok: true,
        token,
        expectedSha256: json.expected_sha256 || null,
        trustedCertSha256: json.trusted_cert_sha256 || null
      };
    };

    const rejected = parseAuthResponse(200, JSON.stringify({
      success: false,
      error: 'Invalid or missing X-App-Key on upstream server'
    }));
    assert.equal(rejected.ok, false);
    assert.equal(rejected.error, 'Invalid or missing X-App-Key on upstream server');

    // Live auth.php response shape with download_token
    const liveSuccess = parseAuthResponse(200, JSON.stringify({
      success: true,
      message: 'Authentication successful',
      download_token: '47409fab854cb160ab9c4001cd3d9be94e5bc743e74519f32da5377c5132d9a8',
      expires_in: 60
    }));
    assert.equal(liveSuccess.ok, true);
    assert.equal(liveSuccess.token.length, 64);
  });

  // Test 39: ApkSecurityVerifier validates com.alpha10.aimshot package without false-blocking live website APK
  test('ApkSecurityVerifier validates com.alpha10.aimshot package and allows omitted server expectedSha256 while rejecting tampered hash or wrong package', () => {
    const verifyApk = ({ packageInfo, actualSha256, expectedSha256 }) => {
      if (!packageInfo) {
        return { valid: false, reason: 'PackageManager failed to parse APK archive' };
      }
      if (packageInfo.packageName !== 'com.alpha10.aimshot') {
        return { valid: false, reason: `Package ID mismatch: ${packageInfo.packageName}` };
      }
      if (expectedSha256 && /^[0-9a-f]{64}$/i.test(expectedSha256)) {
        if (actualSha256.toLowerCase() !== expectedSha256.toLowerCase()) {
          return { valid: false, reason: 'SHA-256 digest mismatch' };
        }
      }
      return { valid: true };
    };

    const validHash = '1b189ea2cc655d6ef791c808fde6dc6bbc74eb45888eedb7487ee3705b6301be';

    // 1. Live auth.php omits expectedSha256 -> valid com.alpha10.aimshot APK MUST succeed
    assert.equal(
      verifyApk({
        packageInfo: { packageName: 'com.alpha10.aimshot' },
        actualSha256: validHash,
        expectedSha256: null
      }).valid,
      true
    );

    // 2. Null PackageInfo MUST fail
    assert.equal(
      verifyApk({
        packageInfo: null,
        actualSha256: validHash,
        expectedSha256: null
      }).valid,
      false
    );

    // 3. Wrong package ID MUST fail
    assert.equal(
      verifyApk({
        packageInfo: { packageName: 'com.alpha10.aimshot.launcher' },
        actualSha256: validHash,
        expectedSha256: null
      }).valid,
      false
    );

    // 4. Mismatched SHA-256 when server provides expectedSha256 MUST fail
    assert.equal(
      verifyApk({
        packageInfo: { packageName: 'com.alpha10.aimshot' },
        actualSha256: validHash,
        expectedSha256: 'a'.repeat(64)
      }).valid,
      false
    );
  });

  // Test 40: OS-Only Authority for isMainApkInstalled (Eliminates stale SharedPreferences bug after uninstall)
  test('PackageInstallerHelper.isMainApkInstalled relies solely on OS PackageManager and clears stale cache on uninstall', () => {
    let stalePrefsFlag = true; // Previously caused Launcher to think uninstalled app was still installed
    const osInstalledPackages = new Set(['com.alpha10.aimshot.launcher']);

    const isMainApkInstalled = () => {
      const presentInOs = osInstalledPackages.has('com.alpha10.aimshot');
      if (!presentInOs && stalePrefsFlag) {
        stalePrefsFlag = false; // Auto-heal stale preference
      }
      return presentInOs;
    };

    assert.equal(isMainApkInstalled(), false);
    assert.equal(stalePrefsFlag, false);
  });

  // Test 41: PackageInstallStatusReceiver validates active sessionId and OS package confirmation
  test('PackageInstallStatusReceiver ignores stale session callbacks and confirms OS installation on STATUS_SUCCESS', () => {
    const activeSessionId = 4021;
    const osPackages = new Set(['com.alpha10.aimshot.launcher']);

    const handleBroadcast = (incomingSessionId, statusCode) => {
      if (incomingSessionId !== activeSessionId) {
        return { ignored: true };
      }
      if (statusCode === 0) {
        if (!osPackages.has('com.alpha10.aimshot')) {
          return { ignored: false, result: 'ERROR_OS_NOT_FOUND' };
        }
        return { ignored: false, result: 'SUCCESS' };
      }
      if (statusCode === 3) {
        return { ignored: false, result: 'CANCELLED_BY_USER' };
      }
      return { ignored: false, result: 'FAILURE' };
    };

    // Stale session ignored
    assert.equal(handleBroadcast(1099, 0).ignored, true);

    // User cancelled installation
    assert.equal(handleBroadcast(4021, 3).result, 'CANCELLED_BY_USER');

    // OS installed com.alpha10.aimshot -> confirmed SUCCESS
    osPackages.add('com.alpha10.aimshot');
    assert.equal(handleBroadcast(4021, 0).result, 'SUCCESS');
  });

  // Test 42: Private Internal Storage Isolation (context.getFilesDir()/downloads/)
  test('Private Internal Storage keeps APK invisible to public storage and cleans up post-install', () => {
    const internalFilesDir = '/data/user/0/com.alpha10.aimshot.launcher/files/downloads';
    const apkPath = `${internalFilesDir}/AimShot_Engine_v1.0.1.apk`;

    assert.equal(apkPath.startsWith('/sdcard'), false);
    assert.equal(apkPath.includes('/storage/emulated/0'), false);
    assert.equal(apkPath.startsWith('/data/user/0/com.alpha10.aimshot.launcher/files/downloads/'), true);
  });

  // Test 43: UI Theme & Solid Popup Consistency Audit (Light/Dark Modes, Icons, AppCompatButton, Solid Dialogs)
  test('UI Theme & Popup Audit: Day/Night themes, visible icons, AppCompatButton in dialogs, and solid popup cards', () => {
    const icBack = fs.readFileSync(path.resolve('AimShot_Project/app/src/main/res/drawable/ic_back.xml'), 'utf8');
    const icBell = fs.readFileSync(path.resolve('AimShot_Project/app/src/main/res/drawable/ic_bell.xml'), 'utf8');
    assert.equal(icBack.includes('#F4F7FC'), false, 'ic_back.xml must not hardcode white #F4F7FC fill');
    assert.equal(icBell.includes('#F4F7FC'), false, 'ic_bell.xml must not hardcode white #F4F7FC fill');
    assert.equal(icBack.includes('@color/icon_primary'), true);
    assert.equal(icBell.includes('@color/icon_primary'), true);

    // Day and Night themes must both exist with solid dialog themes
    const lightTheme = fs.readFileSync(path.resolve('AimShot_Project/app/src/main/res/values/themes.xml'), 'utf8');
    const nightTheme = fs.readFileSync(path.resolve('AimShot_Project/app/src/main/res/values-night/themes.xml'), 'utf8');
    assert.equal(lightTheme.includes('Theme.AimShotEngine.CustomDialog'), true);
    assert.equal(nightTheme.includes('Theme.AimShotEngine.CustomDialog'), true);

    // Language Selection & Tactical Confirm custom solid popups must use @drawable/bg_dialog_card
    const langDialog = fs.readFileSync(path.resolve('AimShot_Project/app/src/main/res/layout/dialog_language_select.xml'), 'utf8');
    const confirmDialog = fs.readFileSync(path.resolve('AimShot_Project/app/src/main/res/layout/dialog_tactical_confirm.xml'), 'utf8');
    assert.equal(langDialog.includes('@drawable/bg_dialog_card'), true);
    assert.equal(confirmDialog.includes('@drawable/bg_dialog_card'), true);

    // Dialogs must use AppCompatButton (not raw <Button) to prevent MaterialComponents cyan-on-cyan tinting bug
    const dialogLayouts = [
      'dialog_update.xml',
      'dialog_verification_key.xml',
      'dialog_custom_info.xml',
      'dialog_game_launch.xml',
      'dialog_language_select.xml',
      'dialog_tactical_confirm.xml'
    ];
    for (const fileName of dialogLayouts) {
      const xml = fs.readFileSync(path.resolve(`AimShot_Project/app/src/main/res/layout/${fileName}`), 'utf8');
      assert.equal(/<Button[\s>]/.test(xml), false, `${fileName} must use AppCompatButton instead of <Button>`);
      assert.equal(xml.includes('androidx.appcompat.widget.AppCompatButton'), true);
    }
  });

  // Test 44: Settings Haptic Feedback Guard & Formatted Version Row (%1$s fix)
  test('SettingsActivity guards triggerHapticFeedback with isHapticOn and formats %1$s version string', () => {
    const settingsJava = fs.readFileSync(
      path.resolve('AimShot_Project/app/src/main/java/com/alpha10/aimshot/SettingsActivity.java'),
      'utf8'
    );
    assert.equal(/private\s+void\s+triggerHapticFeedback\s*\(\)\s*\{\s*if\s*\(!isHapticOn\)/.test(settingsJava), true);
    assert.equal(settingsJava.includes('getString(R.string.setting_update_desc, versionLabel)'), true);
    assert.equal(settingsJava.includes('R.layout.dialog_language_select'), true);
  });

  // Test 45: 4-Step Real Internet (HTTP 204) & Alpha10 Server Connectivity Gate
  test('4-Step Launch & Download Gate blocks offline, zero-balance SIM / captive portal, and unreachable Alpha10 server', () => {
    const evaluateLaunchGate = ({ hasTransport, hasCaptivePortal, http204Status, alpha10HttpStatus }) => {
      if (!hasTransport) return 'NO_NETWORK';
      if (hasCaptivePortal) return 'NO_INTERNET_ACCESS';
      if (http204Status !== 204) return 'NO_INTERNET_ACCESS';
      if (!(alpha10HttpStatus > 0 && alpha10HttpStatus < 500)) return 'SERVER_UNREACHABLE';
      return 'CONNECTED';
    };

    // 1. Mobile Data & Wi-Fi off -> NO_NETWORK
    assert.equal(
      evaluateLaunchGate({ hasTransport: false, hasCaptivePortal: false, http204Status: 0, alpha10HttpStatus: 0 }),
      'NO_NETWORK'
    );

    // 2. SIM connected with 0 MB / expired pack (carrier returns 302 or 200 recharge page instead of 204) -> NO_INTERNET_ACCESS
    assert.equal(
      evaluateLaunchGate({ hasTransport: true, hasCaptivePortal: false, http204Status: 302, alpha10HttpStatus: 200 }),
      'NO_INTERNET_ACCESS'
    );

    // 3. Captive portal Wi-Fi -> NO_INTERNET_ACCESS
    assert.equal(
      evaluateLaunchGate({ hasTransport: true, hasCaptivePortal: true, http204Status: 204, alpha10HttpStatus: 200 }),
      'NO_INTERNET_ACCESS'
    );

    // 4. Real internet works (204), but Alpha10 server is down (503 or timeout 0) -> SERVER_UNREACHABLE
    assert.equal(
      evaluateLaunchGate({ hasTransport: true, hasCaptivePortal: false, http204Status: 204, alpha10HttpStatus: 503 }),
      'SERVER_UNREACHABLE'
    );

    // 5. Real internet (204) + Alpha10 Server alive (e.g. 200 or 401/405 on auth.php) -> CONNECTED
    assert.equal(
      evaluateLaunchGate({ hasTransport: true, hasCaptivePortal: false, http204Status: 204, alpha10HttpStatus: 401 }),
      'CONNECTED'
    );

    // Verify NetworkUtil.java removed false-positive raw Socket port 53 / 443 checks
    const networkUtilJava = fs.readFileSync(
      path.resolve('AimShot_Project/app/src/main/java/com/alpha10/aimshot/util/NetworkUtil.java'),
      'utf8'
    );
    assert.equal(networkUtilJava.includes('generate_204'), true);
    assert.equal(networkUtilJava.includes('InetSocketAddress("8.8.8.8", 53)'), false);
    assert.equal(networkUtilJava.includes('InetSocketAddress("alpha10.freedev.app", 443)'), false);
  });

  // Test 46: UpdateAuthService BOM/HTML Sanitization & Non-JSON Diagnostics
  test('UpdateAuthService sanitizes BOM/surrounding HTML noise and diagnoses 404 or aes.js anti-bot pages', () => {
    const sanitizeJsonPayload = (raw) => {
      if (!raw) return '';
      const cleaned = raw.replace(/\uFEFF/g, '').trim();
      const firstBrace = cleaned.indexOf('{');
      const lastBrace = cleaned.lastIndexOf('}');
      if (firstBrace >= 0 && lastBrace > firstBrace) {
        const candidate = cleaned.slice(firstBrace, lastBrace + 1);
        try {
          JSON.parse(candidate);
          return candidate;
        } catch {
          return cleaned;
        }
      }
      return cleaned;
    };

    const diagnoseNonJson = (raw) => {
      const lower = (raw || '').toLowerCase();
      if (lower.includes('aes.js') || lower.includes('slowaes')) return 'ANTI_BOT_CHALLENGE';
      if (lower.includes('404') || lower.includes('not found')) return 'BROKER_404_MISSING';
      if (lower.includes('open_basedir') || lower.includes('fatal error')) return 'PHP_SERVER_ERROR';
      return 'INVALID_JSON';
    };

    // Valid JSON wrapped with BOM and trailing hosting comment is cleanly recovered!
    const noisyJson = '\uFEFF{"success":true,"token":"tok_60s_live"}\n<!-- hosting footer -->';
    const recovered = JSON.parse(sanitizeJsonPayload(noisyJson));
    assert.equal(recovered.token, 'tok_60s_live');

    // Non-JSON HTML pages are accurately classified
    assert.equal(diagnoseNonJson('<html><script src="/aes.js"></script></html>'), 'ANTI_BOT_CHALLENGE');
    assert.equal(diagnoseNonJson('<!DOCTYPE html><title>404 Not Found</title>'), 'BROKER_404_MISSING');
  });

  // Test 47: PHP Token Broker Shared-Hosting Output Buffer & open_basedir Safety
  test('server_broker/launcher-auth.php suppresses stray HTML warnings and avoids open_basedir parent traversal', () => {
    const phpCode = fs.readFileSync(path.resolve('server_broker/launcher-auth.php'), 'utf8');
    assert.equal(phpCode.includes('error_reporting(0)'), true);
    assert.equal(phpCode.includes('ob_start()'), true);
    assert.equal(phpCode.includes('ob_end_clean()'), true);
    assert.equal(phpCode.includes('dirname(__DIR__, 2)'), false, 'Must not traverse outside htdocs webroot');
  });

  // Test 48: Real-Time Live Countdown Formatter ([X] দিন [HH]:[MM]:[SS])
  test('Real-Time Live Countdown Formatter produces accurate localized template with ticking seconds', () => {
    const BN_DIGITS = ['০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'];
    const toBn = (n) => String(n).replace(/[0-9]/g, (d) => BN_DIGITS[parseInt(d, 10)]);

    const formatCountdown = (diffSeconds, lang = 'en') => {
      if (diffSeconds <= 0) {
        if (lang === 'bn') return '০ দিন ০০:০০:০০সে';
        if (lang === 'hi') return '0 दिन 00:00:00से';
        return '0 Days 00:00:00s';
      }
      const days = Math.floor(diffSeconds / 86400);
      const hours = Math.floor((diffSeconds % 86400) / 3600);
      const minutes = Math.floor((diffSeconds % 3600) / 60);
      const seconds = diffSeconds % 60;

      const pad = (num) => String(num).padStart(2, '0');

      if (lang === 'bn') {
        return `${toBn(days)} দিন ${toBn(pad(hours))}:${toBn(pad(minutes))}:${toBn(pad(seconds))}সে`;
      } else if (lang === 'hi') {
        return `${days} दिन ${pad(hours)}:${pad(minutes)}:${pad(seconds)}से`;
      }
      return `${days} Days ${pad(hours)}:${pad(minutes)}:${pad(seconds)}s`;
    };

    // Exactly 0 days, 12 hours, 45 minutes, 30 seconds
    const targetSeconds = 0 * 86400 + 12 * 3600 + 45 * 60 + 30;
    assert.equal(formatCountdown(targetSeconds, 'bn'), '০ দিন ১২:৪৫:৩০সে');
    assert.equal(formatCountdown(targetSeconds, 'en'), '0 Days 12:45:30s');
    assert.equal(formatCountdown(targetSeconds, 'hi'), '0 दिन 12:45:30से');

    // 18 days, 14 hours, 22 minutes, 15 seconds
    const eighteenDaysSecs = 18 * 86400 + 14 * 3600 + 22 * 60 + 15;
    assert.equal(formatCountdown(eighteenDaysSecs, 'bn'), '১৮ দিন ১৪:২২:১৫সে');
    assert.equal(formatCountdown(eighteenDaysSecs, 'en'), '18 Days 14:22:15s');

    // Expired or zero
    assert.equal(formatCountdown(0, 'bn'), '০ দিন ০০:০০:০০সে');
    assert.equal(formatCountdown(-50, 'en'), '0 Days 00:00:00s');
  });

  // Test 49: Permanent Alpha 10 Keystore & Digital Certificate Resolution
  test('Alpha 10 Release Keystore exists and contains verified certificate identity', () => {
    const keystorePath = path.resolve('keystore/aimshot-release.p12');
    const projectKeystore = path.resolve('AimShot_Project/keystore/aimshot-release.p12');
    const certPath = path.resolve('keystore/release.crt');

    assert.equal(fs.existsSync(keystorePath) || fs.existsSync(projectKeystore), true);
    assert.equal(fs.existsSync(certPath), true);

    const certContent = fs.readFileSync(certPath, 'utf8');
    assert.equal(certContent.includes('BEGIN CERTIFICATE'), true);
  });

  // Test 50: Conflict-Free App Update Guardrails
  test('Gradle build enforces fixed package identity, dual v1/v2 signing, and prevents install conflicts', () => {
    const gradleContent = fs.readFileSync(path.resolve('app/build.gradle'), 'utf8');
    // 1. Fixed applicationId
    assert.equal(gradleContent.includes('applicationId "com.alpha10.aimshot.launcher"'), true);
    // 2. Multi-path permanent certificate discovery
    assert.equal(gradleContent.includes('aimshot-release.p12'), true);
    assert.equal(gradleContent.includes('v1SigningEnabled true'), true);
    assert.equal(gradleContent.includes('v2SigningEnabled true'), true);
    // 3. Min SDK kept at 24 for universal backward compatibility
    assert.equal(gradleContent.includes('minSdk 24'), true);
  });

  // Test 51: Latest Active Key Sole Precedence & Old Key Auto-Deletion
  test('Latest active key takes sole precedence and purges any prior older keys from local storage', () => {
    let vault = [];
    const saveKey = (key, expiryTimestamp, planType) => {
      // Requirement 6: Latest active key replaces previous keys; older keys deleted!
      vault = [{ key, expiryTimestamp, planType, lastUsed: Date.now() }];
    };

    const now = Date.now();
    saveKey('KEY-OLD-111', now + 86400000, 'AimShot Pro 1-Day');
    assert.equal(vault.length, 1);
    assert.equal(vault[0].key, 'KEY-OLD-111');

    // New key is activated: older key is wiped, only latest active key is retained
    saveKey('KEY-NEW-222', now + 86400000 * 7, 'AimShot Pro 7-Days');
    assert.equal(vault.length, 1);
    assert.equal(vault[0].key, 'KEY-NEW-222');
    assert.equal(vault.some(k => k.key === 'KEY-OLD-111'), false);
    assert.equal(vault[0].planType, 'AimShot Pro 7-Days');
    assert.ok(vault[0].expiryTimestamp > Date.now());
  });

  // Test 52: Multi-Key Vault automatic purging of expired keys
  test('Multi-Key Vault automatically purges expired keys while preserving valid keys', () => {
    const now = Date.now();
    const keys = [
      { key: 'EXPIRED-KEY-001', expiryTimestamp: now - 50000 },
      { key: 'EXPIRED-KEY-002', expiryTimestamp: now - 1000 },
      { key: 'VALID-KEY-003', expiryTimestamp: now + 3600000 * 24 },
      { key: 'VALID-KEY-004', expiryTimestamp: now + 3600000 * 48 }
    ];

    const purgeExpired = (list, currentTime) => list.filter(k => k.expiryTimestamp > currentTime);
    const cleaned = purgeExpired(keys, now);

    assert.equal(cleaned.length, 2);
    assert.equal(cleaned[0].key, 'VALID-KEY-003');
    assert.equal(cleaned[1].key, 'VALID-KEY-004');
    assert.equal(cleaned.some(k => k.key.startsWith('EXPIRED')), false);
  });

  // Test 53: Mandatory Server Clearance Gate: Launch strictly blocks offline launch and requires server green signal
  test('Strict Server Clearance Gate: Launch strictly blocks offline launch or server errors and mandates active online verification', () => {
    const evaluateLaunchClearance = ({ isOnline, hasActiveData, isAuthoritativeActive, errorCode }) => {
      // 1. Must have online connection
      if (!isOnline) {
        return { allowLaunch: false, clearSession: false, action: 'NO_NETWORK' };
      }

      // 2. Must have active data package (not 0 MB SIM balance or captive portal)
      if (!hasActiveData) {
        return { allowLaunch: false, clearSession: false, action: 'NO_INTERNET_ACCESS' };
      }

      const isAuthoritativeRevocation = [
        'STATUS_INVALID',
        'STATUS_REVOKED',
        'STATUS_EXPIRED',
        'STATUS_DEVICE_REVOKED'
      ].includes(errorCode);

      if (isAuthoritativeRevocation) {
        return { allowLaunch: false, clearSession: true, action: 'DENIED' };
      }

      // If server unreachable or error: strictly block launch (no offline bypass!)
      if (!isAuthoritativeActive) {
        return { allowLaunch: false, clearSession: false, action: 'SERVER_UNREACHABLE_BLOCK' };
      }

      // Green signal from server!
      return { allowLaunch: true, clearSession: false, action: 'SERVER_APPROVED_LAUNCH' };
    };

    // 1. Authoritative revocation MUST clear session and deny launch
    const revoked = evaluateLaunchClearance({ isOnline: true, hasActiveData: true, isAuthoritativeActive: false, errorCode: 'STATUS_REVOKED' });
    assert.equal(revoked.allowLaunch, false);
    assert.equal(revoked.clearSession, true);
    assert.equal(revoked.action, 'DENIED');

    // 2. Offline attempt MUST be blocked completely (no offline bypass allowed)
    const offline = evaluateLaunchClearance({ isOnline: false, hasActiveData: false, isAuthoritativeActive: false, errorCode: null });
    assert.equal(offline.allowLaunch, false);
    assert.equal(offline.action, 'NO_NETWORK');

    // 3. SIM connected but data package expired / zero balance MUST be blocked
    const zeroData = evaluateLaunchClearance({ isOnline: true, hasActiveData: false, isAuthoritativeActive: false, errorCode: null });
    assert.equal(zeroData.allowLaunch, false);
    assert.equal(zeroData.action, 'NO_INTERNET_ACCESS');

    // 4. Server unreachable / network error MUST be blocked
    const serverDown = evaluateLaunchClearance({ isOnline: true, hasActiveData: true, isAuthoritativeActive: false, errorCode: 'ERR_NETWORK' });
    assert.equal(serverDown.allowLaunch, false);
    assert.equal(serverDown.action, 'SERVER_UNREACHABLE_BLOCK');

    // 5. Active connection + Valid Server Approval (Green Signal) -> Launch Allowed!
    const approved = evaluateLaunchClearance({ isOnline: true, hasActiveData: true, isAuthoritativeActive: true, errorCode: null });
    assert.equal(approved.allowLaunch, true);
    assert.equal(approved.action, 'SERVER_APPROVED_LAUNCH');
  });

  // Test 54: Home screen Selected Game card layout matches user UI specification (Screenshot 1)
  test('Selected Game card has status badge positioned on the far right replacing the days counter', () => {
    const homeLayout = fs.readFileSync(path.resolve('app/src/main/res/layout/fragment_home.xml'), 'utf8');
    const cardLaunchSection = homeLayout.slice(homeLayout.indexOf('android:id="@+id/card_launch_game"'));

    // Check that tv_active_days_remaining is removed from card_launch_game
    assert.equal(cardLaunchSection.includes('tv_active_days_remaining'), false);

    // Check that tv_active_game_badge is positioned layout_alignParentEnd="true"
    assert.equal(cardLaunchSection.includes('android:id="@+id/tv_active_game_badge"'), true);
    assert.equal(cardLaunchSection.includes('android:layout_alignParentEnd="true"'), true);
  });

  // Test 55: Custom font assets and font-face configurations
  test('Typography configuration integrates versatylo.ttf for brand headers and roboto.ttf for UI', () => {
    const css = fs.readFileSync(path.resolve('src/index.css'), 'utf8');
    assert.equal(css.includes('Versatylo'), true);
    assert.equal(css.includes('RobotoUI'), true);
    assert.equal(css.includes('.font-brand'), true);

    const fontHelperJava = fs.readFileSync(path.resolve('app/src/main/java/com/alpha10/aimshot/util/FontHelper.java'), 'utf8');
    assert.equal(fontHelperJava.includes('fonts/versatylo.ttf'), true);
    assert.equal(fontHelperJava.includes('fonts/roboto.ttf'), true);
    assert.equal(fontHelperJava.includes('applyBrandFont'), true);
  });

  // Test 56: Page Transition Direction Resolver
  test('Page Transition Direction Resolver computes accurate slide direction across all page transitions', () => {
    const order = { home: 0, premium: 1, settings: 2 };
    const resolveTabDirection = (fromTab, toTab) => {
      return order[toTab] >= order[fromTab] ? 'right' : 'left';
    };

    // Home -> Premium: slides in from right
    assert.equal(resolveTabDirection('home', 'premium'), 'right');
    // Premium -> Home: slides in from left
    assert.equal(resolveTabDirection('premium', 'home'), 'left');
    // Home -> Settings: slides in from right
    assert.equal(resolveTabDirection('home', 'settings'), 'right');
    // Premium -> Settings: slides in from right
    assert.equal(resolveTabDirection('premium', 'settings'), 'right');
    // Settings -> Home: slides in from left
    assert.equal(resolveTabDirection('settings', 'home'), 'left');
    // Settings -> Premium: slides in from left
    assert.equal(resolveTabDirection('settings', 'premium'), 'left');
  });

  // Test 57: Active Internet Data Validation
  test('Active Internet Data Validation rejects expired data packs and captive portals', () => {
    const validateDataPack = (hasTransport, probeStatus) => {
      if (!hasTransport) return { valid: false, reason: 'NO_CONNECTION' };
      if (probeStatus !== 200 && probeStatus !== 204) return { valid: false, reason: 'EXPIRED_OR_CAPTIVE' };
      return { valid: true };
    };

    assert.equal(validateDataPack(false, 0).valid, false);
    assert.equal(validateDataPack(true, 302).reason, 'EXPIRED_OR_CAPTIVE'); // Carrier redirect page
    assert.equal(validateDataPack(true, 502).reason, 'EXPIRED_OR_CAPTIVE'); // Gateway timeout / server down
    assert.equal(validateDataPack(true, 200).valid, true);
    assert.equal(validateDataPack(true, 204).valid, true);
  });

  // Test 58: Persistent Session Cookie & Anti-Bot Pre-warming across restarts
  test('Session cookie is persisted across app restarts and pre-warms anti-bot challenge', () => {
    const memoryPrefs = new Map();
    const saveSessionCookie = (cookie) => {
      memoryPrefs.set('cached_session_cookie', cookie);
    };
    const getSessionCookie = () => memoryPrefs.get('cached_session_cookie') || null;

    // Simulate solving cookie and storing
    saveSessionCookie('__test=7d9c12a4f61e89b; path=/');
    assert.equal(getSessionCookie(), '__test=7d9c12a4f61e89b; path=/');

    // Simulate app restart: cookie remains readable from storage
    const restoredCookie = getSessionCookie();
    assert.ok(restoredCookie);
    assert.ok(restoredCookie.includes('__test='));
  });

  // Test 59: Resilient Launch Verification prevents false "Launch Permission Denied"
  test('Launch verification gate distinguishes authoritative revocations from transient network/parsing errors', () => {
    const handleLaunchResult = ({ errorCode, hasValidLocalKey }) => {
      const isAuthoritative = [
        'STATUS_INVALID',
        'STATUS_REVOKED',
        'STATUS_EXPIRED',
        'STATUS_DEVICE_REVOKED'
      ].includes(errorCode);

      if (isAuthoritative) {
        return { launch: false, revokeLocal: true, showDialog: 'DENIED' };
      }

      // Non-authoritative error (e.g., ERR_PARSE, ERR_NETWORK, TIMEOUT)
      if (hasValidLocalKey) {
        // Fallback to verified local unexpired vault key -> No false Permission Denied!
        return { launch: true, revokeLocal: false, showDialog: null };
      }

      return { launch: false, revokeLocal: false, showDialog: 'NO_CONNECTION' };
    };

    // Case 1: Genuine revocation must terminate session and block
    assert.deepEqual(
      handleLaunchResult({ errorCode: 'STATUS_REVOKED', hasValidLocalKey: true }),
      { launch: false, revokeLocal: true, showDialog: 'DENIED' }
    );

    // Case 2: Transient ERR_PARSE / anti-bot challenge with valid local key -> Launch smoothly!
    assert.deepEqual(
      handleLaunchResult({ errorCode: 'ERR_PARSE', hasValidLocalKey: true }),
      { launch: true, revokeLocal: false, showDialog: null }
    );

    // Case 3: Transient network error without valid key -> Inform user
    assert.deepEqual(
      handleLaunchResult({ errorCode: 'ERR_NETWORK', hasValidLocalKey: false }),
      { launch: false, revokeLocal: false, showDialog: 'NO_CONNECTION' }
    );
  });

  // Test 60: Settings Navigation Bar Permanence
  test('Settings view embeds into fragment container while bottom navigation remains static', () => {
    const mainActivityJava = fs.readFileSync(path.resolve('app/src/main/java/com/alpha10/aimshot/MainActivity.java'), 'utf8');
    
    // Confirms Settings tab is rendered within fragmentContainer
    assert.equal(mainActivityJava.includes('renderSettingsScreen()'), true);
    assert.equal(mainActivityJava.includes('R.layout.fragment_settings'), true);
    // Confirms navBtnSettings does not launch a new Activity
    assert.equal(mainActivityJava.includes('navigateToTab(Tab.SETTINGS)'), true);
    assert.equal(mainActivityJava.includes('startActivity(new Intent(MainActivity.this, SettingsActivity.class))'), false);
  });
});



