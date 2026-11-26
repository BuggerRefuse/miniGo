[CmdletBinding()]
param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$networkName = "minigo-app-net"
$containerName = "minigo-client-app"

Set-Location $projectRoot

function Require-RunningContainer {
    param([string]$Name)

    $running = docker inspect --format "{{.State.Running}}" $Name 2>$null
    if ($LASTEXITCODE -ne 0 -or $running -ne "true") {
        throw "基础设施容器 $Name 未运行。"
    }
}

function Import-UserEnvironmentVariable {
    param([string]$Name)

    $value = [Environment]::GetEnvironmentVariable($Name, "Process")
    if ([string]::IsNullOrWhiteSpace($value)) {
        $value = [Environment]::GetEnvironmentVariable($Name, "User")
    }
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "缺少环境变量 $Name。"
    }
    Set-Item -Path "Env:$Name" -Value $value
}

Require-RunningContainer "my-mysql"
Require-RunningContainer "MyRedis"
Require-RunningContainer "my-nacos"

foreach ($name in @(
        "MINIGO_DB_PASSWORD",
        "MINIGO_JWT_SECRET",
        "MINIGO_WECHAT_APP_ID",
        "MINIGO_WECHAT_APP_SECRET"
    )) {
    Import-UserEnvironmentVariable $name
}

if (-not $SkipBuild) {
    mvn -q clean package
    if ($LASTEXITCODE -ne 0) {
        throw "Maven 打包失败。"
    }
}

docker network inspect $networkName *> $null
if ($LASTEXITCODE -ne 0) {
    docker network create $networkName *> $null
    if ($LASTEXITCODE -ne 0) {
        throw "创建 Docker 网络 $networkName 失败。"
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
            throw "将 $($item.Container) 接入 $networkName 失败。"
        }
    }
}

docker compose -f compose.yaml up -d --build --no-deps app
if ($LASTEXITCODE -ne 0) {
    throw "应用容器启动失败。"
}

$deadline = (Get-Date).AddSeconds(90)
$healthy = $false
while ((Get-Date) -lt $deadline) {
    try {
        $health = Invoke-RestMethod -Uri "http://127.0.0.1:8081/actuator/health" -TimeoutSec 3
        if ($health.status -eq "UP") {
            $healthy = $true
            break
        }
    } catch {
        # Keep polling while the Spring context starts.
    }
    Start-Sleep -Seconds 2
}

if (-not $healthy) {
    docker logs --tail 120 $containerName
    throw "容器已启动，但健康检查未通过。"
}

Write-Host "miniGo-client 已部署：http://127.0.0.1:8081"
