$env:JAVA_HOME = "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.4\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Write-Host "JAVA_HOME: $env:JAVA_HOME"
Write-Host "Java version:"
java -version
cd api
.\mvnw.cmd clean package -DskipTests
