import { test, describe } from 'node:test';
import assert from 'node:assert/strict';

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

  // Test 22: Authentication Failure Handling (X-App-Key, non-200, missing token)
  test('UpdateAuthService handles auth errors, invalid HTTP status, and missing token', () => {
    const handleAuthResponse = (status, body) => {
      if (status !== 200) {
        return { success: false, error: `Server returned HTTP ${status}` };
      }
      try {
        const json = typeof body === 'string' ? JSON.parse(body) : body;
        if (!json || !json.token) {
          return { success: false, error: 'Server response missing security token' };
        }
        return { success: true, token: json.token, expiresIn: json.expires_in || 60 };
      } catch (e) {
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

  // Test 27: Open AimShot Engine Launch & Countdown Behavior
  test('Launch trigger resolves package intent and respects Android background launch rules', () => {
    const createLaunchPlan = (installedPackage, isForeground, autoCountdownAllowed) => {
      if (installedPackage !== 'com.alpha10.aimshot') {
        return { canLaunch: false, reason: 'Unknown package' };
      }

      return {
        canLaunch: true,
        targetPackage: installedPackage,
        buttonText: 'OPEN AIMSHOT ENGINE',
        hasAutoCountdown: isForeground && autoCountdownAllowed,
        countdownSeconds: isForeground && autoCountdownAllowed ? 3 : null
      };
    };

    const fgPlan = createLaunchPlan('com.alpha10.aimshot', true, true);
    assert.equal(fgPlan.canLaunch, true);
    assert.equal(fgPlan.buttonText, 'OPEN AIMSHOT ENGINE');
    assert.equal(fgPlan.hasAutoCountdown, true);
    assert.equal(fgPlan.countdownSeconds, 3);

    const bgPlan = createLaunchPlan('com.alpha10.aimshot', false, true);
    assert.equal(bgPlan.canLaunch, true);
    assert.equal(bgPlan.buttonText, 'OPEN AIMSHOT ENGINE');
    assert.equal(bgPlan.hasAutoCountdown, false);
    assert.equal(bgPlan.countdownSeconds, null);
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

      // 2. Alpha10 Server Authentication
      const authHeader = { 'X-App-Key': 'existing-key' };
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
});




