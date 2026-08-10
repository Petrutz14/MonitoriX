# MonitoriX — Claude Code Guide

## Greeting

Always open the conversation by addressing the user as **Peter**.

---

## Project Scope

Real-time PC monitoring system. A lightweight Python agent runs on each monitored machine, collects CPU/RAM/disk metrics every 15 seconds, and POSTs them to a Spring Boot backend. The backend persists metrics, evaluates configurable alert rules, and pushes live updates over WebSocket. An Angular frontend renders a live dashboard with per-machine drilldown, alert toasts, and rule management.

---

## Repository Layout

```
MonitoriX/
├── monitor_pc/           # Spring Boot backend (Java 25, Maven)
├── MonitorX_Frontend/    # Angular 21 frontend (TypeScript 5.9)
└── MonitorX_Metrics.py   # Python 3 metrics agent (psutil + requests)
```

---

## Backend — `monitor_pc/`

**Stack:** Spring Boot 4.0.5 · Java 25 · PostgreSQL 16 (Docker) · Lombok · MapStruct 1.6.3 · STOMP WebSocket · Spring Security + OAuth2 Resource Server

**Package root:** `com.monitorpc.monitor_pc`

```
src/main/java/com/monitorpc/monitor_pc/
├── controller/     MachineController, MetricController, AlertController, AuthController
├── service/        MachineService, MetricIngestionService, AlertEvaluationService,
│                   AlertRuleService, MachineHealthService, AuthService
├── model/          Machine, SystemMetric, TopProcess, DiskPartition, AlertRule, Alert, User
├── repository/     one JpaRepository per entity (+ custom @Query methods)
├── dto/            AgentPayloadDTO (inbound), *ResponseDTO (outbound), *RequestDTO (rules in)
│                   LoginRequestDTO, RegisterRequestDTO, AgentRegisterRequestDTO, AuthResponseDTO
├── mapper/         MachineMapper, MetricMapper, AlertMapper  (MapStruct compile-time)
├── enums/          MachineStatus, MetricType, AlertOperator, AlertSeverity, AlertStatus
├── config/         SecurityConfig  (JWT RSA, filter chain, CORS, password encoder)
├── websocket/      WebSocketConfig  (STOMP broker, /ws endpoint, /topic prefix)
└── exception/      ResourceNotFound, GlobalExceptionHandler
```

### Key Data Flows

**Metric ingestion** (`POST /api/metrics`):
1. `MetricIngestionService.ingest()` upserts Machine by `machineId` (hostname); sets `status=ONLINE`, `lastSeen=now`.
2. Saves `SystemMetric` + child `TopProcess` and `DiskPartition` rows.
3. Calls `AlertEvaluationService.evaluate(machine, metric)` **synchronously** — no async job.
4. Broadcasts `MetricResponseDTO` to `/topic/metrics`.

**Alert evaluation** (`AlertEvaluationService.evaluate()`):
- Loads applicable rules: `machine_id IS NULL` (global) OR `machine_id = this machine`.
- Condition met + no ONGOING alert → create alert, broadcast to `/topic/alerts`.
- Condition NOT met + ONGOING alert → set RESOLVED, broadcast.

**Machine health check** (`MachineHealthService.offlineChecker()`):
- `@Scheduled(fixedRate=30000)` — marks machines OFFLINE if `lastSeen < now − 30s`.

### REST API

| Method | Path | Auth required | Description |
|--------|------|---------------|-------------|
| POST | `/api/auth/register` | No | Register a human user |
| POST | `/api/auth/login` | No | Login → returns JWT |
| POST | `/api/auth/register-agent` | No (secret header) | Self-register an agent (see below) |
| GET | `/api/machines` | JWT | List all machines |
| PATCH | `/api/machines/{id}` | JWT | Update display name |
| POST | `/api/metrics` | JWT (`ROLE_AGENT`) | Ingest agent payload |
| GET | `/api/metrics/{id}` | JWT | Latest metric for a machine |
| GET | `/api/metrics/{id}/history` | JWT | Historical metrics |
| GET/POST/DELETE | `/api/alert-rules` | JWT | Manage alert rules |
| PATCH | `/api/alert-rules/{id}/toggle` | JWT | Enable/disable rule |
| GET | `/api/alerts/{id}/active` | JWT | Active alerts for a machine |

### Authentication

JWT-based, stateless. RSA keys in `src/main/resources/certs/`. Token issued on login/register; send as `Authorization: Bearer <token>`.

