<#
.SYNOPSIS
JWT smoke test for ApiCargoHub auth/authorization flow.

.DESCRIPTION
Runs a reusable smoke test covering register/login and role-based access to /api/facturas.
Optionally starts the API with JWT env vars.

.USAGE
# Basic run against already running API
pwsh -File .\scripts\smoke-jwt.ps1

# Custom base URL and credentials
pwsh -File .\scripts\smoke-jwt.ps1 -BaseUrl "http://localhost:8080" -ClientePassword "1234"

# Start API first (requires Java 17 + Maven wrapper in ./api)
pwsh -File .\scripts\smoke-jwt.ps1 -StartApi

# Provide explicit cliente email
pwsh -File .\scripts\smoke-jwt.ps1 -ClienteEmail "cliente.smoke@example.com"
#>

[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$JwtSecret = "local-dev-jwt-secret-change-me-32+chars",
    [int]$JwtExpirationMs = 3600000,
    [string]$ClienteEmail,
    [string]$ClientePassword = "1234",
    [string]$AdminEmail = "admin@admin.com",
    [string]$AdminPassword = "admin",
    [switch]$StartApi
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ClienteEmail)) {
    $ClienteEmail = "cliente.smoke.$((Get-Date).ToString('yyyyMMddHHmmss'))@example.com"
}

$failures = New-Object System.Collections.Generic.List[string]
$apiProcess = $null

function Write-Pass([string]$Message) {
    Write-Host "PASS - $Message" -ForegroundColor Green
}

function Write-Fail([string]$Message) {
    Write-Host "FAIL - $Message" -ForegroundColor Red
}

function Add-Failure([string]$Step, [string]$Message) {
    $script:failures.Add("${Step}: $Message")
    Write-Fail "$Step -> $Message"
}

function Encode([string]$value) {
    return [System.Uri]::EscapeDataString($value)
}

function Read-HttpErrorBody($response) {
    try {
        if ($null -eq $response) {
            return $null
        }

        $stream = $response.GetResponseStream()
        if ($null -eq $stream) {
            return $null
        }

        $reader = New-Object System.IO.StreamReader($stream)
        $text = $reader.ReadToEnd()
        $reader.Dispose()
        return $text
    }
    catch {
        return $null
    }
}

function Invoke-Http {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Url,
        [hashtable]$Headers
    )

    try {
        $params = @{
            Method      = $Method
            Uri         = $Url
            ErrorAction = "Stop"
        }

        if ($Headers) {
            $params["Headers"] = $Headers
        }

        if ($PSVersionTable.PSVersion.Major -lt 6) {
            $params["UseBasicParsing"] = $true
        }

        $resp = Invoke-WebRequest @params
        $body = $resp.Content
        $json = $null

        if (-not [string]::IsNullOrWhiteSpace($body) -and $body.TrimStart().StartsWith("{")) {
            try { $json = $body | ConvertFrom-Json } catch {}
        }

        return [pscustomobject]@{
            StatusCode = [int]$resp.StatusCode
            BodyText   = $body
            BodyJson   = $json
        }
    }
    catch {
        $webResp = $_.Exception.Response
        if ($null -eq $webResp) {
            throw
        }

        $statusCode = 0
        try { $statusCode = [int]$webResp.StatusCode } catch {}

        $body = Read-HttpErrorBody -response $webResp
        $json = $null
        if (-not [string]::IsNullOrWhiteSpace($body) -and $body.TrimStart().StartsWith("{")) {
            try { $json = $body | ConvertFrom-Json } catch {}
        }

        return [pscustomobject]@{
            StatusCode = $statusCode
            BodyText   = $body
            BodyJson   = $json
        }
    }
}

function Is-AlreadyExistsResponse($resp) {
    if ($resp.StatusCode -eq 200) {
        return $true
    }

    if ($resp.StatusCode -ne 400) {
        return $false
    }

    $text = ""
    if ($resp.BodyText) {
        $text = [string]$resp.BodyText
    }

    return ($text -match "(?i)existe|exists|duplic|registrad")
}

