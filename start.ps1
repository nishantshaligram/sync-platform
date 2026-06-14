# Starts the full sync-platform stack: all docker-compose services plus the frontend dev server.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

Write-Host "Starting backend services (docker compose)..." -ForegroundColor Cyan
docker compose up -d

Write-Host "Waiting for gateway to become healthy..." -ForegroundColor Cyan
$timeoutSeconds = 180
$elapsed = 0
$status = ""
while ($elapsed -lt $timeoutSeconds) {
    $status = docker inspect --format='{{.State.Health.Status}}' gateway 2>$null
    if ($status -eq "healthy") { break }
    Start-Sleep -Seconds 3
    $elapsed += 3
}
if ($status -eq "healthy") {
    Write-Host "Gateway is healthy." -ForegroundColor Green
} else {
    Write-Warning "Gateway did not report healthy within $timeoutSeconds seconds (status: $status). Continuing anyway."
}

Write-Host "Starting frontend dev server..." -ForegroundColor Cyan
$frontendDir = Join-Path $root "frontend"
$pidFile = Join-Path $root ".frontend.pid"

$proc = Start-Process -FilePath "npm.cmd" -ArgumentList "run", "dev" `
    -WorkingDirectory $frontendDir -PassThru `
    -RedirectStandardOutput (Join-Path $root "frontend.log") `
    -RedirectStandardError (Join-Path $root "frontend.err.log")
$proc.Id | Out-File -FilePath $pidFile -Encoding ascii

Write-Host ""
Write-Host "All services started." -ForegroundColor Green
Write-Host "  Frontend:      http://localhost:5173"
Write-Host "  Gateway:       http://localhost:8080"
Write-Host "  Eureka:        http://localhost:8761"
Write-Host "  Config Server: http://localhost:8888"
Write-Host ""
Write-Host "Frontend logs: frontend.log / frontend.err.log"
Write-Host "Run .\stop.ps1 to stop everything."
