[CmdletBinding()]
param([string]$Device='331904ac', [string]$AdbPath=(Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'))
$ErrorActionPreference='Stop'
$qa=Join-Path $PSScriptRoot 'output\qa'
New-Item -ItemType Directory -Force -Path $qa | Out-Null
$results=[System.Collections.Generic.List[string]]::new()
function Adb([string[]]$Arguments) {
    $value=& $AdbPath -s $Device @Arguments 2>&1
    if($LASTEXITCODE){throw ($value | Out-String)}
    return ($value | Out-String).Trim()
}
function Snapshot([string]$Name) {
    Adb @('shell','uiautomator','dump','/sdcard/glitchos-qa.xml') | Out-Null
    Adb @('pull','/sdcard/glitchos-qa.xml',(Join-Path $qa ($Name+'.xml'))) | Out-Null
    return [xml](Get-Content (Join-Path $qa ($Name+'.xml')) -Raw)
}
function Find([xml]$Ui,[string]$Label) {
    $node=@($Ui.SelectNodes('//node') | Where-Object {$_.text -eq $Label -or $_.'content-desc' -eq $Label}) | Select-Object -First 1
    if(!$node){throw "UI item missing: $Label"}
    return $node
}
function Tap([xml]$Ui,[string]$Label,[switch]$Long) {
    $n=Find $Ui $Label
    $coords=[regex]::Matches($n.bounds,'\d+') | ForEach-Object {[int]$_.Value}
    $x=[int](($coords[0]+$coords[2])/2);$y=[int](($coords[1]+$coords[3])/2)
    if($Long){Adb @('shell','input','swipe',"$x","$y","$x","$y",'900') | Out-Null}
    else {Adb @('shell','input','tap',"$x","$y") | Out-Null}
}
function EnterSearch([string]$Query) {
    if((Adb @('shell','dumpsys','window')) -notmatch 'mCurrentFocus=.*com.glitchos.mobile') {throw 'Launcher lost focus; refusing to type in another app'}
    Adb @('shell','input','text',$Query) | Out-Null
}
function Check([bool]$Condition,[string]$Message) {if(!$Condition){throw "FAILED: $Message"};$results.Add("PASS: $Message");Write-Output "PASS: $Message"}
function Capture([string]$Name){Adb @('shell','screencap','-p',"/sdcard/glitchos-$Name.png") | Out-Null;Adb @('pull',"/sdcard/glitchos-$Name.png",(Join-Path $qa ($Name+'.png'))) | Out-Null}

$initialMediaAccess=(Adb @('shell','settings','get','secure','enabled_notification_listeners')) -match 'com.glitchos.mobile/com.glitchos.mobile.MediaAccessService'
Check ((Adb @('shell','cmd','role','get-role-holders','--user','0','android.app.role.HOME')) -eq 'com.glitchos.mobile') 'GlitchOS holds HOME role'
Adb @('shell','input','keyevent','KEYCODE_HOME') | Out-Null
$uiHome=Snapshot 'home'
Find $uiHome 'Screen Recorder' | Out-Null
Check $true 'Screen Recorder remains a home favorite'
Capture 'home'
Tap $uiHome 'Settings'
$settings=Snapshot 'android-settings'
Check ((Adb @('shell','dumpsys','window')) -match 'mCurrentFocus=.*com.android.settings') 'Settings launches from favorite'
Adb @('shell','input','keyevent','KEYCODE_HOME') | Out-Null
$uiHome=Snapshot 'home-return'
Check ((Adb @('shell','dumpsys','window')) -match 'mCurrentFocus=.*com.glitchos.mobile') 'HOME returns directly to GlitchOS'
Tap $uiHome '▦  Apps'
$library=Snapshot 'apps'
Find $library 'Apps suchen' | Out-Null
Capture 'apps'
Tap $library 'Apps suchen'
EnterSearch 'com.miui.screenrecorder'
$filtered=Snapshot 'search-package'
Find $filtered 'Screen Recorder' | Out-Null
Check ([bool]($filtered.SelectNodes('//node') | Where-Object {$_.text -match '^1 von '})) 'Search filters by package name'
Adb @('shell','input','keyevent','KEYCODE_BACK') | Out-Null
Capture 'search'
$filtered=Snapshot 'search-no-keyboard'
Tap $filtered 'Screen Recorder'
$recorder=Snapshot 'screen-recorder'
Check ((Adb @('shell','dumpsys','window')) -match 'Window.*com.miui.screenrecorder') 'Screen Recorder launches its floating controls from search'
Adb @('shell','input','keyevent','KEYCODE_HOME') | Out-Null
$uiHome=Snapshot 'home-after-recorder'
Tap $uiHome 'Messages' -Long
$actions=Snapshot 'favorite-actions'
Tap $actions 'Aus Favoriten entfernen'
$removed=Snapshot 'favorite-removed'
Check (-not [bool]($removed.SelectNodes('//node') | Where-Object {$_.'content-desc' -eq 'Messages'})) 'Long press removes a favorite'
Tap $removed '▦  Apps'
$library=Snapshot 'apps-for-favorite'
Tap $library 'Apps suchen'
EnterSearch 'com.google.android.apps.messaging'
Adb @('shell','input','keyevent','KEYCODE_BACK') | Out-Null
$messages=Snapshot 'messages-search'
Tap $messages 'Messages' -Long
$actions=Snapshot 'favorite-add-actions'
Tap $actions 'Zu Favoriten hinzufügen'
Adb @('shell','input','keyevent','KEYCODE_HOME') | Out-Null
$restored=Snapshot 'favorite-restored'
Find $restored 'Messages' | Out-Null
Check $true 'Favorite can be added again'
# Kill only the launcher background process; HOME must restart it and retain preferences.
Adb @('shell','am','start','-W','-a','android.settings.SETTINGS') | Out-Null
Adb @('shell','am','kill','com.glitchos.mobile') | Out-Null
Adb @('shell','input','keyevent','KEYCODE_HOME') | Out-Null
$persisted=Snapshot 'favorite-persisted'
Find $persisted 'Messages' | Out-Null
Check $true 'Favorites persist after process recreation'
Tap $persisted '♫  Medien'
$media=Snapshot 'media'
if(!$initialMediaAccess){Find $media 'Mediensteuerung aktivieren' | Out-Null}
else {Find $media 'Jetzt läuft' | Out-Null}
$currentMediaAccess=(Adb @('shell','settings','get','secure','enabled_notification_listeners')) -match 'com.glitchos.mobile/com.glitchos.mobile.MediaAccessService'
Check ($initialMediaAccess -eq $currentMediaAccess) 'Optional media permission remains as configured by the owner'
Capture 'media'
Adb @('shell','input','keyevent','KEYCODE_HOME') | Out-Null
Adb @('shell','input','keyevent','KEYCODE_APP_SWITCH') | Out-Null
$recents=Snapshot 'recents'
Capture 'recents'
Check ((Adb @('shell','dumpsys','window')) -match 'mCurrentFocus=.*com.mi.android.globallauncher') 'System Recents remains available through POCO service'
Adb @('shell','input','keyevent','KEYCODE_HOME') | Out-Null
Check ((Adb @('shell','dumpsys','activity','services','com.mi.android.globallauncher')) -match 'TouchInteractionService') 'Required POCO Quickstep service remains active'
Check ((Adb @('shell','settings','get','global','adb_enabled')) -eq '1') 'ADB remains enabled'
$results | Set-Content (Join-Path $qa 'test-results.txt')
Write-Output 'Device smoke tests completed.'

