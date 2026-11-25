# EXCHANGE PRO — Architecture

This document explains **how the system is structured and why**. It grows with every phase.

## 1. High-level view

```
                 ┌─────────────────────────────┐
  Browser ──────►│  React SPA (Vite dev server │   dev: /api, /actuator proxied to :8080
                 │  or static files in prod)   │
                 └──────────────┬──────────────┘
                                │ HTTP/JSON  (WebSocket from Phase 3/8)
                 ┌──────────────▼──────────────┐
                 │   Spring Boot backend       │
                 │   (modular monolith)        │
                 │                             │
                 │  auth · user · account      │
                 │  instrument · marketdata    │
                 │  trading · matching ·       │
                 │  orderbook · ledger · trade │
                 │  portfolio · notification   │
                 │  leaderboard · admin        │
                 │  ─────────────────────────  │
                 │  common · config · system   │
                 │  infrastructure             │
                 └───┬───────────┬─────────┬───┘
                     │ JDBC      │ (P6)    │ (P6)
              ┌──────▼─────┐ ┌───▼───┐ ┌───▼───┐
              │ PostgreSQL │ │ Redis │ │ Kafka │
              │ + Flyway   │ └───────┘ └───────┘
              └────────────┘
```

Phase 1 implements the browser, backend and PostgreSQL parts. Redis and Kafka appear in Phase 6.

## 2. Why a modular monolith?

A **monolith** is one deployable application. A **modular** monolith keeps strict internal boundaries between
feature modules, as if they were separate services, while still sharing one process and one database.

- **Simpler to build, test and debug** than microservices: one `mvn verify`, one process, local transactions.
- **Financial consistency is easier**: placing an order and reserving funds can share one ACID database
  transaction. Across microservices this needs sagas or distributed transactions.
- **Still extractable**: because each module talks to others only through its public services, DTOs and events
  (Kafka from Phase 6), a module such as `matching` can later become its own service.

### Module rules

1. Each top-level package under `com.exchangepro` is a module with a `package-info.java` describing it.
2. `common` and `config` must not depend on any feature module.
3. Modules call each other through **service interfaces and DTOs**, never another module's repositories or JPA
   entities.
4. Controllers are thin: they validate input, call one service method and map the result to a DTO.
5. JPA entities never leave the service layer (`spring.jpa.open-in-view=false` enforces this at runtime).

## 3. Backend building blocks (Phase 1)

| Concern | Class / file | How it works |
|---------|--------------|--------------|
| Typed configuration | `config/ExchangeProProperties` | `@ConfigurationProperties` record bound from `exchangepro.*`, validated at startup, so bad config fails fast. |
| Env-var configuration | `application.yml` | Every environment-specific value is `${ENV_VAR:default}`; secrets have **no default**. `spring.config.import` also reads `../.env` for local development. |
| Schema management | `db/migration/V1__baseline.sql` | Flyway applies versioned SQL scripts in order and records them in `flyway_schema_history`. Hibernate runs with `ddl-auto=validate`, so it only **checks** the schema and never changes it. |
| Error handling | `common/error/GlobalExceptionHandler` | `@RestControllerAdvice` maps exceptions to RFC 9457 Problem Details with a stable `code` (`ErrorCode` enum), `timestamp` and `correlationId`. Unexpected errors return a generic message; details go only to the log. |
| Domain errors | `ExchangeProException`, `ResourceNotFoundException` | Expected business failures carry an `ErrorCode` that decides the HTTP status. |
| Request tracing | `common/web/CorrelationIdFilter` | Runs first on every request. Reuses a safe incoming `X-Correlation-Id` or generates a UUID, puts it in the SLF4J MDC (printed in every log line), echoes it in the response, and clears it afterwards because threads are pooled. Unsafe incoming IDs are rejected to prevent log injection. |
| Time | `config/ClockConfig` | A `Clock` bean is injected wherever "now" is needed, so tests can freeze time. This becomes essential for order timestamps and price-time priority. |
| Health | Spring Boot Actuator | `/actuator/health` includes a DB check; `/liveness` and `/readiness` probes are ready for container orchestration. |
| System status | `system/SystemStatusService` | Runs `SHOW server_version`, asks Flyway for the current schema version, and reports the market-data mode. A database failure produces `DOWN` rather than an HTTP 500, so the UI can show what is broken. |
| Market-data safety | `marketdata/MarketDataMode`, `config/MarketDataModeGuard` | `SIMULATION` is labelled `SIMULATED DATA` and `realTime=false`. Starting in `REAL` mode is refused because no licensed provider exists. |
| API docs | `config/OpenApiConfig` + Springdoc | Generates the OpenAPI spec from controllers, served with Swagger UI. |

