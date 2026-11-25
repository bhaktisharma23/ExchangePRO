# EXCHANGE PRO

**A distributed stock-exchange and multi-user trading _simulation_ platform.**

Users register, receive ₹1,00,000 of **virtual** money, explore Indian equities, place market/limit orders that are
matched by a custom Java matching engine, and track their portfolio and profit/loss in real time.

> ⚠️ **Simulation only.** EXCHANGE PRO never executes real trades and never handles real money.
> Prices are **synthetic** (labelled `SIMULATED DATA`) unless a licensed, free, redistributable provider is
> configured — none is today. The platform does **not** connect to NSE/BSE.

---

## Project status

| Phase | Scope | Status |
|------:|-------|--------|
| 1 | Project foundation & architecture | ✅ Complete |
| 2 | Authentication, users, virtual accounts | ⏳ Next |
| 3 | Instruments & market-data simulation | — |
| 4 | Custom Java matching engine | — |
| 5 | Trading APIs & double-entry ledger | — |
| 6 | Kafka, Redis, transactional outbox | — |
| 7 | React trading interface | — |
| 8 | Real-time multi-user trading | — |
| 9 | Administration, security, production engineering | — |
| 10 | Final integration, testing, documentation | — |

## What works today (Phase 1)

- Spring Boot 3.5 backend (Java 21) with a modular-monolith package layout.
- PostgreSQL 16 with Flyway migrations (`V1__baseline`).
- `GET /api/v1/system/status` — version, live database/migration state, active market-data mode.
- Actuator health, liveness and readiness probes; OpenAPI/Swagger UI.
- Consistent RFC 9457 Problem Details error responses with machine-readable `code`.
- Correlation ID on every request (`X-Correlation-Id` header + every log line).
- All configuration via environment variables; no secrets in source.
- React 19 + TypeScript + Vite + Tailwind CSS frontend with a live **System status** screen.
- Backend unit tests, a full-stack integration test against a real PostgreSQL 16, and frontend component tests.

## Tech stack

| Layer | Technologies |
|-------|--------------|
| Backend | Java 21, Spring Boot 3.5, Maven, Spring Web MVC, Bean Validation, Spring Data JPA/Hibernate, Actuator, Springdoc OpenAPI |
| Database | PostgreSQL 16, Flyway |
| Frontend | React 19, TypeScript 5.9, Vite 8, Tailwind CSS 4, React Router 7, TanStack Query 5, Lucide icons |
| Testing | JUnit 5, Mockito, AssertJ, MockMvc, Zonky embedded PostgreSQL, Vitest, Testing Library |
| Infrastructure | Docker Compose |
| _Planned_ | Spring Security + JWT (P2), WebSocket (P3/P8), Kafka + Redis (P6), Zustand + charts (P7), Testcontainers, Prometheus + Grafana, GitHub Actions, k6 (P9) |

## Repository layout

```
ExchangePRO/
├── backend/                  Spring Boot application (Maven, Java 21)
│   ├── src/main/java/com/exchangepro/
│   │   ├── auth user account instrument marketdata trading matching orderbook
│   │   ├── portfolio ledger trade notification leaderboard admin   ← feature modules
│   │   ├── common            error model, web filters (shared by all modules)
│   │   ├── config            CORS, OpenAPI, typed properties, clock
│   │   ├── infrastructure    Kafka/Redis/outbox adapters (Phase 6)
│   │   └── system            system status endpoint
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/     Flyway SQL migrations (V1__baseline.sql, ...)
│   └── src/test/             unit tests (*Test) and integration tests (*IT)
├── frontend/                 React + Vite application
│   └── src/  components pages layouts hooks services stores types utils charts styles test
├── docs/ARCHITECTURE.md      architecture and design decisions
├── docker-compose.yml        local PostgreSQL
└── .env.example              environment variable template
```

## Prerequisites

All free:

| Tool | Version | Notes |
|------|---------|-------|
| JDK | 21 | `java -version` |
| Node.js | 24.x (≥ 22.13) | `node -v` |
| Docker Desktop | any recent | Free for personal/education use. Needed to run PostgreSQL locally (and Kafka/Redis from Phase 6). |

Maven does **not** need to be installed: use the bundled wrapper (`./mvnw` / `mvnw.cmd`).

## Quick start

```bash
# 1. Configure environment (then edit the passwords in .env)
cp .env.example .env

# 2. Start PostgreSQL
docker compose up -d
docker compose ps            # wait until "healthy"

# 3. Start the backend  (http://localhost:8080)
cd backend
./mvnw spring-boot:run       # Windows: mvnw.cmd spring-boot:run

# 4. Start the frontend (http://localhost:5173), in a second terminal
cd frontend
npm install
npm run dev
```

