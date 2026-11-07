# POCO X3 NFC mit GlitchOS einrichten

Diese Anleitung ist für einen POCO X3 NFC mit Android 12 und MIUI 14 gedacht. Sie kann einem lokalen AI-Agenten mit ADB-Zugriff gegeben werden. Der Agent muss vor Änderungen das konkrete Gerät prüfen und jede Änderung mit einem lokalen Backup dokumentieren.

## Was dieses Projekt einrichtet

- GlitchOS Mobile wird als Home-App installiert. Die POCO-Systemkomponenten bleiben erhalten, damit Quickstep und der MIUI-App-Wechsler funktionieren.
- Die Oberfläche bietet App-Suche, Favoriten, Einstellungen, Entwickleroptionen und eine Medienansicht mit Pause, Weiter, Zurück, Spulen und Fortschritt.
- Screen Recorder bleibt verfügbar und wird als Favorit angeboten.
- Das Glitch-UI-Systemtheme passt unterstützte MIUI-Oberflächen an. Die Bootanimation zeigt GLITCH UI mit animierten Glitch-Effekten und eingebettetem Startsound.
- Die Theme-Datei kann über die offizielle Xiaomi-Theme-Importfunktion angewendet werden.
- Optional kann der Benachrichtigungszugriff für die Medienansicht über Androids unterstützten Listener-Befehl erteilt werden.

## Entfernte oder deaktivierte Apps

Die persönliche Einrichtung dieses Projekts hat Lucky Patcher, Google Wallet, Google Maps, APK Extractor und APK Signer entfernt oder deaktiviert. YouTube wurde durch YouTube ReVanced und YouTube Music ReVanced ersetzt. Screen Recorder wurde ausdrücklich behalten.

Ein AI-Agent darf keine Paketnamen raten. Er muss zuerst pm list packages und die installierten APKs prüfen, ein Backup auf dem PC anlegen und nur die vom Nutzer bestätigten Pakete entfernen. Systemdienste wie Downloads Provider, SystemUI, Package Installer, Settings, Quickstep und die MIUI-Komponenten für Recents dürfen nicht entfernt werden.

## Sicherheitsregeln für AI-Agenten

1. Prüfe Modell, Android-Version, Nutzer 0, ADB-Status und aktuelle Home-App.
2. Sichere Paketstatus, Home-Rolle, Launcher-Daten und relevante Dateien in einem privaten Backup außerhalb des Git-Repositories.
3. Installiere und starte GlitchOS, bevor eine bisherige Home-App deaktiviert wird.
4. Entferne nur eindeutig bestätigte Drittanbieter-Apps. Bei Systempaketen zuerst erklären, welche Funktion verloren geht.
5. Prüfe nach jeder Änderung Home, App-Wechsler, Einstellungen, Screen Recorder, Medienwiedergabe und einen Neustart.
6. Veröffentliche niemals ADB-Backups, Kontodaten, Tokens, Signaturschlüssel oder private Gerätereports.

## Installation

```powershell
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
& $adb devices
& $adb -s <SERIAL> install -r .\mobile\output\GlitchOS-Mobile-1.2.0.apk
& $adb -s <SERIAL> shell cmd package set-home-activity --user 0 com.glitchos.mobile/.MainActivity
& $adb -s <SERIAL> shell input keyevent KEYCODE_HOME
```

Baue die APK bei Bedarf mit .\mobile\build.ps1. Die POCO-Launcher-Komponente com.mi.android.globallauncher bleibt aktiviert. Für die Theme-Datei und die Bootanimation gelten die Hinweise in mobile/SYSTEM-THEME.md und mobile/GLITCH-UI.md.

## Wiederherstellung

Die Home-App kann mit .\mobile\restore-poco-home.ps1 zurückgestellt werden. Den Benachrichtigungszugriff kann .\mobile\revoke-media-access.ps1 widerrufen. Ein AI-Agent muss den tatsächlichen Backup-Pfad ausgeben und darf private Backup-Dateien nicht ins Repository committen.

## Aufforderung für einen AI-Agenten

> Lies POCO-X3-NFC-DEBLOAT.md, mobile/README.md, mobile/GLITCH-UI.md und mobile/SYSTEM-THEME.md. Prüfe zuerst mein angeschlossenes POCO X3 NFC. Erstelle ein privates PC-Backup, erkläre jede Paketänderung, installiere GlitchOS, behalte Screen Recorder und die MIUI-Komponenten für Quickstep und Recents. Entferne nur die von mir ausdrücklich bestätigten Apps. Prüfe Home, App-Wechsler, Einstellungen, Mediensteuerung und Neustart. Melde exakt, was verändert und was verifiziert wurde.
