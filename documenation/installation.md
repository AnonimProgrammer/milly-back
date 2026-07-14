# Installation

---

## Summary

How to run **milly-back** locally: prerequisites, environment file, Docker Compose (app + Postgres), or Gradle against a local database. Env variable reference: [environment-setup.md](./environment-setup.md). Day-to-day development: [development-instructions.md](./development-instructions.md).

---

## Table of contents

1. [Prerequisites](#prerequisites)
2. [Environment file](#environment-file)
3. [Docker Compose (recommended)](#docker-compose-recommended)
4. [Local Gradle + Postgres](#local-gradle--postgres)
5. [Verify](#verify)
6. [Useful commands](#useful-commands)
7. [Notes](#notes)

---

## Prerequisites

| Tool | Version / notes |
|------|-----------------|
| **Docker** + **Docker Compose** | For the compose stack |
| **JDK 21** | Only if running with Gradle (Temurin recommended) |
| **Git** | Clone the repo |

The Docker image builds with `eclipse-temurin:21` and runs a JRE Alpine image. You do not need a local JDK when using Compose only.

---

## Environment file

From the `milly-back` project root:

```bash
cp .env.example .env
```

Edit `.env` as needed (database passwords, `CLIENT_URL`, OAuth client IDs, `AI_ENABLED` / `OPENROUTER_API_KEY`, etc.). See [environment-setup.md](./environment-setup.md).

Do not commit `.env`.

---

## Docker Compose (recommended)

Compose starts:

- **`db`** — Postgres 16 (`milly` / credentials from `.env`)
- **`app`** — Spring Boot API built from the local `Dockerfile`

```bash
docker compose up --build
```

What Compose sets for you:

| Item | Behaviour |
|------|-----------|
| Profile | `SPRING_PROFILES_ACTIVE=dev` |
| JDBC URL | Forced to `jdbc:postgresql://db:5432/milly` (overrides localhost in `.env`) |
| App port | Host `${SERVER_PORT:-8080}` → container `8080` |
| DB port | Host `5432` → container `5432` |
| Volume | `milly-db-data` persists Postgres data |
| Health | App waits until Postgres `pg_isready`; image healthchecks `/actuator/health` |

Stop / rebuild:

```bash
docker compose down
docker compose up --build -d
```

Remove the database volume (destructive):

```bash
docker compose down -v
```

---

## Local Gradle + Postgres

Use this when developing against an IDE or host JVM while Postgres runs locally (or only the Compose `db` service).

1. Start Postgres with matching credentials (`DATABASE_*` in `.env` / defaults `milly`/`milly`/`milly` DB).
2. Ensure `DATABASE_URL` points at the host (e.g. `jdbc:postgresql://localhost:5432/milly`).
3. From `milly-back`:

```bash
./gradlew bootRun
```

On Windows:

```bat
gradlew.bat bootRun
```

Flyway applies migrations on startup. Optional: run only the DB via Compose and the app via Gradle:

```bash
docker compose up db -d
./gradlew bootRun
```

---

## Verify

| Check | Expect |
|-------|--------|
| `GET http://localhost:8080/actuator/health` | Healthy / UP |
| Logs | Flyway migrations applied; app listening on `8080` |
| Client app | Set `CLIENT_URL` to your client origin (default `http://localhost:3000`) |

WebSocket endpoint: `ws://localhost:8080/ws` (see [web-socket-flow.md](./web-socket-flow.md)).

---

## Useful commands

```bash
# Build jar (same as Docker build stage, without packaging image)
./gradlew bootJar -x test

# Unit tests
./gradlew test

# Follow Compose logs
docker compose logs -f app
```

Dockerfile summary: multi-stage `bootJar` with JDK 21 → JRE 21 runtime, non-root user, `JAVA_OPTS` with container RAM percentage.

---

## Notes

- Default Spring profile is **`dev`**. Production needs `SPRING_PROFILES_ACTIVE=prod` and required secrets (`DATABASE_*`, `JWT_SECRET`); auth cookies are Secure-only in prod.
- AI is **off** until `AI_ENABLED=true` and `OPENROUTER_API_KEY` are set ([ai/ai-integration.md](./ai/ai-integration.md)).
- Actuator exposes `health` and `info` only (as configured).
