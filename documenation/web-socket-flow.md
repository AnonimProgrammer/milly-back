# WebSocket Connection & Security Flow

---

## Summary

Describes how Milly secures STOMP over WebSocket: staff authenticate via a **single-use ticket** exchanged from a cookie-based JWT; customers connect **anonymously** with table-scoped subscriptions. Staff topics are **venue-scoped** (`/topic/venue/{venueId}/staff`). Covers ticket storage in Caffeine, handshake rules, subscription guards, and failure handling. Global login is in [security-flow.md](./security/security-flow.md) and [token-and-session-management.md](./security/token-and-session-management.md); venue authorization is in [venue-authorization-flow.md](./security/venue-authorization-flow.md). Table chat + AI: [chatbot.md](./ai/chatbot.md). Part of the broader system in [system-design.md](./system-design.md).

---

## Table of contents

1. [Architecture overview](#architecture-overview)
2. [Connection modes](#connection-modes)
3. [Staff flow — ticket exchange](#staff-flow--ticket-exchange)
4. [Subscription guard](#subscription-guard)
5. [Public customer behaviour](#public-customer-behaviour)
6. [Security notes](#security-notes)
7. [Failure modes](#failure-modes)

---

## Architecture overview

Milly uses **STOMP over WebSocket** for real-time order and payment notifications, and for **table chat**. HTTP (REST) remains the source of truth for business reads and writes. Domain mutation events are **server push** after REST; table chat also accepts customer `SEND` messages (see [chatbot.md](./ai/chatbot.md)).

```mermaid
flowchart TB
  subgraph clients [Clients]
    Staff[Staff App /venue/venueId/staff]
    Customer[Customer App /table/tableId]
  end

  subgraph backend [Backend]
    REST[REST API]
    TicketAPI[POST /api/v1/ws-ticket]
    WS[STOMP Broker /ws]
    Cache[(Caffeine Ticket Store)]
    SVC[Domain Services]
    DB[(PostgreSQL)]
  end

  Staff -->|cookie: access token| REST
  Staff -->|cookie: access token| TicketAPI
  TicketAPI -->|validate JWT| Cache
  TicketAPI -->|ticket UUID| Staff
  Staff -->|wss /ws?ticket=...| WS
  WS -->|claim ticket| Cache

  Customer -->|no auth| REST
  Customer -->|wss /ws public| WS

  REST --> SVC --> DB
  SVC -->|publish events| WS
  WS -->|/topic/venue/id/staff| Staff
  WS -->|/topic/table/id| Customer
  WS -->|/topic/table/id/chat| Customer
```

---

## Connection modes

| Mode | Page | REST | WebSocket handshake | Subscriptions allowed |
|------|------|------|---------------------|------------------------|
| **Public** | `/table/{tableId}` | Public endpoints (menu, orders for table, payments) | Anonymous — no ticket | `/topic/table/{tableId}` and `/topic/table/{tableId}/chat` |
| **Authenticated** | `/venue/{venueId}/staff` | Protected endpoints (JWT cookie + venue role) | Ticket required | `/topic/venue/{venueId}/staff` |

Both modes connect to the same STOMP endpoint: `wss://{host}/ws`

Staff append the ticket: `wss://{host}/ws?ticket={uuid}`

---

## Staff flow — ticket exchange

Staff must never put the long-lived JWT in the WebSocket URL. Cookies are not reliably sent on cross-origin WebSocket upgrades, so we use a **short-lived, single-use ticket** bound to the authenticated user.

```mermaid
sequenceDiagram
  autonumber
  participant Staff as Staff App
  participant REST as REST API
  participant Cache as Caffeine Ticket Store
  participant WS as STOMP /ws

  Note over Staff: User already logged in.<br/>access-token cookie is set (HttpOnly).

  Staff->>REST: POST /api/v1/ws-ticket<br/>Cookie: access-token=...
  REST->>REST: Validate JWT from cookie
  REST->>Cache: Store ticket (ticketId, userId, issuedAt, expiresAt)
  REST-->>Staff: 200 ticketId + expiresAt

  Staff->>WS: Connect wss://host/ws?ticket={ticketId}
  WS->>Cache: Lookup and claim ticket (delete on success)
  alt ticket missing, expired, or already used
    WS-->>Staff: Reject handshake (401 / close)
  else ticket valid
    WS->>WS: Bind session → userId
    WS-->>Staff: STOMP CONNECTED
    Staff->>WS: SUBSCRIBE /topic/venue/{venueId}/staff
  end

  Note over WS,Staff: On REST mutation (e.g. order approved),<br/>server publishes to /topic/venue/{venueId}/staff
```

### Ticket store

Tickets live in an in-process **Caffeine** cache with a **30 second TTL**:

| Field | Description |
|-------|-------------|
| `ticketId` | Random UUID sent to the client once |
| `userId` | Staff user this ticket belongs to |
| `issuedAt` | When the ticket was created |
| `expiresAt` | When the ticket expires (issuedAt + 30s) |

A ticket is **single-use** — deleted the moment it is claimed during handshake. On reconnect, the client must call `POST /api/v1/ws-ticket` again for a fresh ticket.

Caffeine is sufficient for single-instance deployments. Use a shared store (e.g. Redis) when running multiple backend replicas.

---

## Subscription guard

Enforce authorization at `SUBSCRIBE` time, not only at handshake:

| Session type | Allowed subscriptions |
|--------------|----------------------|
| Anonymous (customer) | `/topic/table/{tableId}` and `/topic/table/{tableId}/chat` — only the table bound to the session |
| Authenticated staff | `/topic/venue/{venueId}/staff` — only a venue the staff member belongs to |

Any other subscription attempt is rejected.

Anonymous sessions may also `SEND` to `/app/table/{tableId}/chat` when the destination matches the session-bound table. Staff sessions cannot send to table chat destinations. Details: [chatbot.md](./ai/chatbot.md).

### Publish flow (server-side)

```mermaid
sequenceDiagram
  participant Client
  participant REST as REST API
  participant SVC as Domain Service
  participant WS as STOMP Broker
  participant Sub as Subscribers

  Client->>REST: REST mutation (e.g. approve order)
  REST->>SVC: Process request
  SVC->>SVC: Persist to DB
  SVC->>WS: Publish event
  WS->>Sub: /topic/table/{tableId} and/or /topic/venue/{venueId}/staff
```

Clients do not send **business mutations** over WebSocket. The server publishes domain events after successful REST operations. Table chat is the exception: customers send chat text via STOMP; the chatbot module replies on the chat topic.

---

## Public customer behaviour

The customer journey at `/table/{tableId}` is fully **unauthenticated**.

```mermaid
sequenceDiagram
  participant Customer as Customer App
  participant REST as Public REST
  participant WS as STOMP /ws

  Customer->>REST: Load menu and order for table
  Customer->>WS: Connect wss://host/ws (no ticket)
  WS-->>Customer: STOMP CONNECTED
  Customer->>WS: SUBSCRIBE /topic/table/3
  Note over WS: Interceptor verifies<br/>subscription matches allowed table

  Note over REST: Customer places order via REST
  REST->>WS: publish ORDER_PLACED → /topic/venue/{venueId}/staff
  Note over Customer: Staff approves via REST
  REST->>WS: publish ORDER_APPROVED → /topic/table/3
  WS-->>Customer: event received
  Customer->>REST: Refresh order state
```

**Key points:**

- No `POST /api/v1/ws-ticket` for customers.
- Connection is anonymous; security relies on **topic-scoped subscriptions** and **table-scoped REST** writes.
- The customer app uses `tableId` from the route so the subscription/send guard only allows that table’s topics (and chat send destination).
- Knowing a `tableId` is intentional (QR code). Other tables' data must not be exposed via REST or WebSocket.

---

## Security notes

1. **JWT never in the URL.** Only the ephemeral ticket appears in the WebSocket query string. Tickets are useless after claim or 30s expiry.
2. **HttpOnly cookies** for access tokens protect against XSS reading the JWT. The ticket endpoint reads the cookie server-side.
3. **Single-use tickets** prevent replay if the URL is logged or leaked during the brief handshake window.
4. **Short TTL (30s)** limits exposure if a ticket is issued but never used.
5. **Subscription interceptor** is mandatory. Handshake alone is not enough — block staff topics on anonymous sessions, block cross-venue topics on staff, and block cross-table topics on customers.
6. **CORS / origins** — restrict allowed origins in production to known front-end domains.
7. **WSS in production** — always `wss://` behind TLS.
8. **Multi-instance** — Caffeine is per JVM; use Redis (or similar) for a shared ticket store when horizontally scaling.
9. **Rate limit** `POST /api/v1/ws-ticket` to prevent ticket flooding.

---

## Failure modes

| Scenario | Expected behaviour |
|----------|-------------------|
| Ticket expired (>30s) | Handshake rejected; client requests new ticket |
| Ticket already used | Handshake rejected; client requests new ticket |
| No ticket on staff page | Handshake rejected; client fetches ticket first |
| WS drops mid-session | Reconnect: obtain a **new** ticket via `POST /api/v1/ws-ticket`, then reconnect — do not reuse old tickets |
| Invalid / forged ticket | Handshake rejected |
| Customer subscribes to `/topic/venue/{venueId}/staff` | Subscription denied |
| Customer subscribes to wrong table topic | Subscription denied |
| Access-token cookie expired | `POST /api/v1/ws-ticket` returns 401; client ends session and returns to `/` |
