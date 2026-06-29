# milly-back project context

Spring Boot 4 / Java 21 backend for Milly — a multi-venue restaurant ordering platform.

## Architecture

- Clean Architecture with one module per bounded context under `com.milly`
- Modules: `auth`, `venue`, `table`, `menu`, `order`, `billing`, `common`, `config`
- PostgreSQL + Flyway migrations in `src/main/resources/db/migration/`
- REST (`/api/v1`) is source of truth; STOMP/WebSocket for push notifications

## Documentation

- `documenation/system-design.md` — modules, routes, persistence
- `documenation/security-flow.md` — auth, venue roles, JWT cookies
- `documenation/web-socket-flow.md` — WS tickets, STOMP topics

## Conventions

See `git-conventions.md` in this folder for branch and commit message rules.
