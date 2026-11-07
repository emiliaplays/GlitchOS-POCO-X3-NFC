# Glitch UI system theme 1.2.0 — POCO X3 NFC

Applied locally on the owner's POCO X3 NFC, Android 12 / MIUI Global 14.0.5.
This is a device-specific development theme, not a complete operating system or
a claim that every Android/third-party screen has been replaced.

## Verified changes

- Real Control Center: mint enabled tiles and slider fills, navy inactive cards,
  dark enabled symbols, mint settings/edit actions and readable labels.
- Notification shade: navy cards, light text and mint progress/action colors.
- Android Settings: navy background and 32 original mint line icons for actual
  Settings header resource names (phone, updater, Wi-Fi, Bluetooth, security, etc.).
- System Recents retains the actual POCO implementation, horizontal cards,
  Glitch wallpaper and themed app icons; its color modules are included for both
  MIUI Home and POCO resource package names.
- The launcher is updated to 1.2.0 / versionCode 4. Its theme export includes all
  seven system modules, rather than falling back to the original icons/wallpaper
  prototype. No permissions were added.

The separate side volume dialog still renders its active fill gray on this build,
despite the included volume color overrides. Its controls remain native. Some
system shapes, protected screens and app-specific views retain the manufacturer's
implementation. No APK patching, root, bootloader unlock, signature-verification
changes or replacement security dialogs were used.

## Artifacts and source

- APK: output/GlitchOS-Mobile-1.2.0.apk
- MTZ: output/system-theme/GlitchUI-System-1.2.0.mtz
- Module asset bundled into APK: assets/theme-modules.zip
- Builder and original icon drawing source: build-system-theme.py
- Generated XML/PNG source: output/system-theme/modules/
- Resource inventories and screenshots: ignored local/system-theme-resources/

The Python builder uses Pillow and resource inventories from this actual phone,
plus settings-headers.txt to resolve actual Settings header icon names. It builds
normal theme resource ZIPs; it does not edit or redistribute the pulled system APKs.
The module bundle contains original color definitions and generated line art.
The base icon theme still includes the phone's app icons and is a private prototype.
Do not submit it as wholly original artwork to Xiaomi's public store.

The theme modules contain 159 SystemUI, 125 plugin, 82 Settings, 7 Android framework,
32 MIUI framework and 82 colors each for the two Home resource targets. These are
packaged overrides, not a count of independently verified visible changes.

## Installation and checks

The signed factory Theme Manager 2.1.0.8-global has the exported official editor
test activity absent from the current 2.15.9.5-global update. Used Android's
package-specific uninstall-system-updates command, launched ApplyThemeForScreenshot
with theme_file_path and api_called_from, then selected the MTZ in Android's picker.
Wait for application to finish before replacing Theme Manager; replacing it during
application can interrupt module installation.

After the final complete application, reinstalled the saved signed current Themes
update. All seven modules remained in /data/system/theme and Settings was visually
verified with its original mint line icons after the update. An earlier iteration
survived a normal phone reboot; the final icon/Recents refinements were verified
after application and Theme Manager update, but were not rebooted a second time.

The MTZ and all nested module ZIP/XML files were checked. APK signature verification
passed. Home, Settings and actual system Recents were exercised. Media code was
unchanged, so previous media-session behavior was not re-tested during this change.
The device is left at Glitch Home; POCO's Quickstep service remains enabled.

Dynamic Android colors were set locally to a mint preset using the normal secure
theme_customization_overlay_packages setting. Its previous JSON is saved as
local/system-theme-resources/dynamic-colors-before.json. This did not recolor the
separate volume fill; no additional system protections were changed to force it.

## Backups and rollback

The previous 1.1 wallpaper/icon MTZ is preserved at output/theme/theme/GlitchUI-Poco.mtz.
The original/current Themes APKs and a 607-file external Themes data backup are
under local/glitchui-1.1-baseline/ and local/theme-factory-test/. These do not include
private internal Themes account data, which cannot be read without root.

To return to the stock appearance, open Themes > My account > Themes and apply
POCO. The Glitch launcher remains installed; choose POCO in Android's default Home
settings separately if desired. Never uninstall the POCO package on this firmware,
because SystemUI uses its Quickstep/Recents service.

Theme store registration for glitchbyemilia remains pending Xiaomi's review.
No theme or launcher has been uploaded, submitted for content review or published.
