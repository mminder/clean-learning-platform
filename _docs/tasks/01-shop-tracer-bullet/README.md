# 01 Shop tracer bullet: technical plan

Spec: [01 Shop tracer bullet](../../specs/01-shop-tracer-bullet.md)

This plan holds the decisions that several tasks rely on. The tasks link here instead of repeating them. If a task has to change one of them, it updates this file in the same pull request.

## Tasks

| # | Task | Type | Blocked by |
|---|---|---|---|
| 01 | [Cloud environment](01-cloud-environment.md) | HITL | – |
| 02 | [Backend skeleton](02-backend-skeleton.md) | AFK | 01 |
| 03 | [Frontend skeleton](03-frontend-skeleton.md) | AFK | – |
| 04 | [Security baseline](04-security-baseline.md) | AFK | 02 |
| 05 | [CI](05-ci.md) | AFK | 02, 03 |
| 06 | [Shop list: the tracer bullet](06-shop-list.md) | AFK | 03, 04, 05 |
| 07 | [Consultation offering detail](07-consultation-offering-detail.md) | AFK | 06 |
| 08 | [SPA in the jar](08-spa-in-the-jar.md) | AFK | 06 |

Tasks in the same wave can run in parallel:

```
wave 1   01 Cloud environment (HITL)      03 Frontend skeleton
wave 2   02 Backend skeleton
wave 3   04 Security baseline             05 CI
wave 4   06 Shop list ◄── the tracer bullet: data from Postgres in the React UI
wave 5   07 Consultation offering detail  08 SPA in the jar
```

## Toolchain

