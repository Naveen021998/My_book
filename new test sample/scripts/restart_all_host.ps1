param(
  [switch]$WithAdbReverse,
  [string]$Adb = "C:\Users\E515\AppData\Local\Android\Sdk\platform-tools\adb.exe"
)

$ErrorActionPreference = "Stop"

function Stop-Listeners([int[]]$Ports) {
  $netstat = netstat -ano | Select-String -Pattern "LISTENING"
  $pids = New-Object System.Collections.Generic.HashSet[int]

  foreach ($port in $Ports) {
    $matches = $netstat | Select-String -Pattern (":$port\s")
    foreach ($m in $matches) {
      $parts = ($m.Line -split "\s+") | Where-Object { $_ -ne "" }
      $pidRaw = $parts[-1]
      if ($pidRaw -match "^\d+$") {
        [void]$pids.Add([int]$pidRaw)
      }
    }
  }

  foreach ($pid in $pids) {
    try {
      $proc = Get-Process -Id $pid -ErrorAction Stop
      Write-Host "Stopping PID $pid ($($proc.ProcessName)) ..."
      Stop-Process -Id $pid -Force
    } catch {
      # ignore (process already stopped or access denied)
    }
  }
}

Write-Host "Restarting (host-mode) backend + gateway + metro ..."

# Ports used by this repo in host-mode:
# - 8080: api-gateway-service
# - 8081..8086: microservices
# - 8083: metro
Stop-Listeners -Ports @(8080,8081,8082,8083,8084,8085,8086)

function Start-Term([string]$Title, [string]$Command) {
  # New terminal window that stays open (`-NoExit`) so logs are visible.
  Start-Process -FilePath powershell.exe -ArgumentList @(
    "-NoExit",
    "-Command",
    "$Host.UI.RawUI.WindowTitle = '$Title'; $Command"
  )
}

$root = "d:\training\new test sample"

Start-Term "auth-service" "cd '$root\authorization-service'; mvn spring-boot:run"
Start-Term "core-service" "cd '$root\core-application-service'; mvn spring-boot:run"
Start-Term "user-service" "cd '$root\user-service'; mvn spring-boot:run"
Start-Term "notification-service" "cd '$root\notification-service'; mvn spring-boot:run"
Start-Term "file-service" "cd '$root\file-service'; mvn spring-boot:run"
Start-Term "api-gateway" "cd '$root\api-gateway-service'; mvn spring-boot:run"
Start-Term "metro-8083" "cd '$root\mobileappnative'; npm.cmd start -- --port 8083"

if ($WithAdbReverse) {
  if (Test-Path $Adb) {
    & $Adb devices -l
    & $Adb reverse tcp:8083 tcp:8083 | Out-Null
    & $Adb reverse --list
  } else {
    Write-Host "adb not found at: $Adb"
  }
}

Write-Host "Started. Gateway health should be: http://localhost:8080/health"