Open <http://localhost:5173> — the **System status** page shows backend, database and market-data state.
The Vite dev server proxies `/api` and `/actuator` to the backend, so no CORS setup is needed in development.

### Useful URLs

| URL | Purpose |
|-----|---------|
| http://localhost:5173 | Frontend |
| http://localhost:8080/api/v1/system/status | System status JSON |
| http://localhost:8080/actuator/health | Health (incl. database) |
| http://localhost:8080/actuator/health/liveness · `/readiness` | Container probes |
| http://localhost:8080/swagger-ui.html | Interactive API docs |
| http://localhost:8080/v3/api-docs | OpenAPI 3 spec |

## Environment variables

Defined in [`.env.example`](.env.example). The backend reads them from the real environment first, then from the
repository-root `.env` file. Docker Compose reads `.env` automatically.

| Variable | Default | Used by | Description |
|----------|---------|---------|-------------|
| `POSTGRES_DB` | `exchangepro` | Compose | Database name created in the container |
| `POSTGRES_USER` | `exchangepro` | Compose | Database owner role |
| `POSTGRES_PASSWORD` | _(required)_ | Compose | Database password |
| `POSTGRES_PORT` | `5432` | Compose | Host port mapped to PostgreSQL |
| `DB_URL` | `jdbc:postgresql://localhost:5432/exchangepro` | Backend | JDBC URL |
| `DB_USERNAME` | `exchangepro` | Backend | Database user |
| `DB_PASSWORD` | _(required)_ | Backend | Database password |
| `DB_POOL_MAX_SIZE` | `10` | Backend | HikariCP maximum pool size |
| `SERVER_PORT` | `8080` | Backend | HTTP port |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Backend | Comma-separated allowed browser origins |
| `HEALTH_SHOW_DETAILS` | `never` | Backend | Actuator health details: `never` / `when-authorized` / `always` |
| `LOG_LEVEL_APP` | `INFO` | Backend | Log level for `com.exchangepro.*` |
| `MARKET_DATA_MODE` | `SIMULATION` | Backend | `SIMULATION` only; `REAL` is refused at startup until a licensed provider exists |
| `VITE_DEV_BACKEND_URL` | `http://localhost:8080` | Frontend (dev) | Target of the Vite dev proxy |
| `VITE_API_BASE_URL` | _(empty = same origin)_ | Frontend | Absolute API base URL for production builds |

## Testing

### Backend

```bash
cd backend
./mvnw test       # unit + web-slice tests (*Test) — fast, no database
./mvnw verify     # also runs integration tests (*IT) against a real PostgreSQL 16
```

Integration tests use **Zonky embedded PostgreSQL**: real PostgreSQL 16 binaries downloaded through Maven and
started in-process, so they run **without Docker**. Testcontainers will be added when Docker-based services
(Kafka, Redis) arrive in Phase 6.

### Frontend

```bash
cd frontend
npm run typecheck   # TypeScript, strict mode
npm run lint        # ESLint
npm test            # Vitest + Testing Library
npm run build       # production build into dist/
```

> **Windows + Git Bash note:** if Vitest reports `failed to find the current suite`, you entered the folder with a
> lower-case drive letter (`cd /c/...` → `c:\`). Use `cd "C:/ExchangePRO/frontend"` or run the tests from
> PowerShell, CMD or your IDE. It is a Vitest module-resolution quirk, not a test failure.

## Market data policy

The development budget is ₹0, and market data is licensed. Therefore:

1. **SIMULATION mode** (default, fully functional from Phase 3): prices, OHLC candles, volume and bid/ask are
   generated by the platform. Every price display carries the `SIMULATED DATA` label. Generated prices are never
   presented as genuine exchange prices.
2. **REAL mode** is a pluggable provider interface. It will only be enabled for a free source whose terms
   explicitly allow multi-user display/redistribution, with attribution, source and last-updated timestamp shown.
   No such provider is integrated; the backend **refuses to start** with `MARKET_DATA_MODE=REAL`.
3. No scraping of NSE/BSE websites, no paid APIs, no redistribution of personal API access.

## Known limitations (Phase 1)

- No authentication yet — every endpoint is public. Spring Security + JWT arrives in Phase 2.
- `docker compose` could not be executed on the development machine used for Phase 1 (Docker not installed).
  The Compose file is standard, but you should run `docker compose up -d` yourself once to confirm it.
- No GitHub Actions CI yet (Phase 9).

## License

Educational project. Not affiliated with NSE, BSE or any broker.
