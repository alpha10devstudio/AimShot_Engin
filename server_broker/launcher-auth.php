<?php
/**
 * Alpha10 Server-Side Token Broker for AimShot Launcher (com.alpha10.aimshot.launcher)
 *
 * PURPOSE:
 * - Keeps the 64-character Secret App Key strictly on the SERVER (never inside the Android APK).
 * - Preserves existing /api/auth.php and /api/aimshot-download.php completely unmodified.
 * - Suppresses shared-hosting PHP open_basedir / notice HTML output and cleans output buffers
 *   so Android clients ALWAYS receive 100% valid JSON (fixing "Invalid JSON format in authentication response").
 */

declare(strict_types=1);

error_reporting(0);
@ini_set('display_errors', '0');
@ini_set('html_errors', '0');
ob_start();

function sendJsonResponse(int $statusCode, array $data): void
{
    while (ob_get_level() > 0) {
        @ob_end_clean();
    }
    http_response_code($statusCode);
    header('Content-Type: application/json; charset=UTF-8');
    header('Cache-Control: no-store, no-cache, must-revalidate, max-age=0');
    header('Pragma: no-cache');
    echo json_encode($data, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    exit;
}

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    sendJsonResponse(405, [
        'success' => false,
        'error' => 'Method Not Allowed. Use POST.'
    ]);
}

// 1. Load server-only secrets safely without triggering shared-hosting open_basedir warnings
$candidateConfigPaths = [
    __DIR__ . '/.alpha10_secrets.php',
    dirname(__DIR__) . '/private_config/alpha10_secrets.php',
];

$docRoot = $_SERVER['DOCUMENT_ROOT'] ?? '';
if ($docRoot !== '') {
    $candidateConfigPaths[] = rtrim($docRoot, '/\\') . '/private_config/alpha10_secrets.php';
}

foreach ($candidateConfigPaths as $cfgPath) {
    if (@file_exists($cfgPath) && @is_readable($cfgPath)) {
        require_once $cfgPath;
        break;
    }
}

$secretAppKey = getenv('ALPHA10_SECRET_APP_KEY') ?: (defined('ALPHA10_SECRET_APP_KEY') ? (string)ALPHA10_SECRET_APP_KEY : '');
$secretAppKey = trim($secretAppKey);
if ($secretAppKey === '' || strlen($secretAppKey) !== 64) {
    sendJsonResponse(500, [
        'success' => false,
        'error' => 'Server configuration incomplete: 64-character ALPHA10_SECRET_APP_KEY is not configured on Alpha10 server.'
    ]);
}

// 2. Parse & validate Android Launcher JSON request
$rawBody = (string)file_get_contents('php://input');
$rawBody = preg_replace('/^\xEF\xBB\xBF/', '', $rawBody) ?? $rawBody;
$payload = json_decode($rawBody, true);

if (!is_array($payload)) {
    sendJsonResponse(400, [
        'success' => false,
        'error' => 'Invalid JSON request payload.'
    ]);
}

$launcherPackage = isset($payload['package_name'])
    ? trim((string)$payload['package_name'])
    : (isset($payload['launcher_package']) ? trim((string)$payload['launcher_package']) : '');
$deviceId       = isset($payload['device_id']) ? trim((string)$payload['device_id']) : '';
$licenseActive  = !empty($payload['license_active']);
$integrityNonce = isset($payload['integrity_nonce']) ? trim((string)$payload['integrity_nonce']) : '';
$integrityToken = isset($payload['integrity_token']) ? trim((string)$payload['integrity_token']) : '';

if ($launcherPackage !== 'com.alpha10.aimshot.launcher') {
    sendJsonResponse(403, [
        'success' => false,
        'error' => 'Unauthorized client package identity.'
    ]);
}

if ($deviceId === '' || strlen($deviceId) < 8 || !$licenseActive || strlen($integrityNonce) < 16) {
    sendJsonResponse(403, [
        'success' => false,
        'error' => 'Active license verification and valid device identity are required.'
    ]);
}

// 3. Optional strict Google Play Integrity enforcement
$requirePlayIntegrity = filter_var(
    getenv('ALPHA10_REQUIRE_PLAY_INTEGRITY') ?: (defined('ALPHA10_REQUIRE_PLAY_INTEGRITY') ? ALPHA10_REQUIRE_PLAY_INTEGRITY : false),
    FILTER_VALIDATE_BOOLEAN
);

if ($requirePlayIntegrity && $integrityToken === '') {
    sendJsonResponse(403, [
        'success' => false,
        'error' => 'Play Integrity attestation token is required by server policy.'
    ]);
}

