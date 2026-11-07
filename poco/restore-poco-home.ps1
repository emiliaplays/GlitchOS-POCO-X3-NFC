[CmdletBinding()]
param([string]$Device='331904ac',[string]$AdbPath=(Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'))
$ErrorActionPreference='Stop'
& $AdbPath -s $Device shell cmd package set-home-activity --user 0 com.mi.android.globallauncher/com.miui.home.launcher.Launcher
if($LASTEXITCODE){throw 'Could not restore POCO HOME'}
& $AdbPath -s $Device shell input keyevent KEYCODE_HOME
if($LASTEXITCODE){throw 'Could not open HOME'}
# GlitchOS and its favorites remain installed for switching back later.
