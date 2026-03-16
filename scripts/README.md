# Scripts

## JWT smoke test

Reusable PowerShell smoke test for authentication/authorization flow:

- registers a `CLIENTE` user (tolerates already exists)
- logs in as `CLIENTE` and validates JWT presence
- validates `/api/facturas` access matrix (`401` no token, `403` CLIENTE, `200` ADMIN)
- can optionally start the API with JWT env vars

### Usage

```powershell
# run against an already running API
pwsh -File .\scripts\smoke-jwt.ps1

# start API automatically, then run checks
pwsh -File .\scripts\smoke-jwt.ps1 -StartApi

# custom parameters
pwsh -File .\scripts\smoke-jwt.ps1 -BaseUrl "http://localhost:8080" -ClienteEmail "cliente.smoke@example.com"
```
