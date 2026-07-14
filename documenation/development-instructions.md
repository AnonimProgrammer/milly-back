# Development Instructions

---

## Summary

Day-to-day development guide for **milly-back**: run loop, package layout, adding features, migrations, tests, and git conventions. First-time setup: [installation.md](./installation.md). Env vars: [environment-setup.md](./environment-setup.md). Product/module overview: [system-design.md](./system-design.md).

---

## Table of contents

1. [Prerequisites](#prerequisites)
2. [Daily run loop](#daily-run-loop)
3. [Project layout](#project-layout)
4. [Adding a feature](#adding-a-feature)
5. [Database migrations](#database-migrations)
6. [Testing](#testing)
7. [Git conventions](#git-conventions)
8. [Useful commands](#useful-commands)
9. [Where to look next](#where-to-look-next)

---

## Prerequisites

| Tool | Notes |
|------|-------|
| JDK **21** | Toolchain in `build.gradle` |
| Docker | Recommended for Postgres (`docker compose up db`) |
| IDE | IntelliJ / Cursor with Lombok support |

Copy `.env.example` → `.env` before `bootRun` (Gradle loads `.env` into the process environment).

---

## Daily run loop

Typical local stack:

```bash
# Terminal 1 — Postgres only
docker compose up db -d

# Terminal 2 — API with hot reload (devtools on classpath)
./gradlew bootRun
```

On Windows: `gradlew.bat bootRun`.

Full stack in Docker: see [installation.md](./installation.md).

| Endpoint | Use |
|----------|-----|
| `http://localhost:8080` | REST `/api/v1/...` |
| `ws://localhost:8080/ws` | STOMP |
| `GET /actuator/health` | Liveness |

Point `CLIENT_URL` / CORS origin at the client you use (default `http://localhost:3000`).

---

## Project layout

Root package: `com.milly`. One **singular** module per bounded context.

```
com.milly/
├── config/      # Security, WebSocket, Jackson, cache, AI client, CORS
├── common/      # Shared VOs, exceptions, idempotency helpers
├── auth/
├── venue/
├── table/
├── menu/
├── order/
├── billing/
└── chatbot/
```

Feature modules follow a layered layout (omit empty layers):

```
{module}/
├── domain/
│   ├── entity/
│   ├── valueobject/
│   └── event/                 # when needed
├── application/
│   ├── usecase/
│   ├── dto/
│   ├── service/               # when needed
│   └── port/inbound|outbound/ # when ports are explicit
└── infrastructure/
    ├── adapter/inbound/http/  # or websocket/
    ├── adapter/outbound/persistence/ | cache/ | ...
    └── config/                # module-local Spring config
```

**Rules of thumb**

- REST / STOMP adapters stay thin: map request → call use case → map response.
- Venue authorization belongs in use cases via `VenueAuthorizationService`, not only in the controller.
- Cross-cutting HTTP security and `/ws` wiring live in **config**.
- Shared errors and value types live in **common**.
- Prefer calling another module’s use case / port over reaching into its persistence layer.

---

## Adding a feature

1. **Pick the module** (or add a new singular package if it is a new bounded context). Prefer extending an existing module when the concept fits.
2. **Domain** — entity / value object / invariants as needed.
3. **Application** — use case + request/response DTOs; enforce venue role with `requireActiveMember` / `requireAtLeastRole` when the API is staff-scoped.
4. **Inbound adapter** — `*RestAdapter` under `/api/v1/...` or STOMP `@MessageMapping` for chatbot-style flows.
5. **Outbound adapter** — JPA repository, cache store, or AI port implementation in **config** if vendor-specific.
6. **Migration** — if the schema changes, add a Flyway script (below).
7. **Tests** — unit test the use case; add/adjust an itest when the HTTP contract matters.
8. **Docs** — update the relevant flow doc if behaviour is security, WS, billing, or AI related.

Public customer APIs go under `/api/v1/public/...` and must stay table-scoped. Staff APIs typically under `/api/v1/venues/{venueId}/...`. See [security/public-vs-protected-endpoints.md](./security/public-vs-protected-endpoints.md) and [security/venue-authorization-flow.md](./security/venue-authorization-flow.md).

---

## Database migrations

- Location: `src/main/resources/db/migration/`
- Naming: `V{n}__{description}.sql` (Flyway)
- Profile: Flyway enabled; JPA `ddl-auto=validate` — **schema changes require a migration**, not Hibernate auto-update

After adding a migration, restart `bootRun` (or recreate the Compose app). For a dirty local DB, prefer fixing the migration or resetting the Compose volume (`docker compose down -v`) over hacking `ddl-auto`.

---

## Testing

| Suite | Command | What |
|-------|---------|------|
| Unit / slice | `./gradlew test` | `src/test/java` — JUnit 5 |
| Integration | `./gradlew itest` | `src/itest/java` — Testcontainers Postgres |

Integration notes:

- Uses Spring Boot Testcontainers + PostgreSQL.
- Locally, Testcontainers reuse is enabled when `CI` is unset (`TESTCONTAINERS_REUSE_ENABLE=true`).
- Docker must be available for `itest`.
- Default itest timeout budget is generous (task timeout 25 minutes); individual tests default to 2 minutes.

Prefer testing use-case behaviour and authorization decisions in unit tests; use itests for persistence + HTTP wiring.

---

## Git conventions

**Branch**

| Scope | Pattern |
|-------|---------|
| Cross-cutting / docs | `{type}/{feature-title}` |
| One module | `{type}/{module-name}/{feature-title}` |

Allowed types: `feat`, `fix`, `refactor`, `docs`  
Segments: lowercase kebab-case. Module names: `auth`, `venue`, `table`, `menu`, `order`, `billing`, `chatbot`, `config`, `common`.

Examples: `feat/auth/google-oauth2-provider`, `docs/development-instructions`

**Commit**

```
{type}: {Commit title}
```

Same allowed types. Clear, concise title (imperative or sentence case).

Examples: `feat: Google OAuth2 provider integrated`, `docs: Document development workflow`

Also documented in `AGENTS.md` / Copilot instructions for agents.

---

## Useful commands

```bash
./gradlew bootRun          # API (loads .env if present)
./gradlew test             # unit tests
./gradlew itest            # integration tests
./gradlew bootJar -x test  # build jar
docker compose up db -d    # Postgres only
docker compose logs -f app # if running full Compose stack
```

API exploration while the server runs:

| Resource | URL / path |
|----------|------------|
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Bruno collection | `bruno/` at repo root |

See [api-documentation.md](./api-documentation.md) for Bruno setup and cookie-auth notes.

Enable AI locally only when needed (`AI_ENABLED=true` + `OPENROUTER_API_KEY`) — [ai/ai-integration.md](./ai/ai-integration.md).

---

## Where to look next

| Doc | When |
|-----|------|
| [installation.md](./installation.md) | First clone / Docker |
| [environment-setup.md](./environment-setup.md) | Configuring `.env` |
| [api-documentation.md](./api-documentation.md) | Bruno + Swagger |
| [system-design.md](./system-design.md) | Modules and high-level architecture |
| [security/security-flow.md](./security/security-flow.md) | Auth model |
| [web-socket-flow.md](./web-socket-flow.md) | STOMP / tickets |
| [ai/chatbot.md](./ai/chatbot.md) | Table chat development |
| [billing-flow.md](./billing-flow.md) | Payments |
