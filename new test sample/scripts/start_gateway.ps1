param(
  [string]$AuthUpstream = "host.docker.internal:8081",
  [string]$CoreUpstream = "host.docker.internal:8082",
  [string]$UserUpstream = "host.docker.internal:8084",
  [string]$NotificationUpstream = "host.docker.internal:8085",
  [string]$FileUpstream = "host.docker.internal:8086"
)

$ErrorActionPreference = "Stop"

Write-Host "Starting gateway (single URL) on http://localhost:8080 ..."
$env:AUTH_UPSTREAM = $AuthUpstream
$env:CORE_UPSTREAM = $CoreUpstream
$env:USER_UPSTREAM = $UserUpstream
$env:NOTIFICATION_UPSTREAM = $NotificationUpstream
$env:FILE_UPSTREAM = $FileUpstream

docker compose up -d gateway | Out-Null

Write-Host "Gateway started. Health:"
try {
  Invoke-RestMethod http://localhost:8080/health | Out-String | Write-Host
} catch {
  Write-Host "Gateway health check failed. Is Docker running?"
  throw
}
