param(
  [string]$RepoRoot = "d:\training\new test sample"
)

$ErrorActionPreference = 'Stop'

function Start-ServiceWindow([string]$name, [string]$workdir) {
  $cmd = "cd `"$workdir`"; mvn spring-boot:run"
  Start-Process -WindowStyle Normal -FilePath "C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe" -ArgumentList @(
    '-NoExit',
    '-Command',
    "& { $cmd }"
  ) | Out-Null
  Write-Host "Started: $name ($workdir)"
}

Start-ServiceWindow 'api-gateway-service' (Join-Path $RepoRoot 'api-gateway-service')
Start-ServiceWindow 'authorization-service' (Join-Path $RepoRoot 'authorization-service')
Start-ServiceWindow 'core-application-service' (Join-Path $RepoRoot 'core-application-service')
Start-ServiceWindow 'user-service' (Join-Path $RepoRoot 'user-service')
Start-ServiceWindow 'notification-service' (Join-Path $RepoRoot 'notification-service')
Start-ServiceWindow 'file-service' (Join-Path $RepoRoot 'file-service')

Write-Host ""
Write-Host "Next: run .\scripts\verify_health.ps1" 
