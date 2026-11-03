<#
.SYNOPSIS
    Provisions the Render PostgreSQL database in the same workspace & region as the NT14 Gateway,
    wires DATABASE_URL + DEMO_TRAFFIC=false + IP_HASH_SALT on the gateway service, triggers a deploy,
    and verifies live PostgreSQL persistence.

.DESCRIPTION
    Prompts securely for your Render API key (never echoed or saved to disk) unless $env:RENDER_API_KEY
    is already set in your local PowerShell session.
#>
param(
    [string]$GatewayServiceName = "api-rate-limiter-gateway",
    [string]$DatabaseName = "nt14-gateway-db",
    [string]$GatewayPublicUrl = "https://api-rate-limiter-gateway-n0ab.onrender.com"
)

$ErrorActionPreference = "Stop"

# 1. Obtain Render API Key securely without pasting into chat
$apiKey = $env:RENDER_API_KEY
if ([string]::IsNullOrWhiteSpace($apiKey)) {
    $sec = Read-Host -Prompt "Paste your Render API Key (rnd_...)" -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec)
    try {
        $apiKey = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}
if ([string]::IsNullOrWhiteSpace($apiKey)) {
    throw "Render API Key is required."
}

$headers = @{
    "Authorization" = "Bearer $($apiKey.Trim())"
    "Accept"        = "application/json"
    "Content-Type"  = "application/json"
}

Write-Host "==> [1/6] Locating gateway service '$GatewayServiceName' on Render..." -ForegroundColor Cyan
$servicesResp = Invoke-RestMethod -Method Get -Uri "https://api.render.com/v1/services?limit=50" -Headers $headers
$gwItem = $servicesResp | Where-Object {
    $_.service.name -like "*$GatewayServiceName*" -or $_.service.serviceDetails.url -eq $GatewayPublicUrl
} | Select-Object -First 1

if (-not $gwItem) {
    throw "Could not find gateway service matching '$GatewayServiceName' or '$GatewayPublicUrl'."
}

$gw = $gwItem.service
$serviceId = $gw.id
$ownerId   = $gw.ownerId
$region    = $gw.serviceDetails.region
if ([string]::IsNullOrWhiteSpace($region)) { $region = "oregon" }

Write-Host "    Found service: $($gw.name) ($serviceId) | ownerId=$ownerId | region=$region" -ForegroundColor Green

Write-Host "==> [2/6] Checking for existing PostgreSQL instance '$DatabaseName' in workspace $ownerId..." -ForegroundColor Cyan
$pgList = Invoke-RestMethod -Method Get -Uri "https://api.render.com/v1/postgres?limit=50" -Headers $headers
$existingPg = $pgList | Where-Object { $_.postgres.name -eq $DatabaseName -and $_.postgres.ownerId -eq $ownerId } | Select-Object -First 1

if ($existingPg) {
    $pgId = $existingPg.postgres.id
    Write-Host "    Reusing existing PostgreSQL instance: $DatabaseName ($pgId)" -ForegroundColor Green
} else {
    Write-Host "    Creating new PostgreSQL instance '$DatabaseName' in region '$region'..." -ForegroundColor Yellow
    $createBody = @{
        name         = $DatabaseName
        ownerId      = $ownerId
        plan         = "free"
        region       = $region
        version      = "16"
        databaseName = "nt14_gateway"
        databaseUser = "nt14_gateway_user"
    } | ConvertTo-Json -Depth 5

    try {
        $createdPg = Invoke-RestMethod -Method Post -Uri "https://api.render.com/v1/postgres" -Headers $headers -Body $createBody
        $pgId = $createdPg.id
        Write-Host "    Created PostgreSQL instance: $DatabaseName ($pgId)" -ForegroundColor Green
    } catch {
        Write-Host "    Free plan unavailable or already used; retrying with 'basic_256mb'..." -ForegroundColor Yellow
        $createBody = @{
            name         = $DatabaseName
            ownerId      = $ownerId
            plan         = "basic_256mb"
            region       = $region
            version      = "16"
            databaseName = "nt14_gateway"
            databaseUser = "nt14_gateway_user"
        } | ConvertTo-Json -Depth 5
        $createdPg = Invoke-RestMethod -Method Post -Uri "https://api.render.com/v1/postgres" -Headers $headers -Body $createBody
        $pgId = $createdPg.id
        Write-Host "    Created PostgreSQL instance (basic_256mb): $DatabaseName ($pgId)" -ForegroundColor Green
    }
}

