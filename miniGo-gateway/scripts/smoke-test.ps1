[CmdletBinding()]
param(
    [string]$ApiBaseUrl = "http://127.0.0.1:9000",
    [string]$ManagementBaseUrl = "http://127.0.0.1:9091",
    [string]$Username = "demo",
    [string]$Password = "123456"
)

$ErrorActionPreference = "Stop"

function Invoke-HttpRequest {
    param(
        [string]$Uri,
        [string]$Method = "GET",
        [hashtable]$Headers = @{},
        [int]$TimeoutSec = 10
    )

    try {
        $response = Invoke-WebRequest -Uri $Uri -Method $Method -Headers $Headers `
            -UseBasicParsing -TimeoutSec $TimeoutSec
        return [PSCustomObject]@{
            StatusCode = [int]$response.StatusCode
            Headers = $response.Headers
        }
    } catch {
        $response = $_.Exception.Response
        if ($null -eq $response) {
            throw
        }
        return [PSCustomObject]@{
            StatusCode = [int]$response.StatusCode
            Headers = $response.Headers
        }
    }
}

$health = Invoke-RestMethod -Uri "$ManagementBaseUrl/actuator/health" -TimeoutSec 10
if ($health.status -ne "UP") {
    throw "Gateway health check did not pass."
}

$publicActuator = Invoke-HttpRequest -Uri "$ApiBaseUrl/actuator/health"
if ($publicActuator.StatusCode -eq 200) {
    throw "Actuator must not be exposed on the business port."
}

$routes = Invoke-RestMethod -Uri "$ManagementBaseUrl/actuator/gateway/routes" -TimeoutSec 10
$routeIds = @($routes | ForEach-Object { $_.route_id })
if ($routeIds -notcontains "mini-go-client-api") {
    throw "Route 'mini-go-client-api' was not found."
}

$requestId = "smoke-test-0001"
$loginPage = Invoke-HttpRequest -Uri "$ApiBaseUrl/login.html" `
    -Headers @{ "X-Request-Id" = $requestId }
if ($loginPage.StatusCode -ne 200) {
    throw "Login page was not available through the gateway."
}
if ($loginPage.Headers["X-Request-Id"] -ne $requestId) {
    throw "The gateway did not return the expected Request ID."
}

$unauthorized = Invoke-HttpRequest -Uri "$ApiBaseUrl/api/auth/me"
if ($unauthorized.StatusCode -ne 401) {
    throw "An unauthenticated protected request must return 401."
}

$cors = Invoke-HttpRequest -Method Options -Uri "$ApiBaseUrl/api/auth/me" `
    -Headers @{
        Origin = "http://localhost:3000"
        "Access-Control-Request-Method" = "GET"
    }
if ($cors.StatusCode -ne 200 -or
        [string]::IsNullOrWhiteSpace($cors.Headers["Access-Control-Allow-Origin"])) {
    throw "The allowed CORS preflight request did not pass."
}

$disallowedCors = Invoke-HttpRequest -Method Options -Uri "$ApiBaseUrl/api/auth/me" `
    -Headers @{
        Origin = "https://evil.example"
        "Access-Control-Request-Method" = "GET"
    }
if (-not [string]::IsNullOrWhiteSpace(
        $disallowedCors.Headers["Access-Control-Allow-Origin"]
    )) {
    throw "An untrusted origin must not receive CORS permission."
}

$body = @{ username = $Username; password = $Password } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri "$ApiBaseUrl/api/auth/login" `
    -ContentType "application/json" -Body $body -TimeoutSec 15
if ([string]::IsNullOrWhiteSpace($login.accessToken) -or
        [string]::IsNullOrWhiteSpace($login.refreshToken)) {
    throw "Login through the gateway did not return both tokens."
}

$headers = @{ Authorization = "Bearer $($login.accessToken)" }
$me = Invoke-RestMethod -Uri "$ApiBaseUrl/api/auth/me" -Headers $headers -TimeoutSec 10
if ($null -eq $me.id) {
    throw "The current-user request through the gateway failed."
}

$logoutBody = @{ refreshToken = $login.refreshToken } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$ApiBaseUrl/api/auth/logout" `
    -ContentType "application/json" -Body $logoutBody -TimeoutSec 10 | Out-Null

Write-Host "Gateway smoke test passed: health=UP, actuator-hidden=OK, routes=OK, request-id=OK, cors=OK, login=OK, auth=OK, logout=OK"
