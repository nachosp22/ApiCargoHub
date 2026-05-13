# =============================================================================
# ApiCargoHub - Script de Inicio Completo (PowerShell)
# =============================================================================
# Requisitos: Java 17+, Node.js/npm, PostgreSQL corriendo en localhost:5432
# Uso desde scripts/: .\iniciar-todo.ps1
# Uso desde raiz:     .\scripts\iniciar-todo.ps1
# =============================================================================

$ErrorActionPreference = "Stop"

function Write-Section([string]$Title) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  $Title" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
}

function Test-TcpPort([string]$HostName, [int]$Port, [int]$TimeoutMs = 1500) {
    try {
        $client = New-Object Net.Sockets.TcpClient
        $async = $client.BeginConnect($HostName, $Port, $null, $null)
        if (-not $async.AsyncWaitHandle.WaitOne($TimeoutMs, $false)) {
            $client.Close()
            return $false
        }
        $client.EndConnect($async)
        $client.Close()
        return $true
    } catch {
        return $false
    }
}

function Import-DotEnv([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }

    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#") -or -not $line.Contains("=")) {
            return
        }

        $parts = $line.Split("=", 2)
        $name = $parts[0].Trim()
        $value = $parts[1].Trim().Trim('"').Trim("'")

        if ($name) {
            Set-Item -Path "Env:$name" -Value $value
        }
    }
}

function Stop-PortProcess([int]$Port) {
    $lines = netstat -ano | Select-String ":$Port\s"
    foreach ($line in $lines) {
        $tokens = ($line.Line -split "\s+") | Where-Object { $_ }
        $pidValue = $tokens[-1]
        if ($pidValue -match "^\d+$") {
            try {
                $proc = Get-Process -Id ([int]$pidValue) -ErrorAction Stop
                Write-Host "  Cerrando proceso en puerto ${Port}: PID $pidValue ($($proc.ProcessName))" -ForegroundColor DarkGray
                Stop-Process -Id ([int]$pidValue) -Force -ErrorAction SilentlyContinue
            } catch {}
        }
    }
}

# --- RUTAS PORTABLES ---
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$ApiDir = Join-Path $ProjectRoot "api"
$WebDir = Join-Path $ProjectRoot "web"
$DesktopDir = Join-Path $ProjectRoot "desktop"
$ApiEnv = Join-Path $ApiDir ".env"
$BackendLog = Join-Path $ApiDir "backend-startup.log"

# --- CONFIG LOCAL ---
$DbName = "cargohub"
$DefaultJwtSecret = "123456789012345678901234567890123456789012345678"

Write-Host ""
Write-Host "ApiCargoHub - levantar backend + web + desktop" -ForegroundColor Green
Write-Host "Proyecto: $ProjectRoot" -ForegroundColor DarkGray

# --- VERIFICACIONES BASICAS ---
Write-Section "1. Verificando herramientas"

foreach ($cmd in @("java", "node", "npm")) {
    if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
        Write-Host "[ERROR] No se encontro '$cmd' en PATH." -ForegroundColor Red
        exit 1
    }
}

Write-Host "[OK] Java, Node.js y npm detectados" -ForegroundColor Green

# --- CARGAR .ENV ---
Write-Section "2. Cargando variables api/.env"

Import-DotEnv $ApiEnv

if (-not $env:JWT_SECRET -or $env:JWT_SECRET.Trim().Length -lt 32) {
    $env:JWT_SECRET = $DefaultJwtSecret
    if (-not (Test-Path -LiteralPath $ApiEnv)) {
        New-Item -ItemType File -Path $ApiEnv -Force | Out-Null
    }
    Add-Content -LiteralPath $ApiEnv -Value ""
    Add-Content -LiteralPath $ApiEnv -Value "# JWT_SECRET - Generado automaticamente por scripts/iniciar-todo.ps1"
    Add-Content -LiteralPath $ApiEnv -Value "JWT_SECRET=$DefaultJwtSecret"
    Write-Host "[OK] JWT_SECRET generado y exportado" -ForegroundColor Green
} else {
    $env:JWT_SECRET = $env:JWT_SECRET.Trim()
    Write-Host "[OK] JWT_SECRET cargado y exportado" -ForegroundColor Green
}

if (-not $env:CARGO_ANALYSIS_DEV_FALLBACK_ENABLED) {
    $env:CARGO_ANALYSIS_DEV_FALLBACK_ENABLED = "true"
    Write-Host "[OK] Fallback local de analisis de carga activado para demo" -ForegroundColor Green
}

if (-not $env:APP_SEED_ENABLED) {
    $env:APP_SEED_ENABLED = "true"
}

# --- POSTGRESQL ---
Write-Section "3. Verificando PostgreSQL"

if (-not (Test-TcpPort "127.0.0.1" 5432 2000)) {
    Write-Host "[ERROR] PostgreSQL no responde en localhost:5432." -ForegroundColor Red
    Write-Host "        Levanta PostgreSQL antes de ejecutar este script." -ForegroundColor Yellow
    exit 1
}

Write-Host "[OK] PostgreSQL responde en localhost:5432" -ForegroundColor Green

