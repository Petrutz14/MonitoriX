# MonitoriX — Claude Code Guide

## Project Purpose

Real-time PC monitoring system. A lightweight Python agent runs on each machine, collecting CPU/RAM/disk metrics every 15 seconds and POSTing them to a Spring Boot backend. The backend stores metrics, evaluates alert rules, and broadcasts live updates via WebSocket. An Angular frontend displays a live dashboard with alert toasts and per-machine drilldown.

---

## Repository Layout

```
MonitoriX/
├── monitor_pc/           # Spring Boot backend (Java 25, Maven)
├── MonitorX_Frontend/    # Angular 21 frontend (TypeScript 5.9)
└── MonitorX_Metrics.py   # Python 3 metrics agent
```

---

## Backend — `monitor_pc/`

**Stack:** Spring Boot 4.0.5 · Java 25 · PostgreSQL 16 (Docker) · Lombok · MapStruct 1.6.3 · STOMP WebSocket

**Package root:** `com.monitorpc.monitor_pc`

```
src/main/java/com/monitorpc/monitor_pc/
├── controller/     MachineController, MetricController, AlertController
├── service/        MachineService, MetricIngestionService, AlertEvaluationService,
│                   AlertRuleService, MachineHealthService
├── model/          Machine, SystemMetric, TopProcess, DiskPartition, AlertRule, Alert
├── repository/     one JpaRepository per entity
├── dto/            *RequestDTO (in) / *ResponseDTO (out) / AgentPayloadDTO
├── mapper/         MachineMapper, MetricMapper, AlertMapper  (MapStruct)
├── enums/          MachineStatus, MetricType, AlertOperator, AlertSeverity, AlertStatus
├── websocket/      WebSocketConfig
└── exception/      ResourceNotFound, GlobalExceptionHandler
```

**Key flows:**

1. Agent POSTs → `MetricIngestionService.ingest()`: upsert Machine → save SystemMetric + children → evaluate alerts synchronously → broadcast `/topic/metrics`.
2. Alert evaluation: enabled rules where `machine_id IS NULL` (global) or matches → ONGOING/RESOLVED transitions → broadcast `/topic/alerts`.
3. `MachineHealthService` runs `@Scheduled(fixedRate=30000)` and marks machines OFFLINE after 30 s of silence.

**REST API:**

| Method | Path | Description |
|--------|------|-------------|
| GET/PATCH | `/api/machines`, `/api/machines/{id}` | list / update display name |
| POST/GET | `/api/metrics`, `/api/metrics/{id}`, `/api/metrics/{id}/history` | ingest / query |
| CRUD | `/api/alert-rules` | manage rules |
| GET | `/api/alerts/{id}`, `/api/alerts/{id}/active` | query alerts |

**WebSocket:** `ws://localhost:8080/ws` → `/topic/metrics`, `/topic/alerts`

---

## Frontend — `MonitorX_Frontend/`

**Stack:** Angular 21.2 · TypeScript 5.9 · @stomp/stompjs 7.3 · RxJS 7.8 · Vitest · Prettier

```
src/app/
├── components/dashboard/        all-machines overview + alert toasts
├── components/machine-detail/   per-machine drilldown + rule management
├── services/                    machine / metric / alert / websocket services
└── models/                      *-response.model.ts, alert.model.ts
```

Routes: `/` → Dashboard, `/machine/:machineId` → MachineDetail

**Key patterns:**
- Standalone components only — no NgModules.
- `WebSocketService` singleton with ref-counted `connect()`/`disconnect()`.
- `ChangeDetectorRef.detectChanges()` called manually after every async update.
- No Angular Signals — RxJS Subjects/Observables throughout.

---

## Python Agent — `MonitorX_Metrics.py`

Collects CPU, RAM, disk, uptime, top-5 processes, all disk partitions every 15 s and POSTs to `http://localhost:8080/api/metrics`. Auto-detects IP and OS.

---

## Building & Running

### 1. Start PostgreSQL
```bash
cd monitor_pc
docker-compose up -d
```

### 2. Backend
```bash
cd monitor_pc
./mvnw spring-boot:run        # dev — port 8080
./mvnw clean package          # build jar
./mvnw test                   # unit + integration tests
```

### 3. Frontend
```bash
cd MonitorX_Frontend
npm install
ng serve                      # dev — port 4200
ng build                      # production build
npm test                      # vitest
```

### 4. Agent
```bash
pip install psutil requests
python MonitorX_Metrics.py
```

---

## Tests

**Always write tests alongside new code.** Both modules have test infrastructure ready.

- **Backend:** JUnit tests in `src/test/java/`. Use `./mvnw test` to run. Test services with mocked repositories; test controllers with MockMvc.
- **Frontend:** Vitest in `MonitorX_Frontend/`. Run with `npm test`. Test services with HTTP mocks; test components for key rendered output.

Do not ship a feature or bugfix without a corresponding test.

---

## Code Conventions

### Backend
- `@Service @RequiredArgsConstructor` on all services — constructor injection via final fields.
- `@Transactional` on all mutating methods.
- Entities use `@Builder`; never expose entities directly in API — always map via MapStruct.
- Throw `ResourceNotFound` for missing entities; `GlobalExceptionHandler` handles it.
- `machineId` (hostname string) is the domain key — never use DB numeric PK as domain identifier.

### Frontend
- Standalone components — add imports directly in the component `imports` array.
- Always call `cdr.detectChanges()` after async updates.
- Severity order: CRITICAL > HIGH > MEDIUM > LOW.
- CSS classes: `sev-critical / sev-high / sev-medium / sev-low`, `danger / warn / ok` for metric thresholds.

---

## Task Completion Checklist

Before marking any task done:

```bash
# Backend
cd monitor_pc && ./mvnw clean package -q

# Frontend
cd MonitorX_Frontend && ng build && npm test
```

---

## AI Tool Instructions

### Serena MCP
Use Serena for all code navigation and editing in this repo:
- **Discovery:** `get_symbols_overview` before reading files; `find_symbol` to locate specific classes/methods.
- **Editing:** `replace_symbol_body` for full symbol rewrites; `replace_content` for inline line-level fixes.
- **References:** `find_referencing_symbols` before renaming or changing signatures.
- Paths are relative to repo root (`MonitoriX/`), so always prefix with `monitor_pc/` or `MonitorX_Frontend/`.

### Context7 MCP
Use Context7 to fetch up-to-date docs before writing code that touches a library or framework:
- Spring Boot, Spring Data JPA, MapStruct, Angular, RxJS, @stomp/stompjs, Vitest — all benefit from a Context7 lookup before implementation.
- Prefer Context7 over training-data recall for API signatures, config options, and migration notes.

### Serena Memories
Project memories are written — read `mem:core` as the entry point, then follow references to `mem:backend/core`, `mem:frontend/core`, `mem:conventions`, `mem:tech_stack`, `mem:suggested_commands`, `mem:task_completion`.
