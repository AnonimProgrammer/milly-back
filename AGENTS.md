# milly-back — agent instructions

Spring Boot backend for Milly (multi-venue restaurant platform).

## Git branch and commit conventions

Apply whenever suggesting branch names or commit messages.

### Branch naming

**Repo-wide or cross-cutting work:** `{type}/{feature-title}`

**Module-scoped work:** `{type}/{module-name}/{feature-title}`

**Allowed types:** `feat`, `fix`, `refactor`, `docs`

- Use lowercase kebab-case for every segment after the type.
- Module names match backend bounded contexts: `auth`, `venue`, `table`, `menu`, `order`, `billing`, `config`, `common`.

Examples:

- `feat/auth/google-oauth2-provider`
- `fix/billing/update-payment-validation`
- `refactor/order/extract-status-transitions`
- `docs/security-flow-venue-authorization`

### Commit messages

`{type}: {Commit title}`

**Allowed types:** `feat`, `fix`, `refactor`, `docs`

- Prefix with type, then colon and a single space.
- Write a clear, concise title (imperative or sentence case).

Examples:

- `feat: Google OAuth2 provider integrated`
- `fix: Payment update to avoid duplicate charges`
- `refactor: Extract order status machine into domain service`
- `docs: Document WebSocket ticket flow`

Do not create branches or commits unless the user asks.
