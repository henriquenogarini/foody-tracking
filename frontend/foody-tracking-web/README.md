# Foody Tracking Web

Aplicação web para rastreamento de pedidos de delivery, teste técnico Foody Delivery.

Cadastro, login e recuperação de senha; lista de pedidos com status atualizado a cada 10 segundos, filtro, busca e
paginação; criação de pedidos; detalhe com linha do tempo e mudança de status. Consome a API em [`backend/`](../../backend).

![React](https://img.shields.io/badge/React-19-61DAFB) ![TypeScript](https://img.shields.io/badge/TypeScript-6-3178C6) ![Vite](https://img.shields.io/badge/Vite-8-646CFF) ![MUI](https://img.shields.io/badge/MUI-9-007FFF)

---

## Como rodar

Pré-requisitos: **Node.js 22.12+** (o `.nvmrc` fixa o 24 LTS) e a **API rodando** em `http://localhost:8080`.

```bash
# 1. API (em outro terminal)
cd backend
cp .env.example .env
./mvnw spring-boot:run          # perfil dev: cria o admin e 8 pedidos de demonstração

# 2. Frontend
cd frontend/foody-tracking-web
npm ci                          # só na 1ª vez (ou se o package-lock.json mudar)
cp .env.example .env            # VITE_API_URL=http://localhost:8080/api/v1
npm run dev                     # http://localhost:5173
```

**Acesso rápido:** entre com `admin@foody.local` / `admin-dev-password`, ou crie uma conta em **Criar conta**.

> Abra pelo endereço `http://localhost:5173`: é a origem liberada no CORS da API (`127.0.0.1` não funciona).
> Para testar o build de produção: `npm run build && npm run preview` (`http://localhost:4173`, também liberado).

---

## Stack

| Área          | Escolha                                                                               |
| ------------- | ------------------------------------------------------------------------------------- |
| Base          | React 19, TypeScript 6 (`strict`), Vite 8                                             |
| Interface     | MUI 9 (Emotion), tema próprio com os tokens do design (Bricolage Grotesque + Manrope) |
| Dados remotos | TanStack Query 5 (cache, polling de 10 s, invalidação após mutações)                  |
| HTTP          | axios, uma instância com token Bearer e tratamento de 401                             |
| Rotas         | React Router 7, rotas protegidas e de visitante                                       |
| Formulários   | react-hook-form 7 + zod 4 (mesmas regras de validação da API)                         |
| Testes        | Vitest, Testing Library, jsdom e MSW (API simulada)                                   |
| Qualidade     | ESLint (inclui regras de fronteira entre pastas) e Prettier                           |

---

## Funcionalidades

| Tela             | O que faz                                                                                                                                               |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Login / Cadastro | Validação no cliente, sessão salva no `sessionStorage` (termina ao fechar a aba); o cadastro já entra no app                                            |
| Recuperar senha  | E-mail + nova senha, sem confirmação por e-mail (recurso de demonstração da API)                                                                        |
| Pedidos          | Resumo por status, filtros com contagem, busca por cliente ou código (`#1042`), paginação, atualização automática; tabela no desktop e cards no celular |
| Novo pedido      | Cliente, itens dinâmicos (1 a 50), endereço com máscaras de telefone e CEP, total ao vivo; erros da API aparecem no campo certo                         |
| Detalhe          | Itens, cliente, endereço, observações e linha do tempo; avançar status em um clique e cancelar com confirmação                                          |

Sessão expirada (401) leva ao login com aviso e, depois de entrar, de volta à página em que o usuário estava.

---

## Estrutura

Organização por funcionalidade, com dependência em um só sentido: `app` → `features` → `shared`
(verificado pelo ESLint).

```
src/
├── main.tsx
├── app/                 composição: providers, rotas, tema, cache, layout
│   └── layout/          barra superior, barra inferior (celular) e menu da conta
├── features/
│   ├── auth/            sessão, guardas de rota, login, cadastro, recuperar senha
│   │   ├── components/
│   │   └── pages/
│   └── orders/          tipos, regras de status, API, queries, linha do tempo
│       ├── components/
│       └── pages/       lista, novo pedido, detalhe
├── shared/              sem regra de negócio
│   ├── api/             cliente HTTP, token, normalização de erros
│   ├── components/      campos de formulário, cards, diálogo, avisos, estado vazio
│   ├── hooks/
│   └── utils/           moeda, datas, máscaras
└── test/                setup, servidor MSW e utilitários de teste
```

Os testes ficam ao lado do arquivo que testam (`X.test.tsx`).

---

## Scripts

| Comando                              | Uso                                                    |
| ------------------------------------ | ------------------------------------------------------ |
| `npm run dev`                        | Servidor de desenvolvimento em `http://localhost:5173` |
| `npm run build`                      | Checagem de tipos e build de produção em `dist/`       |
| `npm run preview`                    | Serve o build em `http://localhost:4173`               |
| `npm run test`                       | Testes (Vitest)                                        |
| `npm run lint` / `npm run typecheck` | ESLint / TypeScript                                    |
| `npm run format`                     | Prettier                                               |

Antes de cada commit: `npm run lint && npm run typecheck && npm run test && npm run build`.

---

## Variáveis de ambiente

| Variável       | Padrão                         | Uso             |
| -------------- | ------------------------------ | --------------- |
| `VITE_API_URL` | `http://localhost:8080/api/v1` | Endereço da API |

Só o `.env.example` é versionado. Variáveis `VITE_*` entram no build e são públicas por definição.

---

## Decisões técnicas

1. **Sem biblioteca de estado global.** A sessão fica num Context com `useReducer`; o resto é cache do TanStack Query.
2. **Acompanhamento por polling** (10 s, só com a aba visível) em vez de WebSocket, que está fora do escopo.
3. **Contagens e resumo pelos totais da API.** A API não tem endpoint de estatísticas: cada contagem é o
   `totalElements` de uma consulta por status, e o resumo mostra totais gerais.
4. **Erros normalizados.** Todo erro (da API, de rede ou timeout) vira `{ status, code, message, fieldErrors }`;
   os erros de validação da API são aplicados aos campos do formulário.
5. **`sessionStorage` para o token**, para a sessão terminar com a aba. Um 401 só encerra a sessão se veio de uma
   chamada autenticada com o token atual (senha errada no login não desloga ninguém).
6. **Responsivo por breakpoint**: abaixo de 900 px, barra inferior, cards e barras de ação fixas; áreas de toque com
   pelo menos 44 px.
7. **Dependências fixadas**: `@hookform/resolvers` 5.x (a 6.0 tem conflito de dependências opcionais) e
   React Router 7.x; o TypeScript 6 dispensa `baseUrl` no alias `@/`.
