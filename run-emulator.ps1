<#
.SYNOPSIS
    Builds the app and runs it in an Android emulator, creating the virtual device if needed.

.EXAMPLE
    .\run-emulator.ps1
    .\run-emulator.ps1 -AvdName MyPhone -AvdHome D:\Android\avd
#>
param(
    [string]$AvdName = "SciCalc_API36",
    [string]$SystemImage = "system-images;android-36;google_apis;x86_64",
    [string]$Device = "pixel_7",
    [string]$AvdHome = $(if ($env:ANDROID_AVD_HOME) { $env:ANDROID_AVD_HOME }
                         elseif (Test-Path "D:\Android\avd") { "D:\Android\avd" }
                         else { Join-Path $env:USERPROFILE ".android\avd" })
)
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

# Android SDK: ANDROID_HOME, otherwise sdk.dir from local.properties.
$sdk = $env:ANDROID_HOME
if (-not $sdk -and (Test-Path "local.properties")) {
    $line = Select-String -Path "local.properties" -Pattern '^sdk\.dir=(.+)$' | Select-Object -First 1
    if ($line) { $sdk = $line.Matches[0].Groups[1].Value -replace '\\:', ':' -replace '\\\\', '\' }
}
if (-not $sdk -or -not (Test-Path $sdk)) { throw "Android SDK not found: set ANDROID_HOME or sdk.dir in local.properties." }

$adb = Join-Path $sdk "platform-tools\adb.exe"
$emulator = Join-Path $sdk "emulator\emulator.exe"
$avdManager = Join-Path $sdk "cmdline-tools\latest\bin\avdmanager.bat"
$env:ANDROID_AVD_HOME = $AvdHome
$env:ANDROID_SDK_ROOT = $sdk

if (-not (& $emulator -list-avds | Select-String -SimpleMatch $AvdName)) {
    Write-Host "Creating virtual device $AvdName in $AvdHome ..."
    New-Item -ItemType Directory -Force $AvdHome | Out-Null
    "no" | & $avdManager create avd -n $AvdName -k $SystemImage -d $Device
}

if (-not (& $adb devices | Select-String "emulator-\d+\s+device")) {
    Write-Host "Starting emulator $AvdName ..."
    Start-Process $emulator -ArgumentList "-avd", $AvdName, "-no-boot-anim"
    & $adb wait-for-device
    & $adb shell 'while [ -z "$(getprop sys.boot_completed)" ]; do sleep 2; done'
}

Write-Host "Installing the app ..."
& .\gradlew.bat installDebug
if ($LASTEXITCODE -ne 0) { throw "Build failed." }
& $adb shell am start -n com.oop.calculator/.ui.MainActivity
Write-Host "Running. Logs: & '$adb' logcat -s SciCalc"
