import { screen, waitFor, within } from '@testing-library/react'
import { delay, http, HttpResponse } from 'msw'
import type { OrderStatus } from '@/features/orders/types'
import { apiUrl, server } from '@/test/server'
import {
  makeOrder,
  mockOrdersApi,
  renderRoute,
  setViewportWidth,
  testUser,
  toOrder,
} from '@/test/test-utils'

const preparing = () =>
  makeOrder({
    code: 1045,
    status: 'EM_PREPARO',
    customerName: 'José da Silva',
    total: 89.8,
    itemsCount: 2,
    neighborhood: 'Vila São Paulo',
  })

async function openDetail(order = preparing()) {
  const view = renderRoute(`/orders/${order.id}`, { signedInAs: testUser, orders: [order] })
  await screen.findByRole('heading', { level: 1, name: `Pedido #${order.code}` })
  return { ...view, order }
}

function mockPatch(order: ReturnType<typeof preparing>) {
  const received: unknown[] = []
  server.use(
    http.patch(apiUrl('/orders/:id/status'), async ({ request }) => {
      const body = (await request.json()) as { status: OrderStatus }
      received.push(body)
      order.status = body.status
      return HttpResponse.json(toOrder(order))
    }),
  )
  return received
}

describe('OrderDetailPage', () => {
  it('shows the order as in the design', async () => {
    await openDetail()

    expect(screen.getAllByText('Em preparo').length).toBeGreaterThan(0)
    const items = within(screen.getByRole('heading', { name: 'Itens' }).closest('section')!)
    expect(items.getByRole('columnheader', { name: 'Subtotal' })).toBeInTheDocument()
    expect(items.getByText('Pizza calabresa G')).toBeInTheDocument()
    expect(items.getAllByText('R$ 89,80')).toHaveLength(2) // subtotal and total

    const customer = within(screen.getByRole('heading', { name: 'Cliente' }).closest('section')!)
    expect(customer.getByText('José da Silva')).toBeInTheDocument()
    expect(customer.getByText('(11) 98752-1144')).toBeInTheDocument()
    expect(screen.getByText('Rua José Neves, 590')).toBeInTheDocument()
    expect(screen.getByText('Vila São Paulo · São Paulo/SP · 04650-140')).toBeInTheDocument()
  })

  it('shows the timeline with the current step and what is ahead', async () => {
    await openDetail()
    const timeline = within(
      screen.getByRole('heading', { name: 'Linha do tempo' }).closest('section')!,
    )
    expect(await timeline.findByText('Pedido criado por Henrique Nogarini')).toBeInTheDocument()
    expect(timeline.getByText('Cozinha iniciou o preparo').closest('li')).toHaveAttribute(
      'aria-current',
      'step',
    )
    expect(timeline.getAllByText('Aguardando')).toHaveLength(2)
  })

  it('shows the new status at once, before any refresh', async () => {
    const order = preparing()
    const { user } = await openDetail(order)
    // The PATCH answers with the new status, but reads keep returning the old one (a slow refresh):
    // only the response itself can move the screen forward.
    const stale = toOrder(order)
    server.use(
      http.patch(apiUrl('/orders/:id/status'), () =>
        HttpResponse.json({ ...stale, status: 'SAIU_PARA_ENTREGA' }),
      ),
      http.get(apiUrl('/orders/:id'), async () => {
        await delay('infinite')
        return HttpResponse.json(stale)
      }),
    )

    await user.click(screen.getByRole('button', { name: 'Saiu para entrega' }))

    expect(await screen.findByRole('button', { name: 'Marcar como entregue' })).toBeInTheDocument()
    expect(screen.getByText('Pedido #1045: Saiu para entrega')).toBeInTheDocument()
    expect(screen.getByText('Entregador a caminho')).toBeInTheDocument()
  })

  it('explains a conflicting change on the detail page', async () => {
    server.use(
      http.patch(apiUrl('/orders/:id/status'), () =>
        HttpResponse.json(
          {
            status: 409,
            code: 'CONCURRENT_MODIFICATION',
            detail: 'O registro foi alterado por outra operação. Atualize e tente novamente.',
          },
          { status: 409 },
        ),
      ),
    )
    const { user } = await openDetail()
    await user.click(screen.getByRole('button', { name: 'Saiu para entrega' }))

    expect(
      await screen.findByText(
        'O registro foi alterado por outra operação. Atualize e tente novamente.',
      ),
    ).toBeInTheDocument()
  })

  it('keeps the order visible when only the history fails', async () => {
    const order = preparing()
    renderRoute(`/orders/${order.id}`, { signedInAs: testUser, orders: [order] })
    server.use(
      http.get(apiUrl('/orders/:id/history'), () => HttpResponse.json({}, { status: 503 })),
    )

    expect(await screen.findByText('Não foi possível carregar o histórico.')).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: 'Pedido #1045' })).toBeInTheDocument()
  })

  it('cancels from the timeline card after confirming', async () => {
    const order = preparing()
    const received = mockPatch(order)
    const { user } = await openDetail(order)

    await user.click(screen.getByRole('button', { name: 'Cancelar pedido' }))
    const dialog = await screen.findByRole('dialog', { name: 'Cancelar o pedido #1045?' })
    await user.click(within(dialog).getByRole('button', { name: 'Cancelar pedido' }))

    expect(await screen.findByText('Pedido #1045: Cancelado')).toBeInTheDocument()
    expect(received).toEqual([{ status: 'CANCELADO' }])
    await waitFor(() =>
      expect(screen.queryByRole('button', { name: 'Cancelar pedido' })).not.toBeInTheDocument(),
    )
  })

  it('offers no actions once the order is finished', async () => {
    await openDetail(makeOrder({ code: 1042, status: 'ENTREGUE' }))
    expect(
      screen.queryByRole('button', { name: /Marcar|Cancelar|Iniciar|Saiu/ }),
    ).not.toBeInTheDocument()
  })

  it('says "not found" for an order that does not exist (404)', async () => {
    renderRoute('/orders/00000000-0000-4000-8000-000000009999', {
      signedInAs: testUser,
      orders: [],
    })
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Pedido não encontrado' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver pedidos' })).toHaveAttribute('href', '/orders')
  })

  it('says "not found" for an address that is not an order id (400)', async () => {
    server.use(
      http.get(apiUrl('/orders/nope'), () =>
        HttpResponse.json({ status: 400, code: 'VALIDATION_ERROR', detail: 'x' }, { status: 400 }),
      ),
      http.get(apiUrl('/orders/nope/history'), () =>
        HttpResponse.json({ status: 400, code: 'VALIDATION_ERROR', detail: 'x' }, { status: 400 }),
      ),
    )
    renderRoute('/orders/nope', { signedInAs: testUser, orders: null })
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Pedido não encontrado' }),
    ).toBeInTheDocument()
  })

  it('offers a retry when the order cannot load', async () => {
    const order = preparing()
    server.use(
      http.get(apiUrl('/orders/:id'), () => HttpResponse.json({}, { status: 503 })),
      http.get(apiUrl('/orders/:id/history'), () => HttpResponse.json([])),
    )
    const { user } = renderRoute(`/orders/${order.id}`, { signedInAs: testUser, orders: null })
    expect(await screen.findByText('Erro interno. Tente novamente mais tarde.')).toBeInTheDocument()

    mockOrdersApi([order])
    await user.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Pedido #1045' }),
    ).toBeInTheDocument()
  })

  it('has no action bar on phones once the order is finished', async () => {
    setViewportWidth(375)
    await openDetail(makeOrder({ code: 1040, status: 'CANCELADO' }))
    expect(screen.queryByRole('button', { name: /Cancelar o pedido/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Marcar|Iniciar|Saiu/ })).not.toBeInTheDocument()
  })

  it('uses a compact layout with a bottom action bar on phones', async () => {
    setViewportWidth(375)
    await openDetail()

    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    expect(screen.getByText('2× Pizza calabresa G')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cancelar o pedido #1045' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Saiu para entrega' })).toBeInTheDocument()
  })
})
