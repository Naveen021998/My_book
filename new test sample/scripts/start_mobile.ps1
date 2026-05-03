param(
  [string]$RepoRoot = "d:\training\new test sample"
)

$ErrorActionPreference = 'Stop'

$mobileRoot = Join-Path $RepoRoot 'mobileappnative'

Write-Host "Starting Metro on port 8083 in a new window..." -ForegroundColor Cyan

Start-Process -WindowStyle Normal -FilePath "C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe" -ArgumentList @(
  '-NoExit',
  '-Command',
  "& { cd `"$mobileRoot`"; npm.cmd start -- --reset-cache }"
) | Out-Null

Write-Host ""
Write-Host "Next (USB device):" -ForegroundColor Yellow
Write-Host "  & \"C:\Users\E515\AppData\Local\Android\Sdk\platform-tools\adb.exe\" reverse tcp:8083 tcp:8083"
Write-Host "  & \"C:\Users\E515\AppData\Local\Android\Sdk\platform-tools\adb.exe\" reverse tcp:8080 tcp:8080"
Write-Host ""
Write-Host "Install debug APK:" -ForegroundColor Yellow
Write-Host "  cd $mobileRoot"
Write-Host "  npm.cmd run android"
