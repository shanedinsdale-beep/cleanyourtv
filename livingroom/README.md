# Living Room TV (Kogan AI PONT): debloated, with the clean launcher

What's on this TV now, how to change it, and how to put it back. The TV is a Kogan "AI PONT" built by
Changhong (Homwee vendor layer, MediaTek m7632), Google TV on Android 14, at 192.168.86.122.

## What changed

**Debloat (disabled, nothing uninstalled).** Batches 1 to 5 in `backup/` (not in git: they hold your
TV's package names and state). The backup taken before any change is `backup/livingroom-20261023-150009/`.

| Batch | Packages | Why |
|---|---|---|
| 1 | Google Feedback, Feedback Consent, Federated Compute, On-Device Personalization, Privacy Sandbox ad services, Play Games, Anoki content recognition opt-in, Changhong Store and Help | Telemetry, ads, preloads |
| 2 | Netflix, Prime Video, Stan, Google TV Movies, 7plus (`com.swm.live`), Kodi, DebridStream | Streaming apps you didn't tick |
| 3 | YouTube and YouTube Music | You don't use the YouTube button |
| 4 | Miracast (`com.mediatek.androidbox`), USB file explorer and photo player | Extras you didn't tick |
| 5 | Setup wizard `com.google.android.tungsten.setupwraith` | It claims Home ahead of the launcher |

**Home.** The Clean Your TV launcher (`com.gathin.tvhome`) is the default Home. The stock launcher
`com.google.android.apps.tv.launcherx` is disabled.

**Screensaver.** Unchanged: Google's photo screensaver (`dreamx`), showing your Google Photos albums.

**Private DNS.** `dns.adguard-dns.com`, in ad-blocking mode. It doesn't block YouTube ads (YouTube is off
anyway). To turn it off: `tools/tv.py 'settings put global private_dns_mode opportunistic'`.

**Kept on purpose:** HDMI inputs and eARC, Live TV and the MediaTek TV services, AirPlay and screen share,
Chromecast, Google Assistant, Play Store and Play Services, remote and Bluetooth pairing, firmware updates
(`com.homwee.dmt.upgrade`, `com.homwee.upgrade`), picture services (dynamic backlight, light sensor),
`com.mediatek.android.leanbacklauncher.partnercustomizer` (the TV's remote hotkeys need it), SBS On Demand
and Dailymotion.

## The dock and the apps

The same four as the Play Room TV: Stremio, ABC iview, Disney+, Apple TV (`dock_apps` in
`launcher/app/src/main/res/values/config.xml`).

On the TV, no rebuild needed: hold OK on the Settings pill and choose reorder. OK picks up an app, the arrows
move it, Back puts it down, and Back again finishes. The first row is the dock, so moving an app into it changes
the main four. The order is saved on the TV.

The grid below the dock lists every other launchable app (SBS, 10 Play, 9Now, Dailymotion, Play Store...).

**Inputs.** The Inputs pill (top right) lists HDMI 1–4 and AV, with Live TV at the end.

## Re-enable an app

```
TV_HOST=192.168.86.122 tools/tv.py 'pm enable --user 0 org.xbmc.kodi'
```

## Put everything back

```
TV_HOST=192.168.86.122 livingroom/restore.sh
```

This re-enables every package from the five batches, sets the stock launcher back as Home and turns
Private DNS off. The launcher stays installed but isn't Home.

## Build and install the launcher

The same APK works on both TVs (the Streamer has no Live TV app, so it just hides that entry):

```
TV_HOST=192.168.86.122 streamer/build-and-install.sh
```

After installing, disable the stock launcher: `tools/tv.py 'pm disable-user --user 0 com.google.android.apps.tv.launcherx'`.

## Things to know

- **Extra users.** This TV has two unused profiles (users 10 and 11). Package commands must name
  `--user 0`; plain `pm list packages` fails with "Shell does not have permission to access user 10".
- **Wi-Fi.** The saved network belongs to the system now (re-joined in Settings on 2026-10-23).
- **Live TV / HDMI screenshots** come out black (protected video). That's the TV, not the debloat.
- **Black screen after an install.** The display sleeps when idle; press a button on the remote.
- **USB debugging / network debugging.** Switch them off in Developer options and revoke the
  authorisations when you're done. Port 5555 is plaintext.
