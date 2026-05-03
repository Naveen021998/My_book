$ErrorActionPreference = "Stop"

Write-Host "Starting Metro on port 8083 ..."
Set-Location "d:\training\new test sample\mobileappnative"
npm.cmd start -- --port 8083

