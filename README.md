# GlitchOS for POCO X3 NFC

A Glitch UI home screen and Xiaomi theme setup for the POCO X3 NFC (`surya`). Built and tested with Android 12 and MIUI 14.

GlitchOS gives the phone a custom launcher with app search, favorites, a media player panel, and a dark navy and mint interface. The companion Xiaomi theme includes system styling and a custom boot animation with a Glitch UI title and embedded sound.

## GlitchOS in action

| Home | App library |
| --- | --- |
| ![GlitchOS home screen with favorites and the dark glass interface](docs/images/launcher-home.png) | ![GlitchOS app library with search and installed apps](docs/images/launcher-apps.png) |

| Media controls | Android Recents |
| --- | --- |
| ![Now Playing screen with playback controls](docs/images/launcher-media.png) | ![Android recent apps screen alongside GlitchOS](docs/images/launcher-recents.png) |

Screenshots were captured on the POCO X3 NFC during device testing. Android's Recents screen remains provided by MIUI Quickstep.

## What GlitchOS does

GlitchOS replaces the default Home screen with a fast, touch-first launcher. Search installed apps by name or package, pin favorites, open Android settings, and control compatible media sessions from one screen. Its dark navy surfaces, mint highlights, and restrained glitch styling carry through the companion Xiaomi theme.

The project bundles the phone theme and system theme separately. The system theme includes a custom `GLITCH UI` boot animation with a short glitch sequence and embedded sound. Theme support varies across MIUI builds, so the documentation records the tested import route and limitations.

## Project contents

| File | Purpose |
| --- | --- |
| [`poco/`](poco/) | Launcher source, build scripts, system-theme tools, and recovery scripts |
| [`POCO-X3-NFC-DEBLOAT.md`](POCO-X3-NFC-DEBLOAT.md) | Step-by-step handoff for a local AI agent |
| [`GlitchUI-Poco.mtz`](poco/output/theme/theme/GlitchUI-Poco.mtz) | Xiaomi user-interface theme |
| [`GlitchUI-System-1.3.4.mtz`](poco/output/boot/glitch-v5/GlitchUI-System-1.3.4.mtz) | System theme with boot animation and sound |
| [`Mobile README`](poco/README.md) | Build, install, device verification, and launcher recovery |
| [`Theme notes`](poco/SYSTEM-THEME.md) | Xiaomi theme modules, application, validation, and known limits |

## Debloat summary

The personal POCO setup removed or disabled Lucky Patcher, Google Wallet, Google Maps, APK Extractor, and APK Signer. YouTube was replaced by YouTube ReVanced and YouTube Music ReVanced. Screen Recorder was kept. The AI handoff guide requires checking package identities on the target phone and backing up before any package changes.

The launcher does not remove the POCO Quickstep/Recents components. Keep `com.mi.android.globallauncher` enabled because MIUI binds its privileged app-switcher service to that package. The launcher replaces the Home screen while MIUI continues to provide Recents.

## Install and build

See [the POCO AI handoff guide](POCO-X3-NFC-DEBLOAT.md) for the guided install and safety checks. Build the APK from PowerShell with:

```powershell
.\poco\build.ps1 -JavaHomePath 'C:\Program Files\Java\jdk-17' -AndroidSdkPath "$env:LOCALAPPDATA\Android\Sdk"
```

The APK requires Android 8 or newer. The complete tested Glitch UI setup targets POCO X3 NFC on Android 12 and MIUI 14. Other devices and ROM versions need independent checks.

## Recovery

Use [`restore-poco-home.ps1`](poco/restore-poco-home.ps1) to return Home to POCO Launcher. Use [`revoke-media-access.ps1`](poco/revoke-media-access.ps1) to revoke the optional media notification access. Backups, signing keys, and device reports are private local files and should never be committed.

## Verification and limits

The launcher passed the documented device smoke checks on the POCO. Boot animation 1.3.4 was applied and the phone rebooted successfully, though earlier versions triggered MIUI boot-animation crashes. The custom sound is embedded in the current package, but audible playback was not confirmed over USB. See the theme notes for details.

GlitchOS is an independent project and is not affiliated with Xiaomi, POCO, Google, or YouTube. ReVanced is a third-party YouTube client and is not bundled here.
