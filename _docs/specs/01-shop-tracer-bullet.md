# 01 Shop tracer bullet

| | |
|---|---|
| Status | Ready |
| Use cases | [UC-02](../use-cases/uc-02-browse-shop.md), consultation part (steps 1–4) |
| Tasks | [tasks/01-shop-tracer-bullet](../tasks/01-shop-tracer-bullet/README.md) |

## Why

No code exists yet. Before we build business rules, we want one thin path that runs through every layer and every delivery step, ending with data from Postgres shown in the React UI:

```
Browser ─► React page ─► generated API client ─► Spring Security ─► controller ─► use case
        ─► domain ─► port ─► JPA adapter ─► Flyway schema + seed in Postgres
```

This retires the integration risks early: Java 25 with Spring Boot 4, Testcontainers in cloud sessions, the OpenAPI-to-TypeScript pipeline, the security filter chain, CI, and Spring Boot serving the SPA. Later increments copy the patterns instead of inventing them.

UC-02 is the right first slice. It's public, so it needs no login. It's read-only, so it needs no locks. Its data comes from the seed.

## What changes

- A visitor opens the shop and sees the seeded **Career Coaching** offering as a card, with "from CHF 450.00".
- The card opens a detail page with the description, session duration, cancellation deadline, both packages and both eligible experts.
- The `backend/` and `frontend/` projects exist, with the conventions of the architecture docs in place: layer packages checked by ArchUnit, a Flyway schema with the `poc` seed, Testcontainers integration tests, `ProblemDetail` errors, the committed OpenAPI contract with a generated client, and the Spring Security baseline.
- GitHub Actions runs every check on each pull request.
- `./mvnw -Pfrontend package` builds one jar that serves the SPA and the API.
- Cloud agent sessions can build and test both projects.

## User stories

1. As a visitor, I want to see the published offerings as cards, so that I know what the platform sells.
2. As a visitor, I want to see the lowest price of a consultation offering, so that I can judge whether it's for me.
3. As a visitor, I want to see an offering's packages, session duration and cancellation deadline, so that I know what I would buy.
4. As a visitor, I want to see the eligible experts with their name and bio, so that I know who might coach me.
5. As a developer, I want a working example of every layer and every kind of test, so that later use cases follow one pattern.
6. As a developer, I want one command per check and CI on every pull request, so that I know when a change is done.

## Requirements

### Requirement: The shop lists published offerings

The shop at `/` lists every `PUBLISHED` offering as a card, sorted by name ([UC-02](../use-cases/uc-02-browse-shop.md) steps 1–2). In this increment, only consultation offerings exist.

#### Scenario: A visitor sees the seeded offering

- **GIVEN** the `poc` seed
- **WHEN** a visitor opens `/`
- **THEN** one card shows an image placeholder, "Career Coaching", the description shortened to three lines, a **Consultation** badge and "from CHF 450.00"

#### Scenario: Draft and archived offerings stay hidden

- **GIVEN** a `DRAFT` and an `ARCHIVED` consultation offering besides the seed
- **WHEN** anyone calls `GET /api/shop/offerings`
- **THEN** the response contains only Career Coaching

#### Scenario: Offerings are sorted by name

- **GIVEN** a second published offering named "Agile Coaching"
- **WHEN** anyone calls `GET /api/shop/offerings`
- **THEN** "Agile Coaching" comes before "Career Coaching"

### Requirement: Consultation offering detail

A card opens `/offerings/:id`. The detail page shows the description, session duration, cancellation deadline, packages (session count and price, fewest sessions first) and eligible experts (display name, bio, photo placeholder, sorted by display name), as in [UC-02](../use-cases/uc-02-browse-shop.md) steps 3–4.

#### Scenario: A visitor opens Career Coaching

- **WHEN** a visitor clicks the Career Coaching card
- **THEN** the detail page shows 60-minute sessions, a 24 h cancellation deadline, the packages "3 sessions, CHF 450.00" and "5 sessions, CHF 700.00", and Expert 1 and Expert 2 with their bios

#### Scenario: Unknown or unpublished offering

- **WHEN** anyone calls `GET /api/shop/offerings/{id}` with an unknown id, or the id of a `DRAFT` or `ARCHIVED` offering
- **THEN** the response is 404 with a `ProblemDetail` body
- **AND** the SPA shows "Offering not found" with a link back to the shop

#### Scenario: Deep link

- **WHEN** a visitor reloads `/offerings/<Career Coaching id>`
- **THEN** the detail page loads, on the Vite dev server and when the jar serves the SPA

### Requirement: The shop is public, every other API route is closed

