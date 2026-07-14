# API Documentation

---

## Summary

How to explore and exercise the milly-back REST API (`/api/v1`) with **Swagger / OpenAPI** (live from the running app) and the checked-in **Bruno** collection. System overview: [system-design.md](./system-design.md). Install: [installation.md](./installation.md).

---

## Table of contents

1. [Swagger / OpenAPI](#swagger--openapi)
2. [Bruno](#bruno)
3. [Auth notes](#auth-notes)
4. [Related documentation](#related-documentation)

---

## Swagger / OpenAPI

The app uses **springdoc-openapi** (`springdoc-openapi-starter-webmvc-ui`). Controllers are scanned into an OpenAPI 3 document and served with Swagger UI.

### URLs (local default)

| Resource | URL |
|----------|-----|
| Swagger UI | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |
| OpenAPI JSON | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |

These paths are **public** (see [security/public-vs-protected-endpoints.md](./security/public-vs-protected-endpoints.md)).

### Setup

1. Start the API ([installation.md](./installation.md) or `./gradlew bootRun`).
2. Open Swagger UI in the browser.
3. For protected endpoints, authenticate first (e.g. `POST /api/v1/auth/continue`) so the browser stores `access-token` / `refresh-token` cookies, then call staff APIs from the same origin (`http://localhost:8080`).

OpenAPI metadata and the cookie security scheme are defined in `OpenApiConfig` (`config` module). Optional property knobs:

| Property | Default | Purpose |
|----------|---------|---------|
| `springdoc.api-docs.path` | `/v3/api-docs` | OpenAPI document path |
| `springdoc.swagger-ui.path` | `/swagger-ui.html` | Swagger UI entry |

---

## Bruno

The repo includes a Bruno collection at [`bruno/`](../bruno/) (`bruno.json` name: `milly-back`).

### Setup

1. Install [Bruno](https://www.usebruno.com/).
2. Open / import the `bruno` folder from this repository.
3. Select the **local** environment (`bruno/environments/local.bru`).
4. Set `baseUrl` if needed (default `http://localhost:8080`).
5. Fill vars as you go (`venueId`, `tableId`, `testOrderId`, OAuth tokens, etc.).

Folders mirror API areas: `auth`, `venue`, `tables`, `menu-items`, `staff-orders`, `public-*`, `payments`, `system`.

### Typical staff flow in Bruno

1. `auth/continue-login-password` (or sign-up) — sets cookies.
2. Venue / membership calls to obtain `venueId`.
3. Staff-scoped requests under `tables`, `menu-items`, `staff-orders`, etc.

Cookie session behaviour matches production browser clients; see [security/token-and-session-management.md](./security/token-and-session-management.md).

---

## Auth notes

| Client | Mechanism |
|--------|-----------|
| Staff REST | HttpOnly `access-token` cookie (and refresh via `/api/v1/auth/refresh`) |
| Customer REST | `/api/v1/public/**` — no account |
| WebSocket | Staff ticket / anonymous table bind — [web-socket-flow.md](./web-socket-flow.md) |

Swagger UI cookie auth works when the UI and API share the same host/port so the browser sends cookies. Cross-origin setups should prefer Bruno or the frontend app.

---

## Related documentation

| Document | Covers |
|----------|--------|
| [development-instructions.md](./development-instructions.md) | Local run and how to extend APIs |
| [security/public-vs-protected-endpoints.md](./security/public-vs-protected-endpoints.md) | Which routes are public |
| [security/token-and-session-management.md](./security/token-and-session-management.md) | Continue / refresh / logout |
| [billing-flow.md](./billing-flow.md) | Payment endpoints behaviour |
