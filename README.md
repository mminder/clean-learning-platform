# Clean Learning Platform

A web platform where customers buy and book consultations and training courses, experts deliver them, and admins manage the offerings. It's being built as a PoC first, then refined into an MVP.

**Status:** the second increment, [02 Manage consultation offerings](_docs/specs/02-manage-consultation-offerings.md), is complete, after [01 Shop tracer bullet](_docs/specs/01-shop-tracer-bullet.md). A visitor logs in as one of the seeded users; an admin creates and edits consultation offerings with their packages, eligible experts and image, and publishes, unpublishes, archives and deletes them; the shop follows at once, served by the Vite dev server or by the single jar. No next increment is planned yet.

## Docs

- [_docs/](_docs/README.md): use cases, architecture, glossary and decision log
- [AGENTS.md](AGENTS.md): instructions for coding agents

## Planned stack

React SPA · Spring Boot · PostgreSQL · OAuth 2 / OIDC · monorepo