| Concern | Choice |
|---|---|
| Java | 25. In cloud sessions, Ubuntu's `openjdk-25-jdk-headless`; in CI, Temurin. |
| Backend | Maven wrapper; the latest Spring Boot 4.x GA; springdoc-openapi 3.x, API-only starter (no Swagger UI); Testcontainers 2.x; ArchUnit |
| Database | PostgreSQL 18, image `postgres:18-alpine` in docker-compose and in Testcontainers |
| Node | 22 LTS, pinned in `frontend/.nvmrc` |
| Frontend | the stack in [frontend.md](../../architecture/frontend.md#stack): Vite, React 19, TypeScript strict, Tailwind 4, shadcn/ui, React Router 7 in declarative mode, TanStack Query 5, openapi-typescript + openapi-fetch, Vitest + Testing Library, ESLint + Prettier |

These major versions are new, and most examples online are for older ones. Check the current reference docs for:

- **Spring Boot 4:** starters are modular (for example `spring-boot-starter-webmvc`, and Flyway needs `spring-boot-starter-flyway`). Jackson 3 is the default; its packages are `tools.jackson.*`, while the annotations stay in `com.fasterxml.jackson.annotation`.
- **Spring Security 7:** only the lambda DSL is supported.
- **Testcontainers 2:** modules are named `testcontainers-<module>`, and container classes moved to per-module packages (for example `org.testcontainers.postgresql.PostgreSQLContainer`).

## Backend

### Coordinates and configuration

- groupId `com.cleanlearning`, artifactId `clean-learning-platform`, main class `com.cleanlearning.CleanLearningApplication`.
- `application.yml` holds no credentials. It sets `spring.jpa.open-in-view=false`, `spring.jpa.hibernate.ddl-auto=validate` and the Flyway location `classpath:db/migration`.
- `application-poc.yml` points at the docker-compose Postgres (`jdbc:postgresql://localhost:5432/cleanlearning`, user and password `cleanlearning`; dev-only values may be committed, see [security](../../architecture/security.md#configuration-and-secrets)) and adds `classpath:db/seed` to the Flyway locations.

### Packages and names

| Package | Classes in this increment |
|---|---|
| `config` | `SecurityConfig`, `OpenApiConfig`, `RequiredResponseFieldsCustomizer`, `SpaConfig` (the jar serves the SPA, `index.html` for client routes, task 08) |
| `api` | `ApiExceptionHandler` (central `ProblemDetail` mapping) |
| `api.response` | `MoneyResponse`, shared by every feature's responses (public `from(Money)`) |
| `api.shop` | `ShopController` |
| `api.shop.response` | `ShopOfferingResponse`, `ShopOfferingDetailResponse`, `ConsultationOfferingDetailResponse`, `ConsultationPackageResponse`, `EligibleExpertResponse` |
| `application.offering` | `ListShopOfferingsUseCase`, `GetShopOfferingUseCase`, `LoadShopOfferingsPort`, `LoadShopOfferingPort` |
| `domain.shared` | `NotFoundException` |
| `domain.shared.value` | `Money` |
| `domain.offering` | `ShopOffering`, `ShopConsultationOffering`, `ConsultationPackage`, `EligibleExpert` |
| `domain.offering.value` | `OfferingId`, `OfferingType`, `ConsultationPackageId`, `SessionDuration`, `CancellationDeadline` |
| `domain.user.value` | `UserId` |
| `adapters.database.offering` | JPA entities, Spring Data repositories, and `ShopOfferingAdapter`, which implements both ports |

Map only what this increment reads. For example, there's no JPA entity for `app_user` until UC-01 needs one. The adapter may join the user tables to load eligible experts ([D-23](../../decisions.md#architecture)).

### Schema

Migrations in `db/migration`, seed in `db/seed`, numbered as described in [backend.md](../../architecture/backend.md#persistence).

| Migration | Task | Content |
|---|---|---|
| `V1__create_offering.sql` | 06 | `offering`, `consultation_offering`, `consultation_package` |
| `V1_1__seed_offering.sql` | 06 | Career Coaching with its two packages |
| `V2__create_user.sql` | 07 | `app_user`, `user_role`, `expert_profile`, `eligible_expert` |
| `V2_1__seed_user.sql` | 07 | the six users, their roles, two expert profiles, both experts eligible for Career Coaching |

```
offering               id uuid PK, type, name, description, status
consultation_offering  offering_id uuid PK → offering, session_duration_minutes int > 0,
                       cancellation_deadline_hours int >= 0
consultation_package   id uuid PK, consultation_offering_id → consultation_offering,
                       session_count int > 0, price numeric(10,2) >= 0

app_user               id uuid PK, name, email UNIQUE
user_role              user_id → app_user, role, PK (user_id, role)
expert_profile         user_id uuid PK → app_user, display_name, bio
eligible_expert        consultation_offering_id → consultation_offering,
                       expert_id → expert_profile, PK (both)
```

- Every table also has `created_at` and `updated_at` (`timestamptz not null default now()`, D-20).
- `offering.type` is checked against `CONSULTATION`; `TRAINING` is added with trainings. `offering.status` is checked against `DRAFT`, `PUBLISHED` and `ARCHIVED`. `user_role.role` is checked against `CUSTOMER`, `EXPERT` and `ADMIN`.
- Type-specific data lives in a subtype table keyed by the offering id: `consultation_offering` now, `training_offering` later.
- A name column the UI sorts by gets the ICU root collation, `collate "und-x-icu"`, so it sorts alphabetically regardless of case and umlauts, the same on every Postgres ([backend.md](../../architecture/backend.md#persistence)): `offering.name` in 06, `expert_profile.display_name` in 07.
- Not yet: `offering.image_id` and `expert_profile.photo_image_id` (images), `expert_profile.teams_link` (UC-14), the OIDC columns on `app_user` (MVP). Each comes with its use case.

### Seed ids

The seed uses fixed, readable UUIDs. A test class `SeedData` mirrors them.

| Row | Id |
|---|---|
| Customer 1, Customer 2 | `00000000-0000-0000-0000-000000000101`, `…0102` |
| Expert 1, Expert 2 | `00000000-0000-0000-0000-000000000201`, `…0202` |
| Admin 1, Admin 2 | `00000000-0000-0000-0000-000000000301`, `…0302` |
| Career Coaching | `00000000-0000-0000-0000-000000001001` |
| Its packages (3 and 5 sessions) | `00000000-0000-0000-0000-000000001011`, `…1012` |

Names, emails and roles follow the seed table in [UC-01](../../use-cases/uc-01-login.md#business-rules), for example "Customer 1", `customer1@example.com`. Career Coaching follows [backend.md](../../architecture/backend.md#persistence): 60-minute sessions, 24 h deadline, 3 sessions for CHF 450.00 and 5 sessions for CHF 700.00, both experts eligible.

### API

| Route | Response | Task |
|---|---|---|
| `GET /api/shop/offerings` | 200 `ShopOfferingResponse[]`, sorted by name; published offerings with at least one package | 06 |
| `GET /api/shop/offerings/{id}` | 200 `ShopOfferingDetailResponse`; 404 if unknown, not `PUBLISHED` or without packages (like the list, decided 2026-11-08); 400 if the id isn't a UUID | 07 |
| `GET /v3/api-docs` | the OpenAPI document that `backend/openapi.json` is generated from; public (`/v3/api-docs/**` in `SecurityConfig`) | 06 |

```jsonc
// ShopOfferingResponse
{
  "id": "00000000-0000-0000-0000-000000001001",
  "type": "CONSULTATION",
  "name": "Career Coaching",
  "description": "…",
  "price": { "amount": "450.00", "currency": "CHF" }   // cheapest package; the card says "from"
}

// ShopOfferingDetailResponse
{
  "id": "…", "type": "CONSULTATION", "name": "…", "description": "…",
  "consultation": {                                      // trainings will add a "training" part next to it
    "sessionDurationMinutes": 60,
    "cancellationDeadlineHours": 24,
    "packages": [                                        // fewest sessions first
      { "id": "…1011", "sessionCount": 3, "price": { "amount": "450.00", "currency": "CHF" } }
    ],
    "eligibleExperts": [                                 // sorted by display name
      { "id": "…0201", "displayName": "Expert 1", "bio": "…" }
    ]
  }
}
```

- `MoneyResponse.amount` is a string with two decimals ([backend.md](../../architecture/backend.md#rest-conventions)).
- Request and response records have names that are unique across `api`, because the OpenAPI schema name is the simple class name (`LayerRulesTest` checks this, [backend.md](../../architecture/backend.md#rest-conventions)).
- Response fields are **required** in the OpenAPI document unless they're nullable, so the generated TypeScript types aren't all optional. Do this once, with a schema customizer, rather than annotating every field. `RequiredResponseFieldsCustomizer` (in `config`) does it for every schema named `*Response`; request schemas keep what Bean Validation says. A response field that may be null gets JSpecify's `@Nullable`, which springdoc marks nullable, so it stays optional.
- `backend/openapi.json` is pretty-printed, with sorted keys and the fixed server `/`, and regenerated with `./mvnw test -Dtest=OpenApiContractTest -Dopenapi.write=true`.
- **Errors** raised inside Spring MVC are `ProblemDetail` bodies (`application/problem+json`, [backend.md](../../architecture/backend.md#errors)). `ApiExceptionHandler` extends Spring's `ResponseEntityExceptionHandler`, so Spring MVC's own exceptions answer with `ProblemDetail` too: an unknown route 404, a malformed id or request 400. It maps `NotFoundException` (`domain.shared`) to 404. Spring Security's 401 (empty body) and 403 (Boot's error JSON, also for a CSRF rejection), and unexpected 500s (Boot's error JSON), aren't `ProblemDetail`; clients rely only on their status. The error responses aren't in `openapi.json` yet: springdoc leaves them out, and the frontend only needs the status. The task with the first 409 adds one shared `ProblemDetail` schema ([D-34](../../decisions.md#architecture)).

## Frontend

```
frontend/src
├─ app/              router, QueryClient provider, layout with header (product name, "Shop")
├─ api/              schema.d.ts (generated, committed), client.ts (openapi-fetch, same origin)
├─ components/       ImagePlaceholder; ui/: shadcn/ui components, added when used with `npx shadcn@latest add`, then `npm run format` (frontend.md)
├─ lib/              money.ts (formatMoney), api-error.ts (ApiError with the status); `cn` comes from the npm package `cn`
└─ features/shop/    ShopPage, OfferingCard, OfferingDetailPage, queries.ts
```

| Route | Page | Task |
|---|---|---|
| `/` | `ShopPage` | 06 (placeholder in 03) |
| `/offerings/:id` | `OfferingDetailPage` | 07 |
| `*` | `NotFoundPage` | 03 |

- **Scripts:** `dev`, `build`, `preview`, `lint`, `format`, `format:check`, `test` (`vitest run`), `generate:api` (`backend/openapi.json` → `src/api/schema.d.ts`), `check:api` (`openapi-typescript --check`: fail if `schema.d.ts` differs from what `generate:api` would write). Prettier and ESLint skip the generated file, so it stays byte for byte what the generator writes.
- **Query keys:** `['shop', 'offerings']` and `['shop', 'offerings', id]`.
- **API errors:** a query hook throws an `ApiError` that carries the response status, so a page tells a 404 ("not found") apart from other errors (a retry button). The hook decides on the status alone, never on the body, and tests `data === undefined`, not `error`: a 401 has an empty body, and openapi-fetch then returns `error: undefined` ([backend.md](../../architecture/backend.md#errors)). A detail page treats a 400 like a 404: a malformed id in the URL is a missing resource for the visitor. A page that needs a body, from the first 409's `code` on, reads it through the generated `ProblemDetail` type ([D-34](../../decisions.md#architecture)). The `QueryClient`'s default `retry` is `retryUnlessClientError`: a 4xx won't change by asking again, so it shows at once; other errors are retried three times.
- **Money:** `formatMoney({ amount: '1800.00', currency: 'CHF' })` returns `CHF 1,800.00`.
- **Images:** one placeholder component, `components/ImagePlaceholder.tsx`, for offering images and expert photos.

## Tests

| What | How |
|---|---|
| Integration test setup | A meta-annotation `@IntegrationTest`: `@SpringBootTest`, MockMvc, the `poc` profile, and a Testcontainers Postgres through `@ServiceConnection`. One Spring context and one container for the whole run. |
| REST tests | `MockMvcTester`, one happy-path test per route, plus the spec's error scenarios. Extra rows go in with `@Sql` and don't leak into other tests: a second `@Sql` with `executionPhase = AFTER_TEST_METHOD` deletes them (see `ShopControllerTest`). |
| Role matrix | `RoleMatrixTest`: one parameterized test over a single route table (method, path, access). A new route is a new row. |
| Layer rules | `LayerRulesTest` (ArchUnit), following [backend.md](../../architecture/backend.md#layer-rules), plus unique names for request and response records in `api` |
| Seed | `SeedDataTest` checks the six `poc` users and their roles against `SeedData`, since nothing reads users through the API yet |
| SPA in the jar | `SpaFallbackTest`: a client route gets `index.html`, a missing file or API path a 404 (a stub `index.html` in the test resources) |
| Contract | `OpenApiContractTest` compares the generated document with `backend/openapi.json`. On a mismatch it fails and names the regenerate command. |
| Domain | Plain JUnit and AssertJ for `Money`, the "from" price and the value objects |
| Frontend | Vitest for `lib/` logic only ([D-29](../../decisions.md#architecture)) |

## Commands after this increment

Tasks add them to [AGENTS.md](../../../AGENTS.md#commands) as they appear.

| Where | Command | Does |
|---|---|---|
| root | `docker compose up -d` | Postgres on :5432 |
| `backend/` | `./mvnw verify` | all backend tests (needs Docker) |
| `backend/` | `./mvnw spring-boot:run -Dspring-boot.run.profiles=poc` | API on :8080 with seed data |
| `backend/` | `./mvnw test -Dtest=OpenApiContractTest -Dopenapi.write=true` | regenerate `openapi.json` |
| `backend/` | `./mvnw -Pfrontend package` | one jar with the SPA |
| `frontend/` | `npm ci`, then `npm run dev` | SPA on :5173, proxies `/api` to :8080 |
| `frontend/` | `npm run generate:api` | types from `backend/openapi.json` |
