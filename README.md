# milly-back

Spring Boot backend for **Milly** — a multi-venue restaurant ordering platform. One API serves staff (venue operations) and customers (table ordering): REST for reads/writes, STOMP over WebSocket for real-time updates, PostgreSQL as the system of record.

Quick start: [documenation/installation.md](./documenation/installation.md). Day-to-day workflow: [documenation/development-instructions.md](./documenation/development-instructions.md).

---

## Team information

| Full name | LinkedIn | GitHub |
|-----------|----------|--------|
| Ali Safarli | [linkedin.com/in/ali-safarli-7a01a7297](https://www.linkedin.com/in/ali-safarli-7a01a7297) | [github.com/alisafarli06](https://github.com/alisafarli06) |
| Mikayil Guliyev | [linkedin.com/in/mikayil-guliyev-341275306](https://www.linkedin.com/in/mikayil-guliyev-341275306) | [github.com/miko44quliyev](https://github.com/miko44quliyev) |
| Ilkin Ismayilov | [linkedin.com/in/ilkin-ismayilov](https://www.linkedin.com/in/ilkin-ismayilov) | [github.com/ilkinismayilov-905](https://github.com/ilkinismayilov-905) |
| Yusif Xankishiyev | [linkedin.com/in/yusif-xankishiyev](https://www.linkedin.com/in/yusif-xankishiyev) | [github.com/XankisiyevYusif](https://github.com/XankisiyevYusif) |
| Omar Ismayilov | [linkedin.com/in/omar-ismayilov-6b97b9337](https://www.linkedin.com/in/omar-ismayilov-6b97b9337) | [github.com/AnonimProgrammer](https://github.com/AnonimProgrammer) |

---

## Project links

| Link | URL |
|------|-----|
| Backend repository | [github.com/AnonimProgrammer/milly-back](https://github.com/AnonimProgrammer/milly-back) |
| Live application | [milly-front.vercel.app](https://milly-front.vercel.app) |

---

## Documentation guide

Start with the system overview, then jump to the flow you need.

| Document | What it covers |
|----------|----------------|
| [System Design](./documenation/system-design.md) | High-level architecture, modules, related docs index |
| [Installation](./documenation/installation.md) | Docker Compose, local Gradle run |
| [Environment Setup](./documenation/environment-setup.md) | Environment variables and profiles |
| [Development Instructions](./documenation/development-instructions.md) | Layout, adding features, migrations, tests, git |
| [API Documentation](./documenation/api-documentation.md) | Bruno collection + Swagger / OpenAPI |
| [Security Flow](./documenation/security/security-flow.md) | Identity model; links to security subdocs |
| [WebSocket Flow](./documenation/web-socket-flow.md) | STOMP tickets and subscription guards |
| [AI Integration](./documenation/ai/ai-integration.md) | AI ports, OpenRouter, enablement |
| [Table Chatbot](./documenation/ai/chatbot.md) | Table chat WebSocket + AI flow |
| [Billing Flow](./documenation/billing-flow.md) | Payments and billing API behaviour |

Security deep-dives live under [`documenation/security/`](./documenation/security/).

---

## API documentation

| Tool | Use when |
|------|----------|
| **Swagger UI** | Browse live OpenAPI while the server is running |
| **Bruno** | Scripted / cookie-friendly request suites checked into the repo |

Details and setup: [documenation/api-documentation.md](./documenation/api-documentation.md).

| Resource | URL / path |
|----------|------------|
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Bruno collection | [`bruno/`](./bruno/) |

Auth for staff APIs uses HttpOnly `access-token` cookies. Bruno is usually easier for full login → protected-call flows; Swagger is best for discovering shapes and trying individual endpoints.
