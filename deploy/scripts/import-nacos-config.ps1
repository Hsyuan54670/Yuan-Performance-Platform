param(
    [ValidateSet("dev", "test", "prod")]
    [string]$Env = "dev",
    [string]$ServerAddr = "127.0.0.1:8848",
    [string]$Namespace = "public",
    [string]$Group = "DEFAULT_GROUP",
    [string]$Username = "nacos",
    [string]$Password = "nacos"
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

$loginCandidates = @(
    "http://$ServerAddr/nacos/v1/auth/users/login",
    "http://$ServerAddr/nacos/v1/auth/login"
)

$accessToken = $null
foreach ($loginUri in $loginCandidates) {
    try {
        $loginResponse = Invoke-RestMethod -Method Post -Uri $loginUri -Body @{
            username = $Username
            password = $Password
        } -ContentType "application/x-www-form-urlencoded"

        if ($loginResponse -is [string]) {
            $accessToken = $loginResponse
        } elseif ($loginResponse.accessToken) {
            $accessToken = [string]$loginResponse.accessToken
        } elseif ($loginResponse.data -and $loginResponse.data.accessToken) {
            $accessToken = [string]$loginResponse.data.accessToken
        }

        if ($accessToken) {
            break
        }
    } catch {
        continue
    }
}

if (-not $accessToken) {
    throw "Failed to login to Nacos at $ServerAddr with username $Username"
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

    $uri = "http://$ServerAddr/nacos/v1/cs/configs?accessToken=$accessToken"
    $response = Invoke-RestMethod -Method Post -Uri $uri -Body $body -ContentType "application/x-www-form-urlencoded"

    if (-not ($response -eq $true -or [string]$response -eq "true")) {
        throw "Failed to import $($file.Name): $response"
    }

    Write-Host ("Imported {0} => {1}" -f $file.Name, $response) -ForegroundColor Green
}

Write-Host "Nacos import finished." -ForegroundColor Cyan