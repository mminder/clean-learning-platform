# Clean Learning Platform

A web platform where customers buy and book consultations and training courses, experts deliver them, and admins manage the offerings. It's being built as a PoC first, then refined into an MVP.

**Status:** the first increment, [01 Shop tracer bullet](_docs/specs/01-shop-tracer-bullet.md), is complete. The shop list and the consultation offering detail work end to end: the seeded Career Coaching offering goes from Postgres through the Spring Boot API to the React shop, served by the Vite dev server or by the single jar.

## Docs

- [_docs/](_docs/README.md): use cases, architecture, glossary and decision log
- [AGENTS.md](AGENTS.md): instructions for coding agents

## Planned stack

React SPA · Spring Boot · PostgreSQL · OAuth 2 / OIDC · monorepo
