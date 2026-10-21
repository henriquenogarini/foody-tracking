# foody-tracking-web

Web client for the Foody Tracking API — Foody Delivery technical test.

A React single-page app where an authenticated user registers delivery orders, follows each one through the
status state machine (`RECEBIDO` → `EM_PREPARO` → `SAIU_PARA_ENTREGA` → `ENTREGUE`, with `CANCELADO` allowed
before dispatch) and sees the list refresh on its own while orders move.

This project lives in `frontend/foody-tracking-web/` of the `foody-tracking` repository; the API it consumes lives
in `backend/`. Every command below runs from this folder.

## Stack

| Concern            | Choice                                              |
| ------------------ | --------------------------------------------------- |
| UI runtime         | React 19 + TypeScript 6 (`strict`)                  |
| Build / dev server | Vite 8                                              |
| Components         | MUI 9 (Emotion)                                     |
| Server state       | TanStack Query 5                                    |
| HTTP               | axios (single instance with interceptors)           |
| Routing            | React Router 7                                      |
| Forms              | react-hook-form 7 + zod 4                           |
| Tests              | Vitest + Testing Library + jsdom; MSW for API mocks |
| Quality            | ESLint (flat config) + Prettier                     |

## Requirements

- Node.js 22.12+ (`.nvmrc` pins 24 LTS) and npm.
- The API running locally on `http://localhost:8080` (see `backend/README.md`; `./mvnw spring-boot:run` with the
  default `dev` profile is enough, no Docker needed).

## Getting started

```bash
npm ci
cp .env.example .env   # VITE_API_URL=http://localhost:8080/api/v1
npm run dev            # http://localhost:5173
```

The dev server is pinned to `localhost:5173` with `strictPort`, because that is the only origin the API's CORS
policy allows by default. Opening the app through `127.0.0.1` will fail CORS.

## Scripts

| Script                    | What it does                                          |
| ------------------------- | ----------------------------------------------------- |
| `dev`                     | Vite dev server with HMR                              |
| `build`                   | Type-check (`tsc -b`) and production build to `dist/` |
| `preview`                 | Serve the production build locally                    |
| `lint` / `lint:fix`       | ESLint                                                |
| `format` / `format:check` | Prettier                                              |
| `typecheck`               | `tsc -b --noEmit`                                     |
| `test` / `test:watch`     | Vitest (single run / watch)                           |

Quality gate before every commit: `npm run lint && npm run typecheck && npm run test && npm run build`.

## Configuration

| Variable       | Default                        | Purpose             |
| -------------- | ------------------------------ | ------------------- |
| `VITE_API_URL` | `http://localhost:8080/api/v1` | Base URL of the API |

Only `.env.example` is versioned. Vite inlines `VITE_*` values at build time, so they are public by design.

## Architectural decisions

1. **Path alias `@/` → `src/` without `baseUrl`.** TypeScript 6 deprecates `baseUrl`; `paths` resolves relative to
   the tsconfig on its own, and `vite.config.ts` mirrors the alias.
2. **`@hookform/resolvers` pinned to 5.x.** 6.0 ships a self-conflicting optional peer tree (`effect` 3 vs 4) that
   only installs with `--legacy-peer-deps`; 5.x already supports zod 4.
3. **React Router pinned to 7.x**, the major this project was designed against.
4. **No CI pipeline**, matching the backend: the quality gate above runs locally before every commit.
5. **The design is the visual reference, the API is the boundary.** Where the mockups in `design/` show data the API does
   not provide, the UI adapts instead of faking it: status counts and summary cards come from `totalElements` of
   per-status queries (all-time totals, no "today" metrics), and the search box, dark-mode toggle, "remember me" and
   "forgot password" are left out.
