# Script para explorar diferentes parámetros de la API de PandaScore
$apiKey = 'Q5I8b9djTB-mlDgrui9SOryGCV48Um2D2MivlooklfoC9xvWix8'
$baseUrl = 'https://api.pandascore.co'

Write-Host "Testing PandaScore API with different parameters..."
Write-Host ""

# Test 1: Sin parámetro videogame, solo token
Write-Host "=== Test 1: Solo token sin videogame ==="
$uri1 = "$baseUrl/matches?token=$apiKey&page=1&per_page=3"
try {
    $response = Invoke-WebRequest -Uri $uri1 -Method Get -ErrorAction Stop -TimeoutSec 10
    $data = $response.Content | ConvertFrom-Json
    Write-Host "Success! Found $($data.Count) matches"
    if ($data.Count -gt 0) {
        Write-Host "First match videogame: $($data[0].videogame.name)"
    }
} catch {
    Write-Host "Error: $($_.Exception.Message)"
}

Write-Host ""

# Test 2: Utilizando league slug en lugar de videogame
Write-Host "=== Test 2: Usando league slug ==="
$uri2 = "$baseUrl/matches?league_id=4947&token=$apiKey&page=1&per_page=3"
try {
    $response = Invoke-WebRequest -Uri $uri2 -Method Get -ErrorAction Stop -TimeoutSec 10
    $data = $response.Content | ConvertFrom-Json
    Write-Host "Success! Found $($data.Count) matches"
    if ($data.Count -gt 0) {
        Write-Host "First match: $($data[0].name)"
        Write-Host "League: $($data[0].league.name)"
    }
} catch {
    Write-Host "Error: $($_.Exception.Message)"
}

Write-Host ""

# Test 3: Ver qué videogames están disponibles (si existe tal endpoint)
Write-Host "=== Test 3: Listar videogames ==="
$uri3 = "$baseUrl/videogames?token=$apiKey"
try {
    $response = Invoke-WebRequest -Uri $uri3 -Method Get -ErrorAction Stop -TimeoutSec 10
    $data = $response.Content | ConvertFrom-Json
    Write-Host "Success! Found $($data.Count) videogames:"
    $data | ForEach-Object { Write-Host "  - $($_.name) (slug: $($_.slug), id: $($_.id))" }
} catch {
    Write-Host "Error: $($_.Exception.Message)"
}

