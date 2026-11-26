[CmdletBinding()]
param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$networkName = "minigo-app-net"
$containerName = "minigo-gateway"

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

Require-RunningContainer "minigo-client-app"
Require-RunningContainer "my-nacos"

docker network inspect $networkName *> $null
if ($LASTEXITCODE -ne 0) {
    docker network create $networkName *> $null
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to create Docker network '$networkName'."
    }
}

$networkDependencies = @(
    @{ Container = "my-nacos"; Alias = "minigo-nacos" },
    @{ Container = "minigo-client-app"; Alias = "minigo-client-app" }
)

foreach ($item in $networkDependencies) {
    $networks = docker inspect --format "{{json .NetworkSettings.Networks}}" $item.Container | ConvertFrom-Json
    if ($networks.PSObject.Properties.Name -notcontains $networkName) {
        docker network connect --alias $item.Alias $networkName $item.Container
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to connect '$($item.Container)' to '$networkName'."
        }
    }
}

Import-UserEnvironmentVariable "MINIGO_JWT_SECRET"

if (-not $SkipBuild) {
    mvn -q clean package
    if ($LASTEXITCODE -ne 0) {
        throw "Maven package failed."
    }
}

docker compose -f compose.yaml up -d --build --no-deps gateway
if ($LASTEXITCODE -ne 0) {
    throw "Gateway container failed to start."
}

$deadline = (Get-Date).AddSeconds(90)
$healthy = $false
while ((Get-Date) -lt $deadline) {
    try {
        $health = Invoke-RestMethod -Uri "http://127.0.0.1:9091/actuator/health" -TimeoutSec 3
        if ($health.status -eq "UP") {
            $healthy = $true
            break
        }
    } catch {
        # Keep polling while the gateway starts.
    }
    Start-Sleep -Seconds 2
}

if (-not $healthy) {
    docker logs --tail 120 $containerName
    throw "Gateway started, but the health check did not pass."
}

Write-Host "miniGo-gateway deployed: http://127.0.0.1:9000"
