@echo off
setlocal EnableDelayedExpansion

:: ============================================================
:: Script para levantar todas las aplicaciones de ApiCargoHub
:: Requiere: Java 17, Node.js, PostgreSQL corriendo
:: ============================================================

title ApiCargoHub - Iniciador

echo.
echo ============================================================
echo   API CARGOHUB - LEVANTAR TODAS LAS APLICACIONES
echo ============================================================
echo.

:: --- RUTAS DEL PROYECTO ---
set "PROJECT_ROOT=%~dp0.."
set "API_DIR=%PROJECT_ROOT%\api"
set "DESKTOP_DIR=%PROJECT_ROOT%\desktop"
set "API_ENV=%API_DIR%\.env"

:: --- VERIFICACIONES PREVIAS ---

:: Verificar Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java no esta instalado o no esta en PATH.
    echo         Descarga Java 17 desde: https://adoptium.net/
    pause
    exit /b 1
)

:: Verificar Node.js
node -v >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Node.js no esta instalado o no esta en PATH.
    echo         Descarga Node.js desde: https://nodejs.org/
    pause
    exit /b 1
)

:: Verificar npm
npm -v >nul 2>&1
if errorlevel 1 (
    echo [ERROR] npm no esta disponible.
    pause
    exit /b 1
)

echo [OK] Java y Node.js detectados.
echo.

:: --- GESTION DE JWT_SECRET ---

:: Verificar si existe .env en el backend
if exist "%API_ENV%" (
    echo [INFO] Archivo .env encontrado en api/.env
    
    :: Verificar si JWT_SECRET esta definido
    findstr /C:"JWT_SECRET" "%API_ENV%" >nul
    if errorlevel 1 (
        echo [INFO] JWT_SECRET no encontrado en .env. Se solicitara...
        set "JWT_MISSING=true"
    ) else (
        :: Extraer y verificar longitud del JWT_SECRET existente
        for /f "usebackq tokens=1,* delims==" %%a in (`findstr /i "JWT_SECRET" "%API_ENV%"`) do (
            set "EXISTING_SECRET=%%b"
        )
        echo !EXISTING_SECRET! | findstr /r "^........" >nul
        if errorlevel 1 (
            echo [WARN] JWT_SECRET existente tiene menos de 8 caracteres.
            echo        Se recomienda generar uno nuevo de al menos 32 caracteres.
            set /p "RESP=¿Generar nuevo JWT_SECRET? (S/N): "
            if /i "!RESP!"=="S" (
                set "JWT_MISSING=true"
            )
        ) else (
            echo [OK] JWT_SECRET encontrado en .env.
        )
    )
) else (
    echo [INFO] No existe archivo .env en api/
    set "JWT_MISSING=true"
)

:: Si JWT_SECRET falta o es invalido, generarlo
if defined JWT_MISSING (
    echo.
    echo ============================================================
    echo   CONFIGURACION DE JWT_SECRET
    echo ============================================================
    echo.
    echo El backend requiere JWT_SECRET (minimo 32 caracteres).
    echo.
    
    :: Generar secret aleatorio usando PowerShell
    echo Generando JWT_SECRET seguro...
    for /f "delims=" %%s in ('powershell -Command "[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 94 -Minimum 33 } | ForEach-Object { [char]$_ }))"') do set "JWT_SECRET=%%s"
    
    :: Guardar en .env sin sobreescribir archivo existente (append)
    echo. >> "%API_ENV%"
    echo # JWT_SECRET - Generado automaticamente el %date% %time% >> "%API_ENV%"
    echo JWT_SECRET=%JWT_SECRET% >> "%API_ENV%"
    
    echo [OK] JWT_SECRET generado y guardado en api/.env
    echo.
)

:: --- VERIFICAR POSTGRESQL ---

echo.
echo [INFO] Verificando conexion a PostgreSQL...
echo        (Nota: PostgreSQL debe estar corriendo en localhost:5432)

:: Intentar conexion basica a PostgreSQL con timeout
powershell -Command "try { $conn = New-Object System.Data.Odbc.OdbcConnection; $conn.ConnectionString = 'Driver={PostgreSQL Unicode};Server=localhost;Port=5432;Database=cargohub;Uid=postgres;Pwd=postgres;'; $conn.Open(); $conn.Close(); Write-Host 'OK'; exit 0 } catch { Write-Host 'FAIL'; exit 1 }" >nul 2>&1
if errorlevel 1 (
    echo [WARN] No se pudo conectar a PostgreSQL.
    echo        Asegurate de que PostgreSQL este corriendo.
    echo        Verifica las credenciales en api/src/main/resources/application.properties
    echo.
    set /p "RESP=¿Continuar de todos modos? (S/N): "
    if /i not "!RESP!"=="S" (
        exit /b 1
    )
)

:: --- VERIFICAR PUERTO 8080 ---

echo.
echo [INFO] Verificando si el puerto 8080 esta disponible...

netstat -ano | findstr ":8080" >nul 2>&1
if not errorlevel 1 (
    echo [ERROR] El puerto 8080 esta en uso.
    echo.
    echo Para identificar el proceso:
    echo   netstat -ano ^| findstr ":8080"
    echo.
    echo Para liberar el puerto (Windows):
    echo   netstat -ano ^| findstr ":8080" 
    echo   taskkill /PID ^<numero^> /F
    echo.
    pause
    exit /b 1
)

