import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { OrderStatus } from '@/features/orders/types'
import { apiUrl, server } from '@/test/server'
import {
  makeOrder,
  mockOrdersApi,
  renderRoute,
  setViewportWidth,
  testUser,
} from '@/test/test-utils'

const seed = () => [
  makeOrder({ code: 1047, customerName: 'Carla Mendes', status: 'RECEBIDO' }),
  makeOrder({
    code: 1045,
    customerName: 'José da Silva',
    status: 'EM_PREPARO',
    total: 89.8,
    itemsCount: 2,
    neighborhood: 'Vila São Paulo',
  }),
  makeOrder({ code: 1043, customerName: 'Marcos Vinícius', status: 'SAIU_PARA_ENTREGA' }),
  makeOrder({ code: 1042, customerName: 'Mariana Costa', status: 'ENTREGUE', itemsCount: 1 }),
  makeOrder({ code: 1040, customerName: 'Luciana Prado', status: 'CANCELADO' }),
]

async function openList(path = '/orders', list = orders) {
  const view = renderRoute(path, { signedInAs: testUser, orders: list })
  await screen.findByRole('heading', { level: 1, name: 'Pedidos' })
  return view
}

const rowOf = async (code: number) =>
  (await screen.findByRole('link', { name: `#${code}` })).closest('tr') as HTMLElement

/** PATCH /orders/:id/status over `list`: applies the change (so refetches see it) or answers 409. */
function mockStatusChange(list = orders, outcome: 'ok' | 'conflict' = 'ok') {
  const received: { id: string; body: unknown }[] = []
  server.use(
    http.patch(apiUrl('/orders/:id/status'), async ({ params, request }) => {
      const body = (await request.json()) as { status: OrderStatus }
      received.push({ id: String(params.id), body })
      if (outcome === 'conflict') {
        return HttpResponse.json(
          {
            status: 409,
            code: 'ORDER_INVALID_TRANSITION',
            detail: 'Transição de ENTREGUE para EM_PREPARO não é permitida',
          },
          { status: 409 },
        )
      }
      const order = list.find((o) => o.id === params.id)!
      order.status = body.status
      return HttpResponse.json({ ...order })
    }),
  )
  return received
}

let orders = seed()

