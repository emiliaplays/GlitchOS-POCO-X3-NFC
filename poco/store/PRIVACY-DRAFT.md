# GlitchOS Mobile — privacy policy draft

Date: 2026-11-07. Applies to version 1.1.0, package com.glitchos.mobile.
Not ready to publish until the publisher identity and contact below are completed.

Publisher: [publisher must complete]
Privacy/support contact: [publisher must complete]

GlitchOS Mobile runs on your Android device. The current app has no Internet
permission and contains no advertising or analytics SDK. The app does not send
your app list, search terms, favorites or playback information to the developer.

The launcher queries launchable apps to display, search and open them. Favorites
are stored in local app preferences. Search text is used locally and can be kept
as transient Android activity state. Copying a package name places that name in
the Android clipboard only when you choose that action.

Optional media controls require Android notification-listener access to obtain
active media sessions. The app displays track metadata and artwork supplied by
the selected media session and sends playback commands to that session. Its
notification-listener service has no implementation for reading, storing,
dismissing or forwarding notification messages. Android describes this access
as broad notification access; you can revoke it in system settings at any time.

Applying the Glitch design sets home/lock wallpaper through Android's wallpaper
service and saves generated wallpaper and theme files in the app's external-files
directory. The current local theme export includes icons of installed launchable
apps. That file is not uploaded automatically and may reveal installed app names
through its package filenames if you choose to share it.

Other apps you open, the Android operating system and device-vendor services have
their own privacy practices. Android may copy an applied wallpaper into system
storage. Removing the launcher does not necessarily reset the system wallpaper.

Clear the launcher's app storage to remove its saved favorites and local data.
Delete any theme files you manually copied elsewhere separately. Notification
access can be revoked independently without clearing app data.