**Agent self-registration** — `POST /api/auth/register-agent`:
- Header: `X-Agent-Secret: <value>` must match `agent.registration-secret` in `application.properties` (dev value: `demo_secret`).
- `MonitorX_Metrics.py` constant: `AGENT_REGISTRATION_SECRET = "demo_secret"` — keep in sync with `application.properties`.
- Registered agents get `ROLE_AGENT`; only that role may `POST /api/metrics`.

**Public endpoints:** `/api/auth/**`, `/ws/**`, `OPTIONS /**`.
**All others** require a valid JWT.

### WebSocket

- STOMP endpoint: `ws://localhost:8080/ws`
- `/topic/metrics` — broadcasts `MetricResponseDTO` after each ingestion
- `/topic/alerts` — broadcasts `AlertResponseDTO` on state change (ONGOING / RESOLVED)

---

## Frontend — `MonitorX_Frontend/`

**Stack:** Angular 21.2 · TypeScript 5.9 · @stomp/stompjs 7.3 · RxJS 7.8 · Vitest · Prettier

```
src/app/
├── components/
│   ├── dashboard/         dashboard.ts/.html/.css — all-machines overview + alert toasts
│   └── machine-detail/    machine-detail.ts/.html/.css — per-machine drilldown + rule CRUD
├── services/
│   ├── machine.service.ts     GET /api/machines, PATCH displayName
│   ├── metric.service.ts      GET /api/metrics/{id}, /history
│   ├── alert.service.ts       GET/POST/PATCH/DELETE alert-rules, GET alerts
│   └── websocket.service.ts   STOMP client; exposes metric$ and alert$ Observables
├── models/
│   ├── machine-response.model.ts
│   ├── metric-response.model.ts
│   ├── alert.model.ts          (AlertResponse, AlertRuleResponse, AlertRuleRequest)
│   └── machine-status.type.ts
├── app.routes.ts    /  → Dashboard,  /machine/:machineId → MachineDetail
└── app.config.ts
```

---

## Python Agent — `MonitorX_Metrics.py`

Collects CPU, RAM, disk, uptime, top-5 processes, all disk partitions every 15 s and POSTs to `http://localhost:8080/api/metrics`. Auto-detects hostname and OS.

On first run, self-registers via `POST /api/auth/register-agent` using `AGENT_REGISTRATION_SECRET`. Logs in to obtain a JWT, then attaches it as `Authorization: Bearer <token>` on every metrics POST. Key constants at the top of the file:

```python
AGENT_REGISTRATION_SECRET = "demo_secret"   # must match agent.registration-secret in application.properties
AGENT_USERNAME = platform.node()             # hostname as username
AGENT_PASSWORD = "agent-secret-change-me"   # per-agent password
```

---

## Infrastructure — Docker / PostgreSQL

```yaml
services:
  postgres:
    image: postgres:16
    container_name: pc-monitor-db
    environment:
      POSTGRES_DB: pc_monitor
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: admin
    ports:
      - "5432:5432"
    volumes:
      - pc_monitor_data:/var/lib/postgresql/data
volumes:
  pc_monitor_data:
```

Start before running the backend:

```bash
cd monitor_pc
docker-compose up -d
```

Connection string (application.properties): `jdbc:postgresql://localhost:5432/pc_monitor`

---

## Building & Running

### Backend

```bash
cd monitor_pc
docker-compose up -d          # Postgres first
./mvnw spring-boot:run        # dev server — port 8080
./mvnw clean package          # build jar
./mvnw test                   # unit + integration tests
```

### Frontend

```bash
cd MonitorX_Frontend
npm install                   # first time / after dependency changes
ng serve                      # dev — port 4200
ng build                      # production build
npm test                      # vitest
npx prettier --check "src/**/*.ts"  # format check
```

### Agent

```bash
pip install psutil requests
python MonitorX_Metrics.py
```

---

## Code Conventions

### Backend

- Services: `@Service @RequiredArgsConstructor` — inject via final fields only, no explicit constructors.
- All mutating methods: `@Transactional`.
- Entities: `@Builder` + Lombok all-args. **Never expose entities in API responses** — always map via MapStruct.
- DTOs: separate `*RequestDTO` (input) and `*ResponseDTO` (output) per resource.
- Errors: throw `ResourceNotFound` for missing entities; `GlobalExceptionHandler` handles globally.
- `machineId` = hostname string — **never use DB numeric PK as domain identifier**.
- Custom queries: `@Query` + `@Param` in repository interfaces.