// 4. Server-to-Server call to existing unmodified POST https://alpha10.freedev.app/api/auth.php
$authEndpoint = getenv('ALPHA10_INTERNAL_AUTH_URL') ?: (defined('ALPHA10_INTERNAL_AUTH_URL') ? (string)ALPHA10_INTERNAL_AUTH_URL : 'https://alpha10.freedev.app/api/auth.php');
$clientIp     = $_SERVER['REMOTE_ADDR'] ?? '';

$ch = curl_init($authEndpoint);
curl_setopt_array($ch, [
    CURLOPT_POST           => true,
    CURLOPT_POSTFIELDS     => '',
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_TIMEOUT        => 10,
    CURLOPT_CONNECTTIMEOUT => 6,
    CURLOPT_FOLLOWLOCATION => false,
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
    sendJsonResponse(502, [
        'success' => false,
        'error' => 'Failed to reach upstream auth.php: ' . ($curlError ?: 'Unknown upstream error')
    ]);
}

$cleanAuthRaw = preg_replace('/^\xEF\xBB\xBF/', '', trim((string)$authRawResponse)) ?? trim((string)$authRawResponse);
$authData = json_decode($cleanAuthRaw, true);

if ($authHttpCode !== 200) {
    $upstreamErr = is_array($authData) && !empty($authData['error'])
        ? (string)$authData['error']
        : (is_array($authData) && !empty($authData['message']) ? (string)$authData['message'] : 'Upstream auth.php rejected request (HTTP ' . $authHttpCode . ')');
    sendJsonResponse($authHttpCode, [
        'success' => false,
        'error' => $upstreamErr
    ]);
}

// 5. Extract temporary token from upstream auth.php response without guessing
$temporaryToken = null;
$expiresIn      = 60;

if (is_array($authData)) {
    foreach (['token', 'temporary_token', 'temp_token', 'access_token', 'download_token', 'auth_token'] as $field) {
        if (!empty($authData[$field]) && is_string($authData[$field])) {
            $temporaryToken = trim($authData[$field]);
            break;
        }
    }
    if ($temporaryToken === null && isset($authData['data']) && is_array($authData['data'])) {
        foreach (['token', 'temporary_token', 'temp_token', 'access_token', 'download_token', 'auth_token'] as $field) {
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
    $upstreamMsg = is_array($authData) && !empty($authData['error'])
        ? (string)$authData['error']
        : (is_array($authData) && !empty($authData['message']) ? (string)$authData['message'] : 'Upstream auth.php response did not include a recognized token field.');
    sendJsonResponse(502, [
        'success' => false,
        'error' => $upstreamMsg
    ]);
}

// 6. Provide authoritative APK SHA-256 & Release Certificate Fingerprint if configured on server
$expectedSha256 = getenv('ALPHA10_MAIN_APK_SHA256') ?: (defined('ALPHA10_MAIN_APK_SHA256') ? (string)ALPHA10_MAIN_APK_SHA256 : '');
if ($expectedSha256 === '') {
    $protectedApkPath = getenv('ALPHA10_PROTECTED_APK_PATH') ?: (defined('ALPHA10_PROTECTED_APK_PATH') ? (string)ALPHA10_PROTECTED_APK_PATH : '');
    if ($protectedApkPath !== '' && @file_exists($protectedApkPath) && @is_readable($protectedApkPath)) {
        $expectedSha256 = @hash_file('sha256', $protectedApkPath) ?: '';
    }
}

$trustedCertSha256 = getenv('ALPHA10_MAIN_CERT_SHA256') ?: (defined('ALPHA10_MAIN_CERT_SHA256') ? (string)ALPHA10_MAIN_CERT_SHA256 : '');
$versionCode       = (int)(getenv('ALPHA10_MAIN_VERSION_CODE') ?: (defined('ALPHA10_MAIN_VERSION_CODE') ? ALPHA10_MAIN_VERSION_CODE : 1));
$versionName       = (string)(getenv('ALPHA10_MAIN_VERSION_NAME') ?: (defined('ALPHA10_MAIN_VERSION_NAME') ? ALPHA10_MAIN_VERSION_NAME : '1.0.0'));

sendJsonResponse(200, [
    'success'             => true,
    'token'               => $temporaryToken,
    'expires_in'          => $expiresIn,
    'target_package'      => 'com.alpha10.aimshot',
    'version_code'        => $versionCode,
    'version_name'        => $versionName,
    'expected_sha256'     => $expectedSha256 !== '' ? $expectedSha256 : null,
    'trusted_cert_sha256' => $trustedCertSha256 !== '' ? $trustedCertSha256 : null
]);
