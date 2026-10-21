# foody-tracking-api

Delivery order tracking API — Foody Delivery technical test.

A REST API in Java 21 + Spring Boot 3.5 that registers users, authenticates them with JWT,
creates delivery orders and tracks each order through a fixed status state machine
(`RECEBIDO` → `EM_PREPARO` → `SAIU_PARA_ENTREGA` → `ENTREGUE`, with `CANCELADO` allowed
before dispatch). Data is persisted in SQLite, with the schema managed by Flyway.

## Stack

| Concern | Choice |
|---|---|
| Language / runtime | Java 21 (LTS) |
| Framework | Spring Boot 3.5 — Web, Validation, Data JPA, Security + OAuth2 Resource Server |
| Database | SQLite (`sqlite-jdbc`) + Hibernate community dialect; migrations with Flyway |
| API docs | springdoc-openapi (Swagger UI) |
| Tests | JUnit 5, Mockito, AssertJ, Spring test slices, spring-security-test |
| Quality gate | JaCoCo (≥ 80% line coverage), SpotBugs (fails on high-priority bugs), Spotless (google-java-format) |

## Requirements

- JDK 21. Maven is not required: the project ships the Maven Wrapper
  (`./mvnw`, or `mvnw.cmd` on Windows PowerShell).

## Getting started

```bash
cp .env.example .env     # then set APP_SECURITY_JWT_SECRET (e.g. openssl rand -base64 48)
./mvnw spring-boot:run
```

- API base path: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

The `dev` profile bootstraps a dev-only admin (`admin@foody.local` / `admin-dev-password`,
overridable through `APP_SECURITY_ADMIN_EMAIL` / `APP_SECURITY_ADMIN_PASSWORD`). In Swagger UI,
register or use that admin, call `POST /api/v1/auth/login`, then paste the `accessToken` into
the **Authorize** button; the guide at the top of Swagger UI walks through the same steps.

Log timestamps use the JVM zone, so local runs should set it to the application zone:
`TZ=America/Sao_Paulo ./mvnw spring-boot:run`, or
`./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Duser.timezone=America/Sao_Paulo"`.

The SQLite file is created at `./data/foody.db` on the first start and Flyway applies the
migrations automatically.

### Configuration

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | Active profile (`dev`, `test`) |
| `SERVER_PORT` | `8080` | HTTP port |
| `SPRING_DATASOURCE_URL` | `jdbc:sqlite:./data/foody.db?journal_mode=WAL&busy_timeout=5000&foreign_keys=true` | JDBC URL |
| `APP_SECURITY_JWT_SECRET` | dev-only key in `application-dev.yml` | HS256 key, Base64, at least 32 bytes; required outside `dev` |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated allowed origins |
| `APP_SECURITY_ADMIN_EMAIL` | none (`dev` ships a dev-only pair) | E-mail of the single bootstrap `ADMIN`; optional, set both admin variables or neither |
| `APP_SECURITY_ADMIN_PASSWORD` | none (`dev` ships a dev-only pair) | Password of the bootstrap `ADMIN` (8+ characters, at most 72 bytes); optional, set both or neither |
| `APP_TIME_ZONE` | `America/Sao_Paulo` | Application zone id: drives the `Clock` and the zone of every JSON date |
| `TZ` | system zone | Process zone used by log timestamps (Docker `--env-file`, compose `env_file`, shell); a `.env` line does not change the JVM zone of IDE or `mvnw` runs. Keep it equal to `APP_TIME_ZONE` |

Variables can be real environment variables (IDE run configuration, Docker `--env-file`, CI)
or lines in a local `.env` file: `application.yml` imports it with
`spring.config.import=optional:file:.env[.properties]`. `.env` is gitignored; `.env.example`
is versioned with names only.

## Build and quality gate

```bash
./mvnw spotless:apply    # format the code
./mvnw verify            # tests + JaCoCo + SpotBugs + Spotless check
```

Coverage report: `target/site/jacoco/index.html`.

