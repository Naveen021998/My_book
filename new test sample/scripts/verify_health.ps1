param(
  [string]$GatewayBaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = 'Continue'

$endpoints = @(
  "$GatewayBaseUrl/health",
  "$GatewayBaseUrl/api/v1/auth/health",
  "$GatewayBaseUrl/api/v1/health",
  "$GatewayBaseUrl/api/v1/users/health",
  "$GatewayBaseUrl/api/v1/notifications/health",
  "$GatewayBaseUrl/api/v1/files/health"
)

Write-Host "Checking gateway health endpoints..." -ForegroundColor Cyan

foreach ($url in $endpoints) {
  try {
    $resp = Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 5
    Write-Host "OK  $url" -ForegroundColor Green
  } catch {
    Write-Host "FAIL $url" -ForegroundColor Red
    Write-Host "     $($_.Exception.Message)" -ForegroundColor DarkRed
  }
}

Write-Host ""
Write-Host "If any are FAIL: verify the target service is running and gateway routes are correct." -ForegroundColor Yellow
