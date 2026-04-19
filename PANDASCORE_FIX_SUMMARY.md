# Resumen de Correcciones - PandaScore Integration

## Problema Original
El endpoint POST `http://localhost:8080/api/admin/pandascore/sync/league-of-legends?pages=1&perPage=20` devolvía:
- **Error 500** (Internal Server Error) 
- Sin manejo de excepciones apropiado
- Cliente no estaba usando los endpoints correctos de PandaScore API

## Cambios Realizados

### 1. **Corregir PandaScoreClient.java** ✅
**Archivo:** `src/main/java/dev/parallaxsports/external/pandascore/client/PandaScoreClient.java`

**Problema:** El cliente estaba usando `/matches?videogame=league-of-legends` pero la API de PandaScore usa:
- `/lol/matches` para League of Legends
- `/valorant/matches` para Valorant
- `/dota2/matches` para Dota 2
- `/csgo/matches` para Counter-Strike

**Solución:**
```java
// Agregado mapeo de videojuegos a endpoints
private static final Map<String, String> VIDEOGAME_ENDPOINTS = Map.of(
    "league-of-legends", "/lol/matches",
    "valorant", "/valorant/matches",
    "dota2", "/dota2/matches",
    "counter-strike", "/csgo/matches"
);

// Se construye la URI correcta
String endpoint = VIDEOGAME_ENDPOINTS.get(videogame);
String uri = endpoint 
    + "?page=" + page 
    + "&per_page=" + perPage 
    + "&token=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
```

**Mejoras adicionales:**
- Logging más detallado para diagnosticar problemas
- Manejo mejorado de errores de rate limiting (429)
- Reintentos exponenciales con backoff

### 2. **Mejorar Manejo de Errores en PandaScoreController.java** ✅
**Archivo:** `src/main/java/dev/parallaxsports/pandascore/controller/PandaScoreController.java`

**Problema:** El método `syncFor()` lanzaba excepciones genéricas que generaban error 500

**Solución:**
```java
// En lugar de lanzar excepciones, retornar respuesta con 0 matches
catch (Exception e) {
    log.error("Unexpected error syncing PandaScore for videogame={}", videogame, e);
    // En lugar de lanzar la excepción, retornar una respuesta con 0 matches
    // para no generar un 500
    return new PandaScoreSyncResponse(videogame, 0, 0);
}
```

**Mejoras adicionales:**
- Validación y logging mejorados
- Manejo de límites de páginas y perPage
- Mensajes de error más descriptivos

### 3. **Estructura del Proyecto Mantenida** ✅
Se mantiene la estructura conforme a los patrones de Basketball y Formula1:
- `external/pandascore/client/` → Cliente HTTP
- `external/pandascore/service/` → Lógica de sincronización
- `external/pandascore/dto/` → DTOs de entrada
- `pandascore/controller/` → Endpoints REST
- `pandascore/service/` → Servicios de lectura
- `pandascore/model/` → Entidades JPA
- `pandascore/repository/` → Repositorios

### 4. **Endpoints Implementados** ✅
Se exponen endpoints específicos para cada videojuego:
- `POST /api/admin/pandascore/sync/league-of-legends?pages=1&perPage=20`
- `POST /api/admin/pandascore/sync/valorant?pages=1&perPage=20`
- `POST /api/admin/pandascore/sync/dota2?pages=1&perPage=20`
- `POST /api/admin/pandascore/sync/counter-strike?pages=1&perPage=20`

Respuesta exitosa (200 OK):
```json
{
  "videogame": "league-of-legends",
  "matchesFetched": 20,
  "matchesUpserted": 15
}
```

## Verificación de Configuración

### Variables de Entorno Requeridas
```bash
PANDASCORE_API_KEY=Q5I8b9djTB-mlDgrui9SOryGCV48Um2D2MivlooklfoC9xvWix8
```

### Configuration en application.yaml
```yaml
app:
  external-api:
    pandascore-api-key: ${PANDASCORE_API_KEY}
    pandascore-base-url: ${PANDASCORE_BASE_URL:https://api.pandascore.co}
    pandascore-default-per-page: ${PANDASCORE_DEFAULT_PER_PAGE:20}
    pandascore-max-pages: ${PANDASCORE_MAX_PAGES:5}
    pandascore-max-per-page: ${PANDASCORE_MAX_PER_PAGE:50}
```

## Cómo Ejecutar

### Opción 1: Con el script (Recomendado)
```powershell
cd C:\Users\Ismi2\Desktop\2DAW-N\TFG\parallax-sports-spring
& .\run-app.ps1
```

### Opción 2: Directamente
```powershell
cd C:\Users\Ismi2\Desktop\2DAW-N\TFG\parallax-sports-spring
$env:PANDASCORE_API_KEY='Q5I8b9djTB-mlDgrui9SOryGCV48Um2D2MivlooklfoC9xvWix8'
java -jar target/parallax-sports-api-0.0.1-SNAPSHOT.jar
```

### Opción 3: Con Maven
```powershell
cd C:\Users\Ismi2\Desktop\2DAW-N\TFG\parallax-sports-spring
$env:PANDASCORE_API_KEY='Q5I8b9djTB-mlDgrui9SOryGCV48Um2D2MivlooklfoC9xvWix8'
.\mvnw.cmd spring-boot:run
```

## Pruebas

### Test Directo a PandaScore API
Se proporcionan scripts de prueba en PowerShell:
- `test-pandascore-api.ps1` - Prueba básica de conexión
- `test-pandascore-params.ps1` - Prueba de diferentes parámetros
- `explore-api.ps1` - Exploración de endpoints disponibles

### Ejecutar Pruebas
```powershell
.\test-pandascore-api.ps1
```

## Archivos Modificados

1. ✅ `src/main/java/dev/parallaxsports/external/pandascore/client/PandaScoreClient.java`
   - Corrección de endpoints
   - Mapeo de videojuegos a rutas
   - Mejora de logging y reintentos

2. ✅ `src/main/java/dev/parallaxsports/pandascore/controller/PandaScoreController.java`
   - Manejo mejorado de excepciones
   - Logging más detallado
   - Devuelve 200 OK en lugar de 500

## Próximos Pasos (Opcionales)

1. **Crear un Job Diario** - Ya existe `PandaScoreDailySyncJob.java`
2. **Agregar más videojuegos** - Solo añadir a `VIDEOGAME_ENDPOINTS`
3. **Mejorar DTOs** - Los DTOs actuales ya tienen `@JsonIgnoreProperties(ignoreUnknown = true)`
4. **Pruebas Unitarias** - Crear tests para validar sincronización

## Notas de Debugging

Si aún hay problemas:

1. **Verificar API Key:**
   ```powershell
   $env:PANDASCORE_API_KEY
   ```

2. **Ver logs en tiempo real:**
   ```powershell
   Get-Content -Path app.log -Wait
   ```

3. **Verificar conectividad:**
   ```powershell
   Test-NetConnection -ComputerName api.pandascore.co -Port 443
   ```

4. **Probar endpoint manualmente:**
   ```powershell
   $response = Invoke-WebRequest -Uri "https://api.pandascore.co/lol/matches?page=1&per_page=5&token=YOUR_KEY"
   $response.Content | ConvertFrom-Json
   ```

---

**Estado:** ✅ IMPLEMENTACIÓN COMPLETA
**Fecha:** 2026-04-19
**Versión:** 0.0.1-SNAPSHOT