describe('OrdersListPage', () => {
  beforeEach(() => {
    orders = seed()
  })

  it('lists orders with the design columns', async () => {
    await openList()
    const row = within(await rowOf(1045))

    expect(row.getByText('José da Silva')).toBeInTheDocument()
    expect(row.getByText('Vila São Paulo · São Paulo')).toBeInTheDocument()
    expect(row.getByText('2 itens')).toBeInTheDocument()
    expect(row.getByText('R$ 89,80')).toBeInTheDocument()
    expect(row.getByText('Em preparo')).toBeInTheDocument()
    expect(row.getByRole('button', { name: 'Saiu para entrega' })).toBeInTheDocument()
    // Finished orders offer no action.
    expect(within(await rowOf(1042)).queryByRole('button')).not.toBeInTheDocument()
    expect(screen.getByText('Mostrando 1–5 de 5 pedidos')).toBeInTheDocument()
  })

  it('shows per-status counts in the filters and summary', async () => {
    await openList()
    const filters = within(screen.getByRole('group', { name: 'Filtrar por status' }))

    expect(await filters.findByRole('button', { name: 'Todos, 5 pedidos' })).toHaveAttribute(
      'aria-pressed',
      'true',
    )
    expect(filters.getByRole('button', { name: 'Recebido, 1 pedido' })).toBeInTheDocument()
    const summary = within(screen.getByRole('region', { name: 'Resumo dos pedidos' }))
    expect(summary.getByText('Em andamento').parentElement).toHaveTextContent('2')
    expect(summary.getByText('20% dos pedidos')).toBeInTheDocument()
  })

  it('filters by status through the URL', async () => {
    const { user, router } = await openList()
    await user.click(await screen.findByRole('button', { name: 'Em preparo, 1 pedido' }))

    await waitFor(() =>
      expect(screen.queryByRole('link', { name: '#1047' })).not.toBeInTheDocument(),
    )
    expect(screen.getByRole('link', { name: '#1045' })).toBeInTheDocument()
    expect(router.state.location.search).toBe('?status=EM_PREPARO')
  })

  it('searches by customer or code after a short pause', async () => {
    const { user, router } = await openList()
    await user.type(screen.getByRole('searchbox', { name: 'Buscar pedidos' }), '#1043')

    await waitFor(() => expect(router.state.location.search).toBe('?q=%231043'))
    // The previous page stays on screen until the filtered one arrives.
    await waitFor(() => expect(screen.queryByText('Carla Mendes')).not.toBeInTheDocument())
    expect(screen.getByText('Marcos Vinícius')).toBeInTheDocument()
  })

  it('paginates ten orders per page', async () => {
    const many = Array.from({ length: 12 }, (_, i) => makeOrder({ code: 2000 + i }))
    const { user, router } = await openList('/orders', many)
    expect(await screen.findByText('Mostrando 1–10 de 12 pedidos')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Página 2' }))
    expect(await screen.findByText('Mostrando 11–12 de 12 pedidos')).toBeInTheDocument()
    expect(router.state.location.search).toBe('?page=2')
  })

  it('invites to create the first order when there are none', async () => {
    await openList('/orders', [])
    expect(await screen.findByRole('heading', { name: 'Nenhum pedido ainda' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Criar primeiro pedido' })).toHaveAttribute(
      'href',
      '/orders/new',
    )
  })

  it('offers to clear filters that match nothing', async () => {
    const { user } = await openList('/orders?q=ninguem')
    expect(
      await screen.findByRole('heading', { name: 'Nenhum pedido encontrado' }),
    ).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Limpar filtros' }))
    expect(await screen.findByRole('link', { name: '#1047' })).toBeInTheDocument()
  })

  it('shows the error with a retry when the list cannot load', async () => {
    server.use(http.get(apiUrl('/orders'), () => HttpResponse.json({}, { status: 503 })))
    const { user } = renderRoute('/orders', { signedInAs: testUser, orders: null })

    expect(await screen.findByText('Erro interno. Tente novamente mais tarde.')).toBeInTheDocument()

    mockOrdersApi(orders)
    await user.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(await screen.findByRole('link', { name: '#1042' })).toBeInTheDocument()
  })

  it('moves an order to its next status and refreshes list and counts', async () => {
    const received = mockStatusChange()
    const { user } = await openList()

    await user.click(within(await rowOf(1047)).getByRole('button', { name: 'Iniciar preparo' }))

    expect(await screen.findByText('Pedido #1047: Em preparo')).toBeInTheDocument()
    expect(received).toEqual([{ id: orders[0].id, body: { status: 'EM_PREPARO' } }])
    expect(await screen.findByRole('button', { name: 'Em preparo, 2 pedidos' })).toBeInTheDocument()
    expect(
      within(await rowOf(1047)).getByRole('button', { name: 'Saiu para entrega' }),
    ).toBeInTheDocument()
  })

  it('cancels only after confirmation', async () => {
    const received = mockStatusChange()
    const { user } = await openList()

    await user.click(
      within(await rowOf(1047)).getByRole('button', { name: 'Mais ações do pedido #1047' }),
    )
    await user.click(await screen.findByRole('menuitem', { name: 'Cancelar pedido' }))
    const dialog = await screen.findByRole('dialog', { name: 'Cancelar o pedido #1047?' })
    expect(received).toHaveLength(0)

    await user.click(within(dialog).getByRole('button', { name: 'Cancelar pedido' }))
    expect(await screen.findByText('Pedido #1047: Cancelado')).toBeInTheDocument()
    expect(received).toEqual([{ id: orders[0].id, body: { status: 'CANCELADO' } }])
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  })

  it('explains a conflicting change and refreshes the list', async () => {
    mockStatusChange(orders, 'conflict')
    const { user } = await openList()
    const requests = mockOrdersApi(orders)

    await user.click(within(await rowOf(1047)).getByRole('button', { name: 'Iniciar preparo' }))

    expect(
      await screen.findByText('Transição de ENTREGUE para EM_PREPARO não é permitida'),
    ).toBeInTheDocument()
    await waitFor(() => expect(requests.length).toBeGreaterThan(0))
  })

  it('goes back to the first page when the filter changes', async () => {
    const many = Array.from({ length: 12 }, (_, i) => makeOrder({ code: 3000 + i }))
    const { user, router } = await openList('/orders?page=2', many)
    await screen.findByText('Mostrando 11–12 de 12 pedidos')

    await user.click(screen.getByRole('button', { name: 'Recebido, 12 pedidos' }))
    await waitFor(() => expect(router.state.location.search).toBe('?status=RECEBIDO'))
  })

  it('moves a page past the end back to the last page', async () => {
    const { router } = await openList('/orders?page=9')
    expect(await screen.findByText('Mostrando 1–5 de 5 pedidos')).toBeInTheDocument()
    expect(router.state.location.search).toBe('')
  })

  it('ignores an unknown status in the URL', async () => {
    await openList('/orders?status=FOO&page=1.5')
    expect(await screen.findByText('Mostrando 1–5 de 5 pedidos')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Todos, 5 pedidos' })).toHaveAttribute(
      'aria-pressed',
      'true',
    )
  })

  it('sends one search request for a burst of typing', async () => {
    const { user } = await openList()
    const requests = mockOrdersApi(orders)

    await user.type(screen.getByRole('searchbox', { name: 'Buscar pedidos' }), 'Mariana')
    await screen.findByText('Mostrando 1–1 de 1 pedidos')

    const searches = requests.filter((params) => params.has('q') && !params.has('status'))
    expect(searches.map((params) => params.get('q'))).toEqual(['Mariana'])
  })

  it('opens the order when its row is clicked', async () => {
    const { user, router } = await openList()
    await user.click(within(await rowOf(1045)).getByText('José da Silva'))
    expect(router.state.location.pathname).toBe(`/orders/${orders[1].id}`)
  })

  it('uses cards and a floating "Novo pedido" button on phones', async () => {
    setViewportWidth(375)
    await openList()

    const card = (await screen.findByRole('link', { name: 'Pedido #1047' })).closest(
      'article',
    ) as HTMLElement
    expect(within(card).getByText('3 itens · há 1 min')).toBeInTheDocument()
    expect(within(card).getByRole('button', { name: 'Iniciar preparo' })).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    // The floating button lives in the page; the tab bar has its own "Novo pedido".
    expect(
      within(screen.getByRole('main')).getByRole('link', { name: 'Novo pedido' }),
    ).toHaveAttribute('href', '/orders/new')
  })
})
