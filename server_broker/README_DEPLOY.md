# Alpha10 Server-Side Token Broker (`launcher-auth.php`) — ডিপ্লয়মেন্ট নির্দেশিকা

Android APK-এর ভেতরে **৬৪ ক্যারেক্টারের Secret App Key (`X-App-Key`)** রাখা সম্পূর্ণ নিষিদ্ধ ও অনিরাপদ (APK Decompile করলেই যে কেউ বের করতে পারে)। তাই বিদ্যমান `auth.php` এবং `aimshot-download.php` অপরিবর্তিত রেখে নতুন একটি **Server-Side Token Broker (`launcher-auth.php`)** তৈরি করা হয়েছে।

## ১. ফাইল আপলোড লোকেশন
এই ফোল্ডারের আপডেটেড `launcher-auth.php` ফাইলটি আপনার Alpha10 সার্ভারে নিচের পাথে আপলোড করুন:

```text
https://alpha10.freedev.app/api/launcher-auth.php
```

> **গুরুত্বপূর্ণ:** বিদ্যমান `/api/auth.php`, `/api/aimshot-download.php`, ডাটাবেস বা মূল ডাউনলোড লজিকে কোনো পরিবর্তনের প্রয়োজন নেই।
> শেয়ার্ড PHP হোস্টিং (`freedev.app`) এর `open_basedir` সীমাবদ্ধতা এবং HTML Warning/BOM যাতে JSON রেসপন্স নষ্ট করতে না পারে, সেজন্য `launcher-auth.php`-তে `error_reporting(0)`, `ob_start()` এবং পরিষ্কার বাফার আউটপুট যুক্ত করা হয়েছে।

## ২. সার্ভার কনফিগারেশন (Environment Variables অথবা `.alpha10_secrets.php`)

যদি আপনার হোস্টিং প্যানেলে সরাসরি Environment Variable সেট করা যায়, তবে নিচের ভেরিয়েবলগুলো সেট করুন। আর যদি শেয়ার্ড হোস্টিংয়ে (`freedev.app` / cPanel) Environment Variable সেট করার অপশন না থাকে, তবে `/api/.alpha10_secrets.php` নামে একটি ফাইল তৈরি করে `.htaccess` দিয়ে ডাইরেক্ট ব্রাউজার অ্যাক্সেস ব্লক করে রাখুন:

```php
<?php
// ফাইল পাথ: /htdocs/api/.alpha10_secrets.php
return [
    'ALPHA10_SECRET_APP_KEY' => 'আপনার_আসল_৬৪_ক্যারেক্টারের_SECRET_APP_KEY_এখানে_দিন',
    'ALPHA10_PROTECTED_APK_PATH' => __DIR__ . '/../private/AimShot_Engine.apk', // অথবা খালি রাখুন যদি নিচের হ্যাশ দেন
    'ALPHA10_MAIN_APK_SHA256' => 'আপনার_Main_AimShot_APK_এর_৬৪_ক্যারেক্টারের_SHA256_হ্যাশ',
    'ALPHA10_MAIN_CERT_SHA256' => '1C:8D:56:83:62:EE:D9:04:02:36:43:0D:AC:8D:1F:BE:30:06:0E:9F:FD:2D:F4:01:AB:69:04:E7:94:86:A5:A2',
];
```

এবং `/api/.htaccess`-এ নিচের সুরক্ষা নিয়মটি যুক্ত রাখতে পারেন:
```apache
<Files ".alpha10_secrets.php">
    Require all denied
</Files>
```

| Variable Name | আবশ্যকতা | বিবরণ |
| :--- | :--- | :--- |
| `ALPHA10_SECRET_APP_KEY` | **আবশ্যক (Required)** | আপনার বিদ্যমান `auth.php`-তে ব্যবহৃত আসল ৬৪ ক্যারেক্টারের Secret App Key। এটি কখনোই ক্লায়েন্ট APK-তে পাঠানো হয় না। |
| `ALPHA10_PROTECTED_APK_PATH` | প্রস্তাবিত (Recommended) | সার্ভারে রাখা আসল `Main AimShot Engine (com.alpha10.aimshot)` APK ফাইলের লোকাল পাথ। এটি দিলে সার্ভার স্বয়ংক্রিয়ভাবে আসল SHA-256 ও File Size গণনা করে পাঠাবে। |
| `ALPHA10_MAIN_APK_SHA256` | বিকল্প (Fallback) | `ALPHA10_PROTECTED_APK_PATH` না দিলে আসল Main APK-এর ৬৪ ক্যারেক্টারের SHA-256 হ্যাশ এখানে দিন (অথবা GitHub Actions Secret `MAIN_APK_SHA256` হিসেবে দিন)। |
| `ALPHA10_MAIN_CERT_SHA256` | প্রস্তাবিত (Recommended) | Main APK যেই Release Keystore দিয়ে সাইন করা হয়েছে তার SHA-256 Certificate Fingerprint (`XX:XX:...`)। |
| `ALPHA10_MAIN_VERSION_CODE` | ঐচ্ছিক | Main APK-এর `versionCode` (যেমন `2`)। |
| `ALPHA10_MAIN_VERSION_NAME` | ঐচ্ছিক | Main APK-এর `versionName` (যেমন `1.0.1`)। |

## ৩. কাস্টম ব্রোকার URL (ঐচ্ছিক `ALPHA10_AUTH_URL`)
যদি ফ্রি শেয়ার্ড হোস্টিংয়ে (`freedev.app`) JavaScript anti-bot (`aes.js`) নন-ব্রাউজার মোবাইল API রিকোয়েস্ট আটকায়, তবে আপনি এই প্রজেক্টের Node/Express ব্যাকএন্ডের `/api/launcher-auth` রাউটটি Cloud Run বা যেকোনো HTTPS হোস্টে চালিয়ে GitHub Actions Secret `ALPHA10_AUTH_URL`-এ সেই URL সেট করতে পারবেন।

## ৪. কাজের প্রবাহ (End-to-End Flow)
1. **AimShot Launcher (`com.alpha10.aimshot.launcher`)** প্রথমে ৪-ধাপে সক্রিয় ইন্টারনেট (`HTTP 204`) ও Alpha10 Server সংযোগ যাচাই করে।
2. এরপর কোনো গোপন কী ছাড়াই `POST https://alpha10.freedev.app/api/launcher-auth.php`-এ JSON পেলোড পাঠায়।
3. **`launcher-auth.php`** সার্ভারের ভেতর থেকেই `X-App-Key` সহ `POST https://alpha10.freedev.app/api/auth.php` কল করে ৬০ সেকেন্ডের একবার ব্যবহারযোগ্য টোকেন ও `expected_sha256` ফেরত দেয়।
4. **AimShot Launcher** সেই টোকেন দিয়ে `GET https://alpha10.freedev.app/api/aimshot-download.php` (`Authorization: Bearer <TOKEN>`) কল করে Private Internal Storage-এ APK ডাউনলোড, যাচাই ও ইনস্টল করে।
