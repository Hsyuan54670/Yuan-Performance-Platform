param(
    [ValidateSet("dev", "test", "prod")]
    [string]$Env = "dev",
    [string]$ServerAddr = "127.0.0.1:8848",
    [string]$Namespace = "public",
    [string]$Group = "DEFAULT_GROUP"
)

$ErrorActionPreference = "Stop"
$baseDir = Split-Path -Parent $PSScriptRoot
$configDir = Join-Path $baseDir "nacos\$Env"

if (-not (Test-Path $configDir)) {
    throw "Config directory not found: $configDir"
}

$files = Get-ChildItem $configDir -Filter *.yml | Sort-Object Name
if (-not $files) {
    throw "No config files found under $configDir"
}

Write-Host "Importing Nacos configs from $configDir" -ForegroundColor Cyan

foreach ($file in $files) {
    $body = @{
        dataId  = $file.Name
        group   = $Group
        tenant  = $Namespace
        type    = "yaml"
        content = Get-Content $file.FullName -Raw
    }

    $uri = "http://$ServerAddr/nacos/v1/cs/configs"
    $response = Invoke-RestMethod -Method Post -Uri $uri -Body $body -ContentType "application/x-www-form-urlencoded"
    Write-Host ("Imported {0} => {1}" -f $file.Name, $response) -ForegroundColor Green
}

Write-Host "Nacos import finished." -ForegroundColor Cyan
