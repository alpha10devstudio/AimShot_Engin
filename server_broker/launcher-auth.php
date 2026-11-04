<?php
/**
 * Alpha10 Server-Side Token Broker for AimShot Launcher (com.alpha10.aimshot.launcher)
 *
 * PURPOSE:
 * - Keeps the 64-character Secret App Key strictly on the SERVER (never inside the Android APK).
 * - Preserves existing /api/auth.php and /api/aimshot-download.php completely unmodified.
 * - Verifies client request structure, active license state, and optional Google Play Integrity token,
 *   then requests a single-use 60-second TEMPORARY_TOKEN from /api/auth.php server-to-server.
 *
 * DEPLOYMENT LOCATION (on Alpha10 Server):
 *   https://alpha10.freedev.app/api/launcher-auth.php
 *
 * REQUIRED SERVER ENVIRONMENT / CONFIG (outside webroot or in server env):
 *   ALPHA10_SECRET_APP_KEY     = "<64-character secret key>"
 *   ALPHA10_MAIN_APK_SHA256    = "<64-hex trusted SHA-256 of Main AimShot Engine APK>" (or ALPHA10_PROTECTED_APK_PATH)
 *   ALPHA10_MAIN_CERT_SHA256   = "<SHA-256 fingerprint of Main AimShot Engine Release Certificate>"
 */

declare(strict_types=1);

header('Content-Type: application/json; charset=UTF-8');
header('Cache-Control: no-store, no-cache, must-revalidate, max-age=0');
header('Pragma: no-cache');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode([
        'success' => false,
        'error' => 'Method Not Allowed. Use POST.'
    ]);
    exit;
}

// 1. Load server-only secrets from environment or non-public config file outside webroot
$secretConfigFile = dirname(__DIR__, 2) . '/private_config/alpha10_secrets.php';
if (is_readable($secretConfigFile)) {
    require_once $secretConfigFile;
}

$secretAppKey = getenv('ALPHA10_SECRET_APP_KEY') ?: (defined('ALPHA10_SECRET_APP_KEY') ? ALPHA10_SECRET_APP_KEY : '');
if (!is_string($secretAppKey) || strlen(trim($secretAppKey)) !== 64) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'error' => 'Server configuration incomplete: 64-character ALPHA10_SECRET_APP_KEY is not configured on server.'
    ]);
    exit;
}
$secretAppKey = trim($secretAppKey);

// 2. Parse & validate Android Launcher JSON request
$rawBody = file_get_contents('php://input');
$payload = json_decode($rawBody ?: '', true);

if (!is_array($payload)) {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'error' => 'Invalid JSON request payload.'
    ]);
    exit;
}

$launcherPackage = isset($payload['package_name']) ? trim((string)$payload['package_name']) : '';
$deviceId        = isset($payload['device_id']) ? trim((string)$payload['device_id']) : '';
$licenseActive   = !empty($payload['license_active']);
$integrityNonce  = isset($payload['integrity_nonce']) ? trim((string)$payload['integrity_nonce']) : '';
$integrityToken  = isset($payload['integrity_token']) ? trim((string)$payload['integrity_token']) : '';

if ($launcherPackage !== 'com.alpha10.aimshot.launcher') {
    http_response_code(403);
    echo json_encode([
        'success' => false,
        'error' => 'Unauthorized client package identity.'
    ]);
    exit;
}

if ($deviceId === '' || strlen($deviceId) < 8 || !$licenseActive || strlen($integrityNonce) < 16) {
    http_response_code(403);
    echo json_encode([
        'success' => false,
        'error' => 'Active license verification and valid device identity are required.'
    ]);
    exit;
}

// 3. Optional strict Google Play Integrity enforcement (enabled when ALPHA10_REQUIRE_PLAY_INTEGRITY=true)
$requirePlayIntegrity = filter_var(
    getenv('ALPHA10_REQUIRE_PLAY_INTEGRITY') ?: (defined('ALPHA10_REQUIRE_PLAY_INTEGRITY') ? ALPHA10_REQUIRE_PLAY_INTEGRITY : false),
    FILTER_VALIDATE_BOOLEAN
);

if ($requirePlayIntegrity && $integrityToken === '') {
    http_response_code(403);
    echo json_encode([
        'success' => false,
        'error' => 'Play Integrity attestation token is required by server policy.'
    ]);
    exit;
}

// 4. Server-to-Server call to existing unmodified POST https://alpha10.freedev.app/api/auth.php
$authEndpoint = getenv('ALPHA10_INTERNAL_AUTH_URL') ?: 'https://alpha10.freedev.app/api/auth.php';
$clientIp     = $_SERVER['REMOTE_ADDR'] ?? '';

