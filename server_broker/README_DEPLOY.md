# Alpha10 Server-Side Token Broker (`launcher-auth.php`) — ডিপ্লয়মেন্ট নির্দেশিকা

Android APK-এর ভেতরে **৬৪ ক্যারেক্টারের Secret App Key (`X-App-Key`)** রাখা সম্পূর্ণ নিষিদ্ধ ও অনিরাপদ (APK Decompile করলেই যে কেউ বের করতে পারে)। তাই বিদ্যমান `auth.php` এবং `aimshot-download.php` অপরিবর্তিত রেখে নতুন একটি **Server-Side Token Broker (`launcher-auth.php`)** তৈরি করা হয়েছে।

## ১. ফাইল আপলোড লোকেশন
এই ফোল্ডারের `launcher-auth.php` ফাইলটি আপনার Alpha10 সার্ভারে নিচের পাথে আপলোড করুন:

```text
https://alpha10.freedev.app/api/launcher-auth.php
```

> **গুরুত্বপূর্ণ:** বিদ্যমান `/api/auth.php`, `/api/aimshot-download.php`, ডাটাবেস বা `.htaccess` ফাইলে কোনো পরিবর্তনের প্রয়োজন নেই।

## ২. সার্ভার এনভায়রনমেন্ট ভেরিয়েবল (Environment Variables)
সার্ভারের `.env`, Apache/Nginx/LiteSpeed config অথবা PHP config-এ (পাবলিক ওয়েব রুটের বাইরে) নিচের মানগুলো সেট করুন:

| Variable Name | আবশ্যকতা | বিবরণ |
| :--- | :--- | :--- |
| `ALPHA10_SECRET_APP_KEY` | **আবশ্যক (Required)** | আপনার বিদ্যমান `auth.php`-তে ব্যবহৃত আসল ৬৪ ক্যারেক্টারের Secret App Key। এটি কখনোই ক্লায়েন্টে পাঠানো হয় না। |
| `ALPHA10_PROTECTED_APK_PATH` | প্রস্তাবিত (Recommended) | সার্ভারে রাখা আসল `Main AimShot Engine (com.alpha10.aimshot)` APK ফাইলের লোকাল পাথ। এটি দিলে সার্ভার স্বয়ংক্রিয়ভাবে আসল SHA-256 ও File Size গণনা করে পাঠাবে। |
| `ALPHA10_MAIN_APK_SHA256` | বিকল্প (Fallback) | `ALPHA10_PROTECTED_APK_PATH` না দিলে আসল Main APK-এর ৬৪ ক্যারেক্টারের SHA-256 হ্যাশ এখানে দিন। (দুটির একটি অবশ্যই দিতে হবে, নতুবা Launcher নিরাপত্তার স্বার্থে APK ইনস্টল ব্লক করবে)। |
| `ALPHA10_MAIN_CERT_SHA256` | প্রস্তাবিত (Recommended) | Main APK যেই Release Keystore দিয়ে সাইন করা হয়েছে তার SHA-256 Certificate Fingerprint (`XX:XX:...`)। না দিলে ডিফল্ট স্থায়ী Release Fingerprint ব্যবহৃত হবে। |
| `ALPHA10_MAIN_VERSION_CODE` | ঐচ্ছিক | Main APK-এর `versionCode` (যেমন `2`)। |
| `ALPHA10_MAIN_VERSION_NAME` | ঐচ্ছিক | Main APK-এর `versionName` (যেমন `1.0.1`)। |
| `ALPHA10_REQUIRE_PLAY_INTEGRITY` | ঐচ্ছিক | Google Play Console-এ Play Integrity API লিংক করার পর `true` করুন এবং `ALPHA10_GCP_SERVICE_ACCOUNT_JSON` যুক্ত করুন। |

## ৩. কাজের প্রবাহ (End-to-End Flow)
1. **AimShot Launcher (`com.alpha10.aimshot.launcher`)** কোনো গোপন কী ছাড়াই `POST https://alpha10.freedev.app/api/launcher-auth.php`-এ JSON পেলোড (`launcher_package`, `target_package`, `device_id`, `license_key`, `integrity_token`) পাঠায়।
2. **`launcher-auth.php`** রিকোয়েস্ট যাচাই করে সার্ভারের ভেতর থেকেই `X-App-Key` সহ `POST https://alpha10.freedev.app/api/auth.php` কল করে।
3. `auth.php` থেকে প্রাপ্ত ৬০ সেকেন্ডের একবার ব্যবহারযোগ্য টোকেন এবং আসল `expected_sha256` ও `trusted_cert_sha256` Launcher-এ ফেরত দেয়।
4. **AimShot Launcher** সেই টোকেন দিয়ে `GET https://alpha10.freedev.app/api/aimshot-download.php` (`Authorization: Bearer <TOKEN>`) কল করে Private Internal Storage-এ APK ডাউনলোড, যাচাই ও ইনস্টল করে।
