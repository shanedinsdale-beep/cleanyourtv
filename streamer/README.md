# Google TV Streamer: debloated, with the clean launcher

What's on this TV now, how to change it, and how to put it back.

## What changed

**Debloat (disabled, nothing uninstalled).** Batches 1 to 4 in `backup/` (not in git: they hold your
TV's package names and state):

| Batch | Packages | Why |
|---|---|---|
| 1 | Google Feedback, Feedback Consent, remote-control logging, Federated Compute, On-Device Personalization, two Privacy Sandbox ad-service packages, Play Games, the ambient screensaver `dreamx` and its backdrop | Telemetry, ads, preloads |
| 2 | Netflix, Prime Video, Stan, Google TV Movies, `com.swm.live` | Streaming apps you didn't tick |
| 3 | YouTube (`com.google.android.youtube.tv`) | You don't use the YouTube button |
| 4 | Setup wizard `com.google.android.tungsten.setupwraith` | It claimed Home ahead of the launcher |

**Home.** The Clean Your TV launcher (`com.gathin.tvhome`) is the default Home. The stock launcher
`com.google.android.apps.tv.launcherx` is disabled.

**Screensaver.** Google's photo screensaver (`com.google.android.apps.tv.dreamx`), the TV's default, which
shows your Google Photos albums. It was re-enabled after the debloat; the clock screensaver
(`com.gathin.tvhome/.ClockDream`) is still in the launcher if you want it back.


**Private DNS.** `dns.adguard-dns.com`, in ad-blocking mode. It doesn't block YouTube ads (YouTube is
off anyway).

**Kept on purpose:** Play Store and Play Services, Google Assistant, casting (Cast receiver and phone
remote), Bluetooth and remote pairing, TVAxel (the remote's audio routing and pairing), updates, and the
TV's own settings.

## The dock and the apps

The dock is the four apps in `launcher/app/src/main/res/values/config.xml`, under `dock_apps`:

1. Stremio (`com.stremio.one`)
2. ABC iview (`au.net.abc.iview`)
3. Disney+ (`com.disney.disneyplus`)
4. Apple TV (`com.apple.atve.androidtv.appletv`)

On the TV, no rebuild needed: hold OK on the Settings pill and choose reorder. OK picks up an app, the arrows
move it, Back puts it down, and Back again finishes. The first row is the dock, so moving an app into it changes
the main four. The order is saved on the TV.

To change the default order or apps instead, edit that list, then run `streamer/build-and-install.sh`. Changing
`dock_apps` this way resets any order saved on the TV. Any
package name works, and `tools/tv.py 'pm list packages'` shows the names on this TV.

The grid below the dock lists every other launchable app, including 10 Play, 9Now and the Play Store.

## The wallpaper and the look

The launcher's look is set in `config.xml` as well (columns, dock title, glow, clock format). A theme
changes the colours and wallpaper without a rebuild: `tools/theme.sh themes/<name>` pushes a
`config.json` and optional wallpaper to the TV, and `themes/README.md` describes the format. To use a
different wallpaper alone, put your image into a theme folder (`themes/<name>/wallpaper.jpg`) and push
it with `tools/theme.sh`.

## Re-enable an app

Each app above is disabled, not uninstalled. Re-enable one with:

```
TV_HOST=192.168.86.45 tools/tv.py 'pm enable --user 0 com.netflix.ninja'
```

Put it back in the dock by adding its package name to `dock_apps` in `config.xml` and running
`streamer/build-and-install.sh`.

## Put everything back

```
TV_HOST=192.168.86.45 streamer/restore.sh
```

This re-enables every package from the four batches (each batch's recorded state is used, so anything
already disabled before stays disabled), sets the stock launcher back as Home, restores Google's
screensaver and turns Private DNS off. The launcher stays installed but isn't Home. Remove it with
`tools/tv.py 'pm uninstall --user 0 com.gathin.tvhome'` if you want.

## Build and install the launcher

Installing also grants the location permission, so the Wi-Fi pill can show the network name.

```
TV_HOST=192.168.86.45 streamer/build-and-install.sh
```

On an x86_64 computer with the JDK and Android SDK, it runs `./gradlew` directly. On an arm64 Linux
machine such as a Raspberry Pi, it builds inside an amd64 container, which takes about 1.5 hours.

## Things to know

- **Home.** If Home ever resolves to setupwraith, it's the setup wizard claiming HOME again. Run
  `streamer/restore.sh`, then `build-and-install.sh`, or disable setupwraith from batch 4 again.
- **Wi-Fi.** The saved network belongs to the system now (it was re-joined in Settings). Don't disable
  setupwraith before checking `dumpsys wifi | grep -o 'cname=[^ ]*'`.
- **Inputs pill.** The Streamer has no HDMI inputs, so the Inputs pill is hidden and Live TV is off.
- **"Your apps" row.** The stock home's cached row still lists the disabled apps until the launcher
  replaces it. The new launcher's grid doesn't show them.
- **Screensaver.** The clock screensaver was checked on the TV and works. The photo screensaver is back
  (`dreamx`), so its sign-in and album choice happen on the TV with the remote.
- **Black screen after an install.** The TV's display sleeps when it is idle, and a sleeping display draws
  nothing. Press a button on the remote, or `tools/tv.py 'input keyevent KEYCODE_WAKEUP'`.
- **Reinstalling the launcher.** The build's signing key must match the installed one. The build script keeps
  the key in `~/.android-amd64`; if you built with a different key, `pm uninstall com.gathin.tvhome` first.
- **USB debugging.** Still on, so ADB is still open on port 5555. Switch it off (and Wireless
  debugging) in Developer options, and revoke the USB debugging authorisations, when you're done.
  Port 5555 is plaintext.