### Frontend

- Standalone components only — no NgModules. Add framework imports directly in the component `imports` array.
- Always call `cdr.detectChanges()` after every async callback — do not rely on zone.js.
- Services: `@Injectable({ providedIn: 'root' })`.
- Severity order: CRITICAL=0 > HIGH=1 > MEDIUM=2 > LOW=3.
- CSS threshold classes: `danger` (≥85%), `warn` (≥60%), `ok`.
- Severity CSS classes: `sev-critical`, `sev-high`, `sev-medium`, `sev-low`.
- No Angular Signals — use RxJS Subjects/Observables throughout.

---

## Security Best Practices

### Credentials & Secrets

- **Never commit real credentials** to source control. The current `docker-compose.yml` and `application.properties` use `admin/admin` — acceptable for local dev only; replace with environment-variable injection before any deployment.
- Store secrets in environment variables or a secrets manager (Vault, AWS Secrets Manager, etc.); never hardcode in application files.
- **Never expose API keys, tokens, or secrets in frontend code** — Angular bundles are fully readable by the browser. All authenticated calls go through the backend.

### API & Backend

- Validate all input at the API boundary (use `@Valid` + Bean Validation); never trust agent-supplied data blindly.
- CORS: whitelist specific origins (`allowedOrigins`) — never use `*` in production.
- Rate-limit the `POST /api/metrics` endpoint to prevent agent-spoofed floods.
- JWT/token expiry must be enforced; reject expired tokens server-side.
- Use parameterized queries only (Spring Data JPA / `@Query` with `@Param`) — never string-concatenate SQL.
- Do not log sensitive fields (passwords, tokens, PII) at any log level.
- Set `spring.jpa.show-sql=false` and `format_sql=false` in any non-dev profile.

### Database

- Use Row Level Security (RLS) in PostgreSQL for any multi-tenant extension of this system — each tenant's rows must be invisible to others at the DB layer, not only at the application layer.
- Grant the application DB user only the minimum required privileges (SELECT/INSERT/UPDATE on needed tables; no SUPERUSER, no DDL rights in prod).
- Use a dedicated migration tool (Flyway or Liquibase) instead of `ddl-auto=update` in non-dev environments — `update` can silently drop columns.

### Frontend

- Never store JWTs or session tokens in `localStorage`; prefer `httpOnly` cookies managed by the backend.
- Sanitize all user-supplied strings before rendering (Angular's DomSanitizer for dynamic HTML).
- Apply `Content-Security-Policy` headers on the server that serves the Angular build.
- HTTPS only in any environment beyond localhost.

---

## Tests

**Always write tests alongside new code.**

- **Backend:** JUnit in `src/test/java/`. Test services with mocked repositories; test controllers with MockMvc.
- **Frontend:** Vitest in `MonitorX_Frontend/`. Test services with HTTP mocks; test components for key rendered output.

### Task Completion Gate

Run before marking any task done:

```bash
# Backend
cd monitor_pc && ./mvnw clean package -q

# Frontend
cd MonitorX_Frontend && ng build && npm test
```

---

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `Invalid registration secret` (agent register) | `AGENT_REGISTRATION_SECRET` in `MonitorX_Metrics.py` doesn't match `agent.registration-secret` in `application.properties` | Sync both to the same value |
| `String cannot be resolved to a type` at startup | Stale/corrupt `.class` files in `target/` | `./mvnw clean compile` or IntelliJ **Build → Rebuild Project** |

---

## AI Tool Instructions

### Serena MCP

Use Serena for all code navigation and editing:

- **Before reading any file:** call `get_symbols_overview` to understand its structure.
- **Finding symbols:** use `find_symbol`; restrict with `relative_path` to avoid noise.
- **Editing:** use `replace_symbol_body` for full rewrites; `replace_content` for inline fixes.
- **Impact analysis:** call `find_referencing_symbols` before renaming or changing signatures.
- Paths are relative to repo root (`MonitoriX/`) — always prefix with `monitor_pc/` or `MonitorX_Frontend/`.
- Do **not** use `Read`/`Edit` on code files — Serena tools are more precise and token-efficient.

### Context7 MCP

Use Context7 before writing any code that touches a library or framework:

- Covers: Spring Boot, Spring Data JPA, Spring Security, MapStruct, Angular, RxJS, @stomp/stompjs, Vitest.
- Prefer Context7 over training-data recall for API signatures, config options, and version-specific migration notes.
- Call it even for "well-known" APIs — training data lags behind releases.