echo [OK] Puerto 8080 disponible.
echo.

:: ============================================================
:: LEVANTAR BACKEND (Spring Boot) EN NUEVA VENTANA
:: ============================================================

echo ============================================================
echo   LEVANTANDO BACKEND (Spring Boot)
echo ============================================================
echo.
echo El backend se iniciara en una nueva ventana.
echo Esperando a que este listo en http://localhost:8080...
echo.

:: Crear archivo de log para el backend
set "BACKEND_LOG=%API_DIR%\backend-startup.log"

:: Iniciar backend en nueva ventana y esperar
start "ApiCargoHub - Backend" cmd /k "cd /d "%API_DIR%" && mvnw.cmd spring-boot:run > "%BACKEND_LOG%" 2>&1"

:: --- ESPERAR A QUE EL BACKEND ESTE LISTO ---

echo Verificando que el backend responda...
set "BACKEND_READY=false"
set "MAX_ATTEMPTS=60"
set "ATTEMPT=0"

:wait_backend
set /a ATTEMPT+=1

if %ATTEMPT% gtr %MAX_ATTEMPTS% (
    echo.
    echo [ERROR] Tiempo de espera agotado. El backend no respondio.
    echo.
    echo Revisa los logs en: %BACKEND_LOG%
    echo.
    echo Errores comunes:
    echo   - JWT_SECRET no valido o vacio
    echo   - PostgreSQL no disponible
    echo   - Error en la configuracion de Spring Boot
    echo.
    pause
    exit /b 1
)

:: Verificar si el puerto 8080 esta respondiendo
powershell -Command "try { $r = Invoke-WebRequest -Uri 'http://localhost:8080/api/conductores' -Method GET -TimeoutSec 2 -UseBasicParsing -ErrorAction SilentlyContinue; if ($r.StatusCode) { Write-Host 'OK' } } catch { Write-Host 'FAIL' }" >nul 2>&1
if errorlevel 1 (
    :: Intentar con curl si esta disponible
    curl -s --connect-timeout 2 http://localhost:8080 >nul 2>&1
    if errorlevel 1 (
        timeout /t 5 /nobreak >nul
        echo Intentando... (%ATTEMPT%/%MAX_ATTEMPTS%)
        goto :wait_backend
    )
)

:: Verificar una vez mas para confirmar
powershell -Command "try { $r = Invoke-WebRequest -Uri 'http://localhost:8080/api/conductores' -Method GET -TimeoutSec 5 -UseBasicParsing -ErrorAction SilentlyContinue; if ($r.StatusCode) { Write-Host 'OK' } } catch { Write-Host 'FAIL' }" >nul 2>&1
if errorlevel 1 (
    timeout /t 5 /nobreak >nul
    echo Verificando de nuevo...
    goto :wait_backend
)

set "BACKEND_READY=true"

echo.
echo [OK] Backend listo y respondiendo en http://localhost:8080
echo.

:: ============================================================
:: LEVANTAR DESKTOP (Vue) EN NUEVA VENTANA
:: ============================================================

echo ============================================================
echo   LEVANTANDO DESKTOP (Vue.js)
echo ============================================================
echo.

:: Verificar node_modules
if not exist "%DESKTOP_DIR%\node_modules" (
    echo [INFO] Instalando dependencias de desktop...
    cd /d "%DESKTOP_DIR%"
    call npm install
    if errorlevel 1 (
        echo [ERROR] Fallo al instalar dependencias de desktop.
        pause
        exit /b 1
    )
)

:: Copiar .env.example a .env si no existe
if not exist "%DESKTOP_DIR%\.env" (
    if exist "%DESKTOP_DIR%\.env.example" (
        copy "%DESKTOP_DIR%\.env.example" "%DESKTOP_DIR%\.env" >nul
        echo [OK] Archivo .env creado desde .env.example
    )
)

:: Iniciar desktop en nueva ventana
start "ApiCargoHub - Desktop" cmd /k "cd /d "%DESKTOP_DIR%" && npm run dev"

echo [INFO] Desktop iniciando en nueva ventana...
echo.

:: --- RESUMEN FINAL ---

echo.
echo ============================================================
echo   APLICACIONES LEVANTADAS
echo ============================================================
echo.
echo   Backend (Spring Boot): http://localhost:8080
echo   Desktop (Vue.js):      http://localhost:5173 (aprox.)
echo.
echo ============================================================
echo   INSTRUCCIONES
echo ============================================================
echo.
echo   - Cada aplicacion esta en su propia ventana de terminal
echo   - Para detener: presiona Ctrl+C en la ventana correspondiente
echo   - O cierra las ventanas directamente
echo.
echo   Datos de acceso a la API:
echo     Base URL: http://localhost:8080
echo     JDBC: jdbc:postgresql://localhost:5432/cargohub
echo     Usuario DB: postgres / postgres
echo.
echo ============================================================
echo   ARCHIVOS DE LOG
echo ============================================================
echo.
echo   Backend: %BACKEND_LOG%
echo.
echo ============================================================
echo.

:: Esperar a que el usuario cierre las ventanas
echo Presiona cualquier tecla para minimizar esta ventana...
pause >nul

endlocal
