#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
sdk="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$sdk" ]]; then
  echo "Set ANDROID_SDK_ROOT to an SDK with Android 35 and build tools 35.0.0." >&2
  exit 1
fi
tools="${ANDROID_BUILD_TOOLS:-$sdk/build-tools/35.0.0}"
platform="${ANDROID_PLATFORM_JAR:-$sdk/platforms/android-35/android.jar}"
output="$PWD/../dist"
mkdir -p "$output"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/assets" "$work/classes" "$work/dex"
cp ../web/assets/stage.jpg "$work/assets/stage.jpg"
zxing="${ZXING_JAR:-}"
if [[ -z "$zxing" || ! -f "$zxing" ]]; then
  echo "Set ZXING_JAR to ZXing core 3.5.3." >&2
  exit 1
fi
cp LICENSE-ZXING.txt "$work/assets/LICENSE-ZXING.txt"
sources=(src/org/lifeac/lighting/companion/*.java)
compiler=(javac)
if [[ -n "${ECJ_JAR:-}" ]]; then compiler=(java -jar "$ECJ_JAR"); fi
"${compiler[@]}" -source 8 -target 8 -classpath "$platform:$zxing" -d "$work/classes" "${sources[@]}"
(cd "$work/classes" && zip -qr "$work/classes.jar" .)
"$tools/d8" --min-api 29 --lib "$platform" --output "$work/dex" "$work/classes.jar" "$zxing"
"$tools/aapt2" compile --dir res -o "$work/resources.zip"
"$tools/aapt2" link -I "$platform" --manifest AndroidManifest.xml -A "$work/assets" -o "$work/unsigned.apk" "$work/resources.zip"
(cd "$work/dex" && zip -q "$work/unsigned.apk" classes.dex)
"$tools/zipalign" -f 4 "$work/unsigned.apk" "$work/aligned.apk"
# Keep the same key for updates to this test pilot.
keystore="${PILOT_KEYSTORE:-$output/android-pilot.keystore}"
if [[ ! -f "$keystore" ]]; then
  (umask 077; keytool -genkeypair -keystore "$keystore" -storepass android -keypass android \
    -alias androiddebugkey -dname 'CN=LifeAC Lighting Pilot,O=Debug,C=AU' -keyalg RSA -keysize 2048 -validity 3650)
fi
apk="$output/LifeAC-Lighting-Pilot.apk"
"$tools/apksigner" sign --ks "$keystore" --ks-key-alias androiddebugkey --ks-pass pass:android --out "$apk" "$work/aligned.apk"
"$tools/apksigner" verify --verbose "$apk"
