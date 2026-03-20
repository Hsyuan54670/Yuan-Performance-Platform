$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$composeFile = Join-Path $repoRoot "deploy\docker\docker-compose.infra.yml"

Write-Host "Starting YUAN V1 infra stack..." -ForegroundColor Cyan
docker compose -f $composeFile up -d

Write-Host "`nInfra started." -ForegroundColor Green
Write-Host "Recommended backend start order:" -ForegroundColor Yellow
Write-Host "1. yuan-auth"
Write-Host "2. yuan-test"
Write-Host "3. yuan-monitor"
Write-Host "4. yuan-analysis"
Write-Host "5. yuan-report"
Write-Host "6. yuan-gateway"
Write-Host "7. yuan-web"