function Ensure-Java17 {
    try {
        $versionOut = (& java -version 2>&1 | Out-String)
    }
    catch {
        throw "Java no está disponible en PATH. Instala/configura Java 17 y vuelve a intentar."
    }

    $major = $null
    if ($versionOut -match '"(?<v>\d+)(\.\d+)?') {
        $major = [int]$Matches['v']
    }

    if ($major -ne 17) {
        throw "Se requiere Java 17 para -StartApi. Detectado: $versionOut"
    }
}

function Wait-ApiReachable([string]$url, [int]$timeoutSeconds = 90) {
    $deadline = (Get-Date).AddSeconds($timeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $probe = Invoke-Http -Method "GET" -Url "$url/api/facturas"
            if ($probe.StatusCode -in 200, 401, 403) {
                return $true
            }
        }
        catch {}

        Start-Sleep -Seconds 2
    }

    return $false
}

try {
    $BaseUrl = $BaseUrl.TrimEnd("/")

    if ($StartApi) {
        Ensure-Java17

        $apiDir = (Resolve-Path (Join-Path $PSScriptRoot "..\api")).Path
        $mvnwCmd = Join-Path $apiDir "mvnw.cmd"
        if (-not (Test-Path $mvnwCmd)) {
            throw "No se encontró $mvnwCmd. No se puede iniciar la API automáticamente."
        }

        $env:JWT_SECRET = $JwtSecret
        $env:JWT_EXPIRATION_MS = [string]$JwtExpirationMs

        Write-Host "Iniciando API en segundo plano..." -ForegroundColor Cyan
        $apiProcess = Start-Process -FilePath $mvnwCmd -ArgumentList "spring-boot:run" -WorkingDirectory $apiDir -PassThru

        if (-not (Wait-ApiReachable -url $BaseUrl -timeoutSeconds 90)) {
            throw "La API no respondió en $BaseUrl tras iniciar el proceso. Revisa logs de Spring/Maven y conexión a PostgreSQL."
        }

        Write-Host "API disponible en $BaseUrl" -ForegroundColor Cyan
    }
    else {
        if (-not (Wait-ApiReachable -url $BaseUrl -timeoutSeconds 8)) {
            throw "No se puede conectar a la API en $BaseUrl. Inicia el backend (o usa -StartApi) y vuelve a intentar."
        }
    }

    # 1) register CLIENTE (tolerate already exists)
    $registerClienteUrl = "$BaseUrl/api/auth/register?email=$(Encode $ClienteEmail)&password=$(Encode $ClientePassword)&rol=CLIENTE"
    $registerClienteResp = Invoke-Http -Method "POST" -Url $registerClienteUrl
    if (Is-AlreadyExistsResponse -resp $registerClienteResp) {
        Write-Pass "1) register CLIENTE ($ClienteEmail)"
    }
    else {
        Add-Failure "1) register CLIENTE" "HTTP $($registerClienteResp.StatusCode). Body: $($registerClienteResp.BodyText)"
    }

    # 2) login CLIENTE and verify token present
    $loginClienteUrl = "$BaseUrl/api/auth/login?email=$(Encode $ClienteEmail)&password=$(Encode $ClientePassword)"
    $loginClienteResp = Invoke-Http -Method "POST" -Url $loginClienteUrl
    $clienteToken = $null
    if ($loginClienteResp.StatusCode -eq 200 -and $null -ne $loginClienteResp.BodyJson) {
        $clienteToken = $loginClienteResp.BodyJson.accessToken
    }

    if (-not [string]::IsNullOrWhiteSpace([string]$clienteToken)) {
        Write-Pass "2) login CLIENTE returns access token"
    }
    else {
        Add-Failure "2) login CLIENTE" "Token ausente o login fallido (HTTP $($loginClienteResp.StatusCode))."
    }

    # 3) GET /api/facturas without token -> expect 401
    $facturasNoTokenResp = Invoke-Http -Method "GET" -Url "$BaseUrl/api/facturas"
    if ($facturasNoTokenResp.StatusCode -eq 401) {
        Write-Pass "3) GET /api/facturas sin token -> 401"
    }
    else {
        Add-Failure "3) GET /api/facturas sin token" "Esperado 401, recibido $($facturasNoTokenResp.StatusCode)."
    }

    # 4) GET /api/facturas with CLIENTE token -> expect 403
    if ([string]::IsNullOrWhiteSpace([string]$clienteToken)) {
        Add-Failure "4) GET /api/facturas con token CLIENTE" "No se pudo ejecutar porque no hay token CLIENTE."
    }
    else {
        $clienteHeaders = @{ Authorization = "Bearer $clienteToken" }
        $facturasClienteResp = Invoke-Http -Method "GET" -Url "$BaseUrl/api/facturas" -Headers $clienteHeaders
        if ($facturasClienteResp.StatusCode -eq 403) {
            Write-Pass "4) GET /api/facturas con CLIENTE -> 403"
        }
        else {
            Add-Failure "4) GET /api/facturas con CLIENTE" "Esperado 403, recibido $($facturasClienteResp.StatusCode)."
        }
    }

    # 5) login ADMIN (if fails, try register ADMIN and retry)
    $adminToken = $null
    $loginAdminUrl = "$BaseUrl/api/auth/login?email=$(Encode $AdminEmail)&password=$(Encode $AdminPassword)"
    $loginAdminResp = Invoke-Http -Method "POST" -Url $loginAdminUrl
    if ($loginAdminResp.StatusCode -eq 200 -and $null -ne $loginAdminResp.BodyJson) {
        $adminToken = $loginAdminResp.BodyJson.accessToken
    }

    if ([string]::IsNullOrWhiteSpace([string]$adminToken)) {
        $registerAdminUrl = "$BaseUrl/api/auth/register?email=$(Encode $AdminEmail)&password=$(Encode $AdminPassword)&rol=ADMIN"
        $registerAdminResp = Invoke-Http -Method "POST" -Url $registerAdminUrl
        if (-not (Is-AlreadyExistsResponse -resp $registerAdminResp)) {
            Add-Failure "5) register ADMIN fallback" "HTTP $($registerAdminResp.StatusCode). Body: $($registerAdminResp.BodyText)"
        }

        $loginAdminRespRetry = Invoke-Http -Method "POST" -Url $loginAdminUrl
        if ($loginAdminRespRetry.StatusCode -eq 200 -and $null -ne $loginAdminRespRetry.BodyJson) {
            $adminToken = $loginAdminRespRetry.BodyJson.accessToken
        }
    }

    if (-not [string]::IsNullOrWhiteSpace([string]$adminToken)) {
        Write-Pass "5) login ADMIN returns access token"
    }
    else {
        Add-Failure "5) login ADMIN" "No se obtuvo token ADMIN."
    }

    # 6) GET /api/facturas with ADMIN token -> expect 200
    if ([string]::IsNullOrWhiteSpace([string]$adminToken)) {
        Add-Failure "6) GET /api/facturas con token ADMIN" "No se pudo ejecutar porque no hay token ADMIN."
    }
    else {
        $adminHeaders = @{ Authorization = "Bearer $adminToken" }
        $facturasAdminResp = Invoke-Http -Method "GET" -Url "$BaseUrl/api/facturas" -Headers $adminHeaders
        if ($facturasAdminResp.StatusCode -eq 200) {
            Write-Pass "6) GET /api/facturas con ADMIN -> 200"
        }
        else {
            Add-Failure "6) GET /api/facturas con ADMIN" "Esperado 200, recibido $($facturasAdminResp.StatusCode)."
        }
    }

    if ($failures.Count -gt 0) {
        Write-Host "`nResumen de fallos:" -ForegroundColor Yellow
        foreach ($f in $failures) {
            Write-Host " - $f" -ForegroundColor Yellow
        }
        exit 1
    }

    Write-Host "`nSmoke JWT completado correctamente." -ForegroundColor Green
    exit 0
}
catch {
    Write-Fail $_.Exception.Message
    exit 2
}
finally {
    if ($null -ne $apiProcess) {
        try {
            if (-not $apiProcess.HasExited) {
                Stop-Process -Id $apiProcess.Id -Force
                Write-Host "Proceso API detenido (PID $($apiProcess.Id))." -ForegroundColor DarkGray
            }
        }
        catch {
            Write-Host "No se pudo detener automáticamente el proceso API (PID $($apiProcess.Id))." -ForegroundColor DarkYellow
        }
    }
}
