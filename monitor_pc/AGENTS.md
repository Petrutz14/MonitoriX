# MonitoriX — Agent Guide

## Greeting

Always open your response by addressing the user as **Peter**.

---

## Start Here — Every Session

Before writing a single line of code or making any edit, run this sequence:

1. **Load Serena** — call `mcp__serena__initial_instructions` to load the Serena Instructions Manual.
2. **Read memories** — call `mcp__serena__read_memory` for `core`, then follow its links to `backend/core`, `frontend/core`, `conventions`, `tech_stack`, `suggested_commands`, `task_completion` as relevant to the task.
3. **Survey scope** — call `mcp__serena__get_symbols_overview` on the directory or file you are about to touch before reading any body.
4. **Check docs** — if the task involves a framework or library, call `mcp__plugin_context7_context7__resolve-library-id` then `mcp__plugin_context7_context7__query-docs` before writing code.

---

## Code Navigation — Serena MCP

Use Serena for all navigation and editing in this repo. Do not use `Read`/`Edit`/`Grep` on code files when a Serena tool covers the same need.

| Need | Serena tool |
|------|-------------|
| Understand a file's structure | `get_symbols_overview` |
| Find a class / method by name | `find_symbol` |
| Read a method's body | `find_symbol` with `include_body=true` |
| Rewrite a whole symbol | `replace_symbol_body` |
| Fix a few lines inside a symbol | `replace_content` |
| Check what calls a symbol | `find_referencing_symbols` |
| Insert code at top/bottom of file | `insert_before_symbol` / `insert_after_symbol` |

**Path convention:** all paths are relative to repo root (`MonitoriX/`). Always prefix with `monitor_pc/` or `MonitorX_Frontend/` when calling Serena tools.

---

## Dependency / Framework Docs — Context7 MCP

Before writing any code that touches a library or framework, fetch current docs:

```
resolve-library-id  →  query-docs
```

Covers: Spring Boot, Spring Data JPA, Spring Security, MapStruct, Angular, RxJS, @stomp/stompjs, Vitest, Lombok.

Do not rely on training-data recall for API signatures — library APIs change across versions and training data lags.

---

## Project Snapshot

Full detail lives in Serena memories. Quick reference:

**Backend** (`monitor_pc/` — Spring Boot 4 / Java 25 / PostgreSQL 16)
- Package root: `com.monitorpc.monitor_pc`
- Layers: `controller → service → repository → model`; `dto` and `mapper` (MapStruct) at every API boundary.
- Key flow: agent `POST /api/metrics` → `MetricIngestionService.ingest()` → save rows → evaluate alerts → broadcast to `/topic/metrics` and `/topic/alerts`.
- `machineId` = hostname string. Never use DB numeric PK as domain identifier.
- Never expose JPA entities directly — always map via MapStruct.
- Auth: `config/SecurityConfig` — RSA JWT, stateless. `AuthService` handles register/login/registerAgent. `ROLE_AGENT` required for metrics ingest.

**Frontend** (`MonitorX_Frontend/` — Angular 21 / TypeScript 5.9)
- Standalone components only; no NgModules.
- `WebSocketService` singleton — STOMP over `ws://localhost:8080/ws`; exposes `metric$` and `alert$` Observables.
- Always call `cdr.detectChanges()` after async updates.
- Severity order: CRITICAL > HIGH > MEDIUM > LOW.
- Auth: basic sign-up/login UI in progress (WIP as of 2026-08-10).

**Agent** (`MonitorX_Metrics.py`) — self-registers via `POST /api/auth/register-agent`, logs in to get JWT, then POSTs metrics every 15 s with `Authorization: Bearer <token>`.

---

## Infrastructure

PostgreSQL runs in Docker. Must be running before the backend starts:

```bash
cd monitor_pc && docker-compose up -d
```

