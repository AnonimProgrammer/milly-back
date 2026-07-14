# Public vs Protected Endpoints

---

## Summary

How Milly classifies REST and WebSocket entry points: which paths are public, which require a global session, which require `ADMIN`, and where venue role checks still apply after authentication. Overview: [security-flow.md](./security-flow.md).

---

## Table of contents

1. [API surface](#api-surface)
2. [Access matrix](#access-matrix)
3. [Configured permit-all routes](#configured-permit-all-routes)
4. [Authenticated and admin routes](#authenticated-and-admin-routes)
5. [Venue-scoped staff routes](#venue-scoped-staff-routes)
6. [Examples](#examples)

---

## API surface

All REST endpoints are versioned under **`/api/v1`**. The STOMP endpoint **`/ws`** is not versioned — it is a transport channel, not a REST resource. Future breaking API changes ship as `/api/v2`, etc.

---

## Access matrix

| Pattern | Access |
|---------|--------|
| `POST /api/v1/auth/continue` | Public — login / first-time sign-up |
| `POST /api/v1/auth/refresh` | Public — uses `refresh-token` cookie |
| `POST /api/v1/auth/logout` | Public — clears session cookies |
| `/api/v1/public/**` | Public — customer table flow |
| `/ws/**` | Public at HTTP handshake — topic access enforced on STOMP subscribe |
| `/actuator/**` | Public (as configured) |
| `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` | Public — OpenAPI JSON and Swagger UI |
| `/api/v1/admin/**` | Authenticated + system role `ADMIN` |
| Other `/api/v1/**` | Authenticated global session required |
| Venue-scoped staff routes | Authenticated **and** sufficient venue membership/role |

Other auth routes such as `GET /api/v1/auth/me` and `POST /api/v1/ws-ticket` require authentication.

---

## Configured permit-all routes

From `SecurityConfig`, these matchers are `permitAll`:

- `/api/v1/auth/continue`
- `/api/v1/auth/refresh`
- `/api/v1/auth/logout`
- `/api/v1/public/**`
- `/actuator/**`
- `/ws/**`
- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`

Everything else under `/api/v1/**` defaults to authenticated unless matched above. Admin paths are additionally restricted by role.

Customer security relies on **table-scoped** public REST paths and WebSocket subscription guards — not on a customer account.

---

## Authenticated and admin routes

| Kind | Rule |
|------|------|
| Default staff / user APIs | Valid `access-token` cookie → Spring Security authenticated principal |
| Admin APIs | Authenticated + `ROLE_ADMIN` (`hasRole("ADMIN")`) |
| Missing / invalid session | **401 Unauthorized** |
| Authenticated but wrong system role | **403 Forbidden** |

---

## Venue-scoped staff routes

Paths that include a `venueId` (for example `/api/v1/venues/{venueId}/...`) still need a second check after authentication: active membership and sufficient venue role. That layer is documented in [venue-authorization-flow.md](./venue-authorization-flow.md).

Failure mapping:

| Check | Failure |
|-------|---------|
| No valid global session | **401** |
| Session OK, membership/role insufficient | **403** |

---

## Examples

### Public customer menu

```http
GET /api/v1/public/tables/{tableId}/menu
```

No cookies required.

### Public auth continue

```http
POST /api/v1/auth/continue
Content-Type: application/json
```

No prior session required; success sets cookies (see [token-and-session-management.md](./token-and-session-management.md)).

### Protected current user

```http
GET /api/v1/auth/me
Cookie: access-token=<jwt>
```

| Outcome | Response |
|---------|----------|
| No / invalid cookie | `401 Unauthorized` |
| Valid session | `200 OK` + current user |

### Admin users

```http
GET /api/v1/admin/users
Cookie: access-token=<jwt>
```

Requires `ADMIN` system role; otherwise **403** (or **401** if unauthenticated).

### Venue-scoped staff

```http
GET /api/v1/venues/{venueId}/orders
Cookie: access-token=<jwt>
```

| Outcome | Response |
|---------|----------|
| No / invalid cookie | `401 Unauthorized` |
| Valid session, no active membership | `403 Forbidden` |
| Valid session, active member | `200 OK` |
