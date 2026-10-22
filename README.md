# Foody Tracking

Sistema simplificado de rastreamento de pedidos de delivery, teste técnico Foody Delivery.

Uma API REST em **Java + Spring Boot** (SQLite) e uma aplicação web em **React**: cadastro e login, criação de pedidos,
lista com o status atual de cada pedido (atualizada a cada 10 s) e acompanhamento das etapas até a entrega.

| Pasta                                                         | O que é                                             | Documentação                                                |
| ------------------------------------------------------------- | --------------------------------------------------- | ----------------------------------------------------------- |
| [`backend/`](backend)                                         | API REST, autenticação JWT, SQLite + Flyway, Docker | [README do backend](backend/README.md)                      |
| [`frontend/foody-tracking-web/`](frontend/foody-tracking-web) | Aplicação React                                     | [README do frontend](frontend/foody-tracking-web/README.md) |
| [`scripts/smoke-test.mjs`](scripts/smoke-test.mjs)            | Smoke test da API contra os requisitos do teste     | abaixo                                                      |
| [`scripts/seed-demo.mjs`](scripts/seed-demo.mjs)              | Popula o banco com usuários e pedidos de exemplo    | abaixo                                                      |

---

## Rodando em 3 passos

Pré-requisitos: **Docker** (com Compose v2) e **Node.js 22.12+**.

**1. API (Docker)**

```bash
cd backend
cp .env.example .env              # já vem preenchido para avaliação
docker compose up --build -d      # http://localhost:8080 — pronto quando o container ficar "healthy"
```

**2. Frontend**

```bash
cd frontend/foody-tracking-web
cp .env.example .env              # VITE_API_URL=http://localhost:8080/api/v1
npm ci
npm run dev                       # http://localhost:5173
```

