param(
  [int]$Pages = 1,
  [int]$PerPage = 20,
  [int]$HealthTimeoutSeconds = 60
)

# Script para comprobar PandaScore sync end-to-end
# - Carga .env en la sesión
# - Compila el jar si no existe
# - Arranca la app con logging DEBUG para pandascore
# - Espera /actuator/health UP
# - Ejecuta GET /api/pandascore/matches y POST /api/pandascore/sync/league-of-legends
# - Guarda respuestas en ./pandascore-check/

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

$envFile = ".env"
if (Test-Path $envFile) {
  Write-Host "Loading .env variables from $envFile"
  Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*([^#=]+)=(.*)$') {
      $n = $matches[1].Trim()
      $v = $matches[2].Trim()
      if ($v.StartsWith('"') -and $v.EndsWith('"')) { $v = $v.Trim('"') }
      if ($v.StartsWith("'") -and $v.EndsWith("'")) { $v = $v.Trim("'") }
      Set-Item -Path Env:$n -Value $v
      Write-Host "Set env $n"
    }
  }
} else {
  Write-Host ".env not found in $scriptDir"
}

# Build jar if missing
$jar = "target\parallax-sports-api-0.0.1-SNAPSHOT.jar"
if (-not (Test-Path $jar)) {
  Write-Host "Jar not found, running mvn package (skipping tests)"
  & .\mvnw.cmd -DskipTests package
  if ($LASTEXITCODE -ne 0) {
    Write-Error "mvn package failed with code $LASTEXITCODE"
    exit 1
  }
}

# Kill any process using 8080
$tcp = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($tcp) {
  $pid = $tcp.OwningProcess
  Write-Host "Stopping process on port 8080, pid=$pid"
  Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
  Start-Sleep -Seconds 1
} else {
  Write-Host "No process detected on port 8080"
}

# Ensure debug logging for pandascore
$env:JAVA_TOOL_OPTIONS = '-Dlogging.level.dev.parallaxsports.external.pandascore=DEBUG'

# Prepare output folder
$outDir = Join-Path $scriptDir 'pandascore-check'
New-Item -Path $outDir -ItemType Directory -Force | Out-Null
$appOut = Join-Path $outDir 'app.log'
$appErr = Join-Path $outDir 'app.err'

Write-Host "Starting application (logs -> $appOut , $appErr)"
$proc = Start-Process -FilePath 'java' -ArgumentList '-jar', $jar -NoNewWindow -PassThru -RedirectStandardOutput $appOut -RedirectStandardError $appErr
Write-Host "Started process with pid=$($proc.Id)"

# Wait for actuator/health
$up = $false
for ($i=0; $i -lt $HealthTimeoutSeconds; $i++) {
  try {
    $h = Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/actuator/health' -UseBasicParsing -TimeoutSec 5
    if ($h.status -eq 'UP') { $up = $true; break }
  } catch {
    Start-Sleep -Seconds 1
  }
}

if (-not $up) {
  Write-Host "Actuator health did not become UP within $HealthTimeoutSeconds seconds. Dumping last 200 lines of app.log"
  if (Test-Path $appOut) { Get-Content $appOut -Tail 200 -ErrorAction SilentlyContinue }
  Write-Host "Also showing app.err tail"
  if (Test-Path $appErr) { Get-Content $appErr -Tail 200 -ErrorAction SilentlyContinue }
  # stop process
  try { Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue } catch {}
  exit 2
}

# Perform GET /matches
$matchesFile = Join-Path $outDir 'matches-response.json'
try {
  Write-Host "Calling GET /api/pandascore/matches"
  $matches = Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/api/pandascore/matches' -UseBasicParsing -TimeoutSec 30
  $matches | ConvertTo-Json -Depth 5 | Out-File $matchesFile -Encoding utf8
  Write-Host "Saved GET response -> $matchesFile"
} catch {
  Write-Error "GET /matches failed: $_"
  Write-Host "Last 200 lines of app.log:"
  Get-Content $appOut -Tail 200 -ErrorAction SilentlyContinue
}

# Perform POST /sync
$syncFile = Join-Path $outDir 'sync-response.json'
try {
  Write-Host "Calling POST /api/pandascore/sync/league-of-legends?pages=$Pages&perPage=$PerPage"
  $sync = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/pandascore/sync/league-of-legends?pages=$Pages&perPage=$PerPage" -UseBasicParsing -TimeoutSec 120
  $sync | ConvertTo-Json -Depth 5 | Out-File $syncFile -Encoding utf8
  Write-Host "Saved POST response -> $syncFile"
} catch {
  Write-Error "POST /sync failed: $_"
  Write-Host "Last 200 lines of app.log:"
  Get-Content $appOut -Tail 200 -ErrorAction SilentlyContinue
}

Write-Host "Tail last 200 lines of $appOut"
Get-Content $appOut -Tail 200 -ErrorAction SilentlyContinue

# Stop the process
try {
  Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
  Write-Host "Stopped process pid=$($proc.Id)"
} catch {
  Write-Warning "Failed to stop process pid=$($proc.Id)"
}

Write-Host "Results saved in $outDir"

