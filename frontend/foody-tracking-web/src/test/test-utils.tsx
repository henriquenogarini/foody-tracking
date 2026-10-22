import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
// Not 'react-router/dom': under Vitest it can resolve to a separate CJS/ESM build with its own context.
import { createMemoryRouter, RouterProvider } from 'react-router'
import { routes } from '@/app/router'
import { theme } from '@/app/theme'
import { AuthProvider } from '@/features/auth/AuthProvider'
import { TOKEN_STORAGE_KEY } from '@/features/auth/session'
import type { User } from '@/features/auth/types'
import type { Order, OrderStatus, OrderSummary, StatusHistoryEntry } from '@/features/orders/types'
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

export function mockMe(user: User = testUser) {
  server.use(http.get(apiUrl('/auth/me'), () => HttpResponse.json(user)))
}

let nextCode = 1001
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

const PATH: OrderStatus[] = ['RECEBIDO', 'EM_PREPARO', 'SAIU_PARA_ENTREGA', 'ENTREGUE']

export function toOrder(summary: OrderSummary): Order {
  return {
    id: summary.id,
    code: summary.code,
    status: summary.status,
    customer: { name: summary.customerName, phone: '+5511987521144', email: null },
    items: [
      {
        description: 'Pizza calabresa G',
        quantity: summary.itemsCount,
        unitPrice: summary.total / summary.itemsCount,
        subtotal: summary.total,
      },
    ],
    deliveryAddress: {
      street: 'Rua José Neves',
      number: '590',
      complement: null,
      neighborhood: summary.neighborhood,
      city: summary.city,
      state: 'SP',
      postalCode: '04650-140',
      lat: null,
      lng: null,
    },
    notes: null,
    total: summary.total,
    createdAt: summary.createdAt,
    updatedAt: summary.updatedAt,
  }
}

export function toHistory(summary: OrderSummary): StatusHistoryEntry[] {
  const steps =
    summary.status === 'CANCELADO'
      ? (['RECEBIDO', 'CANCELADO'] as OrderStatus[])
      : PATH.slice(0, PATH.indexOf(summary.status) + 1)
  return steps.map((toStatus, index) => ({
    fromStatus: index === 0 ? null : steps[index - 1],
    toStatus,
    changedAt: summary.createdAt,
    changedBy: testUser.id,
    changedByName: testUser.name,
  }))
}

/** Fake GET /orders; returns the received query strings for assertions. */
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
    http.get(apiUrl('/orders/:id'), ({ params }) => {
      const order = orders.find((o) => o.id === params.id)
      return order
        ? HttpResponse.json(toOrder(order))
        : HttpResponse.json(
            { status: 404, code: 'ORDER_NOT_FOUND', detail: 'Pedido não encontrado.' },
            { status: 404 },
          )
    }),
    http.get(apiUrl('/orders/:id/history'), ({ params }) => {
      const order = orders.find((o) => o.id === params.id)
      return order
        ? HttpResponse.json(toHistory(order))
        : HttpResponse.json({ status: 404, code: 'ORDER_NOT_FOUND', detail: 'x' }, { status: 404 })
    }),
  )
  return requests
}

/** Call before rendering; the default is 1024px (desktop). */
export function setViewportWidth(width: number) {
  window.innerWidth = width
}

interface RenderRouteOptions {
  signedInAs?: User
  /** `null` skips the fake orders API, for tests that register their own. */
  orders?: OrderSummary[] | null
}

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
