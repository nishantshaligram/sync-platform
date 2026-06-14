# Stops the full sync-platform stack: the frontend dev server plus all docker-compose services.
$ErrorActionPreference = "Continue"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

$pidFile = Join-Path $root ".frontend.pid"
if (Test-Path $pidFile) {
    $frontendPid = Get-Content $pidFile
    Write-Host "Stopping frontend dev server (PID $frontendPid)..." -ForegroundColor Cyan
    if (Get-Process -Id $frontendPid -ErrorAction SilentlyContinue) {
        taskkill /PID $frontendPid /T /F | Out-Null
    }
    Remove-Item $pidFile -Force
} else {
    Write-Host "No frontend PID file found - skipping frontend shutdown." -ForegroundColor Yellow
}

Write-Host "Stopping backend services (docker compose)..." -ForegroundColor Cyan
docker compose stop

Write-Host ""
Write-Host "All services stopped." -ForegroundColor Green
Write-Host "Run .\start.ps1 to start everything again."
