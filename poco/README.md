# GlitchOS Mobile

Current mobile release: **1.2.0, Glitch UI · POCO Edition**.
See [SYSTEM-THEME.md](SYSTEM-THEME.md) for the installed system theme, validation and limits.
See [GLITCH-UI.md](GLITCH-UI.md) for applied system styling, exported Xiaomi theme,
app removals, verification and the Global Themes import limitation.

Native touch adaptation of **GlitchOS TV 1.4.0**, based on repository commit
`409a0ff1f9eda21d5872f2b03bad09d4870b7580` from `emiliaplays/GlitchOS-TV-installer`.
This directory is independent of the TV source, installer, and release metadata.
Package: `com.glitchos.mobile`, version 1.2.0, minimum Android 8, target Android 14.

The mobile UI reuses the TV launcher's glass and landscape drawable implementations,
icon resource, media-session controller and notification access service. Its activity
uses touch layouts and includes system apps in discovery, preserves activity aliases,
and handles package changes while visible or on the next resume.

Features:

- Responsive app grid with original installed app icons.
- Search by app label or package; long press for favorites, app info, or package copy.
- Favorites survive process recreation and restarts. Screen Recorder is a default favorite.
- Android settings, developer settings and default launcher selection.
- Optional media controls using Android media sessions, without Internet permission.
- Native status bar and system navigation remain available. No accessibility redirect,
  root, background polling, replacement firmware or automatic media-access grant.

This version does not implement Android app widgets, icon packs, arbitrary wallpaper
selection or its own system Recents. Glass consists of gradients and translucent
surfaces, as in the TV source; it is not live blur.

## Build

```powershell
.\mobile\build.ps1 -JavaHomePath 'C:\Program Files\Java\jdk-17' -AndroidSdkPath "$env:LOCALAPPDATA\Android\Sdk"
```

Uses the upstream aapt2/javac/D8/alignment/signing build flow, with SDK 36 and Build
Tools 36.0.0. Output: `mobile/output/GlitchOS-Mobile-1.2.0.apk`. Signing identity and
password are generated once in ignored `mobile/.local/`; keep them for compatible updates.

## POCO X3 NFC installation

The repository also includes the portable Xiaomi theme packages. `GlitchUI-Poco.mtz`
contains the phone UI theme. `GlitchUI-System-1.3.4.mtz` contains the system modules,
boot animation and embedded start sound used on the POCO X3 NFC. Import the MTZ through
the Xiaomi Themes app or the documented tester import flow. Do not publish a personal
ADB backup or signing key.

Test device: `surya`, M2007J20CG, Android 12, MIUI 14, owner user 0.

```powershell
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
& $adb -s 331904ac install -r .\mobile\output\GlitchOS-Mobile-1.2.0.apk
& $adb -s 331904ac shell am start -W -n com.glitchos.mobile/.MainActivity
& $adb -s 331904ac shell cmd package set-home-activity --user 0 com.glitchos.mobile/.MainActivity
& $adb -s 331904ac shell input keyevent KEYCODE_HOME
```

**Keep `com.mi.android.globallauncher` enabled.** On this actual device SystemUI binds
to its `com.miui.home.recents.TouchInteractionService` using `QUICKSTEP_SERVICE`.
GlitchOS replaces the visible Home screen but cannot replace this privileged service.
Removing POCO's package would risk losing the system app switcher. Navigation settings
were left unchanged (three buttons). The TV installer must not be run on this phone.

Original Home/disabled packages/navigation state are saved privately in
`local/poco-mobile-baseline/`. Restore the original Home screen with:

```powershell
.\mobile\restore-poco-home.ps1
```

This keeps GlitchOS and its preferences installed. Home can also be changed through
GlitchOS settings → Standard-Launcher wählen.

## Verification

Run `mobile/test-device.ps1` on the configured test phone, unlocked. It uses actual
touch input and checks HOME, settings, search, Screen Recorder, favorite removal and
restoration, process recreation, optional media-access state and system Recents.
It restores the tested Messages favorite at the end. Device screenshots/XML/results
are local under ignored `mobile/output/qa/`. Media playback with granted notification
access is optional and has not been enabled automatically.

Verified on the physical POCO on 2026-11-06: all 13 smoke assertions passed. A temporary
`com.glitchos.qaprobe` APK was additionally installed, found through package search and
launched; uninstalling it while the search was visible immediately removed its tile.
The probe was removed after testing. No navigation settings were changed, and the
original POCO package remains enabled. A full phone reboot and active media playback
with notification access were not tested.

Installed APK SHA-256:
`2b675fc0a5ef86a44805d2f6862a796a4d74a66c170d0e1af75da0f779e11d8f`.

The work is in local branch `feat/poco-mobile-launcher`; no GitHub push or release
publication was performed.

Later on 2026-11-06, at the owner's request to avoid MIUI's warning/countdown, media
notification access was explicitly granted using Android's supported ADB command
`cmd notification allow_listener com.glitchos.mobile/com.glitchos.mobile.MediaAccessService 0`.
The enabled listener and its system binding were verified. MIUI Security Center and
global permission warning behavior were left intact. Revoke this specific grant with
`mobile/revoke-media-access.ps1`. The pre-grant listener state is saved privately in
`local/notification-access-2026-11-06/`.

## Mobile 1.0.1 player update (2026-11-07)

The media page now has a dedicated native `MobileNowPlayingView`: album art from
the selected session (app-icon fallback), app display name, track/artist, playback
status, live elapsed/duration labels, a seek bar, previous/next and +/-10 seconds.
Unsupported actions are disabled. Unknown/live durations are shown explicitly.
The session's pending intent is preferred when opening the media app.

The progress timer updates existing views, stops when hidden/paused, and does not
rebuild the screen during scrubbing. Seeks are guarded against switching sessions
mid-gesture. No network permission or artwork downloader was added.

Real YouTube Music ReVanced testing passed pause/resume, forward/backward ten seconds,
and absolute scrubbing. Evidence: `output/qa/player-control-results.txt` and local
screenshots. The 1.0.0 HOME/package-discovery tests above describe the original release.

The Music playback issue was separately traced to upstream stream HTTP 403 responses.
Changing the app's ReVanced spoof client from Android Reel (no auth) to visionOS
resolved the reproduced one-minute stall in the test: 99,451 ms of actual playback
over 101 wall-clock seconds, with no new HTTP 403/413 errors. These streaming settings
belong to the media app; the launcher controls the Android media session.



