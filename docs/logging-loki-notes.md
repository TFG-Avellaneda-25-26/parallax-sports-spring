# Logging, Loki, Prometheus, and Grafana Notes

This document explains how observability is wired in this project and why Grafana can show "connected datasource" but still show no project data.

## 1. Architecture: Who Talks to Whom
There are two independent pipelines:

### Logs pipeline
Application -> Loki (via Logback appender) -> Grafana Explore

- The app sends logs directly to Loki using `loki-logback-appender`.
- Grafana does not scrape logs from your app.
- Grafana only queries Loki.

### Metrics pipeline
Application -> Prometheus scrape -> Grafana dashboards/queries

- The app exposes metrics on `/actuator/prometheus`.
- Prometheus must scrape that endpoint periodically.
- Grafana queries Prometheus, not your app directly.

Key implication: a Grafana datasource can be healthy while still showing no project data if ingestion/scraping is not happening.

## 2. What Was Configured in This Project
### App-side logging
- `logback-spring.xml` defines:
  - `CONSOLE` appender
  - `LOKI` appender
  - labels: `app`, `host`, `level`
  - JSON message payload with logger/thread/message/traceId

### App-side metrics
- Actuator exposure includes `prometheus` endpoint in `application.yaml`.
- Security config now allows `/actuator/prometheus` publicly for scraping and protects the rest of actuator.

### Datasource URLs
- App uses `observability.loki-url` as base URL.
- Logback appender adds `/loki/api/v1/push`.

## 3. Most Likely Reasons You See No Data
### A) Prometheus has no scrape target for your app
Very common.
Even with Grafana connected to Prometheus, if Prometheus is not scraping your app endpoint, no app metrics appear.

What to verify:
- In Prometheus UI -> Status -> Targets
- Your app target should be `UP`

If missing, add scrape config in Prometheus:
```yaml
scrape_configs:
  - job_name: "parallax-sports-api"
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ["<app-host>:8080"]
```

### B) `/actuator/prometheus` blocked by security
If endpoint returns 401/403, Prometheus scrape fails.

Quick check from machine that runs Prometheus:
```bash
curl -i http://<app-host>:8080/actuator/prometheus
```
Expected: HTTP 200 with metrics text.

### C) Loki URL format mismatch
If `LOKI_URL` already contains `/loki/api/v1/push`, and logback appender appends it again, ingestion fails.

Use base URL only:
- good: `http://localhost:3100`
- bad: `http://localhost:3100/loki/api/v1/push`

### D) App cannot reach Loki over network
Connection test in Grafana only proves Grafana -> Loki, not app -> Loki.

From app host:
```bash
curl -sS "$LOKI_URL/ready"
```

### E) Query labels don’t match
Logs are labeled with `app=${spring.application.name}`.
In this project that is `parallax-sports-api`.

Use query:
```logql
{app="parallax-sports-api"}
```

## 4. Practical End-to-End Validation
### Step 1: Restart app after logback changes
`logback-spring.xml` is loaded at startup.

### Step 2: Trigger known logs
Call `/api/auth/register` or `/api/auth/login` to generate auth/security logs.

### Step 3: Check Loki directly
```bash
curl -G -sS "$LOKI_URL/loki/api/v1/query" --data-urlencode 'query={app="parallax-sports-api"}'
```

### Step 4: Confirm Prometheus scrape
- Prometheus Targets page: job should be `UP`.
- Direct endpoint call should return metrics.

### Step 5: Query in Grafana
- Logs (Loki): `{app="parallax-sports-api"}`
- Metrics (Prometheus), example:
  - `rate(http_server_requests_seconds_count[5m])`

## 5. About Alloy vs Direct Appender
You asked if you can skip Alloy and push directly from app.
Yes, this project currently does that and it is valid.

When Alloy helps more:
- centralized collection for many services
- buffering/retries/backpressure outside app process
- relabeling/enrichment and fan-out pipelines

For a single-project phase, direct appender is simpler and fine.

## 6. File References
- `src/main/resources/logback-spring.xml`
- `src/main/resources/application.yaml`
- `src/main/java/dev/parallaxsports/core/config/SecurityConfig.java`
- `pom.xml`