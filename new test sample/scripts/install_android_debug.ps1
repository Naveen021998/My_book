param(
  [string]$Adb = "C:\Users\E515\AppData\Local\Android\Sdk\platform-tools\adb.exe",
  [string]$SdkRoot = "C:\Users\E515\AppData\Local\Android\Sdk"
)

$ErrorActionPreference = "Stop"

$env:ANDROID_SDK_ROOT = $SdkRoot
$env:ANDROID_HOME = $SdkRoot

Set-Location "d:\training\new test sample\mobileappnative\android"
.\gradlew.bat :app:installDebug --no-daemon

& $Adb shell monkey -p com.mobileappnative -c android.intent.category.LAUNCHER 1 | Out-Null
Write-Host "Installed and launched com.mobileappnative"

