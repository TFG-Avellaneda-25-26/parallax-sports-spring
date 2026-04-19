# Script de diagnóstico para PandaScore
Write-Host "===================================="
Write-Host "PandaScore Diagnostic Tool"
Write-Host "===================================="
Write-Host ""

# 1. Verificar API Key
Write-Host "[1] Checking API Key..."
$apiKey = $env:PANDASCORE_API_KEY
if ($apiKey) {
    Write-Host "✓ PANDASCORE_API_KEY is set"
    Write-Host "  Length: $($apiKey.Length) chars"
} else {
    Write-Host "✗ PANDASCORE_API_KEY is NOT set!"
    Write-Host "  Run: `$env:PANDASCORE_API_KEY='YOUR_KEY'"
}
Write-Host ""

# 2. Test direct PandaScore API
Write-Host "[2] Testing direct PandaScore API..."
$apiKey = 'Q5I8b9djTB-mlDgrui9SOryGCV48Um2D2MivlooklfoC9xvWix8'
$testUrl = "https://api.pandascore.co/lol/matches?page=1&per_page=3&token=$apiKey"

try {
    $response = Invoke-WebRequest -Uri $testUrl -Method Get -TimeoutSec 10 -ErrorAction Stop
    $data = $response.Content | ConvertFrom-Json
    Write-Host "✓ API reachable - returned $($data.Count) matches"
} catch {
    Write-Host "✗ API not reachable: $($_.Exception.Message)"
}
Write-Host ""

# 3. Check application logs
Write-Host "[3] Last 30 lines of app.log..."
if (Test-Path "app.log") {
    Get-Content app.log -Tail 30
} else {
    Write-Host "✗ app.log not found"
}
Write-Host ""

# 4. Show key configuration values
Write-Host "[4] Configuration from application.yaml..."
Write-Host "  PANDASCORE_API_KEY: (from .env)"
Write-Host "  PANDASCORE_BASE_URL: https://api.pandascore.co"
Write-Host "  PANDASCORE_DEFAULT_PER_PAGE: 20"
Write-Host "  PANDASCORE_MAX_PAGES: 5"
Write-Host ""

# 5. Instructions
Write-Host "[5] Next Steps..."
Write-Host ""
Write-Host "1. Compile:"
Write-Host "   cd C:\Users\Ismi2\Desktop\2DAW-N\TFG\parallax-sports-spring"
Write-Host "   .\mvnw.cmd clean package -DskipTests -q"
Write-Host ""
Write-Host "2. Run application:"
Write-Host "   `$env:PANDASCORE_API_KEY='Q5I8b9djTB-mlDgrui9SOryGCV48Um2D2MivlooklfoC9xvWix8'"
Write-Host "   java -jar target/parallax-sports-api-0.0.1-SNAPSHOT.jar"
Write-Host ""
Write-Host "3. In another terminal, test endpoint:"
Write-Host "   `$response = Invoke-WebRequest -Uri 'http://localhost:8080/api/admin/pandascore/sync/league-of-legends?pages=1&perPage=5' -Method Post"
Write-Host "   `$response.Content"
Write-Host ""
Write-Host "===================================="