Container: `pc-monitor-db`, DB: `pc_monitor`, port `5432`. Credentials in `application.properties` are dev-only (`admin/admin`) — replace with env vars before any deployment.

---

## REST API Surface

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | No | Register human user |
| POST | `/api/auth/login` | No | Login → JWT |
| POST | `/api/auth/register-agent` | `X-Agent-Secret` header | Agent self-registration |
| GET | `/api/machines` | JWT | List all machines |
| PATCH | `/api/machines/{id}` | JWT | Update display name |
| POST | `/api/metrics` | JWT (`ROLE_AGENT`) | Ingest agent payload |
| GET | `/api/metrics/{id}` | JWT | Latest metric for a machine |
| GET | `/api/metrics/{id}/history` | JWT | Historical metrics |
| GET/POST/DELETE | `/api/alert-rules` | JWT | Manage alert rules |
| PATCH | `/api/alert-rules/{id}/toggle` | JWT | Enable / disable rule |
| GET | `/api/alerts/{id}/active` | JWT | Active alerts for a machine |

**Public:** `/api/auth/**`, `/ws/**`, `OPTIONS /**`. Everything else requires a valid Bearer JWT.

**Agent self-registration secret:** `X-Agent-Secret` header value must match `agent.registration-secret` in `application.properties`. Dev value: `demo_secret`. Keep `AGENT_REGISTRATION_SECRET` in `MonitorX_Metrics.py` in sync.

---

## WebSocket

- Endpoint: `ws://localhost:8080/ws` (STOMP)
- `/topic/metrics` — `MetricResponseDTO` broadcast after each ingestion
- `/topic/alerts` — `AlertResponseDTO` broadcast on ONGOING / RESOLVED transitions

---

## Build & Test Commands

```bash
# Backend — run after every backend change
cd monitor_pc
docker-compose up -d          # Postgres must be running
./mvnw spring-boot:run        # dev server, port 8080
./mvnw clean package -q       # compile + tests (gate before marking done)

# Frontend — run after every frontend change
cd MonitorX_Frontend
npm install                   # first time or after dep changes
ng serve                      # dev server, port 4200
ng build && npm test          # gate before marking done
npx prettier --check "src/**/*.ts"
```

---

## Task Completion Checklist

Before marking any task complete:

- [ ] Backend changed → `./mvnw clean package -q` passes (zero errors, zero test failures)
- [ ] Frontend changed → `ng build && npm test` passes
- [ ] New feature or bugfix has a corresponding test (backend: JUnit + MockMvc; frontend: Vitest)
- [ ] No secrets, API keys, or hardcoded credentials added to any file
- [ ] Entities are never returned directly from controllers — MapStruct mapper used
- [ ] `cdr.detectChanges()` called after every async update in Angular components

---

## Security Checklist

Apply to every change:

- **No secrets in code.** Credentials, API keys, and tokens go in environment variables or a secrets manager — never in source files or the Angular bundle.
- **Input validation.** Validate at the API boundary with `@Valid` + Bean Validation. Never trust agent-supplied data.
- **Parameterized queries only.** Use Spring Data JPA / `@Query` with `@Param`. No string-concatenated SQL ever.
- **CORS.** Whitelist specific origins in production — never `allowedOrigins("*")`.
- **No sensitive logging.** Never log passwords, tokens, or PII at any level.
- **ddl-auto.** Use `validate` or a migration tool (Flyway/Liquibase) in non-dev environments — not `update`.
- **RLS.** If the system expands to multiple tenants, enforce Row Level Security in PostgreSQL — application-layer filtering alone is insufficient.
- **Minimal DB privileges.** Application DB user gets only SELECT/INSERT/UPDATE on its tables; no DDL rights in production.
- **HTTPS only** beyond localhost.
- **No tokens in localStorage.** Prefer `httpOnly` cookies for any session tokens.
- **Angular DomSanitizer.** Sanitize any user-supplied strings rendered as dynamic HTML.
