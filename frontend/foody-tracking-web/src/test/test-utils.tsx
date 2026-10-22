import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
// RouterProvider from 'react-router' (not 'react-router/dom'): under Vitest the two entry points can
// resolve to different CJS/ESM builds with separate contexts. The /dom variant only adds flushSync.
import { createMemoryRouter, RouterProvider } from 'react-router'
import { routes } from '@/app/router'
import { theme } from '@/app/theme'
import { AuthProvider } from '@/features/auth/AuthProvider'
import { TOKEN_STORAGE_KEY } from '@/features/auth/session'
import type { User } from '@/features/auth/types'
import type { OrderSummary } from '@/features/orders/types'
import type { PageResponse } from '@/shared/api/types'
import { SnackbarProvider } from '@/shared/components/SnackbarProvider'
import { apiUrl, server } from './server'

export const testUser: User = {
  id: '3f6c1a2e-8d4b-4c7a-9e1f-2b5d7a9c0e11',
  name: 'Henrique Nogarini',
  email: 'henrique@restaurante.com',
  role: 'USER',
  createdAt: '2026-10-21T13:00:00-03:00',
}

/** Answers GET /auth/me with `user`, as the API does for a valid token. */
export function mockMe(user: User = testUser) {
  server.use(http.get(apiUrl('/auth/me'), () => HttpResponse.json(user)))
}

let nextCode = 1001
/** An order summary as the API lists it; override what the test cares about. */
export function makeOrder(overrides: Partial<OrderSummary> = {}): OrderSummary {
  const code = overrides.code ?? nextCode++
  return {
    id: `00000000-0000-4000-8000-${String(code).padStart(12, '0')}`,
    code,
    status: 'RECEBIDO',
    customerName: 'Carla Mendes',
    total: 142.7,
    itemsCount: 3,
    neighborhood: 'Moema',
    city: 'São Paulo',
    createdAt: new Date(Date.now() - 5 * 60_000).toISOString(),
    updatedAt: new Date(Date.now() - 60_000).toISOString(),
    ...overrides,
  }
}

/**
 * Fake GET /orders over `orders`, filtering by status and q (name contains / exact code, like the API)
 * and paginating. Returns the received query strings so tests can assert on what was asked.
 */
export function mockOrdersApi(orders: OrderSummary[]) {
  const requests: URLSearchParams[] = []
  server.use(
    http.get(apiUrl('/orders'), ({ request }) => {
      const params = new URL(request.url).searchParams
      requests.push(params)
      const status = params.get('status')
      const q = params.get('q')?.trim().replace(/^#/, '').toLowerCase()
      const page = Number(params.get('page') ?? 0)
      const size = Number(params.get('size') ?? 20)
      const matching = orders.filter(
        (order) =>
          (!status || order.status === status) &&
          (!q || order.customerName.toLowerCase().includes(q) || String(order.code) === q),
      )
      const body: PageResponse<OrderSummary> = {
        content: matching.slice(page * size, page * size + size),
        page,
        size,
        totalElements: matching.length,
        totalPages: Math.ceil(matching.length / size),
      }
      return HttpResponse.json(body)
    }),
  )
  return requests
}

/** Desktop is the default (1024px); call before rendering to get the phone layout, e.g. 375. */
export function setViewportWidth(width: number) {
  window.innerWidth = width
}

interface RenderRouteOptions {
  /** Starts with a stored session for this user, over a fake orders API (empty unless `orders`). */
  signedInAs?: User
  /** `null` skips the fake orders API, for tests that register their own GET /orders. */
  orders?: OrderSummary[] | null
}

/** Renders the real route tree at `path` with fresh providers, as the app does at startup. */
export function renderRoute(path: string, { signedInAs, orders = [] }: RenderRouteOptions = {}) {
  if (signedInAs) {
    sessionStorage.setItem(TOKEN_STORAGE_KEY, 'stored-token')
    mockMe(signedInAs)
    if (orders) mockOrdersApi(orders)
  }
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  const view = render(
    <ThemeProvider theme={theme}>
      <SnackbarProvider>
        <QueryClientProvider client={queryClient}>
          <AuthProvider>
            <RouterProvider router={router} />
          </AuthProvider>
        </QueryClientProvider>
      </SnackbarProvider>
    </ThemeProvider>,
  )
  return { ...view, router, queryClient, user: userEvent.setup() }
}
