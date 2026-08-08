# MonitoriX — Agent Instructions

This file governs how AI agents operate in this repository.
Read it before taking any action.

---

## Orientation

- Full project overview: `CLAUDE.md` (same directory as this file).
- Serena memories: start from `mem:core`, follow references to backend/frontend/conventions memories.
- Repo root: `MonitoriX/`. Backend at `monitor_pc/`, frontend at `MonitorX_Frontend/`.

---

## Required Tool Usage

### Serena MCP — always use for code work

| Action | Tool |
|--------|------|
| Understand a file | `get_symbols_overview` first |
| Find a class / method | `find_symbol` |
| Read a method body | `find_symbol` with `include_body=true` |
| Rewrite a method/class | `replace_symbol_body` |
| Line-level edit inside a method | `replace_content` |
| Check callers before changing a signature | `find_referencing_symbols` |

Do **not** use Read/Edit/Grep on code files when Serena tools can do the job.

Paths are always relative to repo root. Prefix backend files with `monitor_pc/src/...` and frontend files with `MonitorX_Frontend/src/...`.

### Context7 MCP — always use before writing library code

Fetch current docs for any library before implementing against it:
```
Spring Boot · Spring Data JPA · Spring WebSocket / STOMP
MapStruct · Lombok
Angular · RxJS · @stomp/stompjs · Vitest · Prettier
```
Do not rely on training-data recall for API signatures or config keys — fetch docs first.

---

## Coding Rules

### Backend (`monitor_pc/`)

- All services: `@Service @RequiredArgsConstructor`. Dependencies injected via `final` fields — no `@Autowired`, no manual constructors.
- Mutating methods: always `@Transactional`.
- Entities: `@Builder`. Never return entities from controllers — map via MapStruct only.
- Missing resources: throw `ResourceNotFound`. Do not catch it — `GlobalExceptionHandler` handles it.
- `machineId` = hostname string (domain key). Never use DB numeric PK as a business identifier.
- Custom queries: `@Query` + `@Param` in the repository interface — not in the service.

### Frontend (`MonitorX_Frontend/`)

- Standalone components only. Declare `imports` on each component, not in a module.
- Call `cdr.detectChanges()` after every async update. Do not rely on automatic zone.js tick.
- Services: `@Injectable({ providedIn: 'root' })`.
- No Angular Signals — use RxJS `Subject`/`Observable` for reactivity.
- Model files: `*.model.ts` for objects, `*.type.ts` for string unions.

---

## Tests — Mandatory

**Every new feature and bugfix must ship with a test.** Do not mark a task complete without one.

### Backend tests
- Location: `monitor_pc/src/test/java/`
- Pattern: JUnit 5, mock repositories for service tests, MockMvc for controller tests.
- Run: `./mvnw test`

### Frontend tests
- Location: alongside source or in a `__tests__` folder.
- Pattern: Vitest; mock HTTP calls for service tests; component tests for key output.
- Run: `npm test` from `MonitorX_Frontend/`

---

## Task Completion — Always Run Before Finishing

```bash
# Backend — must pass
cd monitor_pc && ./mvnw clean package -q

# Frontend — must pass
cd MonitorX_Frontend && ng build && npm test
```

---

## Workflow

1. Read `mem:core` via Serena; follow references to relevant module memory.
2. Use `get_symbols_overview` to map affected files before touching code.
3. Fetch Context7 docs for any library you will write against.
4. Implement — use Serena editing tools for all code changes.
5. Write tests.
6. Run completion checklist above.
7. Commit with a clear message (feat / fix / refactor / test prefix).

---

## What Not To Do

- Do not expose JPA entities in REST responses.
- Do not add fields to entities without a DB migration or schema update in mind.
- Do not bypass `ChangeDetectorRef.detectChanges()` in Angular components.
- Do not delete or modify Serena memory files manually — use `mcp__serena__write_memory` / `mcp__serena__edit_memory`.
- Do not use `git push --force` on `master`.
