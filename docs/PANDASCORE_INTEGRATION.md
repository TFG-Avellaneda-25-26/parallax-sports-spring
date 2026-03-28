# Integración de PandaScore en Parallax Sports API

Este documento explica cómo se ha integrado la API de PandaScore en el proyecto, qué cambios se han aplicado en el código y en la base de datos, y cómo probarlo paso a paso con el ejemplo de Valorant.

## Resumen rápido

- Se añadió un cliente para consultar la API de PandaScore y servicios para sincronizar y persistir partidos.
- Se añadieron DTOs (externos e internos) y una entidad JPA `PandaScoreMatch` mapeada a la tabla `pandascore_matches`.
- Se exponen endpoints REST para pruebas (sin persistir), sincronización y lectura de matches persistidos.
- La integración usa la variable de entorno `PANDASCORE_API_KEY` para autenticar las llamadas externas.

---

## Cambios principales en el código

- `dev.parallaxsports.external.pandascore.client.PandaScoreClient`
  - Cliente HTTP que realiza peticiones a PandaScore. Añade la cabecera `Authorization: Bearer <API_KEY>` y consulta endpoints como `/matches` filtrando por `videogame`.

- `dev.parallaxsports.external.pandascore.dto.PandaScoreMatchDto`
  - DTO que mapea el JSON devuelto por PandaScore (id, name, begin_at, end_at, status, slug, league, opponents, results...).

- `dev.parallaxsports.pandascore.dto.PandaScoreMatchResponse`
  - DTO de respuesta usado por los endpoints locales (incluye `videogame`).

- `dev.parallaxsports.pandascore.model.PandaScoreMatch`
  - Entidad JPA con columnas: `pandascore_id`, `name`, `slug`, `league_name`, `videogame`, `status`, `begin_at`, `end_at`, `raw_json`, `created_at`, `updated_at`.
  - `raw_json` guarda el payload original para facilitar depuración.

- `dev.parallaxsports.pandascore.repository.PandaScoreMatchRepository`
  - Repositorio Spring Data JPA con métodos derivados (sin JPQL):
    - `findByLeagueNameOrderByBeginAtDesc(...)`
    - `findByVideogameIgnoreCaseOrderByBeginAtDesc(String videogame)`

- `dev.parallaxsports.external.pandascore.service.PandaScoreSyncWriteService`
  - Servicio que convierte DTOs a entidad y realiza upsert (crea/actualiza) en la BD. Serializa el DTO a `raw_json` con `ObjectMapper`.

- `dev.parallaxsports.external.pandascore.service.PandaScoreSyncService`
  - Orquestador que `fetch` de `PandaScoreClient` y delega en `PandaScoreSyncWriteService`.

- `dev.parallaxsports.pandascore.controller.PandaScoreController`
  - Endpoints añadidos:
    - `GET /api/pandascore/test/{videogame}` — Llama a PandaScore y devuelve resumen (NO persiste). Útil para depuración sin tocar la BD.
    - `POST /api/pandascore/sync/{videogame}` — Realiza fetch y persistencia (upsert).
    - `GET /api/pandascore/matches?videogame=...` — Lista matches persistidos filtrando por `videogame`.

- `src/main/resources/logback-spring.xml`
  - Contiene appender LOKI apuntando por defecto a `http://localhost:3100`. Si no tienes Loki corriendo, verás warnings en arranque. No es crítico para la funcionalidad.

- `dev.parallaxsports.notification.service.AlertSchemaStartupValidator`
  - Se ha hecho opt-in mediante `@ConditionalOnProperty` para que NO impida el arranque local si no existen las tablas de alertas. Activarla requiere `app.alert-schema-validation.enabled=true`.

---

## Cambios en la base de datos

Tabla principal añadida/especificada: `pandascore_matches`.

SQL recomendado para crear la tabla (PostgreSQL):

```sql
CREATE TABLE IF NOT EXISTS public.pandascore_matches (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pandascore_id BIGINT NOT NULL,
    name VARCHAR(1024) NOT NULL,
    slug VARCHAR(512),
    league_name VARCHAR(255),
    videogame VARCHAR(128),
    status VARCHAR(50),
    begin_at TIMESTAMP WITH TIME ZONE,
    end_at TIMESTAMP WITH TIME ZONE,
    raw_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_pandascore_matches_begin_at ON public.pandascore_matches (begin_at);
CREATE INDEX IF NOT EXISTS idx_pandascore_matches_pandascore_id ON public.pandascore_matches (pandascore_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_pandascore_matches_pandascore_id'
    ) THEN
        ALTER TABLE public.pandascore_matches
            ADD CONSTRAINT uq_pandascore_matches_pandascore_id UNIQUE (pandascore_id);
    END IF;
END$$;
```

Si la tabla existe pero faltan columnas (`videogame`, `raw_json`) añade:

```sql
ALTER TABLE public.pandascore_matches ADD COLUMN IF NOT EXISTS videogame VARCHAR(128);
ALTER TABLE public.pandascore_matches ADD COLUMN IF NOT EXISTS raw_json TEXT;
```

