[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$ProbeApk,[string]$Device='331904ac',[string]$AdbPath=(Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'))
$ErrorActionPreference='Stop'
$qa=Join-Path $PSScriptRoot 'output\qa'
function Run-Adb([string[]]$Arguments){$value=& $AdbPath -s $Device @Arguments 2>&1;if($LASTEXITCODE){throw ($value|Out-String)};($value|Out-String).Trim()}
function Read-Ui([string]$Name){Run-Adb @('shell','uiautomator','dump','/sdcard/glitchos-package-qa.xml')|Out-Null;Run-Adb @('pull','/sdcard/glitchos-package-qa.xml',(Join-Path $qa ($Name+'.xml')))|Out-Null;[xml](Get-Content (Join-Path $qa ($Name+'.xml')) -Raw)}
function Touch-Label([xml]$Ui,[string]$Label){$node=$Ui.SelectNodes('//node')|Where-Object {$_.text -eq $Label -or $_.'content-desc' -eq $Label}|Select-Object -First 1;if(!$node){throw "Missing: $Label"};$b=[regex]::Matches($node.bounds,'\d+')|ForEach-Object{[int]$_.Value};Run-Adb @('shell','input','tap',([int](($b[0]+$b[2])/2)).ToString(),([int](($b[1]+$b[3])/2)).ToString())|Out-Null}
if((Run-Adb @('shell','pm','list','packages','--user','0','com.glitchos.qaprobe')) -match 'package:com.glitchos.qaprobe'){throw 'Probe already installed; refusing to overwrite unknown state'}
try{
    Run-Adb @('shell','input','keyevent','KEYCODE_HOME')|Out-Null
    Run-Adb @('install','-r',$ProbeApk)|Out-Null
    $ui=Read-Ui 'probe-installed-home'
    Touch-Label $ui '▦  Apps'
    $ui=Read-Ui 'probe-library'
    Touch-Label $ui 'Apps suchen'
    if((Run-Adb @('shell','dumpsys','window')) -notmatch 'mCurrentFocus=.*com.glitchos.mobile'){throw 'Launcher not focused; refusing input'}
    Run-Adb @('shell','input','text','com.glitchos.qaprobe')|Out-Null
    Run-Adb @('shell','input','keyevent','KEYCODE_BACK')|Out-Null
    $ui=Read-Ui 'probe-search'
    Touch-Label $ui 'GlitchOS QA Probe'
    $ui=Read-Ui 'probe-launched'
    if((Run-Adb @('shell','dumpsys','window')) -notmatch 'mCurrentFocus=.*com.glitchos.qaprobe'){throw 'New package did not launch'}
    'PASS: Newly installed developer APK appears and launches'|Tee-Object -FilePath (Join-Path $qa 'package-test-results.txt')
    Run-Adb @('shell','input','keyevent','KEYCODE_HOME')|Out-Null
    $ui=Read-Ui 'probe-return'
    Touch-Label $ui '▦  Apps'
    $ui=Read-Ui 'probe-before-remove'
    Touch-Label $ui 'Apps suchen'
    Run-Adb @('shell','input','text','com.glitchos.qaprobe')|Out-Null
    Run-Adb @('shell','input','keyevent','KEYCODE_BACK')|Out-Null
    $ui=Read-Ui 'probe-visible-before-remove'
    Run-Adb @('uninstall','com.glitchos.qaprobe')|Out-Null
    $ui=Read-Ui 'probe-after-remove'
    if($ui.SelectNodes('//node')|Where-Object {$_.'content-desc' -eq 'GlitchOS QA Probe'}){throw 'Uninstalled package remains in grid'}
    if(-not ($ui.SelectNodes('//node')|Where-Object {$_.text -eq 'Keine passende App gefunden.'})){throw 'Expected empty search after removal'}
    'PASS: Removing developer APK refreshes visible search immediately'|Tee-Object -FilePath (Join-Path $qa 'package-test-results.txt') -Append
}finally{
    $installed=Run-Adb @('shell','pm','list','packages','--user','0','com.glitchos.qaprobe')
    if($installed -match 'package:com.glitchos.qaprobe'){Run-Adb @('uninstall','com.glitchos.qaprobe')|Out-Null}
    Run-Adb @('shell','input','keyevent','KEYCODE_HOME')|Out-Null
}