`/api/shop/**` is public. Every other `/api/**` route needs a logged-in user, and the role prefixes are enforced ([security](../architecture/security.md#roles-by-route-prefix)). Login itself comes with UC-01.

#### Scenario: Anonymous shop access

- **WHEN** an anonymous visitor calls `GET /api/shop/offerings`
- **THEN** the response is 200

#### Scenario: Anonymous access to a protected route

- **WHEN** an anonymous visitor calls a route under `/api/customer`, `/api/expert`, `/api/admin` or `/api/me`
- **THEN** the response is 401, without a redirect or a login page

#### Scenario: Wrong role

- **WHEN** a user with only `CUSTOMER` calls a route under `/api/admin`
- **THEN** the response is 403

### Requirement: The API contract drives the frontend types

The backend generates `backend/openapi.json`, which is committed. The frontend generates its types from it ([D-29](../decisions.md#architecture)).

#### Scenario: A stale contract fails the backend build

- **WHEN** a controller or DTO changes and `backend/openapi.json` isn't regenerated
- **THEN** `./mvnw verify` fails and names the command that regenerates it

#### Scenario: Stale frontend types fail the frontend check

- **WHEN** `backend/openapi.json` changes and the generated frontend types aren't regenerated
- **THEN** `npm run check:api` fails, and so does CI

### Requirement: The layer rules are enforced

#### Scenario: A layer violation fails the build

- **WHEN** a class in `domain` imports Spring or JPA, or a class in `api` uses `adapters.database`
- **THEN** `./mvnw verify` fails

### Requirement: One jar serves SPA and API

#### Scenario: The jar runs the shop

- **GIVEN** the jar built with `./mvnw -Pfrontend package` and a running Postgres
- **WHEN** it runs with the `poc` profile
- **THEN** `http://localhost:8080/` shows the shop, and reloading `/offerings/<id>` shows the detail page

### Requirement: CI checks every pull request

#### Scenario: A pull request runs all checks

- **WHEN** a pull request is opened or updated
- **THEN** GitHub Actions runs the backend build with all tests, the frontend lint, format check, tests, build and API type check, and the jar build with the SPA

### Requirement: Cloud sessions can run every check

#### Scenario: A fresh cloud session

- **WHEN** an agent starts in a fresh cloud session of this repository
- **THEN** `./mvnw verify` (Java 25, Testcontainers with Docker) and the frontend checks pass without manual setup

## Decisions

Agreed with the product owner while planning (2026-11-07).

1. **Scope:** the shop list and the consultation detail. Trainings come with their own schema in a later increment.
2. **Images:** a placeholder for now. The `image` table, uploads and `GET /api/images/{id}` come with [UC-14](../use-cases/uc-14-manage-expert-profile.md) and [UC-16](../use-cases/uc-16-manage-consultation-offering.md).
3. **No Buy button yet.** Buy starts [UC-03](../use-cases/uc-03-purchase-consultation-package.md), which needs login ([UC-01](../use-cases/uc-01-login.md)).
4. **"Short description" means the description, shortened to three lines on the card.** There's no separate field; [UC-02](../use-cases/uc-02-browse-shop.md) now says so.
5. **Security baseline now, login later.** The route-prefix rules and their tests come first, so every new route is denied by default. The CSRF cookie setup for the SPA comes with the first mutating request (UC-01 login).
6. **CI on GitHub Actions from now on**, instead of at MVP time ([D-30](../decisions.md#architecture), changed).
7. **Base package `com.cleanlearning`** ([backend](../architecture/backend.md#packages)).
8. **Seed only what the schema holds:** the six users, the two expert profiles and Career Coaching. The training and availability seeds come with their schema.
9. **A package is `ConsultationPackage` in code** and `consultation_package` in the database, because `java.lang.Package` exists ([glossary](../glossary.md#catalog)).
10. **No dates are shown in this increment.** The Zurich date helpers and the `Clock` bean arrive with the first use case that needs them.
11. **Executed by Claude agents in cloud sessions.** The first task prepares the cloud environment. Tasks 02–08 run as subagents, one per task with a review subagent after each, orchestrated from one cloud session, and the whole increment goes onto one pull request (changed 2026-11-07 by the product owner; it was one task per session and pull request before).

## Out of scope

- Trainings in the shop ([UC-02](../use-cases/uc-02-browse-shop.md) step 5): next increments.
- Images ([D-21](../decisions.md#cross-cutting)): with UC-14 and UC-16.
- Buy, login, `GET /api/me`, the header's login and role sections: with UC-01 and UC-03.
- The CSRF cookie setup: with UC-01.
- Keycloak in `docker-compose.yml`: with OIDC in the MVP.
- Dockerfile, hosting, Playwright E2E: MVP.

## Demo

```bash
docker compose up -d
```

```bash
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=poc
```

```bash
cd frontend && npm run dev
```

1. Open `http://localhost:5173`: the Career Coaching card shows "from CHF 450.00".
2. Click the card: the detail page shows both packages and both experts.
3. Reload the detail page: it still loads.
4. Open `http://localhost:5173/offerings/00000000-0000-0000-0000-000000000000`: "Offering not found".

Then the single jar:

```bash
cd backend && ./mvnw -Pfrontend package && java -jar target/clean-learning-platform-*.jar --spring.profiles.active=poc
```

5. Open `http://localhost:8080` and repeat steps 1–3.

## Risks

| Risk | Mitigation |
|---|---|
| Spring Boot 4, Jackson 3, Testcontainers 2 and springdoc 3 are new major versions, and many examples online are for older ones. | The tasks name the known changes. Agents check the current reference docs instead of relying on memory. |
| Cloud sessions ship with JDK 21 and no running Docker daemon. | Task 01 installs JDK 25 and starts Docker in every session. Both were checked in a cloud session while planning. |
| The first tasks set patterns that every later task copies. | The technical plan fixes the names, the shapes and the test setup. The tracer-bullet task is reviewed with extra care. |
