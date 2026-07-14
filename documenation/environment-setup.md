# Environment Setup

---

## Summary

Environment variables for **milly-back**. Copy [`.env.example`](../.env.example) to `.env` for local development and Docker Compose. Spring maps these into `application*.properties`. Profiles: `dev` (default) and `prod`.

Install and run steps: [installation.md](./installation.md). Day-to-day development: [development-instructions.md](./development-instructions.md).

---

## Table of contents

1. [How config is loaded](#how-config-is-loaded)
2. [Core](#core)
3. [Database](#database)
4. [Auth and sessions](#auth-and-sessions)
5. [Client / CORS](#client--cors)
6. [Storage (S3)](#storage-s3)
7. [AI](#ai)
8. [Other TTLs](#other-ttls)
9. [Profile differences](#profile-differences)

---

## How config is loaded

| Source | When |
|--------|------|
| `application.properties` | Always — defaults and `${ENV:fallback}` bindings |
| `application-dev.properties` | `SPRING_PROFILES_ACTIVE=dev` |
| `application-prod.properties` | `SPRING_PROFILES_ACTIVE=prod` |
| Process env / `.env` | Docker Compose `env_file: .env`; local exports |

`.env` is gitignored. Prefer editing `.env` from `.env.example` rather than committing secrets.

Docker Compose also **overrides** `DATABASE_URL` to `jdbc:postgresql://db:5432/milly` so the app container reaches the `db` service (hostname `db`, not `localhost`).

---

## Core

| Variable | Required | Default | Purpose |
|----------|----------|---------|---------|
| `SPRING_PROFILES_ACTIVE` | No | `dev` | Active Spring profile |
| `SERVER_PORT` | No | `8080` | HTTP port (Compose maps host port from this) |

---

## Database

| Variable | Required | Default (dev) | Purpose |
|----------|----------|---------------|---------|
| `DATABASE_URL` | Prod: yes | `jdbc:postgresql://localhost:5432/milly` | JDBC URL |
| `DATABASE_USERNAME` | Prod: yes | `milly` | DB user |
| `DATABASE_PASSWORD` | Prod: yes | `milly` | DB password |

Flyway runs on startup (`classpath:db/migration`). JPA `ddl-auto` is `validate` in both profiles.

Compose Postgres uses the same username/password and database name `milly`.

---

## Auth and sessions

| Variable | Required | Default | Purpose |
|----------|----------|---------|---------|
| `JWT_SECRET` | Prod: yes | Dev default in `application.properties` | HMAC key for JWT signing (use a long random secret) |
| `JWT_ACCESS_TTL_SECONDS` | No | `900` | Access token lifetime |
| `JWT_REFRESH_TTL_SECONDS` | No | `1209600` | Refresh token lifetime (14 days) |
| `AUTH_COOKIES_SECURE` | No | `false` (forced `true` in prod profile) | `Secure` flag on auth cookies |
| `GOOGLE_CLIENT_ID` | For Google sign-in | empty | Google ID token audience |
| `GOOGLE_ISSUER` | No | `https://accounts.google.com` | Expected Google issuer |
| `APPLE_CLIENT_ID` | For Apple sign-in | empty | Apple identity token audience |
| `WS_TICKET_TTL_SECONDS` | No | `30` | Staff WebSocket ticket TTL |

Auth behaviour: [security/token-and-session-management.md](./security/token-and-session-management.md).

---

## Client / CORS

| Variable | Required | Default | Purpose |
|----------|----------|---------|---------|
| `CLIENT_URL` | No | `http://localhost:3000` | Frontend origin for CORS / WebSocket allowed origin |

Point this at the client app origin you use in the browser (scheme + host + port).

---

## Storage (S3)

Used when features need blob storage (e.g. assets). Leave empty if unused locally.

| Variable | Required | Default | Purpose |
|----------|----------|---------|---------|
| `AWS_ACCESS_KEY_ID` | When using S3 | empty | Access key |
| `AWS_SECRET_ACCESS_KEY` | When using S3 | empty | Secret key |
| `S3_BUCKET` | When using S3 | empty | Bucket name |
| `S3_REGION` | No | `eu-west-1` | AWS region |
| `S3_PUBLIC_BASE_URL` | No | empty | CDN / custom public base; if empty, standard S3 URL form is used |

---

## AI

Disabled by default. Details: [ai/ai-integration.md](./ai/ai-integration.md).

| Variable | Required | Default | Purpose |
|----------|----------|---------|---------|
| `AI_ENABLED` | No | `false` | Enable LangChain4j + OpenRouter adapters |
| `OPENROUTER_API_KEY` | When `AI_ENABLED=true` | empty | OpenRouter API key (startup fails if missing while enabled) |
| `AI_MODEL` | No | `google/gemini-2.0-flash-001` | OpenRouter model id |
| `OPENROUTER_BASE_URL` | No | `https://openrouter.ai/api/v1` | OpenAI-compatible base URL |
| `AI_MAX_TOKENS` | No | `1024` | Max completion tokens |
| `AI_LOG_REQUESTS` | No | `false` (`true` in `dev` profile props) | Log provider requests |
| `AI_LOG_RESPONSES` | No | `false` (`true` in `dev` profile props) | Log provider responses |
| `OPENROUTER_HTTP_REFERER` | No | `CLIENT_URL` | OpenRouter `HTTP-Referer` header |
| `OPENROUTER_APP_TITLE` | No | `Milly` | OpenRouter `X-Title` header |
| `AI_CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD` | No | `50` | Failure rate % to open breaker |
| `AI_CIRCUIT_BREAKER_WAIT_DURATION_SECONDS` | No | `30` | Open-state wait |
| `AI_CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE` | No | `10` | Sliding window size |
| `AI_CIRCUIT_BREAKER_MINIMUM_NUMBER_OF_CALLS` | No | `3` | Min calls before evaluating |

---

## Other TTLs

| Variable | Required | Default | Purpose |
|----------|----------|---------|---------|
| `IDEMPOTENCY_TTL_SECONDS` | No | `86400` | How long an idempotent response is replayable |
| `VENUE_INVITATION_TTL_SECONDS` | No | `86400` | Venue invitation lifetime in cache |

---

## Profile differences

| Topic | `dev` | `prod` |
|-------|-------|--------|
| Datasource | Optional defaults for local Postgres | `DATABASE_*` required (no defaults) |
| `JWT_SECRET` | Fallback allowed | Must be set via env |
| Auth cookies `Secure` | From `AUTH_COOKIES_SECURE` (often `false`) | Forced `true` |
| AI request/response logging | Enabled in `application-dev.properties` | Follow `AI_LOG_*` (default off) |

Health check endpoint (Docker / Compose): `GET /actuator/health`.