## Project structure

The code is organized by feature package. Packages are added incrementally; this is the
target layout.

```
br.com.foody.tracking
├── config/      security, JWT, OpenAPI and Jackson configuration
├── shared/      error model (RFC 9457 ProblemDetail) and pagination response
├── auth/        registration, login, current user      (api → application → domain)
└── order/       orders, items, status state machine    (api → application → domain)
```

## Architectural decisions

1. **Layered monolith organized by feature.** Each feature owns its `api`, `application`
   and `domain` layers. Dependencies always point `api → application → domain`, and
   `domain` knows nothing about Spring Web or DTOs.
2. **Flyway owns the schema (`ddl-auto: none`).** SQLite's loose column typing makes
   Hibernate's `validate` unreliable, so the migrations are the single source of truth and
   repository tests prove the entity mapping.
3. **Single-connection pool for SQLite.** SQLite allows one writer at a time.
   `maximum-pool-size: 1` with WAL journaling and `busy_timeout=5000&foreign_keys=true` prevents
   "database is locked" errors instead of hiding them behind retries.
4. **Configuration through environment variables, with `.env` for local development.**
   Spring Boot's `spring.config.import` loads `.env` without any extra dependency, so the
   same file works for the IDE, Docker and CI.
5. **Small Conventional Commits on `main`** during the initial setup. A branch flow is
   introduced once the core features start.
6. **Error handling extends `ResponseEntityExceptionHandler`, with `createResponseEntity` as the
   single funnel.** The framework keeps handling its own errors (unknown route, 405, 415...) and
   every body passes through one hook that stamps `timestamp`. Those framework-built errors carry
   no `code`: the nine codes of the API contract are the contract and do not grow for them.
7. **`ApiErrorCode` holds an `int` status, not `HttpStatus`.** The enum reaches `domain` through
   `DomainException`, so it must not pull Spring Web into the domain layer.
8. **Fixed `pt-BR` locale and translated framework error details.** `spring.web.locale-resolver:
   fixed` pins the locale regardless of `Accept-Language`, and `messages.properties` translates the
   framework-built `detail`s; the same fixed locale makes Bean Validation default messages
   Portuguese without a `message=` on every annotation.
9. **MapStruct mappers, one per feature.** Mapping code is generated at compile time (no
   reflection) and type-checked by the compiler. Derived fields (`id` from `publicId`, and the
   like) are explicit `@Mapping`s, and every mapper has a unit test through `Mappers.getMapper`
   because the generated classes count toward coverage.
10. **Application time zone `America/Sao_Paulo`, declared once.** `app.time-zone` feeds a
    `ZoneId` bean, the injected `Clock` and Jackson, so services and JSON agree on Brasília time
    (`-03:00`). The database stores UTC instants; the zone is presentation. A zone id, never a
    fixed offset, so tzdata handles daylight saving if it returns.
11. **JWT HS256 through Spring's resource server and Nimbus.** No third-party JWT library; the
    algorithm is pinned on both sides and the secret is validated at startup. The signed `role`
    claim is trusted for the 2h token life (no per-request lookup on a single SQLite
    connection), and a stale bearer header is ignored on login/register.
12. **Two roles, `USER` and `ADMIN`.** Registration always yields `USER`; the single `ADMIN` is
    bootstrapped from environment variables and an existing account is never promoted.
    Admin-only endpoints use `@PreAuthorize`; the data scope (own vs. all orders) is decided in
    the service and expressed in the repository query.