Recomendación: usar Flyway o Liquibase para migraciones en entornos productivos.

---

## Variables de entorno y configuración

- Variable obligatoria para llamadas externas: `PANDASCORE_API_KEY`.
- `application.yaml` usa `app.external-api.pandascore-api-key: ${PANDASCORE_API_KEY}`.

Ejemplo (PowerShell) temporal para arrancar la app:

```powershell
$env:PANDASCORE_API_KEY = 'TU_API_KEY_AQUI'
.\mvnw.cmd -DskipTests spring-boot:run
```

O ejecutando el JAR:

```powershell
$env:PANDASCORE_API_KEY = 'TU_API_KEY_AQUI'
java -jar target\parallax-sports-api-0.0.1-SNAPSHOT.jar
```

---

## Endpoints (resumen)

- GET /api/pandascore/test/{videogame}?page=1&perPage=20
  - No persiste, devuelve `matchesFetched` y un `sample` del primer match.

- POST /api/pandascore/sync/{videogame}?pages=1&perPage=20
  - Fetch + persist (upsert). Devuelve counters: `matchesFetched`, `matchesUpserted`.

- GET /api/pandascore/matches?videogame=valorant
  - Devuelve los matches persistidos (mapeados a `PandaScoreMatchResponse`).

---

## Ejemplo paso a paso: probar con Valorant

Requisitos previos:
- Base de datos PostgreSQL accesible y configurada en `application.yaml`.
- Haber ejecutado el script SQL para crear `pandascore_matches` si no existe.
- Tener `PANDASCORE_API_KEY` válido y exportado en la sesión donde arrancarás la app.

1) Establecer variable de entorno (PowerShell):

```powershell
Push-Location 'C:\Users\Ismi2\Desktop\2DAW-N\TFG\parallax-sports-spring'
$env:PANDASCORE_API_KEY = 'TU_API_KEY'
```

2) Arrancar la aplicación:

```powershell
.\mvnw.cmd -DskipTests spring-boot:run
# o
java -jar target\parallax-sports-api-0.0.1-SNAPSHOT.jar
```

3) Probar el endpoint de diagnóstico (no persistente):

```powershell
curl "http://localhost:8080/api/pandascore/test/valorant?page=1&perPage=20" -H "Accept: application/json"
```

Respuesta esperada (ejemplo):

```json
{
  "videogame": "valorant",
  "matchesFetched": 20,
  "sample": { /* primer match devuelto por PandaScore */ }
}
```

- Si `matchesFetched` = 0, comprobar: clave API (en la sesión del proceso), conectividad, o que PandaScore tenga datos para esa petición.

4) Si la prueba devuelve datos, ejecutar la sincronización (persistencia):

```powershell
curl -X POST "http://localhost:8080/api/pandascore/sync/valorant?pages=1&perPage=20" -H "Content-Type: application/json"
```

Respuesta esperada (ejemplo):

```json
{
  "videogame": "valorant",
  "matchesFetched": 20,
  "matchesUpserted": 18
}
```

5) Consultar matches persistidos:

```powershell
curl "http://localhost:8080/api/pandascore/matches?videogame=valorant" -H "Accept: application/json"
```

6) Consulta opcional directa en la BD (psql):

```sql
select id, pandascore_id, name, league_name, videogame, begin_at, status
from pandascore_matches
where videogame = 'valorant'
order by begin_at desc
limit 50;
```

---

## Diagnóstico directo contra PandaScore (si el endpoint test devuelve 0)

Probar la llamada directa a la API de PandaScore con curl:

```bash
curl -v "https://api.pandascore.co/matches?filter[videogame]=valorant&per_page=20" \
  -H "Authorization: Bearer TU_API_KEY"
```

- 401/403 -> API key inválida o sin permisos.
- 429 -> rate limit (esperar o reducir peticiones).
- Lista vacía -> PandaScore no tiene matches para ese filtro/página.

---

## Problemas comunes y soluciones rápidas

- 403 / Forbidden al llamar a los endpoints locales:
  - Verifica que la ruta `/api/pandascore/test/*` está `permitAll()` (debería estarlo).
  - Si la ruta requiere autenticación en tu entorno, asegúrate de enviar un token válido o prueba el endpoint `test`.

- La aplicación no arranca por validaciones de esquema de alertas:
  - La validación se ha hecho opt-in. Para activarla pon `app.alert-schema-validation.enabled=true` en `application.yaml`.

- Warnings de Loki en arranque:
  - Si no tienes Loki corriendo en `http://localhost:3100` verás warnings. No afectan a la integración; cambia `observability.loki-url` o elimina el appender si es necesario.

---

Si quieres, puedo:
- Crear una migración Flyway `V1__create_pandascore_matches.sql` y añadir la dependencia para que la tabla se cree automáticamente al arrancar.
- Añadir un endpoint admin para contar matches por `videogame`.


Documento creado: `docs/PANDASCORE_INTEGRATION.md`

