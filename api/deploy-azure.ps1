# ==============================================
# CargoHub — Deploy a Azure (Azure for Students)
# ==============================================
#
# Requisitos previos:
#   1. Instalar Azure CLI: winget install Microsoft.AzureCLI
#   2. Iniciar sesión: az login
#   3. Activar suscripción: az account set --subscription "Azure for Students"
#
# Este script crea:
#   - Azure Container Registry (ACR) para la imagen Docker
#   - App Service Plan (B1 — gratis con crédito)
#   - App Service para la API
#   - PostgreSQL Flexible Server (B1ms — gratis con crédito)
#
# OPCIÓN ALTERNATIVA (más barata):
#   Usá una VM B1s con docker-compose. Descomentá las líneas del final.

param(
  [string]$ResourceGroup = "cargohub-rg",
  [string]$Location = "westeurope",
  [string]$AppName = "cargohub-api",
  [string]$DbServerName = "cargohub-db",
  [string]$DbName = "cargohub",
  [string]$DbUser = "cargohub",
  [string]$AcrName = "cargohubregistry"
)

$ErrorActionPreference = "Stop"

# ── Pedir credenciales una sola vez ──
Write-Host "=== Configuración de credenciales ===" -ForegroundColor Cyan
$dbPassword = Read-Host "Contraseña de PostgreSQL (mín 8 chars, mayúscula, número, especial)"
$jwtSecret = Read-Host "JWT Secret (mín 32 caracteres aleatorios)"

Write-Host ""
Write-Host "=== Creando grupo de recursos: $ResourceGroup ===" -ForegroundColor Cyan
az group create --name $ResourceGroup --location $Location

Write-Host ""
Write-Host "=== Creando Azure Container Registry ===" -ForegroundColor Cyan
az acr create `
  --resource-group $ResourceGroup `
  --name $AcrName `
  --sku Basic `
  --admin-enabled true

Write-Host ""
Write-Host "=== Construyendo y subiendo imagen Docker ===" -ForegroundColor Cyan
Push-Location ..
az acr build `
  --registry $AcrName `
  --image cargohub-api:latest `
  --file Dockerfile .
Pop-Location

Write-Host ""
Write-Host "=== Creando App Service Plan (B1) ===" -ForegroundColor Cyan
az appservice plan create `
  --resource-group $ResourceGroup `
  --name "$AppName-plan" `
  --sku B1 `
  --is-linux

Write-Host ""
Write-Host "=== Creando App Service ===" -ForegroundColor Cyan
az webapp create `
  --resource-group $ResourceGroup `
  --plan "$AppName-plan" `
  --name $AppName `
  --deployment-container-image-name "$AcrName.azurecr.io/cargohub-api:latest"

Write-Host ""
Write-Host "=== Creando PostgreSQL Flexible Server (B1ms) ===" -ForegroundColor Cyan
az postgres flexible-server create `
  --resource-group $ResourceGroup `
  --name $DbServerName `
  --location $Location `
  --admin-user $DbUser `
  --admin-password $dbPassword `
  --sku-name Standard_B1ms `
  --tier Burstable `
  --storage-size 32 `
  --version 16 `
  --database-name $DbName `
  --public-access 0.0.0.0

Write-Host ""
Write-Host "=== Configurando variables de entorno en App Service ===" -ForegroundColor Cyan
$dbHost = "$DbServerName.postgres.database.azure.com"
az webapp config appsettings set `
  --resource-group $ResourceGroup `
  --name $AppName `
  --settings `
    SPRING_PROFILES_ACTIVE=prod `
    "DB_URL=jdbc:postgresql://$DbHost`:5432/$DbName`?sslmode=require" `
    "DB_USERNAME=$DbUser" `
    "DB_PASSWORD=$dbPassword" `
    "JWT_SECRET=$jwtSecret" `
    JWT_EXPIRATION_MS=3600000 `
    "CORS_ALLOWED_ORIGINS=https://*.vercel.app" `
    "PORT=8080" `
    APP_SEED_ENABLED=false

Write-Host ""
Write-Host "=== Configurando autenticación ACR → App Service ===" -ForegroundColor Cyan
$acrCreds = az acr credential show --name $AcrName --query "{username:username, password:passwords[0].value}" | ConvertFrom-Json
az webapp config container set `
  --resource-group $ResourceGroup `
  --name $AppName `
  --container-image-name "$AcrName.azurecr.io/cargohub-api:latest" `
  --container-registry-url "https://$AcrName.azurecr.io" `
  --container-registry-user $acrCreds.username `
  --container-registry-password $acrCreds.password

Write-Host ""
Write-Host "==================================" -ForegroundColor Green
Write-Host "  DESPLIEGUE COMPLETADO" -ForegroundColor Green
Write-Host "==================================" -ForegroundColor Green
Write-Host ""
Write-Host "API:  https://$AppName.azurewebsites.net" -ForegroundColor Cyan
Write-Host "BD:   $DbServerName.postgres.database.azure.com" -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠️  Configurá CORS_ALLOWED_ORIGINS en App Service con los dominios reales de tu frontend."
Write-Host "⚠️  Configurá CLOUDINARY_* si usás fotos."
Write-Host "⚠️  Para probar: curl https://$AppName.azurewebsites.net/api/health"

# ============================================================
# OPCIÓN ALTERNATIVA: VM con Docker Compose (más barata)
# ============================================================
# Descomentá este bloque si preferís una sola VM en vez de App Service + PostgreSQL Flexible.
#
# az vm create `
#   --resource-group $ResourceGroup `
#   --name "cargohub-vm" `
#   --image Ubuntu2204 `
#   --size Standard_B1s `
#   --admin-username azureuser `
#   --generate-ssh-keys
#
# az vm open-port --resource-group $ResourceGroup --name "cargohub-vm" --port 8080 --priority 1010
# az vm open-port --resource-group $ResourceGroup --name "cargohub-vm" --port 80 --priority 1020
#
# Write-Host "Conectate: ssh azureuser@<ip-pública>"
# Write-Host "Instalá Docker:"
# Write-Host "  curl -fsSL https://get.docker.com | sh"
# Write-Host "  sudo usermod -aG docker azureuser"
# Write-Host "Subí los archivos y ejecutá: docker compose up -d"
