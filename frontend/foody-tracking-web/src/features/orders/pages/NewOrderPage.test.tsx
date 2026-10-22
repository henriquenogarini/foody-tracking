import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { delay, http, HttpResponse } from 'msw'
import { apiUrl, server } from '@/test/server'
import { renderRoute, setViewportWidth, testUser } from '@/test/test-utils'

type User = ReturnType<typeof renderRoute>['user']

async function openForm() {
  const view = renderRoute('/orders/new', { signedInAs: testUser })
  await screen.findByRole('heading', { level: 1, name: 'Novo pedido' })
  return view
}

async function fillValidOrder(user: User) {
  await user.type(screen.getByLabelText('Nome'), 'José da Silva')
  await user.type(screen.getByLabelText('Telefone'), '11987521144')
  await user.type(screen.getByLabelText('Descrição'), 'Pizza calabresa G')
  await user.type(screen.getByLabelText('Preço unit.'), '59,90')
  await user.type(screen.getByLabelText('CEP'), '04650140')
  await user.type(screen.getByLabelText('Rua'), 'Rua José Neves')
  await user.type(screen.getByLabelText('Número'), '590')
  await user.type(screen.getByLabelText('Bairro'), 'Vila São Paulo')
  await user.type(screen.getByLabelText('Cidade'), 'São Paulo')
  await user.selectOptions(screen.getByLabelText('UF'), 'SP')
}

function mockCreate() {
  const received: unknown[] = []
  server.use(
    http.post(apiUrl('/orders'), async ({ request }) => {
      received.push(await request.json())
      return HttpResponse.json(
        { id: 'new-order-id', code: 1048, status: 'RECEBIDO' },
        { status: 201 },
      )
    }),
  )
  return received
}

