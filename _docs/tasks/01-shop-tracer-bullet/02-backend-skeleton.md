# 02 Backend skeleton

| | |
|---|---|
| Increment | [01 Shop tracer bullet](../../specs/01-shop-tracer-bullet.md) |
| Type | AFK |
| Blocked by | [01](01-cloud-environment.md) |
| Status | Done |

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

- [x] `docker compose up -d` starts Postgres 18 on port 5432.
- [x] `./mvnw verify` passes. It runs the context test against a Testcontainers Postgres, and `LayerRulesTest`.
- [x] `./mvnw spring-boot:run -Dspring-boot.run.profiles=poc` starts against the docker-compose Postgres, and Flyway runs.
- [x] Each ArchUnit rule has been seen failing once on a deliberate violation, which was not committed.
- [x] No credentials outside `application-poc.yml`.
- [x] AGENTS.md has the status and the backend commands.

## Tests

- `LayerRulesTest`
- one `@IntegrationTest` that starts the application context

## Out of scope

- Spring Security ([04](04-security-baseline.md)), springdoc and the first tables ([06](06-shop-list.md)).
- The `Clock` bean and Bean Validation: added by the first task that uses them.

## Notes

- **Versions:** Spring Boot 4.1.2 (latest GA on 2026-11-07; 4.2.0 is at RC2), which manages Testcontainers 2.0.5, Flyway 12.4.0, Hibernate 7.4, JUnit Jupiter 6.0.3 and PostgreSQL JDBC 42.7.14. ArchUnit 1.5.1. Maven wrapper 3.3.4 (script only, no jar) with Maven 3.9.11.
- **Test dependencies:** besides `spring-boot-starter-test`, Boot 4 needs `spring-boot-starter-webmvc-test` for `@AutoConfigureMockMvc` (now in `org.springframework.boot.webmvc.test.autoconfigure`) and `spring-boot-testcontainers` for `@ServiceConnection`.
- **`LayerRulesTest`:** every rule uses `allowEmptyShould(true)` and the layered rule `withOptionalLayers(true)`, so empty layers pass. `config` may use every layer and is used by none. The `Port` naming rule skips `package-info`, which compiles to a synthetic interface. Test classes aren't analyzed. The domain rule is an allow-list, as in [backend.md](../../architecture/backend.md#layer-rules): `domain` depends only on `java..` and `domain..`, which also covers "no Spring, Jakarta Persistence or Hibernate" (review: it caught a domain class using `org.slf4j.Logger`, which the earlier deny-list missed; records with primitives, arrays, enums and lambdas pass).
- **Deliberate violations** (temporary classes, deleted again): a domain class using an application interface, `api` and `adapters.database` using each other, and an application class using `api` (layer rule, 4 violations); domain classes using Spring, Hibernate and `@Entity` (domain rule); an application class with an `EntityManager` (JPA rule); an `@Entity` in `domain`; an `@Service` named `Booking`; an interface named `Helper` in `application`. All six rules failed.
- **Credentials:** besides `application-poc.yml`, the dev-only database password is in `docker-compose.yml`, which the task requires and [security](../../architecture/security.md#configuration-and-secrets) allows. Testcontainers uses its own defaults.
- **Flyway:** with no migrations yet, Flyway creates `flyway_schema_history` and logs "No migrations found"; the `db/migration` and `db/seed` folders come with task 06.
