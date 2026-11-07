# 02 Backend skeleton

| | |
|---|---|
| Increment | [01 Shop tracer bullet](../../specs/01-shop-tracer-bullet.md) |
| Type | AFK |
| Blocked by | [01](01-cloud-environment.md) |
| Status | Todo |

## Goal

A Spring Boot application that starts against Postgres, plus a test setup that later tasks copy: `./mvnw verify` runs an integration test against a Testcontainers Postgres and the ArchUnit layer rules.

## What to build

Read first: [backend.md](../../architecture/backend.md), and the plan's [toolchain](README.md#toolchain) and [backend](README.md#backend) sections.

- **`docker-compose.yml`** at the repository root, with a single `postgres` service: `postgres:18-alpine`, port 5432, database, user and password `cleanlearning`, and a named volume. Keycloak comes with OIDC in the MVP.
- **`backend/`:** a Maven project with the wrapper, Java 25, and the coordinates and configuration from the plan. Add only the dependencies needed now: Web MVC, Data JPA, Flyway with Postgres support, and the PostgreSQL driver. For tests: Spring Boot test, Testcontainers Postgres and ArchUnit. Every test runs in `./mvnw verify`.
- **Layer packages:** `config`, `api`, `application`, `domain` and `adapters.database` under `com.cleanlearning`, each with a `package-info.java` that says in one sentence what belongs there.
- **`LayerRulesTest` (ArchUnit):**
  - the dependency table in [backend.md](../../architecture/backend.md#layer-rules); empty layers are allowed;
  - `domain` depends on no Spring, Jakarta Persistence or Hibernate class;
  - `application` depends on no Jakarta Persistence class;
  - `@Entity` classes live only in `adapters.database`;
  - `@Service` classes in `application` end in `Service`, and interfaces in `application` end in `Port`.
- **Test setup** from the plan's [tests](README.md#tests) section: a `TestcontainersConfiguration` with `postgres:18-alpine` through `@ServiceConnection`, the `@IntegrationTest` meta-annotation, and one test that starts the application context.
- **Docs:** in [AGENTS.md](../../../AGENTS.md), replace "documentation only" in **Status** and add the backend commands. In the root `README.md`, update the status. Add a `.gitignore` for Maven and IDE files.

## Acceptance criteria

- [ ] `docker compose up -d` starts Postgres 18 on port 5432.
- [ ] `./mvnw verify` passes. It runs the context test against a Testcontainers Postgres, and `LayerRulesTest`.
- [ ] `./mvnw spring-boot:run -Dspring-boot.run.profiles=poc` starts against the docker-compose Postgres, and Flyway runs.
- [ ] Each ArchUnit rule has been seen failing once on a deliberate violation, which was not committed.
- [ ] No credentials outside `application-poc.yml`.
- [ ] AGENTS.md has the status and the backend commands.

## Tests

- `LayerRulesTest`
- one `@IntegrationTest` that starts the application context

## Out of scope

- Spring Security ([04](04-security-baseline.md)), springdoc and the first tables ([06](06-shop-list.md)).
- The `Clock` bean and Bean Validation: added by the first task that uses them.

## Notes
