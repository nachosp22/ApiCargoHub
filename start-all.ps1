# ============================================================
# ApiCargoHub - Levantar todo en local
# ============================================================

Write-Host "=== ApiCargoHub - Levantando todo ===" -ForegroundColor Cyan
Write-Host ""

# --- Variables de entorno comunes ---
$env:DB_PASSWORD = "postgres"
$env:JWT_SECRET = "MiSuperSecretKeyParaJWTDeAlMenos32Caracteres!"
$env:GEMINI_API_KEY = "AIzaSyBGJIO1k42F1VCg3e0F06xCXa4vvsgoR6U"
$env:CLOUDINARY_CLOUD_NAME = "dkwa5pff2"
$env:CLOUDINARY_API_KEY = "153226742336582"
$env:CLOUDINARY_API_SECRET = "sVX7goasnAJy-qAWhPlK9FtPbn0"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:5173,http://localhost:5174,capacitor://localhost"

$root = "C:\Users\nacho\Desktop\ApiCargoHub"

# --- 1. API (Spring Boot - Puerto 8080) ---
Write-Host "[1/3] Iniciando API en puerto 8080..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-Command",
    "`$env:DB_PASSWORD='postgres';",
    "`$env:JWT_SECRET='MiSuperSecretKeyParaJWTDeAlMenos32Caracteres!';",
    "`$env:GEMINI_API_KEY='AIzaSyBGJIO1k42F1VCg3e0F06xCXa4vvsgoR6U';",
    "`$env:CLOUDINARY_CLOUD_NAME='dkwa5pff2';",
    "`$env:CLOUDINARY_API_KEY='153226742336582';",
    "`$env:CLOUDINARY_API_SECRET='sVX7goasnAJy-qAWhPlK9FtPbn0';",
    "`$env:CORS_ALLOWED_ORIGINS='http://localhost:5173,http://localhost:5174,capacitor://localhost';",
    "Set-Location '$root\api';",
    "Write-Host 'API iniciando...' -ForegroundColor Green;",
    ".\mvnw.cmd spring-boot:run"
) -WindowStyle Normal

# --- 2. Web (Vue/Vite - Puerto 5173) ---
Write-Host "[2/3] Iniciando Web en puerto 5173..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-Command",
    "Set-Location '$root\web';",
    "Write-Host 'Web (Vite) iniciando en http://localhost:5173' -ForegroundColor Green;",
    "npm run dev"
) -WindowStyle Normal

# --- 3. Desktop (Electron/Vite) ---
Write-Host "[3/3] Iniciando Desktop..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-Command",
    "Set-Location '$root\desktop';",
    "Write-Host 'Desktop (Vite) iniciando...' -ForegroundColor Green;",
    "npm run dev"
) -WindowStyle Normal

Write-Host ""
Write-Host "=== Todo lanzado ===" -ForegroundColor Cyan
Write-Host "API:    http://localhost:8080" -ForegroundColor White
Write-Host "Web:    http://localhost:5173" -ForegroundColor White
Write-Host "Desktop: ventana aparte (Vite)" -ForegroundColor White
Write-Host ""
Write-Host "Cerrá las ventanas individualmente para detener cada servicio." -ForegroundColor DarkGray
