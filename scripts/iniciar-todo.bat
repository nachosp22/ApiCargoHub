@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "PS_SCRIPT=%SCRIPT_DIR%iniciar-todo.ps1"

if not exist "%PS_SCRIPT%" (
    echo [ERROR] No se encontro iniciar-todo.ps1 en scripts\
    pause
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%PS_SCRIPT%"
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
    echo.
    echo [ERROR] iniciar-todo.ps1 termino con codigo %EXIT_CODE%.
    pause
)

exit /b %EXIT_CODE%
