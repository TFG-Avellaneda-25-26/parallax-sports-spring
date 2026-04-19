# Script para probar directamente la API de PandaScore
$apiKey = 'Q5I8b9djTB-mlDgrui9SOryGCV48Um2D2MivlooklfoC9xvWix8'
$baseUrl = 'https://api.pandascore.co'
$videogame = 'valorant'
$page = 1
$perPage = 5

# Construir la URI exactamente como lo hace el cliente
$uri = "$baseUrl/matches?videogame=$videogame&page=$page&per_page=$perPage&token=$apiKey"

Write-Host "Testing PandaScore API directly..."
Write-Host "URI: $baseUrl/matches?videogame=$videogame&page=$page&per_page=$perPage&token=****"
Write-Host ""

try {
    $response = Invoke-WebRequest -Uri $uri -Method Get -ErrorAction Stop -TimeoutSec 30
    Write-Host "Success! Status: $($response.StatusCode)"
    Write-Host "Response length: $($response.Content.Length) bytes"
    Write-Host ""
    Write-Host "First 500 chars of response:"
    Write-Host $response.Content.Substring(0, [Math]::Min(500, $response.Content.Length))
    Write-Host ""
    Write-Host "Parsing JSON..."
    $data = $response.Content | ConvertFrom-Json
    Write-Host "Number of matches: $($data.Count)"
    if ($data.Count -gt 0) {
        Write-Host "First match: $($data[0] | ConvertTo-Json -Depth 1)"
    }
} catch {
    Write-Host "Error: $($_.Exception.Message)"
    Write-Host "Status Code: $($_.Exception.Response.StatusCode)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $reader.BaseStream.Position = 0
        $errorBody = $reader.ReadToEnd()
        Write-Host "Error Body: $errorBody"
    }
}