## 4. Frontend structure

```
src/
  components/   reusable presentational components (ui/ = generic primitives)
  pages/        one component per route; composes components + hooks
  layouts/      application shells (header, footer, <Outlet/>)
  hooks/        data hooks wrapping TanStack Query (useSystemStatus)
  services/     HTTP layer: apiClient (fetch + Problem Details → ApiError), per-domain API objects
  stores/       client state with Zustand (from Phase 2/7)
  types/        TypeScript mirrors of backend DTOs
  utils/        pure functions (formatting)
  charts/       chart components (from Phase 3/7)
  styles/       Tailwind entry + design tokens
  test/         test setup and helpers
```

**Data flow:** `page → hook (TanStack Query) → service → apiClient → fetch`. Components never call `fetch`
directly and contain no business rules. They render state that hooks and services provide.

**Server state vs client state:** data owned by the backend (balances, orders, prices) lives in the TanStack Query
cache, which handles caching, refetching and loading/error states. Zustand is reserved for pure UI/client state
(e.g. the auth session and the selected instrument).

**Design tokens:** the palette is defined once in `styles/index.css` (`@theme`). Components use semantic utilities
(`bg-surface`, `text-fg-muted`, `text-up`, `text-down`, `border-line`) rather than hex codes.

## 5. Testing strategy

| Level | Tool | Example |
|-------|------|---------|
| Unit | JUnit 5, Mockito, AssertJ | `SystemStatusServiceTest` mocks JDBC to test the DOWN path |
| Web slice | `@WebMvcTest` + MockMvc | `SystemStatusControllerTest` covers JSON shape, correlation header and CORS |
| Integration | `@SpringBootTest` + embedded PostgreSQL 16 | `ExchangeProApplicationIT` boots the full app, runs Flyway, calls real HTTP endpoints |
| Frontend unit/component | Vitest, Testing Library | `SystemStatusPage.test.tsx` covers loading, data, DB down, backend unreachable and refresh |

Naming: `*Test` runs in `mvn test` (Surefire); `*IT` runs in `mvn verify` (Failsafe).

## 6. Decisions log

| # | Decision | Reason |
|---|----------|--------|
| D1 | Spring Boot **3.5.x** although 4.x exists | Project brief requires Boot 3.x. 3.5 is the latest 3.x line. |
| D2 | Zonky embedded PostgreSQL for integration tests (in addition to the planned Testcontainers) | Docker was not available on the development machine. Zonky runs the real PostgreSQL binaries without Docker, so tests still exercise the true database rather than H2. |
| D3 | Problem Details produced by our own `GlobalExceptionHandler`; Boot's `spring.mvc.problemdetails` stays disabled | Boot's built-in handler would take precedence for framework exceptions and omit our `code`/`correlationId` fields (caught by the integration test). |
| D4 | Added a `system` module (not in the original list) | Operational status is cross-cutting and belongs neither to `admin` (role-protected) nor `common` (no endpoints). |
| D5 | TypeScript 5.9 instead of 7.x | `typescript-eslint` does not yet support TypeScript 7. |
| D6 | jsdom 29 instead of 31 | jsdom 30+ requires Node ≥ 24.15; the development machine has Node 24.11. |
| D7 | Vite dev proxy for `/api` | Same-origin requests in development; mirrors a reverse proxy in production. CORS is still configured for direct access. |
