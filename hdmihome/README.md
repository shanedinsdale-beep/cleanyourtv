# HDMI Home

A Home screen for a TV whose only job is to show a streaming box plugged into one of its HDMI ports.
Whenever the TV would show its own home screen (power on, Home on the TV's remote, Back out of the
HDMI picture), HDMI Home shows "Switching to Google TV" for two seconds and opens that input. If the
input reports nothing connected, it waits and switches as soon as the box turns on.

HDMI-CEC usually does this job, but not always; HDMI Home catches the times it doesn't.

Needs Android TV 8.0 or newer. No ADB needed: it installs from a USB stick.

## Using it

- **Countdown (2 seconds):** OK switches straight away. Back, Menu or an arrow opens the TV menu.
- **TV menu:** go to the input, pick a different input for Google TV (remembered), open the TV's own
  home screen, or Settings. Back leaves the menu and starts the countdown again.
- **Home** from anywhere, including the TV's own home screen opened from the menu, comes back here
  and switches to the input.

It picks the input labelled HDMI 1 at first. If the box moves, use "Google TV is on a different input".

## Install from a USB stick

1. Copy [`app-release.apk`](app-release.apk) (in this folder) to a USB stick (FAT32 or exFAT) and plug it into the TV.
2. Open it with a file manager on the TV. If the TV has none, install one from the Play Store
   (for example "File Commander" or "X-plore").
3. Android asks to allow installs from that file manager: allow it
   (Settings → Device Preferences → Security & restrictions → Unknown sources), then install.
4. Press **Home** on the TV's remote. Android asks which Home app to use: choose **HDMI Home**, then
   **Always**.

## Undo

Settings → Apps → HDMI Home → **Clear defaults** puts the TV's own home screen back (or **Uninstall**
to remove it). Reach Settings from the HDMI Home menu: press Back during the countdown.

## Build

On an x86_64 machine with JDK 17 and the Android SDK: `cd hdmihome && ./gradlew assembleRelease`.
The APK is `app/build/outputs/apk/release/app-release.apk`; copy it over `app-release.apk` in this folder to update the committed one. On an arm64 Pi, build in the amd64
container the same way as `streamer/build-and-install.sh`.
