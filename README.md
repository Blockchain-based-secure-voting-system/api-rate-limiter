# NT14 API Rate Limiter & Optimizer Platform

A complete, end-to-end API rate limiting and security monitoring platform comprising:
1. **Kotlin/Ktor Gateway Backend (`:gateway_server`)**: Production-grade rate limiter implementing Token Bucket + Sliding Window algorithms with real-time WebSocket telemetry and control-plane REST APIs.
2. **Android Jetpack Compose Client (`:app`)**: Optimizer dashboard, dynamic rate-limit management, abuse detection, and live threat alerting.

---

## Repository Structure

```
├── app/                  # Android Jetpack Compose Application (com.cutm.nt14)
│   ├── src/main/java/com/cutm/nt14/
│   │   ├── data/local/   # Room DB (Entities, DAOs: RequestLog, RateLimit, AbuseEvent)
│   │   ├── data/remote/  # GatewayWebSocketClient, Google Sheets Sync
│   │   ├── domain/       # AbuseDetector, RateLimitOptimizer
│   │   └── ui/           # Compose Dashboard, Endpoints, RateLimits, Security Incidents
├── gateway_server/       # Kotlin/Ktor Rate Limiter Backend Service
│   ├── src/main/kotlin/
│   │   ├── algorithms/   # Thread-safe TokenBucket & SlidingWindow
│   │   ├── core/         # RateLimiter (Decision 4a AND logic), WebSocketManager
│   │   ├── models/       # Shared @Serializable Models & GatewayEvent
│   │   ├── routes/       # DemoRoutes, RuleRoutes, SimulateRoutes, EventsWebSocket
│   │   └── tools/        # SimulateTraffic CLI test runner
│   └── src/test/kotlin/  # Unit tests
└── settings.gradle.kts   # Root multi-project build orchestrating :app and :gateway_server
```

---

## Quick Start

### 1. Launch Gateway Backend
```powershell
.\gradlew.bat :gateway_server:run
```
Server starts on `http://0.0.0.0:8000`.

### 2. Verify with CLI Simulator
```powershell
.\gradlew.bat :gateway_server:runSimulate
```

### 3. Run Android Application
Open the project in Android Studio and run `:app` on an Android Emulator. The app connects to the gateway via loopback address `ws://10.0.2.2:8000/ws/events?api_key=dev-local-key` to receive live telemetry and trigger alerts.

---

## Detailed Documentation
See [`gateway_server/README.md`](gateway_server/README.md) for full endpoint specifications, algorithm semantics, and header contracts.
