# Script para ejecutar la aplicacion con variables de entorno desde .env
$env_file = ".\.env"

# Leer el archivo .env y establecer las variables de entorno
if (Test-Path $env_file) {
    Get-Content $env_file | ForEach-Object {
        if ($_ -match '^([^=]+)=(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            if ($value) {
                [Environment]::SetEnvironmentVariable($key, $value, 'Process')
                Write-Host "Set: $key"
            }
        }
    }
}

# Verificar que el API key esta configurado
$apiKey = [Environment]::GetEnvironmentVariable('PANDASCORE_API_KEY', 'Process')
if ($apiKey) {
    Write-Host "PANDASCORE_API_KEY is set (length: $($apiKey.Length))"
} else {
    Write-Host "PANDASCORE_API_KEY is NOT set"
}

# Ejecutar la aplicacion
Write-Host "Starting application..."
.\mvnw.cmd spring-boot:run


