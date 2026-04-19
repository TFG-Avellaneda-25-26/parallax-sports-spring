# Script para explorar la API correcta de PandaScore
$apiKey = 'Q5I8b9djTB-mlDgrui9SOryGCV48Um2D2MivlooklfoC9xvWix8'
$baseUrl = 'https://api.pandascore.co'

Write-Host "Exploring PandaScore API structure..."
Write-Host ""

# Intentar con diferentes sufijos después de matches
$endpoints = @(
    "/leagues?token=$apiKey",
    "/lol/matches?token=$apiKey",
    "/valorant/matches?token=$apiKey",
    "/dota2/matches?token=$apiKey",
    "/csgo/matches?token=$apiKey",
    "/leagues/valorant?token=$apiKey"
)

foreach ($endpoint in $endpoints) {
    Write-Host "Testing: $endpoint"
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl$endpoint" -Method Get -ErrorAction Stop -TimeoutSec 5
        $data = $response.Content | ConvertFrom-Json
        if ($data -is [array]) {
            Write-Host "  ✓ Success! Found $($data.Count) items"
            if ($data.Count -gt 0) {
                Write-Host "    First item: $($data[0] | Select-Object -ExpandProperty name -ErrorAction SilentlyContinue)"
                Write-Host "    Type: $($data[0].GetType().Name)"
            }
        } else {
            Write-Host "  ✓ Success! Response type: $($data.GetType().Name)"
        }
    } catch {
        $statusCode = $_.Exception.Response.StatusCode
        Write-Host "  ✗ Error: $statusCode"
    }
    Write-Host ""
}

