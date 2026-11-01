#!/usr/bin/env bash
# Build the launcher and install it on this Google TV Streamer as Home, without disabling the stock
# launcher or setupwraith (those are the debloat lists' job; this only adds the launcher).
#
# Usage: TV_HOST=192.168.86.45 streamer/build-and-install.sh
#
# The debug signing key lives in $HOME/.android-amd64 for the container build, so rebuilds sign the same way.
# The first build with that key cannot replace an APK signed another way: uninstall com.gathin.tvhome once
# (pm uninstall com.gathin.tvhome, with the stock launcher back as Home), then run this again.
#
# Building: on an x86_64 computer with JDK 17 and the Android SDK (platform 34, build-tools 34.0.0),
# ./gradlew runs directly. On an arm64 Linux machine (a Raspberry Pi, say) the SDK's aapt2 is x86_64
# and needs emulation, so the build runs in an amd64 container instead (Docker, binfmt for amd64).
set -euo pipefail
: "${TV_HOST:?set TV_HOST to the TV IP address}"
cd "$(dirname "$0")/.."
export TV_HOST
PY=tools/venv/bin/python
APK=launcher/app/build/outputs/apk/release/app-release.apk

if [ "$(uname -m)" = x86_64 ] && [ -n "${ANDROID_HOME:-}" ] && [ -n "${JAVA_HOME:-}" ]; then
  (cd launcher && ./gradlew assembleRelease -q --console=plain)
else
  echo "Building in an amd64 container (this takes about 1.5 hours on a Raspberry Pi 5)."
  # The Gradle cache with every dependency already downloaded; set GRADLE_CACHE to use another.
  GRADLE_CACHE="${GRADLE_CACHE:-$HOME/toolchain/gradle-home}"
  docker run --rm --platform linux/amd64 \
    -v "$PWD/launcher:/work" -v "${ANDROID_SDK:-$HOME/android-sdk}:/sdk" -v "$GRADLE_CACHE:/root/.gradle" \
    -v "$HOME/.android-amd64:/root/.android" \
    -e ANDROID_HOME=/sdk -w /work eclipse-temurin:17-jdk \
    bash -c 'echo "sdk.dir=/sdk" > local.properties && chmod +x gradlew && ./gradlew assembleRelease -q --console=plain'
fi
[ -f "$APK" ] || { echo "ERROR: $APK was not built" >&2; exit 1; }

"$PY" -I tools/b64push.py "$APK" /data/local/tmp/tvhome.apk
OUT=$("$PY" -I tools/tv.py 'pm install -r /data/local/tmp/tvhome.apk; rm -f /data/local/tmp/tvhome.apk')
echo "$OUT"
case "$OUT" in *Success*) ;; *) echo "ERROR: install failed; nothing else was changed." >&2; exit 1 ;; esac

# Start it first and check it really came up (a start that only prints "Error" would leave Home broken).
START=$("$PY" -I tools/tv.py 'am start -W -n com.gathin.tvhome/.MainActivity')
echo "$START"
printf '%s' "$START" | grep -qF 'Status: ok' || { echo "ERROR: the launcher did not start." >&2; exit 1; }

# Grant location (Wi-Fi name in the pill), precompile, then make it the default Home.
printf '%s\n' \
  "pm grant --user 0 com.gathin.tvhome android.permission.ACCESS_FINE_LOCATION 2>/dev/null; echo location-granted" \
  "cmd package compile -m speed -f com.gathin.tvhome" \
  "cmd package set-home-activity --user 0 com.gathin.tvhome/.MainActivity" \
  | "$PY" -I tools/tv.py --batch

HOME_NOW=$("$PY" -I tools/tv.py 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1' | tr -d '\r')
echo "Home now resolves to: $HOME_NOW"
case "$HOME_NOW" in
  com.gathin.tvhome/*) echo "Installed. Press Home on the remote to see it." ;;
  *) echo "Home still resolves to $HOME_NOW. The stock launcher (com.google.android.apps.tv.launcherx) or setupwraith is" >&2
     echo "still claiming Home; see streamer/README.md, 'Home' section." >&2; exit 1 ;;
esac
