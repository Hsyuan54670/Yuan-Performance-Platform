$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$composeFile = Join-Path $repoRoot "deploy\docker\docker-compose.infra.yml"

Write-Host "Stopping YUAN V1 infra stack..." -ForegroundColor Cyan
docker compose -f $composeFile down
Write-Host "Infra stopped." -ForegroundColor Green
