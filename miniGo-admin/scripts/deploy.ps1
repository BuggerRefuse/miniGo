[CmdletBinding()]
param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$networkName = "minigo-app-net"
$containerName = "minigo-admin"

Set-Location $projectRoot

function Require-RunningContainer {
    param([string]$Name)

    $running = docker inspect --format "{{.State.Running}}" $Name 2>$null
    if ($LASTEXITCODE -ne 0 -or $running -ne "true") {
        throw "Dependency container '$Name' is not running."
    }
}

function Import-UserEnvironmentVariable {
    param([string]$Name)

    $value = [Environment]::GetEnvironmentVariable($Name, "Process")
    if ([string]::IsNullOrWhiteSpace($value)) {
        $value = [Environment]::GetEnvironmentVariable($Name, "User")
    }
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "Missing environment variable '$Name'."
    }
    Set-Item -Path "Env:$Name" -Value $value
}

Require-RunningContainer "my-mysql"
Require-RunningContainer "MyRedis"
Require-RunningContainer "my-nacos"

foreach ($name in @("MINIGO_DB_PASSWORD", "MINIGO_JWT_SECRET")) {
    Import-UserEnvironmentVariable $name
}

if (-not $SkipBuild) {
    mvn -q clean package
    if ($LASTEXITCODE -ne 0) {
        throw "Maven package failed."
    }
}

docker network inspect $networkName *> $null
if ($LASTEXITCODE -ne 0) {
    docker network create $networkName *> $null
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to create Docker network '$networkName'."
    }
}

$infrastructure = @(
    @{ Container = "my-mysql"; Alias = "minigo-mysql" },
    @{ Container = "MyRedis"; Alias = "minigo-redis" },
    @{ Container = "my-nacos"; Alias = "minigo-nacos" }
)

foreach ($item in $infrastructure) {
    $networks = docker inspect --format "{{json .NetworkSettings.Networks}}" $item.Container | ConvertFrom-Json
    if ($networks.PSObject.Properties.Name -notcontains $networkName) {
        docker network connect --alias $item.Alias $networkName $item.Container
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to connect '$($item.Container)' to '$networkName'."
        }
    }
}

docker compose -f compose.yaml up -d --build --no-deps admin
if ($LASTEXITCODE -ne 0) {
    throw "Admin container failed to start."
}

$deadline = (Get-Date).AddSeconds(90)
$healthy = $false
while ((Get-Date) -lt $deadline) {
    $running = docker inspect --format "{{.State.Running}}" $containerName 2>$null
    $logs = docker logs --tail 200 $containerName 2>&1 | Out-String
    if ($LASTEXITCODE -eq 0 -and $running -eq "true" -and
            $logs -match "Started MiniGoAdminApplication") {
        $healthy = $true
        break
    }
    Start-Sleep -Seconds 2
}

if (-not $healthy) {
    docker logs --tail 120 $containerName
    throw "Admin container started, but the application did not finish startup."
}

Write-Host "miniGo-admin deployed internally on port 8082; use http://127.0.0.1:9000"