# Crear DB si hay herramientas PostgreSQL disponibles; si no, seguimos y Spring mostrara error claro si falta.
$psql = Get-Command "psql.exe" -ErrorAction SilentlyContinue
$createdb = Get-Command "createdb.exe" -ErrorAction SilentlyContinue
if ($psql -and $createdb) {
    $env:PGPASSWORD = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "postgres" }
    $dbUser = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "postgres" }
    $dbExists = & $psql.Source -U $dbUser -h localhost -p 5432 -tAc "SELECT 1 FROM pg_database WHERE datname='$DbName';" 2>$null
    if ($dbExists -match "1") {
        Write-Host "[OK] Base de datos '$DbName' existe" -ForegroundColor Green
    } else {
        Write-Host "[INFO] Creando base de datos '$DbName'..." -ForegroundColor Yellow
        & $createdb.Source -U $dbUser -h localhost -p 5432 $DbName 2>$null
        Write-Host "[OK] Base de datos '$DbName' creada" -ForegroundColor Green
    }
} else {
    Write-Host "[INFO] psql/createdb no estan en PATH; omito creacion automatica de DB" -ForegroundColor DarkGray
}

# --- LIMPIAR PUERTOS ---
Write-Section "4. Limpiando procesos previos"

foreach ($port in @(8080, 5173, 5174)) {
    Stop-PortProcess $port
}

Start-Sleep -Seconds 1
Write-Host "[OK] Puertos revisados" -ForegroundColor Green

# --- BACKEND ---
Write-Section "5. Iniciando Backend Spring Boot"

if (-not (Test-Path -LiteralPath (Join-Path $ApiDir "mvnw.cmd"))) {
    Write-Host "[ERROR] No se encontro api/mvnw.cmd" -ForegroundColor Red
    exit 1
}

if (Test-Path -LiteralPath $BackendLog) {
    Remove-Item -LiteralPath $BackendLog -Force
}

$backendArgs = "/k cd /d `"$ApiDir`" && call mvnw.cmd spring-boot:run 1>`"$BackendLog`" 2>&1"
Start-Process -FilePath "cmd.exe" -ArgumentList $backendArgs -WindowStyle Normal
Write-Host "[INFO] Backend iniciado en nueva ventana" -ForegroundColor Yellow
Write-Host "[INFO] Esperando puerto 8080..." -ForegroundColor Yellow

$maxAttempts = 60
$ready = $false
for ($attempt = 1; $attempt -le $maxAttempts; $attempt++) {
    if (Test-TcpPort "127.0.0.1" 8080 1500) {
        $ready = $true
        break
    }

    Write-Host "  Esperando backend... $attempt/$maxAttempts" -ForegroundColor DarkGray
    Start-Sleep -Seconds 3
}

if (-not $ready) {
    Write-Host "[ERROR] El backend no abrio el puerto 8080 a tiempo." -ForegroundColor Red
    Write-Host "        Logs: $BackendLog" -ForegroundColor Yellow
    exit 1
}

Write-Host "[OK] Backend listo en http://localhost:8080" -ForegroundColor Green

# --- WEB ---
Write-Section "6. Iniciando Web Cliente Vue"

if (Test-Path -LiteralPath (Join-Path $WebDir "package.json")) {
    if (-not (Test-Path -LiteralPath (Join-Path $WebDir "node_modules"))) {
        Write-Host "[INFO] Instalando dependencias de web..." -ForegroundColor Yellow
        Push-Location $WebDir
        npm install
        Pop-Location
    }

    Start-Process -FilePath "cmd.exe" -ArgumentList "/k cd /d `"$WebDir`" && npm run dev" -WindowStyle Normal
    Write-Host "[OK] Web iniciada (Vite asignara puerto 5173/5174 si esta ocupado)" -ForegroundColor Green
} else {
    Write-Host "[INFO] No existe web/package.json; omito web" -ForegroundColor DarkGray
}

# --- DESKTOP ---
Write-Section "7. Iniciando Desktop Vue"

if (Test-Path -LiteralPath (Join-Path $DesktopDir "package.json")) {
    if (-not (Test-Path -LiteralPath (Join-Path $DesktopDir "node_modules"))) {
        Write-Host "[INFO] Instalando dependencias de desktop..." -ForegroundColor Yellow
        Push-Location $DesktopDir
        npm install
        Pop-Location
    }

    Start-Process -FilePath "cmd.exe" -ArgumentList "/k cd /d `"$DesktopDir`" && npm run dev" -WindowStyle Normal
    Write-Host "[OK] Desktop iniciado (Vite asignara puerto 5173/5174 si esta ocupado)" -ForegroundColor Green
} else {
    Write-Host "[INFO] No existe desktop/package.json; omito desktop" -ForegroundColor DarkGray
}

# --- RESUMEN ---
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  TODO LANZADO" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Backend API:  http://localhost:8080" -ForegroundColor White
Write-Host "  Web/Desktop:  revisar las ventanas de Vite" -ForegroundColor White
Write-Host "  Log backend:  $BackendLog" -ForegroundColor DarkGray
Write-Host ""
Write-Host "Para detener todo: Ctrl+C en cada ventana o cerrarlas." -ForegroundColor DarkGray
Write-Host ""

Pause
