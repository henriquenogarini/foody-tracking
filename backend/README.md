# Foody Tracking API

API REST para rastreamento de pedidos de delivery — teste técnico Foody Delivery.

Cadastro e login com JWT, criação de pedidos, acompanhamento por uma máquina de estados e histórico de cada mudança.
Java 21 + Spring Boot 3.5, persistência em SQLite com Flyway.

![Java](https://img.shields.io/badge/Java-21-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F) ![SQLite](https://img.shields.io/badge/SQLite-Flyway-003B57)

---

## Como rodar

Pré-requisitos: **JDK 21** (o Maven vem no wrapper `./mvnw`) ou **Docker** com Compose v2.

```bash
cd backend
cp .env.example .env          # já vem preenchido para avaliação
```

| Modo | Comando | Perfil |
|---|---|---|
| Local | `./mvnw spring-boot:run` | `dev` — cria o admin e 8 pedidos de demonstração |
| Docker | `docker compose up --build -d` | `prod` — banco no volume `foody-data` |

| Endereço | URL |
|---|---|
| API | `http://localhost:8080/api/v1` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Health | `http://localhost:8080/actuator/health` |

**Acesso rápido:** entre com `admin@foody.local` / `admin-dev-password` em `POST /auth/login`, copie o `accessToken`
e clique em **Authorize** no Swagger. O guia no topo do Swagger mostra o mesmo passo a passo.

> **Windows (PowerShell):** `copy .env.example .env` e `.\mvnw.cmd spring-boot:run`.
> Os horários dos logs seguem o fuso da JVM; no modo local use
> `./mvnw spring-boot:run -Dspring-boot.run.jvmArguments=-Duser.timezone=America/Sao_Paulo`.
> Para recomeçar do zero: `rm -f data/foody.db*` (local) ou `docker compose down -v` (Docker).

---

## Stack

| Área | Escolha |
|---|---|
| Linguagem / framework | Java 21, Spring Boot 3.5 (Web, Validation, Data JPA, Security, OAuth2 Resource Server) |
| Banco | SQLite (`sqlite-jdbc` + dialeto da Hibernate), migrações com Flyway |
| Autenticação | JWT HS256 emitido e validado pelo Spring Security (Nimbus), senhas com BCrypt |
| Mapeamento | MapStruct (DTOs em `record`) |
| Documentação | springdoc-openapi (Swagger UI) |
| Testes | JUnit 5, Mockito, AssertJ, `@WebMvcTest`, `@DataJpaTest`, `@SpringBootTest` |
| Qualidade | JaCoCo (≥ 80% de linhas), SpotBugs, Spotless (google-java-format) |
| Entrega | Dockerfile multi-stage + `compose.yaml` |

---

## Variáveis de ambiente

Lidas do ambiente ou do `.env` (importado pelo `application.yml`). O `.env.example` está organizado por seções.

| Variável | Padrão | Uso |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | Perfil local. A imagem Docker sempre usa `prod` |
| `SERVER_PORT` | `8080` | Porta HTTP (no Docker, só o lado do host) |
| `APP_TIME_ZONE` / `TZ` | `America/Sao_Paulo` | Fuso da aplicação (datas no JSON) / fuso dos logs no container |
| `SPRING_DATASOURCE_URL` | `jdbc:sqlite:./data/foody.db?…` | Arquivo SQLite (WAL, `busy_timeout`, `foreign_keys`) |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:4173` | Origens do frontend (Vite dev e preview) |
| `APP_SECURITY_JWT_SECRET` | dev: chave pública · prod: nenhum | Segredo HS256 em Base64 (≥ 32 bytes). **Obrigatório em `prod`** |
| `APP_SECURITY_ADMIN_EMAIL` / `APP_SECURITY_ADMIN_PASSWORD` | dev: par público · prod: vazio | Único ADMIN, criado na inicialização (os dois ou nenhum) |
| `APP_SECURITY_PASSWORD_RESET_ENABLED` | `true` | Troca de senha de demonstração; `false` remove a rota |

> Os valores do `.env.example` são **públicos, para avaliação**. Em um deploy real, gere outro segredo
> (`openssl rand -base64 48`) e outra senha de admin.

---

## API

Base `/api/v1`, JSON, `Authorization: Bearer <token>`. Contrato completo no Swagger.

| Método | Rota | Acesso | Sucesso |
|---|---|---|---|
| `POST` | `/auth/register` | público | `201` usuário criado (sempre `USER`) |
| `POST` | `/auth/login` | público | `200` token (expira em 2 h) |
| `POST` | `/auth/password-reset` | público | `204` (demonstração, ver abaixo) |
| `GET` | `/auth/me` | autenticado | `200` usuário logado |
| `GET` | `/users` | `ADMIN` | `200` página de usuários |
| `POST` | `/orders` | autenticado | `201` pedido + `Location` |
| `GET` | `/orders?status=&q=&page=&size=&sort=` | autenticado | `200` página de resumos |
| `GET` | `/orders/{id}` | autenticado | `200` pedido |
| `PATCH` | `/orders/{id}/status` | autenticado | `200` pedido com o novo status |
| `GET` | `/orders/{id}/history` | autenticado | `200` histórico de status |

**Escopo:** `USER` vê e altera só os próprios pedidos (pedido de outro usuário → `404`, nunca `403`); `ADMIN` vê todos.
**Busca `q`:** `1042` ou `#1042` busca pelo código; qualquer outro texto busca no nome do cliente, sem diferenciar
maiúsculas.

### Máquina de estados

| De | Para |
|---|---|
| `RECEBIDO` | `EM_PREPARO`, `CANCELADO` |
| `EM_PREPARO` | `SAIU_PARA_ENTREGA`, `CANCELADO` |
| `SAIU_PARA_ENTREGA` | `ENTREGUE` |
| `ENTREGUE`, `CANCELADO` | — (finais) |

Todo pedido nasce em `RECEBIDO`; transição inválida ou para o mesmo status → `409 ORDER_INVALID_TRANSITION`.

### Erros

Todo erro segue o RFC 9457 (`ProblemDetail`) com `code` e `timestamp`, e `errors[]` nas falhas de validação:

```json
{ "status": 409, "title": "Conflict", "code": "ORDER_INVALID_TRANSITION",
  "detail": "Transição de SAIU_PARA_ENTREGA para CANCELADO não é permitida",
  "instance": "/api/v1/orders/…/status", "timestamp": "2026-10-21T14:00:00-03:00" }
```

Códigos: `VALIDATION_ERROR`, `AUTH_INVALID_CREDENTIALS`, `AUTH_TOKEN_INVALID`, `ACCESS_DENIED`, `ORDER_NOT_FOUND`,
`USER_EMAIL_ALREADY_EXISTS`, `ORDER_INVALID_TRANSITION`, `CONCURRENT_MODIFICATION`, `INTERNAL_ERROR`, `NOT_FOUND`,
`METHOD_NOT_ALLOWED`, `NOT_ACCEPTABLE`, `UNSUPPORTED_MEDIA_TYPE`.

---

## Arquitetura

Monólito em camadas, organizado **por módulo e, dentro dele, por responsabilidade**:

```
br.com.foody.tracking
├── config/              Clock, Jackson, JWT, OpenAPI, segurança
├── modules/
│   ├── auth/            controller · dto · entity · enums · helper · mapper · repository · security · service
│   └── order/           controller · dto · entity · enums · event · listener · mapper · repository · service
└── shared/
    ├── exception/       handler · model · type (todas as exceções da aplicação)
    ├── validation/      validadores reutilizáveis
    └── web/             paginação e whitelist de ordenação
```

`controller → service → repository / entity`. Controllers só validam e delegam; services recebem e devolvem DTOs e
mapeiam dentro da transação; entidades não conhecem web nem DTOs; `shared` e `config` não dependem de `modules`.
Classes com `@Transactional`/`@PreAuthorize` não são `final` (o Spring cria proxies por subclasse); as demais são.

| Padrão | Onde |
|---|---|
| Modelo de domínio rico | Regra de transição no `OrderStatus`, mutação no `Order.changeStatus`, criação por `Order.create` |
| CQRS leve | `OrderService` (escrita) e `OrderQueryService` (leitura, `readOnly`, projeção sem N+1) |
| Observer | `OrderStatusChangedEvent` entregue após o commit — ponto de extensão para webhook/fila |
| Repository | Escopo do usuário expresso na própria query, nunca filtrado em memória |
| Template Method | `GlobalExceptionHandler` sobre o `ResponseEntityExceptionHandler` do Spring |
| DTO + Mapper | Records `*DTO` + MapStruct; nenhuma entidade sai do service |

---

## Decisões técnicas

- **SQLite com um único writer:** pool de 1 conexão + WAL + `busy_timeout`; o Flyway é dono do schema (`ddl-auto: none`).
- **Concorrência:** `@Version` no pedido; conflito vira `409 CONCURRENT_MODIFICATION`.
- **Datas:** o banco guarda o instante em UTC; o JSON sai sempre em Brasília (`-03:00`) a partir de `app.time-zone`;
  o `Clock` injetado trabalha em milissegundos, a mesma precisão gravada.
- **Dinheiro:** `BigDecimal` com 2 casas (`HALF_UP`); o total é sempre calculado no servidor. O preço unitário vai até
  99.999,99 para que o maior pedido aceito caiba exato no `NUMERIC` do SQLite.
- **Código do pedido:** sequencial legível (`1001`, `1002`…), gerado dentro da transação de escrita.
- **Usuários:** e-mail normalizado (minúsculas, sem espaços) antes de qualquer consulta, com índice único `NOCASE` como
  última barreira; cadastro sempre cria `USER`.
- **Login:** e-mail inexistente e senha errada respondem o mesmo `401`, com custo de BCrypt equivalente.
- **Mensagens:** locale fixo `pt-BR`, então as mensagens de validação saem sempre em português.
- **Histórico:** cada linha traz `changedBy` (UUID) e `changedByName`, resolvido numa única consulta (sem N+1).
- **Paginação:** ordenação por whitelist em cada endpoint e desempate por `code`, para páginas estáveis.
- **Busca:** o termo é normalizado em Java, mas o `lower()` do SQLite só cobre ASCII: buscar `JOSÉ` ou `josé` encontra
  `José`, porém um nome gravado como `JOSÉ` não é encontrado por `josé`.
- **Troca de senha (demonstração):** sem confirmação por e-mail — quem sabe o e-mail troca a senha de um `USER`;
  o `ADMIN` nunca é alterado e tokens já emitidos valem até expirar. Desligue com
  `APP_SECURITY_PASSWORD_RESET_ENABLED=false`.
- **Docker:** a imagem força o perfil `prod` (`-Dspring.profiles.active=prod`), roda como usuário sem privilégios e
  mantém o Swagger ligado para a avaliação. O SQLite é embarcado, então não há container de banco.
- **Produção:** TLS termina no proxy reverso (`forward-headers-strategy: framework` no `prod`); instância única por
  causa do SQLite. Escalar significa migrar para PostgreSQL (driver + `flyway-database-postgresql`).

<details>
<summary>Perfil PostgreSQL (esboço, fora do build)</summary>

```yaml
# application-postgres.yml
# spring:
#   datasource:
#     url: ${SPRING_DATASOURCE_URL}        # jdbc:postgresql://host:5432/foody
#     username: ${SPRING_DATASOURCE_USERNAME}
#     password: ${SPRING_DATASOURCE_PASSWORD}
#     driver-class-name: org.postgresql.Driver
#     hikari:
#       maximum-pool-size: 10
#   jpa:
#     properties:
#       hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect
```

As migrações precisariam de revisão para PostgreSQL (tipos e o índice `NOCASE`).
</details>

---

## Qualidade

```bash
./mvnw spotless:apply   # formata
./mvnw verify           # testes + JaCoCo (≥ 80%) + SpotBugs + Spotless
```

Testes unitários (matriz completa de status, `Order`, services com Mockito), `@WebMvcTest` por controller
(401/403/ProblemDetail), `@DataJpaTest` em SQLite em memória + Flyway e dois fluxos `@SpringBootTest` que compartilham um
único contexto. Relatório de cobertura em `target/site/jacoco/index.html`. Não há pipeline de CI: o `verify` roda antes de cada commit
(Conventional Commits, uma branch `feature/*` por etapa, PR para a `main`).