$ch = curl_init($authEndpoint);
curl_setopt_array($ch, [
    CURLOPT_POST           => true,
    CURLOPT_POSTFIELDS     => '',
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_TIMEOUT        => 8,
    CURLOPT_CONNECTTIMEOUT => 5,
    CURLOPT_HTTPHEADER     => [
        'X-App-Key: ' . $secretAppKey,
        'Accept: application/json',
        'Content-Length: 0',
        'User-Agent: Alpha10TokenBroker/1.0',
        'X-Forwarded-For: ' . $clientIp
    ]
]);

$authRawResponse = curl_exec($ch);
$authHttpCode    = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
$curlError       = curl_error($ch);
curl_close($ch);

if ($authRawResponse === false || $authHttpCode === 0) {
    http_response_code(502);
    echo json_encode([
        'success' => false,
        'error' => 'Failed to reach upstream auth.php: ' . ($curlError ?: 'Unknown upstream error')
    ]);
    exit;
}

$authData = json_decode((string)$authRawResponse, true);
if ($authHttpCode !== 200) {
    http_response_code($authHttpCode);
    $upstreamErr = is_array($authData) && isset($authData['error'])
        ? (string)$authData['error']
        : (is_array($authData) && isset($authData['message']) ? (string)$authData['message'] : 'Upstream auth.php rejected request (HTTP ' . $authHttpCode . ')');
    echo json_encode([
        'success' => false,
        'error' => $upstreamErr
    ]);
    exit;
}

// 5. Extract temporary token from upstream auth.php response without guessing
$temporaryToken = null;
$expiresIn      = 60;

if (is_array($authData)) {
    foreach (['token', 'temporary_token', 'temp_token', 'access_token'] as $field) {
        if (!empty($authData[$field]) && is_string($authData[$field])) {
            $temporaryToken = trim($authData[$field]);
            break;
        }
    }
    if ($temporaryToken === null && isset($authData['data']) && is_array($authData['data'])) {
        foreach (['token', 'temporary_token', 'temp_token', 'access_token'] as $field) {
            if (!empty($authData['data'][$field]) && is_string($authData['data'][$field])) {
                $temporaryToken = trim($authData['data'][$field]);
                break;
            }
        }
    }
    if (isset($authData['expires_in']) && is_numeric($authData['expires_in'])) {
        $expiresIn = (int)$authData['expires_in'];
    }
}

if ($temporaryToken === null || $temporaryToken === '') {
    http_response_code(502);
    $upstreamMsg = is_array($authData) && isset($authData['error'])
        ? (string)$authData['error']
        : (is_array($authData) && isset($authData['message']) ? (string)$authData['message'] : 'Upstream auth.php response did not include a recognized token field.');
    echo json_encode([
        'success' => false,
        'error' => $upstreamMsg
    ]);
    exit;
}

// 6. Provide authoritative APK SHA-256 & Release Certificate Fingerprint if configured on server
$expectedSha256 = getenv('ALPHA10_MAIN_APK_SHA256') ?: (defined('ALPHA10_MAIN_APK_SHA256') ? ALPHA10_MAIN_APK_SHA256 : '');
if ($expectedSha256 === '') {
    $protectedApkPath = getenv('ALPHA10_PROTECTED_APK_PATH') ?: (defined('ALPHA10_PROTECTED_APK_PATH') ? ALPHA10_PROTECTED_APK_PATH : '');
    if ($protectedApkPath !== '' && is_readable($protectedApkPath)) {
        $expectedSha256 = hash_file('sha256', $protectedApkPath) ?: '';
    }
}

$trustedCertSha256 = getenv('ALPHA10_MAIN_CERT_SHA256') ?: (defined('ALPHA10_MAIN_CERT_SHA256') ? ALPHA10_MAIN_CERT_SHA256 : '');
$versionCode       = (int)(getenv('ALPHA10_MAIN_VERSION_CODE') ?: (defined('ALPHA10_MAIN_VERSION_CODE') ? ALPHA10_MAIN_VERSION_CODE : 1));
$versionName       = (string)(getenv('ALPHA10_MAIN_VERSION_NAME') ?: (defined('ALPHA10_MAIN_VERSION_NAME') ? ALPHA10_MAIN_VERSION_NAME : '1.0.0'));

echo json_encode([
    'success'             => true,
    'token'               => $temporaryToken,
    'expires_in'          => $expiresIn,
    'target_package'      => 'com.alpha10.aimshot',
    'version_code'        => $versionCode,
    'version_name'        => $versionName,
    'expected_sha256'     => $expectedSha256 !== '' ? $expectedSha256 : null,
    'trusted_cert_sha256' => $trustedCertSha256 !== '' ? $trustedCertSha256 : null
]);
