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

The SQLite file is created at `./data/foody.db` on the first start and Flyway applies the
migrations automatically.

### Configuration

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | Active profile (`dev`, `test`) |
| `SERVER_PORT` | `8080` | HTTP port |
| `SPRING_DATASOURCE_URL` | `jdbc:sqlite:./data/foody.db?journal_mode=WAL&busy_timeout=5000` | JDBC URL |
| `APP_SECURITY_JWT_SECRET` | dev-only key in `application-dev.yml` | HS256 key, Base64, at least 32 bytes; required outside `dev` |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated allowed origins |

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
   `maximum-pool-size: 1` with WAL journaling and `busy_timeout=5000` prevents
   "database is locked" errors instead of hiding them behind retries.
4. **Configuration through environment variables, with `.env` for local development.**
   Spring Boot's `spring.config.import` loads `.env` without any extra dependency, so the
   same file works for the IDE, Docker and CI.
5. **Small Conventional Commits on `main`** during the initial setup. A branch flow is
   introduced once the core features start.

Entries are appended as new decisions are made.
