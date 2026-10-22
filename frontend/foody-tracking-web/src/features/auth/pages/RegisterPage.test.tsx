import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { apiUrl, server } from '@/test/server'
import { mockMe, renderRoute, testUser } from '@/test/test-utils'

async function fillForm(user: ReturnType<typeof renderRoute>['user'], confirm = 'senha-forte-1') {
  await user.type(screen.getByLabelText('Nome completo'), testUser.name)
  await user.type(screen.getByLabelText('E-mail'), testUser.email)
  await user.type(screen.getByLabelText('Senha'), 'senha-forte-1')
  await user.type(screen.getByLabelText('Confirmar senha'), confirm)
  await user.click(screen.getByRole('button', { name: 'Criar conta' }))
}

async function openRegister() {
  const view = renderRoute('/register')
  await screen.findByRole('heading', { name: 'Crie sua conta' })
  return view
}

describe('RegisterPage', () => {
  it('creates the account, signs in and opens the orders list', async () => {
    const received: unknown[] = []
    server.use(
      http.post(apiUrl('/auth/register'), async ({ request }) => {
        received.push(await request.json())
        return HttpResponse.json({ ...testUser }, { status: 201 })
      }),
      http.post(apiUrl('/auth/login'), () =>
        HttpResponse.json({ accessToken: 'new-token', tokenType: 'Bearer', expiresIn: 7200 }),
      ),
    )
    mockMe()
    const { user, router } = await openRegister()

    await fillForm(user)

    expect(await screen.findByRole('heading', { level: 1, name: 'Pedidos' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/orders')
    // confirmPassword is a client-only field: the API receives exactly its contract.
    expect(received).toEqual([
      { name: testUser.name, email: testUser.email, password: 'senha-forte-1' },
    ])
  })

  it('blocks mismatched passwords without calling the API', async () => {
    const { user } = await openRegister()
    await fillForm(user, 'outra-senha')
    expect(
      await screen.findByText('As senhas não coincidem. Digite a mesma senha nos dois campos.'),
    ).toBeInTheDocument()
  })

  it('puts the duplicate e-mail message on the e-mail field', async () => {
    server.use(
      http.post(apiUrl('/auth/register'), () =>
        HttpResponse.json(
          {
            status: 409,
            detail: 'Já existe um usuário cadastrado com este e-mail.',
            code: 'USER_EMAIL_ALREADY_EXISTS',
          },
          { status: 409 },
        ),
      ),
    )
    const { user } = await openRegister()
    await fillForm(user)

    expect(
      await screen.findByText('Já existe um usuário cadastrado com este e-mail.'),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('E-mail')).toHaveAttribute('aria-invalid', 'true')
  })

  it('maps API validation errors to the matching fields', async () => {
    server.use(
      http.post(apiUrl('/auth/register'), () =>
        HttpResponse.json(
          {
            status: 400,
            detail: 'Um ou mais campos são inválidos.',
            code: 'VALIDATION_ERROR',
            errors: [{ field: 'name', message: 'tamanho deve ser entre 0 e 100' }],
          },
          { status: 400 },
        ),
      ),
    )
    const { user } = await openRegister()
    await fillForm(user)

    expect(await screen.findByText('tamanho deve ser entre 0 e 100')).toBeInTheDocument()
    expect(screen.getByLabelText('Nome completo')).toHaveAttribute('aria-invalid', 'true')
  })

  it('points to the login when the account was created but the automatic sign-in failed', async () => {
    server.use(
      http.post(apiUrl('/auth/register'), () => HttpResponse.json(testUser, { status: 201 })),
      http.post(apiUrl('/auth/login'), () => HttpResponse.error()),
    )
    const { user, router } = await openRegister()
    await fillForm(user)

    expect(
      await screen.findByText(/Conta criada, mas não foi possível entrar agora/),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Entre com seu e-mail e senha' })).toHaveAttribute(
      'href',
      '/login',
    )
    expect(router.state.location.pathname).toBe('/register')
  })
})
