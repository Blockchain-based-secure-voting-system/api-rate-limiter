# NT14 API Rate Limiter & Optimizer Platform

A complete, end-to-end API rate limiting, durable PostgreSQL telemetry, and synthetic API monitoring platform comprising:
1. **Kotlin/Ktor Gateway Backend (`:gateway_server`)**: Production-grade rate limiter implementing Token Bucket + Sliding Window algorithms, HikariCP + Flyway PostgreSQL persistence, bounded asynchronous batch logging, synthetic upstream probing (`TargetMonitor`), and RBAC-protected REST + WebSocket APIs.
2. **Android Jetpack Compose Client (`:app`)**: Live Dashboard, Upstream Monitoring tab with latency sparklines, keyset-paginated Logs (`real` vs `demo` filtering), aggregated Reports, dynamic rate-limit rules, and client ban management.

---

## Durable PostgreSQL Schema (`V1__init.sql`)

Migrations run automatically on startup via Flyway (`gateway_server/src/main/resources/db/migration/V1__init.sql`) whenever `DATABASE_URL` is set:

| Table | Primary Key | Purpose & Retention |
|---|---|---|
| `request_logs` | `id BIGINT` (`ms << 16 \| seq`) | Every proxied/demo decision (`ts`, `client_id` masked, `client_ip_hash` HMAC-SHA256, `method`, `path`, `status`, `latency_ms`, `decision`, `source` `real\|demo`, `upstream`). **Retention: 7 days**. |
| `metrics_1m` | `(minute, endpoint, source)` | Per-minute rollups (`requests`, `allowed`, `throttled`, `errors`, `p50`, `p95`, `latency_sum_ms`). **Retention: 90 days**. |
| `clients` | `id TEXT` | Per-client lifetime counters (`label`, `source`, `first_seen`, `last_seen`, `total_requests`, `throttled_count`). |
| `bans` | `client_key TEXT` | Active IP/client bans (`reason`, `created_at`, `expires_at`, `created_by`), restored on gateway startup. |
| `rules` | `endpoint_id TEXT` | Dynamic rate-limit rules (`limit_per_min`, `burst_limit`, `action`, `updated_at`), restored on startup. |
| `incidents` | `id TEXT` | Security & target-down incidents (`ts`, `type`, `severity`, `detail`, `target_id`, `resolved_at`). |
| `monitored_targets` | `id BIGSERIAL` | Synthetic probe targets (`name`, `url`, `method`, `expected_status`, `interval_sec`, `timeout_ms`, `enabled`). Seeded with `https://polylance-fv-1-45wy.onrender.com/health` and `https://polylance-fv-1-45wy.onrender.com/api/jobs`. |
| `probe_results` | `id BIGSERIAL` | Probe outcomes (`target_id`, `ts`, `up`, `status_code`, `latency_ms`, `error`, `transition` `up->down\|down->up`). **Retention: 30 days**. |

### Asynchronous Batch Writer & Privacy
- **Bounded Queue (`RequestLogWriter`)**: The request hot path only calls non-blocking `ArrayBlockingQueue.offer` (capacity `10,000`). A background coroutine flushes up to `1,000` entries every `1 s` using `reWriteBatchedInserts=true`. If the queue ever fills, entries are dropped and counted (`PersistenceStatus.dropped`) so the request hot path never blocks.
- **IP Privacy**: Raw client IPs are never stored in `request_logs` or `clients`. `client_id` stores either `user:<email>` or a masked IP (`ip:203.0.113.x`), and `client_ip_hash` stores `HMAC-SHA256(ip, IP_HASH_SALT)` for correlation and filtering.

---

## Render Dashboard Configuration (No Render API Key Required)

1. **Create / Attach PostgreSQL on Render**:
   - In the [Render Dashboard](https://dashboard.render.com/), click **New +** &rarr; **PostgreSQL** (e.g., `nt14-gateway-db`, Free or Starter plan in the same region as your gateway).
   - Once created, copy the **Internal Database URL** (for the Render web service) and the **External Database URL** (if querying from your laptop).
2. **Set Environment Variables on the Gateway Web Service**:
   - Open your gateway service (`api-rate-limiter-gateway-n0ab`) &rarr; **Environment** tab &rarr; set:
     - `DATABASE_URL` = `<Internal Database URL from step 1>`
     - `DEMO_TRAFFIC` = `false`
     - `JWT_SECRET` = `<your 32+ character secret>`
     - `IP_HASH_SALT` = `<your random salt string>`
     - `ADMIN_EMAILS` = `akpolylance@gmail.com`
     - `GOOGLE_CLIENT_ID` = `589442578334-c07uh623kie143m2blh3i9an6u52i5si.apps.googleusercontent.com`
     - `TRUSTED_PROXIES` = `127.0.0.1,::1,localhost,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16`
   - Click **Save, Rebuild, and Deploy**. Flyway automatically runs `V1__init.sql` on boot.

---

## How to View & Verify Data

### 1. Inspect Database Row Counts (`SELECT count(*)`)
Using the built-in Gradle SQL runner (no `psql` binary required):
```powershell
$env:JAVA_HOME = "d:\Android\.jdk17"
$env:DATABASE_URL = "postgres://user:pass@host:5432/db"
.\gradlew.bat :gateway_server:dbQuery
```
Or run a custom query:
```powershell
.\gradlew.bat :gateway_server:dbQuery -Psql="SELECT id, ts, client_id, client_ip_hash, path, status, latency_ms, source, upstream FROM request_logs ORDER BY id DESC LIMIT 10"
```

### 2. Query REST Endpoints
```powershell
# 1. Keyset-paginated logs (filterable by source=real|demo, status=200|429|5xx, client, from, to)
curl.exe -s -H "Authorization: Bearer $TOKEN" "https://api-rate-limiter-gateway-qgfy.onrender.com/api/logs?limit=20&source=real"

# 2. Time-series rollups from metrics_1m (range=1h|24h|7d)
curl.exe -s -H "Authorization: Bearer $TOKEN" "https://api-rate-limiter-gateway-qgfy.onrender.com/api/metrics?range=1h&source=real"

# 3. Aggregated traffic report from PostgreSQL
curl.exe -s -H "Authorization: Bearer $TOKEN" "https://api-rate-limiter-gateway-qgfy.onrender.com/api/reports?range=24h&source=real"

# 4. Monitored upstream targets & latency sparklines
curl.exe -s -H "Authorization: Bearer $TOKEN" "https://api-rate-limiter-gateway-qgfy.onrender.com/api/targets/status"

# 5. Admin CSV export
curl.exe -s -H "Authorization: Bearer $ADMIN_TOKEN" "https://api-rate-limiter-gateway-qgfy.onrender.com/api/export?format=csv&source=real" -o logs.csv
```

### 3. Run Automated Persistence & Monitoring Tests
```powershell
$env:JAVA_HOME = "d:\Android\.jdk17"
.\gradlew.bat :gateway_server:test :app:testDebugUnitTest
```

See [`gateway_server/README.md`](gateway_server/README.md) for full endpoint specifications and architecture details.