13. **Login hardening.** Unknown e-mail and wrong password return the same 401, and the unknown
    e-mail path still runs one BCrypt match against a dummy hash, so timing does not reveal
    accounts. Passwords are capped at 72 bytes, BCrypt's input limit. `login` runs outside a
    transaction, so BCrypt and JWT signing do not hold the single SQLite connection (the lookup
    uses the repository's own read-only transaction). `register` keeps the hash inside its
    transaction: the exists-check and the insert must be atomic, and the accepted cost is about
    100 ms of the single connection per registration.
14. **Swagger documents errors through `ApiErrorResponse`.** springdoc cannot see the contract
    extensions stored in `ProblemDetail.properties` (`code`, `timestamp`, `errors`), so a
    documentation-only record mirrors the full error body and every error response references it.
15. **E-mail uniqueness: service pre-check first, index last.** SQLite surfaces a violation of
    the case-insensitive unique index as a generic `JpaSystemException`, not
    `DataIntegrityViolationException`, so it cannot be mapped reliably. The service checks inside
    the transaction (pool size 1 serializes writers) and the index stays as the last guard.
16. **Client `sort` is whitelisted per endpoint.** Spring Data would otherwise accept any entity
    path from `?sort=` (e.g. `passwordHash`, or `owner.passwordHash` across a join), leaking data
    through ordering. Each query service checks the requested properties with `SortWhitelist`;
    a disallowed property answers the same 400 `VALIDATION_ERROR` body as an unknown one.

17. **Order status state machine lives in the domain.** `OrderStatus.canTransitionTo` owns the
    matrix and `Order.changeStatus` is the only mutation; the schema guards only what never
    changes (no same-status row, birth in `RECEBIDO`), so a matrix change never needs a migration.

    | From | Allowed targets |
    |---|---|
    | `RECEBIDO` | `EM_PREPARO`, `CANCELADO` |
    | `EM_PREPARO` | `SAIU_PARA_ENTREGA`, `CANCELADO` |
    | `SAIU_PARA_ENTREGA` | `ENTREGUE` |
    | `ENTREGUE`, `CANCELADO` | none (terminal) |

    A transition to the same status is invalid (409). Every order is born in `RECEBIDO` through
    `Order.create`, which also computes the total and writes the first history row; time is
    passed in by the caller, never read inside the domain.
18. **The paged order list reads a projection, not entities.** `OrderSummaryView` selects the
    flat columns plus `size(o.items)` as `itemsCount`, so one page costs exactly two statements
    (page + count), with no N+1 and no fetch join on a paged query. SQLite drops trailing zeros
    from `NUMERIC`, so money getters (and the mapper, for the projection) re-apply scale 2.

19. **Owner scope by the user's public id.** The authenticated principal carries only the
    user's `publicId` (the JWT `sub`), so the owner-scoped queries filter on
    `o.owner.publicId` (`findByPublicIdAndOwnerPublicId`, `findSummariesByOwnerPublicId...`)
    instead of the internal `owner_id` named in the original plan: one statement, backed by the
    unique `users.public_id` index, and no extra user lookup on every read.

20. **Role scope is chosen in the service, expressed in the query.** `USER` reads and writes go
    through the owner-scoped repository methods and `ADMIN` through the unscoped ones; another
    user's order answers 404 `ORDER_NOT_FOUND`, never 403, so its existence is not revealed.
    One package-private `OrderFinder` holds that choice for both services, and the controller
    only turns the JWT into `AuthenticatedUser` and delegates.
21. **Order code = `COALESCE(MAX(code), 1000) + 1` inside the write transaction.** SQLite allows
    one `AUTOINCREMENT` per table (taken by `id`), and the single-connection pool serializes
    writers, so two orders cannot compute the same code; `uq_orders_code` is the last guard.
22. **Status changes publish `OrderStatusChangedEvent`, delivered after commit.** The
    `@TransactionalEventListener(AFTER_COMMIT)` listener only logs today; it is the extension
    point for a webhook or queue, and a rolled-back change notifies no one. The service flushes
    before publishing, so a stale `@Version` fails inside the request as 409
    `CONCURRENT_MODIFICATION` instead of at commit time.
23. **Commands and queries are separate services.** `OrderService` (writes, events, locking) and
    `OrderQueryService` (`readOnly`, projections for the list) have different transaction
    semantics and fetch shapes; each stays small.

Entries are appended as new decisions are made.
