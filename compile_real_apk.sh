#!/usr/bin/env bash
set -e

echo "=== Compiling Authentic Android APK ==="

WORKDIR="/app/applet"
cd "$WORKDIR"

rm -rf build_temp
mkdir -p build_temp/classes
mkdir -p build_temp/gen

ANDROID_JAR="/usr/lib/android-sdk/platforms/android-23/android.jar"

# 1. Create keystore if not exists
if [ ! -f "debug.keystore" ]; then
    keytool -genkeypair -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
fi

# 2. Generate R.java and package base resources + binary AndroidManifest.xml
echo "[1/5] Compiling resources and binary AndroidManifest.xml with aapt..."
aapt package -f -m \
    -M app/src/main/AndroidManifest.xml \
    -S app/src/main/res \
    -I "$ANDROID_JAR" \
    -J build_temp/gen \
    -F build_temp/resources.apk \
    --min-sdk-version 24 \
    --target-sdk-version 34 \
    --version-code 1 \
    --version-name "1.0.0"

# 3. Compile Java source files
echo "[2/5] Compiling Java source files..."
# Find all java files in app/src/main/java and generated R.java
JAVA_FILES=$(find app/src/main/java build_temp/gen -name "*.java")

# Compile using javac (compatible with Java 8 bytecode)
javac -encoding UTF-8 -source 1.8 -target 1.8 \
    -cp "$ANDROID_JAR" \
    -d build_temp/classes \
    $JAVA_FILES

# 4. Convert .class files to classes.dex using dalvik-exchange (dx)
echo "[3/5] Converting classes to Dalvik Executable (classes.dex)..."
/usr/bin/dalvik-exchange --dex --output=build_temp/classes.dex build_temp/classes

# Add classes.dex to the resources apk
cd build_temp
aapt add resources.apk classes.dex
cd "$WORKDIR"

# 5. Zipalign (4-byte alignment required by Android)
echo "[4/5] Aligning APK with zipalign..."
zipalign -f -p 4 build_temp/resources.apk build_temp/aligned.apk

# 6. Sign APK with apksigner (V1 + V2 + V3 signature schemes)
echo "[5/5] Signing APK with apksigner..."
apksigner sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android \
    --out AimShot-Engine-v1.0.0.apk build_temp/aligned.apk

# Verify signature
echo "Verifying APK..."
apksigner verify --verbose AimShot-Engine-v1.0.0.apk

# Copy to destinations
cp AimShot-Engine-v1.0.0.apk public/AimShot-Engine-v1.0.0.apk
mkdir -p app/build/outputs/apk/debug
cp AimShot-Engine-v1.0.0.apk app/build/outputs/apk/debug/app-debug.apk
mkdir -p AimShot_Project/app/build/outputs/apk/debug
cp AimShot-Engine-v1.0.0.apk AimShot_Project/app/build/outputs/apk/debug/app-debug.apk

echo "SUCCESS! Valid binary signed APK created: AimShot-Engine-v1.0.0.apk ($(stat -c%s AimShot-Engine-v1.0.0.apk) bytes)"
