$env:JAVA_HOME = "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.4\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:JWT_SECRET = "1234567890123456789012345678901234"
Write-Host "Starting Spring Boot API..."
cd api
java -jar target/backend-0.0.1-SNAPSHOT.jar
