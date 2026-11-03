# NT14 Rate Limiter Gateway Backend (Kotlin / Ktor + PostgreSQL)

High-performance API rate limiter gateway, durable PostgreSQL telemetry engine, and synthetic upstream monitor pairing with the **NT14 Android Optimizer Application**.

---

## 1. Durable Storage & Schema (`V1__init.sql`)

When `DATABASE_URL` is configured (`postgres://...` or `jdbc:postgresql://...`), the gateway initializes a `HikariDataSource` pool and runs Flyway migrations from `src/main/resources/db/migration/V1__init.sql`:

```sql
request_logs      (id BIGINT PK, ts TIMESTAMPTZ, client_id TEXT, client_ip_hash TEXT, method TEXT, path TEXT, status INT, latency_ms BIGINT, decision TEXT, source TEXT ['real'|'demo'], upstream TEXT)
metrics_1m        (minute TIMESTAMPTZ, endpoint TEXT, source TEXT, requests BIGINT, allowed BIGINT, throttled BIGINT, errors BIGINT, p50 DOUBLE PRECISION, p95 DOUBLE PRECISION, latency_sum_ms BIGINT, PRIMARY KEY (minute, endpoint, source))
clients           (id TEXT PK, label TEXT, source TEXT, first_seen TIMESTAMPTZ, last_seen TIMESTAMPTZ, total_requests BIGINT, throttled_count BIGINT)
bans              (client_key TEXT PK, reason TEXT, created_at TIMESTAMPTZ, expires_at TIMESTAMPTZ, created_by TEXT)
rules             (endpoint_id TEXT PK, limit_per_min INT, burst_limit INT, action TEXT, updated_at TIMESTAMPTZ)
incidents         (id TEXT PK, ts TIMESTAMPTZ, type TEXT, severity TEXT, detail TEXT, target_id BIGINT, resolved_at TIMESTAMPTZ)
monitored_targets (id BIGSERIAL PK, name TEXT UNIQUE, url TEXT, method TEXT, expected_status INT, interval_sec INT, timeout_ms INT, enabled BOOLEAN, created_at TIMESTAMPTZ)
probe_results     (id BIGSERIAL PK, target_id BIGINT FK, ts TIMESTAMPTZ, up BOOLEAN, status_code INT, latency_ms BIGINT, error TEXT, transition TEXT)
```

### Hot-Path Batching & Retention
- **Non-blocking Queue**: `RequestLogWriter` uses a bounded `ArrayBlockingQueue(10_000)` flushed every `1 s` in batches of `1,000` rows. Overflow is dropped and counted (`dropped` counter exposed in `/health`).
- **Scheduled Retention**: `RetentionJob` runs hourly and purges `request_logs` older than **7 days**, `metrics_1m` older than **90 days**, `probe_results` older than **30 days**, and expired `bans`.

---

## 2. REST & WebSocket API Reference

| Method | Endpoint | Auth / Role | Description |
|---|---|---|---|
| `GET` | `/health` | Public | Health, uptime, live metrics, `persistence` queue/batch status, `demoTraffic` flag |
| `GET` | `/api/polylance/escrows` | Public (Rate-Limited) | Proxies live PolyLance escrows (`/api/jobs`), logged with `source=real` |
| `GET` | `/api/polylance/talents` | Public (Rate-Limited) | Proxies live PolyLance talents (`/api/sync`), logged with `source=real` |
| `GET` | `/api/polylance/attestations` | Public (Rate-Limited) | Proxies live PolyLance attestations (`/api/sync`), logged with `source=real` |
| `GET` | `/api/logs` | `VIEWER` or `ADMIN` | Keyset-paginated logs (`?cursor&limit&status&client&from&to&source=real\|demo`) |
| `GET` | `/api/metrics` | `VIEWER` or `ADMIN` | Time-series rollups from `metrics_1m` (`?range=1h\|24h\|7d&source=real\|demo`) |
| `GET` | `/api/reports` | `VIEWER` or `ADMIN` | Aggregated traffic report from PostgreSQL (`?range=1h\|24h\|7d&source=real\|demo`) |
| `GET` | `/api/targets/status` | `VIEWER` or `ADMIN` | Monitored targets with up/down status, 24h uptime %, and latency sparklines |
| `GET/POST/PUT/DELETE` | `/api/targets` | `ADMIN` only | Full CRUD for `monitored_targets` + `POST /api/targets/{id}/probe` |
| `GET` | `/api/export` | `ADMIN` only | Streams CSV export (`?from&to&format=csv&source&status&client`) |
| `GET` | `/api/rules` | `VIEWER` or `ADMIN` | List rate limit rules (persisted in `rules`) |
| `POST/PUT/DELETE` | `/api/rules` | `ADMIN` only | Create, update, or remove rate limit rules |
| `GET` | `/api/bans` | `VIEWER` or `ADMIN` | List active bans (persisted in `bans`) |
| `POST/DELETE` | `/api/bans` | `ADMIN` only | Ban or unban a client |
| `WS` | `/ws/events` | `VIEWER` or `ADMIN` | Real-time stream; initial `snapshot` is loaded from PostgreSQL (`snapshotFrom="db"`) |