**3. Navegador:** abra **http://localhost:5173** e entre com `admin@foody.local` / `admin-dev-password`
(ou crie uma conta em **Criar conta**). O banco começa vazio; para testar com dados, veja
[Banco populado](#banco-populado-opcional).

> Use `localhost`, não `127.0.0.1`: o CORS da API libera `http://localhost:5173` (dev) e `http://localhost:4173`
> (`npm run preview`). Swagger da API: http://localhost:8080/swagger-ui.html. Para parar: `docker compose down`
> (`-v` também apaga o banco).

---

## Banco populado (opcional)

O banco começa vazio, para você testar do zero. Se preferir já ver a lista cheia (filtros, contagens, paginação,
linha do tempo), rode o script de exemplo **com a API no ar** (passo 1), a partir da raiz do repositório:

```bash
node scripts/seed-demo.mjs        # API padrão http://localhost:8080/api/v1; passe outra URL como 1º argumento
```

Depois recarregue o navegador. Ele cria **26 pedidos** com clientes, itens e endereços variados, em todos os status,
passando pela própria API (mesmas validações e regras de transição, com histórico real):

| Conta         | E-mail                | Senha                  | O que vê                                                      |
| ------------- | --------------------- | ---------------------- | ------------------------------------------------------------- |
| Usuário comum | `usuario@foody.local` | `usuario-dev-password` | Só os próprios **14 pedidos** (criados pelo script)           |
| Admin         | `admin@foody.local`   | `admin-dev-password`   | Os seus **12 pedidos** e os do usuário comum: **26** no total |

- **Distribuição**: 6 Recebido, 5 Em preparo, 4 Saiu para entrega, 8 Entregue e 3 Cancelado; as duas contas têm
  pedidos em todos os status. Os avanços de status são feitos pelo admin (o restaurante) e os cancelamentos por quem
  criou o pedido, então a linha do tempo mostra quem fez cada etapa.
- **Usuários**: o admin vem do `backend/.env` (criado quando a API sobe; o script lê as credenciais de lá). O usuário
  comum é criado pelo script; sem rodá-lo, essa conta não existe.
- **Rodar de novo** não duplica nada: se o usuário comum já tem pedidos, o script avisa e para. Use
  `node scripts/seed-demo.mjs --force` para criar mais um lote.
- **Voltar ao banco vazio**: `cd backend && docker compose down -v && docker compose up -d`.
- Os horários ficam próximos do momento em que o script roda: pela API não dá para criar um pedido no passado.

Requer Node.js 22+ (o mesmo do frontend) e nenhuma dependência.

---

## Arquivos `.env`

Os dois `.env.example` já funcionam juntos; só copie. Altere apenas se mudar portas ou endereços:

| Arquivo                            | Variável                                 | Valor padrão                                  | Quando mexer                            |
| ---------------------------------- | ---------------------------------------- | --------------------------------------------- | --------------------------------------- |
| `backend/.env`                     | `SERVER_PORT`                            | `8080`                                        | Porta da API no host                    |
| `backend/.env`                     | `APP_CORS_ALLOWED_ORIGINS`               | `http://localhost:5173,http://localhost:4173` | Se o frontend rodar em outro endereço   |
| `backend/.env`                     | `APP_SECURITY_ADMIN_EMAIL` / `_PASSWORD` | `admin@foody.local` / `admin-dev-password`    | Admin criado na subida (o seed usa)     |
| `backend/.env`                     | `APP_SECURITY_JWT_SECRET`                | chave de avaliação                            | Em qualquer ambiente real               |
| `frontend/foody-tracking-web/.env` | `VITE_API_URL`                           | `http://localhost:8080/api/v1`                | Se a API estiver em outra porta ou host |

Se mudar a porta da API, ajuste `VITE_API_URL`; se mudar a porta do frontend, inclua-a em `APP_CORS_ALLOWED_ORIGINS`.

---

## Roteiro de teste no navegador

1. **Criar conta**: na tela de login, **Criar conta** → nome, e-mail e senha (8+ caracteres). Você já entra no app.
2. **Rotas protegidas**: clique em **Conta** → **Sair** e tente abrir `http://localhost:5173/orders`: o app volta ao login.
3. **Criar pedido**: entre de novo → **Novo pedido** → cliente, itens (adicione e remova linhas), endereço com CEP e UF →
   **Criar pedido**. O total é calculado ao vivo e o pedido abre como **Recebido**.
4. **Atualizar status**: no detalhe, avance **Iniciar preparo → Saiu para entrega → Marcar como entregue** e veja a
   linha do tempo; em outro pedido, use **Cancelar pedido** (pede confirmação).
5. **Lista**: em **Pedidos**, veja o status atual de cada pedido, os filtros com contagem, a busca por cliente ou
   código (`#1001`) e a paginação. Abra em duas abas e mude um status: a outra atualiza sozinha em até 10 s.
6. **Celular**: com as ferramentas do navegador em modo dispositivo (375 px), a lista vira cards e as ações ficam na
   barra inferior.

**Smoke test da API** (com a API no ar; cria dois usuários de teste e alguns pedidos):

```bash
node scripts/smoke-test.mjs       # 29 verificações: auth, criar, listar, buscar por ID, status, histórico, isolamento
```

---

## Requisitos do teste

| Requisito                                                                                 | Onde                                                                                                         |
| ----------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| Cadastro (nome, e-mail, senha) e login (e-mail, senha)                                    | `POST /auth/register`, `POST /auth/login` (JWT) · telas de cadastro e login                                  |
| Apenas usuários autenticados acessam o sistema                                            | Todas as rotas exigem token (401 sem ele) · rotas protegidas no React                                        |
| Criar pedido (cliente, itens, endereço)                                                   | `POST /orders` · tela **Novo pedido**                                                                        |
| Atualizar status (`RECEBIDO`, `EM_PREPARO`, `SAIU_PARA_ENTREGA`, `ENTREGUE`, `CANCELADO`) | `PATCH /orders/{id}/status` com máquina de estados (transição inválida → 409) · botões na lista e no detalhe |
| Listar pedidos e buscar por ID                                                            | `GET /orders` (paginado, filtro por status, busca) e `GET /orders/{id}` · lista e detalhe                    |
| Persistência em SQLite                                                                    | SQLite + Flyway; no Docker, o banco fica no volume `foody-data`                                              |
| React: listar pedidos com status atual e criar pedido                                     | Lista com atualização automática e formulário de novo pedido                                                 |
| Git com histórico                                                                         | Conventional Commits, uma branch por etapa e Pull Requests para a `main`                                     |

Além do pedido: histórico de status (`GET /orders/{id}/history`), recuperação de senha de demonstração, cada usuário
vê só os próprios pedidos (o admin vê todos), Swagger, Docker e layout responsivo.

---

## Tecnologias e padrões

**Backend**: Java 21, Spring Boot 3.5 (Web, Validation, Data JPA, Security/OAuth2 Resource Server), SQLite + Flyway,
JWT, MapStruct, springdoc-openapi, JUnit 5 + Mockito, JaCoCo, SpotBugs, Spotless, Docker.

- Monólito em camadas organizado por módulo (`auth`, `order`): `controller → service → repository / entity`.
- Modelo de domínio rico: a máquina de estados vive em `OrderStatus` e `Order.changeStatus`.
- CQRS leve (serviço de escrita e de leitura com projeções), Observer (evento após o commit), DTO + Mapper, erros no
  padrão RFC 7807 (`ProblemDetail`) com código estável.

**Frontend**: React 19, TypeScript 6, Vite 8, MUI 9, TanStack Query 5, React Router 7, react-hook-form + zod, axios,
Vitest + Testing Library + MSW.

- Organização por funcionalidade (`features/auth`, `features/orders`) com dependência em um só sentido
  `app → features → shared`, verificada pelo ESLint.
- Estado do servidor no TanStack Query (cache, polling, invalidação); sessão num Context com `useReducer`.
- Formulários com as mesmas regras de validação da API; erros da API aplicados ao campo certo.

---

## Testes automatizados

```bash
cd backend && ./mvnw verify                                   # testes + cobertura ≥ 80% + análise estática
cd frontend/foody-tracking-web && npm run lint && npm run typecheck && npm run test
```
