#!/bin/bash
# PostItDat - Auto Setup & Build Script
# Run ini di terminal/Colab: bash setup-and-build.sh

set -e
GRADLE_VERSION="8.4"
GRADLE_DIR="$HOME/.gradle-dist/gradle-$GRADLE_VERSION"

echo "=== PostItDat Build Setup ==="
echo ""

# 1. Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java tidak ditemukan. Install dulu:"
    echo "   Ubuntu/Colab: sudo apt-get install openjdk-17-jdk"
    exit 1
fi
echo "✅ Java: $(java -version 2>&1 | head -1)"

# 2. Download Gradle jika belum ada
if [ ! -f "$GRADLE_DIR/bin/gradle" ]; then
    echo ""
    echo "📥 Download Gradle $GRADLE_VERSION..."
    mkdir -p "$HOME/.gradle-dist"
    cd "$HOME/.gradle-dist"
    wget -q --show-progress "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -O gradle.zip
    unzip -q gradle.zip
    rm gradle.zip
    echo "✅ Gradle terinstall"
else
    echo "✅ Gradle sudah ada"
fi

GRADLE_CMD="$GRADLE_DIR/bin/gradle"

# 3. Set Android SDK path
if [ -z "$ANDROID_HOME" ]; then
    # Coba auto-detect
    POSSIBLE_PATHS=(
        "$HOME/Android/Sdk"
        "$HOME/android-sdk"
        "/opt/android-sdk"
        "/usr/lib/android-sdk"
        "/content/android-sdk"  # Colab
    )
    for p in "${POSSIBLE_PATHS[@]}"; do
        if [ -d "$p" ]; then
            export ANDROID_HOME="$p"
            break
        fi
    done
fi

if [ -z "$ANDROID_HOME" ]; then
    echo ""
    echo "📥 Download Android SDK Command Line Tools..."
    mkdir -p "$HOME/android-sdk/cmdline-tools"
    cd "$HOME/android-sdk/cmdline-tools"
    wget -q --show-progress "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" -O cmdtools.zip
    unzip -q cmdtools.zip
    mv cmdline-tools latest
    rm cmdtools.zip
    export ANDROID_HOME="$HOME/android-sdk"
    export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
    
    echo "📥 Install Android SDK components..."
    yes | sdkmanager --licenses > /dev/null 2>&1 || true
    sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
    echo "✅ Android SDK siap"
fi

export ANDROID_HOME="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
echo "✅ Android SDK: $ANDROID_HOME"

# 4. Setup local.properties
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "sdk.dir=$ANDROID_HOME" > "$SCRIPT_DIR/local.properties"

# 5. Build APK
echo ""
echo "🔨 Building APK..."
cd "$SCRIPT_DIR"
"$GRADLE_CMD" assembleDebug --no-daemon

APK_PATH="$SCRIPT_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    echo ""
    echo "✅✅✅ BUILD SUKSES!"
    echo "📱 APK ada di: $APK_PATH"
    ls -lh "$APK_PATH"
    
    # Copy ke lokasi mudah
    cp "$APK_PATH" "$SCRIPT_DIR/PostItDat.apk"
    echo "📱 Juga tersimpan di: $SCRIPT_DIR/PostItDat.apk"
else
    echo "❌ Build gagal - cek error di atas"
fi
