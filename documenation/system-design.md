# System Design

**Author:** Omar Ismayilov

---

## Summary

High-level overview of the Milly restaurant ordering platform: multi-venue identity, how **staff** and **customer** apps talk to the backend, where data is stored, and how modules are organized on both sides. Users authenticate **globally** (system level); access to a venue's operations is granted via **venue roles**. REST (`/api/v1`) handles reads and writes; STOMP over WebSocket pushes real-time events after mutations. PostgreSQL is the system of record; Caffeine holds ephemeral handshake data (WebSocket tickets, invitation redemption).

For WebSocket details see [web-socket-flow.md](./web-socket-flow.md). For login, OAuth, venue membership, and authorization see [security-flow.md](./security-flow.md).

---

## Table of contents

1. [System context](#system-context)
2. [Identity model](#identity-model)
3. [Pages and routes](#pages-and-routes)
4. [Communication model](#communication-model)
5. [Persistence](#persistence)
6. [Backend modules](#backend-modules)
7. [Frontend modules](#frontend-modules)
8. [Client flows](#client-flows)

---

## System context

Milly is a **multi-venue** restaurant platform. A user account exists independently of any venue. To operate a restaurant, the user either **registers a new venue** (becomes Manager) or **joins an existing venue** via an invitation. Customers at a table remain anonymous — no account required.

| Journey | Route | Auth | Purpose |
|---------|-------|------|---------|
| **Onboarding** | `/`, `/login`, `/register-venue`, `/join-venue` | Global user session | Sign up, create or join a venue |
| **Staff portal** | `/venue/{venueId}/staff` | Global session + venue role | Orders, menu, tables, QR (by role) |
| **Customer** | `/table/{tableId}` | None | Browse menu, order, pay |

One **Next.js** frontend and one **Spring Boot** backend serve all journeys.

```mermaid
flowchart TB
  subgraph clients [Next.js Frontend]
    Onboard[Onboarding]
    Staff[Staff /venue/id/staff]
    Customer[Customer /table/tableId]
  end

  subgraph backend [Spring Boot Backend]
    REST[REST API]
    WS[STOMP Broker /ws]
    Modules[Domain Modules]
  end

  subgraph storage [Storage]
    PG[(PostgreSQL)]
    Cache[(Caffeine)]
  end

  Onboard --> REST
  Staff --> REST
  Staff --> WS
  Customer --> REST
  Customer --> WS

  REST --> Modules
  Modules --> PG
  Modules --> WS
  WS --> Cache
```

---

## Identity model

Milly separates **system identity** from **venue access**.

### System level

| Concept | Description |
|---------|-------------|
| **User** | Global account — created on sign-up / OAuth |
| **System role** | `USER` — every authenticated account has this role by default |
| **Session** | JWT in HttpOnly cookies; proves who the user is, not which venue they operate |

Sign-in methods (planned): email/password, Google OAuth2, Apple (optional).

### Venue level

| Concept | Description |
|---------|-------------|
| **Venue** | A restaurant (name, location, …) — tenant boundary for menu, tables, orders |
| **Venue membership** | Link between a user and a venue |
| **Venue role** | What the user can do **inside that venue** |

| Venue role | Access |
|------------|--------|
| **Manager** | Orders, menu, tables, QR codes, invitations, venue settings |
| **Waiter** | Orders only (view, approve, reject, close) |

A user can belong to **multiple venues** with different roles at each.

### Authorization rule

Every staff request is checked in two steps:

1. **Authenticated?** — valid global session (cookie).
2. **Authorized for this venue?** — user has a venue membership with the required role.

If step 2 fails → **403 Forbidden**. The frontend may hide UI by role, but the **backend always enforces** permissions. The frontend can validate the current user's venue role via a backend call (e.g. proxy / session endpoint) — never trust client-side role state alone.

```mermaid
flowchart LR
  Request[Staff request] --> Auth{Global session valid?}
  Auth -->|No| U401[401 Unauthorized]
  Auth -->|Yes| Venue{Venue role sufficient?}
  Venue -->|No| Forbidden[403]
  Venue -->|Yes| Handler[Execute use case]
```

---

## Pages and routes

### Home and authentication

| Route | Purpose |
|-------|---------|
| `/` | Welcome screen — **Join Venue** and **Register Venue** |
| `/login` | Sign in / sign up (email, Google, Apple). Both home buttons lead here; intended post-login destination is remembered |

After login, the user is redirected based on which home action they chose:

- **Register Venue** → `/register-venue`
- **Join Venue** → `/join-venue`

### Register venue

| Route | Purpose |
|-------|---------|
| `/register-venue` | Form: venue name, location, and other basic fields → **Create** |

On create, the backend creates the venue and assigns the user **Manager** at that venue. User is redirected to `/venue/{venueId}/staff`.

### Join venue

| Route | Purpose |
|-------|---------|
| `/join-venue` | **My venues** — list of venues the user belongs to, with role shown per row |
| `/join-venue` (action) | **Join new venue** — paste invitation link or code → **Confirm** |

Backend validates the invitation (not expired, not already used), creates venue membership with the role the Manager assigned, then redirects to `/venue/{venueId}/staff`.

### Staff portal

| Route | Purpose |
|-------|---------|
| `/venue/{venueId}/staff` | Venue-scoped dashboard. Tabs and actions depend on **venue role** (Waiter vs Manager) |

On load, backend returns venue details and the user's role at that venue. No role → 403 on API; frontend shows access restricted.

### Customer (unchanged)

| Route | Purpose |
|-------|---------|
| `/table/{tableId}` | Public QR entry — menu, order, pay. No login |

---

## Communication model

| Channel | Role | Direction |
|---------|------|-----------|
| **REST** | Source of truth — load and mutate data | Client ↔ Backend |
| **STOMP / WebSocket** | Real-time notifications after REST mutations | Backend → Client |

Typical flow:

1. Client calls REST (e.g. place order, approve order).
2. Backend persists to PostgreSQL (scoped to `venueId` where applicable).
3. Backend publishes an event to the appropriate STOMP topic.
4. Subscribed clients receive the event and refresh state via REST.

```mermaid
sequenceDiagram
  participant Client
  participant REST as REST API
  participant DB as PostgreSQL
  participant WS as STOMP Broker
  participant Peer as Other Client

  Client->>REST: Mutation (POST / PATCH)
  REST->>DB: Persist
  REST-->>Client: 200 OK
  REST->>WS: Publish event
  WS-->>Peer: Push to venue-scoped topic
  Peer->>REST: Refresh state
```

### STOMP topics (venue-scoped)

| Topic | Subscriber | Example events |
|-------|------------|----------------|
| `/topic/venue/{venueId}/staff` | Staff at that venue (authenticated + venue role) | Order placed, order closed, payment received |
| `/topic/table/{tableId}` | Customer at that table (public) | Order approved, payment received |

Staff subscribe only to the topic for the venue they are viewing. Subscription guard rejects cross-venue topics.

### Authentication per channel

| Client | REST | WebSocket |
|--------|------|-----------|
| Staff | JWT cookie + venue role on each request | Single-use ticket via `POST /api/v1/ws-ticket` → `wss://host/ws?ticket=...` |
| Customer | Public endpoints, table-scoped | Anonymous `wss://host/ws`, subscribe to own table topic only |

See [web-socket-flow.md](./web-socket-flow.md) for ticket exchange and subscription guards. See [security-flow.md](./security-flow.md) for login, OAuth, session handling, and venue authorization.

---

## Persistence

| Store | Technology | Purpose | Lifetime |
|-------|------------|---------|----------|
| **Primary database** | PostgreSQL | Users, venues, memberships, menu, tables, orders, payments, invitations | Permanent |
| **Ephemeral cache** | Caffeine (in-memory) | WebSocket handshake tickets; optional hot path for invitation redemption | Short-lived, single-use |

PostgreSQL holds all business and identity data. Caffeine is not a domain entity cache — it holds short-lived tokens consumed at handshake or redemption (same pattern as WebSocket tickets).

---

## Backend modules

Backend follows **Clean Architecture** with one module per bounded context. Module names are **singular**.

```
com.milly/
├── config/                         # Global: Security, WebSocket broker, Jackson
├── common/                         # Shared kernel: IDs, Money, errors
├── auth/                           # Global user identity, login, JWT, WS tickets
├── venue/                          # Venues, memberships, venue roles, invitations
├── table/
├── menu/
├── order/
└── billing/
```

### Per-module structure

Each feature module follows the same layered layout (not every layer is required in every module):

```
{module}/
├── domain/
│   ├── entity/
│   ├── value-object/
│   └── event/
├── application/
│   ├── port/inbound/
│   ├── port/outbound/
│   ├── usecase/
│   └── dto/
└── infrastructure/
    ├── adapter/inbound/http/
    ├── adapter/outbound/persistence/
    ├── adapter/outbound/ws/
    └── config/
```

### Module responsibilities

| Module | Owns |
|--------|------|
| **common** | Shared value objects, domain exceptions |
| **auth** | Global user sign-up/sign-in (password, OAuth), JWT cookies, `POST /api/v1/ws-ticket`, Caffeine ticket store |
| **venue** | Venue CRUD, user–venue memberships, venue roles (Manager, Waiter), invitation link/code generation and redemption |
| **table** | Tables within a venue (QR targets) |
| **menu** | Menu catalog per venue |
| **order** | Order lifecycle per venue, STOMP order events |
| **billing** | Payments per venue, STOMP payment events |

All venue-bound modules scope data and operations by `venueId`. Requests without a valid membership and role are rejected.

---

## Frontend modules

```
src/
├── app/                            # Next.js routes only
└── modules/
    ├── shared/                     # API client, WS client, store, UI primitives
    ├── auth/                       # Login, sign-up, OAuth, session
    ├── venue/                      # Register venue, join venue, my venues, invitations UI
    ├── tables/
    ├── menu/
    ├── orders/
    ├── billing/
    ├── customer/                   # UI only — /table/{tableId} flow
    └── staff/                      # UI only — /venue/{venueId}/staff shell
```

### Module responsibilities

| Module | Maps to backend | Role |
|--------|-----------------|------|
| **shared** | common + config | REST client, STOMP client, app store, shared UI |
| **auth** | auth | Login, sign-up, OAuth, global session |
| **venue** | venue | Register venue, join venue, my venues list, paste invitation |
| **tables** | table | Table CRUD, QR generation (Manager) |
| **menu** | menu | Menu browse (customer) and CRUD (Manager) |
| **orders** | order | Order placement, staff kanban, pending view |
| **billing** | billing | Bill view, payments, payment progress |
| **customer** | — | State machine: menu → pending → bill |
| **staff** | — | Venue-scoped portal; tabs filtered by venue role |

### Routes

| Route | Entry | Modules |
|-------|-------|---------|
| `/` | Home — Join / Register venue | — |
| `/login` | Auth | `auth` |
| `/register-venue` | Create venue | `auth`, `venue` |
| `/join-venue` | My venues + join via invitation | `auth`, `venue` |
| `/venue/[venueId]/staff` | Staff dashboard | `staff`, `orders`, `tables`, `menu`, `billing` |
| `/table/[tableId]` | Customer flow | `customer`, `menu`, `orders`, `billing` |

---

## Client flows

### Onboarding (register venue)

```mermaid
flowchart LR
  Home[/ Home] -->|Register Venue| Login[/login]
  Login --> Register[/register-venue]
  Register -->|Create venue| API[POST venue]
  API --> Role[Assign Manager role]
  Role --> Staff[/venue/id/staff]
```

User signs in globally, fills venue details, backend creates venue + Manager membership, redirects to staff portal.

### Onboarding (join venue)

```mermaid
flowchart LR
  Home[/ Home] -->|Join Venue| Login[/login]
  Login --> Join[/join-venue]
  Join -->|Paste invitation| API[Redeem invitation]
  API --> Role[Assign venue role]
  Role --> Staff[/venue/id/staff]
```

Invitation is single-use and time-limited (same idea as WebSocket tickets). Backend assigns Waiter or Manager as configured when the invitation was created.

### Staff (inside a venue)

```mermaid
flowchart LR
  Page[/venue/id/staff] --> Check[Backend: membership + role]
  Check -->|403| Block[Access restricted]
  Check -->|OK| Cookie[access-token cookie]
  Cookie --> REST[Venue-scoped REST]
  Cookie --> Ticket[POST /api/v1/ws-ticket]
  Ticket --> WS[STOMP /ws?ticket=...]
  WS --> Sub[SUBSCRIBE /topic/venue/id/staff]
```

Waiter sees orders only. Manager sees orders, menu, tables, QR, invitations. Backend enforces on every call regardless of UI state.

### Customer

```mermaid
flowchart LR
  QR[Scan QR] --> Page[/table/tableId]
  Page --> REST[Public REST]
  Page --> WS[STOMP /ws]
  WS --> Sub[SUBSCRIBE /topic/table/id]
  REST --> PG[(PostgreSQL)]
  REST --> Push[Publish events]
  Push --> Sub
```

Customer needs no login. REST and WebSocket stay scoped to the table (and its venue on the backend).