Write-Host "==> [3/6] Waiting for PostgreSQL connection info..." -ForegroundColor Cyan
$internalUrl = $null
$externalUrl = $null
for ($attempt = 1; $attempt -le 40; $attempt++) {
    try {
        $connInfo = Invoke-RestMethod -Method Get -Uri "https://api.render.com/v1/postgres/$pgId/connection-info" -Headers $headers
        $internalUrl = $connInfo.internalConnectionString
        $externalUrl = $connInfo.externalConnectionString
        if (-not [string]::IsNullOrWhiteSpace($internalUrl)) { break }
    } catch {
        # Still provisioning
    }
    Write-Host "    Waiting for database provisioning (attempt $attempt/40)..."
    Start-Sleep -Seconds 5
}

if ([string]::IsNullOrWhiteSpace($internalUrl)) {
    throw "Timed out waiting for PostgreSQL connection info for $pgId."
}
Write-Host "    PostgreSQL Internal Connection String ready." -ForegroundColor Green

Write-Host "==> [4/6] Updating environment variables on $($gw.name) ($serviceId)..." -ForegroundColor Cyan
$currentEnvResp = Invoke-RestMethod -Method Get -Uri "https://api.render.com/v1/services/$serviceId/env-vars?limit=100" -Headers $headers
$envMap = @{}
foreach ($item in $currentEnvResp) {
    $envMap[$item.envVar.key] = $item.envVar.value
}

# Generate a fresh 32-byte IP_HASH_SALT if not already set
if (-not $envMap.ContainsKey("IP_HASH_SALT") -or [string]::IsNullOrWhiteSpace($envMap["IP_HASH_SALT"])) {
    $saltBytes = New-Object byte[] 32
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($saltBytes)
    $envMap["IP_HASH_SALT"] = [Convert]::ToBase64String($saltBytes)
}

$envMap["DATABASE_URL"] = $internalUrl
$envMap["DEMO_TRAFFIC"] = "false"

$envPayload = @()
foreach ($k in $envMap.Keys) {
    $envPayload += @{ key = $k; value = [string]$envMap[$k] }
}

$putEnvBody = ConvertTo-Json -InputObject $envPayload -Depth 5
Invoke-RestMethod -Method Put -Uri "https://api.render.com/v1/services/$serviceId/env-vars" -Headers $headers -Body $putEnvBody | Out-Null
Write-Host "    Set DATABASE_URL, DEMO_TRAFFIC=false, and IP_HASH_SALT (preserved $($envMap.Count) total env vars)." -ForegroundColor Green

Write-Host "==> [5/6] Triggering Render deploy on $($gw.name)..." -ForegroundColor Cyan
$deployResp = Invoke-RestMethod -Method Post -Uri "https://api.render.com/v1/services/$serviceId/deploys" -Headers $headers -Body '{"clearCache":"do_not_clear"}'
Write-Host "    Deploy triggered: $($deployResp.id). Polling $GatewayPublicUrl/health until persistence.mode == 'postgres'..." -ForegroundColor Yellow

for ($i = 1; $i -le 60; $i++) {
    Start-Sleep -Seconds 10
    try {
        $health = Invoke-RestMethod -Method Get -Uri "$GatewayPublicUrl/health" -TimeoutSec 15
        $mode = $health.persistence.mode
        Write-Host "    [$i/60] /health -> status=$($health.status), persistence.mode=$mode, demoTraffic=$($health.demoTraffic)"
        if ($mode -eq "postgres") {
            Write-Host "==> [6/6] SUCCESS! Live gateway is now backed by PostgreSQL with demoTraffic=false!" -ForegroundColor Green
            $health | ConvertTo-Json -Depth 6 | Write-Host
            if (-not [string]::IsNullOrWhiteSpace($externalUrl)) {
                Write-Host "`nTo inspect live PostgreSQL counts from this machine, run:" -ForegroundColor Cyan
                Write-Host '  $env:JAVA_HOME = "d:\Android\.jdk17"'
                Write-Host "  .\gradlew.bat :gateway_server:dbQuery"
            }
            break
        }
    } catch {
        Write-Host "    [$i/60] Waiting for new container to come up..."
    }
}
