[CmdletBinding()]
param([string]$Device='331904ac',[string]$AdbPath=(Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'))
& $AdbPath -s $Device shell cmd notification disallow_listener com.glitchos.mobile/com.glitchos.mobile.MediaAccessService 0
if($LASTEXITCODE){throw 'Could not revoke media access'}