describe('NewOrderPage', () => {
  it('masks phone and CEP while typing', async () => {
    const { user } = await openForm()
    await user.type(screen.getByLabelText('Telefone'), '11987521144')
    await user.type(screen.getByLabelText('CEP'), '04650140')

    expect(screen.getByLabelText('Telefone')).toHaveValue('(11) 98752-1144')
    expect(screen.getByLabelText('CEP')).toHaveValue('04650-140')
  })

  it('shows what is missing instead of calling the API', async () => {
    const received = mockCreate()
    const { user } = await openForm()
    await user.click(screen.getByRole('button', { name: 'Criar pedido' }))

    expect(await screen.findByText('Informe o nome do cliente.')).toBeInTheDocument()
    expect(screen.getByText('Selecione a UF.')).toBeInTheDocument()
    expect(screen.getByLabelText('Nome')).toHaveFocus()
    expect(received).toHaveLength(0)
  })

  it('adds and removes items and keeps a live total', async () => {
    const { user } = await openForm()
    await user.type(screen.getByLabelText('Descrição'), 'Pizza calabresa G')
    await user.type(screen.getByLabelText('Preço unit.'), '59,90')

    await user.click(screen.getByRole('button', { name: 'Adicionar item' }))
    await user.type(screen.getByLabelText('Descrição do item 2'), 'Guaraná 2L')
    await user.clear(screen.getByLabelText('Qtd. do item 2'))
    await user.type(screen.getByLabelText('Qtd. do item 2'), '2')
    await user.type(screen.getByLabelText('Preço unit. do item 2'), '14,95')

    const summary = within(screen.getByRole('heading', { name: 'Resumo' }).parentElement!)
    expect(summary.getByText('1× Pizza calabresa G')).toBeInTheDocument()
    expect(summary.getByText('2× Guaraná 2L')).toBeInTheDocument()
    expect(summary.getByText('R$ 89,80')).toBeInTheDocument()
    expect(screen.getByText('2 itens')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Remover item 1' }))
    expect(summary.getAllByText('R$ 29,90')).toHaveLength(2)
    expect(screen.queryByRole('button', { name: /Remover item/ })).not.toBeInTheDocument()
  })

  it('creates the order with the API contract and opens it', async () => {
    const received = mockCreate()
    const { user, router } = await openForm()
    await fillValidOrder(user)
    await user.click(screen.getByRole('button', { name: 'Criar pedido' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/orders/new-order-id'))
    expect(await screen.findByText('Pedido #1048 criado')).toBeInTheDocument()
    expect(received).toEqual([
      {
        customer: { name: 'José da Silva', phone: '+5511987521144' },
        items: [{ description: 'Pizza calabresa G', quantity: 1, unitPrice: 59.9 }],
        deliveryAddress: {
          postalCode: '04650-140',
          street: 'Rua José Neves',
          number: '590',
          neighborhood: 'Vila São Paulo',
          city: 'São Paulo',
          state: 'SP',
        },
      },
    ])
  })

  it('puts API validation errors on the matching fields', async () => {
    server.use(
      http.post(apiUrl('/orders'), () =>
        HttpResponse.json(
          {
            status: 400,
            code: 'VALIDATION_ERROR',
            detail: 'Um ou mais campos são inválidos.',
            errors: [
              { field: 'items[0].unitPrice', message: 'deve ser menor ou igual a 99999.99' },
            ],
          },
          { status: 400 },
        ),
      ),
    )
    const { user } = await openForm()
    await fillValidOrder(user)
    await user.click(screen.getByRole('button', { name: 'Criar pedido' }))

    expect(await screen.findByText('deve ser menor ou igual a 99999.99')).toBeInTheDocument()
    expect(screen.getByLabelText('Preço unit.')).toHaveAttribute('aria-invalid', 'true')
  })

  it('shows other API failures at the top and allows another try', async () => {
    server.use(http.post(apiUrl('/orders'), () => HttpResponse.json({}, { status: 503 })))
    const { user } = await openForm()
    await fillValidOrder(user)
    await user.click(screen.getByRole('button', { name: 'Criar pedido' }))

    expect(await screen.findByText('Erro interno. Tente novamente mais tarde.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Criar pedido' })).toBeEnabled()
  })

  it('uses item cards and a bottom bar with the total on phones', async () => {
    setViewportWidth(375)
    const { user } = await openForm()
    await user.click(screen.getByRole('button', { name: 'Adicionar item' }))

    expect(screen.getAllByRole('button', { name: /Remover item/ })).toHaveLength(2)
    expect(screen.getByText('Total · 2 itens')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Resumo' })).not.toBeInTheDocument()
  })

  it('sends optional fields as typed when they are filled in', async () => {
    const received = mockCreate()
    const { user } = await openForm()
    await fillValidOrder(user)
    await user.type(screen.getByLabelText('E-mail'), 'jose@exemplo.com')
    await user.type(screen.getByLabelText('Complemento'), 'Sala 03')
    await user.type(
      screen.getByLabelText('Instruções para a cozinha ou o entregador'),
      'Sem cebola.',
    )
    await user.click(screen.getByRole('button', { name: 'Criar pedido' }))

    await waitFor(() => expect(received).toHaveLength(1))
    expect(received[0]).toMatchObject({
      customer: { email: 'jose@exemplo.com' },
      deliveryAddress: { complement: 'Sala 03' },
      notes: 'Sem cebola.',
    })
  })

  it('never submits twice while the request is in flight', async () => {
    const received: unknown[] = []
    server.use(
      http.post(apiUrl('/orders'), async ({ request }) => {
        received.push(await request.json())
        await delay(200)
        return HttpResponse.json({ id: 'slow-id', code: 1049, status: 'RECEBIDO' }, { status: 201 })
      }),
    )
    const { user } = await openForm()
    await fillValidOrder(user)
    const submit = screen.getByRole('button', { name: 'Criar pedido' })
    await user.click(submit)

    await waitFor(() => expect(submit).toBeDisabled())
    // A user cannot click it now; fire the event directly to prove the form still sends once.
    fireEvent.click(submit)

    await screen.findByText('Pedido #1049 criado')
    expect(received).toHaveLength(1)
  })

  it('moves focus to the next line after removing an item', async () => {
    const { user } = await openForm()
    await user.click(screen.getByRole('button', { name: 'Adicionar item' }))
    await user.click(screen.getByRole('button', { name: 'Remover item 1' }))

    await waitFor(() => expect(screen.getByLabelText('Descrição')).toHaveFocus())
  })

  it('shows API errors for fields the form does not have as a general message', async () => {
    server.use(
      http.post(apiUrl('/orders'), () =>
        HttpResponse.json(
          {
            status: 400,
            code: 'VALIDATION_ERROR',
            detail: 'Um ou mais campos são inválidos.',
            errors: [{ field: 'deliveryAddress.lat', message: 'informe lat e lng juntos' }],
          },
          { status: 400 },
        ),
      ),
    )
    const { user } = await openForm()
    await fillValidOrder(user)
    await user.click(screen.getByRole('button', { name: 'Criar pedido' }))

    expect(await screen.findByText('Um ou mais campos são inválidos.')).toBeInTheDocument()
  })
})
