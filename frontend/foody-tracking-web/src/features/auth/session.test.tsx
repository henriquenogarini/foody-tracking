import { act, screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { notifyUnauthorized } from '@/shared/api/authToken'
import { api } from '@/shared/api/http'
import { apiUrl, server } from '@/test/server'
import { mockMe, renderRoute, testUser } from '@/test/test-utils'
import { TOKEN_STORAGE_KEY } from './session'

const ordersHeading = () => screen.findByRole('heading', { level: 1, name: 'Pedidos' })
const loginHeading = () => screen.findByRole('heading', { name: 'Bem-vindo de volta' })

function mockLogin(token = 'fresh-token') {
  server.use(
    http.post(apiUrl('/auth/login'), () =>
      HttpResponse.json({ accessToken: token, tokenType: 'Bearer', expiresIn: 7200 }),
    ),
  )
}

describe('session and route guards', () => {
  it('sends an anonymous visitor to the login page', async () => {
    const { router } = renderRoute('/orders/new')
    expect(await loginHeading()).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
  })

  it('returns to the page the visitor asked for after signing in', async () => {
    mockLogin()
    mockMe()
    const { router, user } = renderRoute('/orders/new?from=test')
    await loginHeading()

    await user.type(screen.getByLabelText('E-mail'), testUser.email)
    await user.type(screen.getByLabelText('Senha'), 'senha-forte-1')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Novo pedido' }),
    ).toBeInTheDocument()
    expect(router.state.location.pathname + router.state.location.search).toBe(
      '/orders/new?from=test',
    )
    expect(sessionStorage.getItem(TOKEN_STORAGE_KEY)).toBe('fresh-token')
  })

  it('restores a stored session on reload and keeps a signed-in user away from /login', async () => {
    const { router } = renderRoute('/login', { signedInAs: testUser })
    expect(await ordersHeading()).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/orders')
  })

  it('ends the session and explains why when the API rejects the stored token', async () => {
    sessionStorage.setItem(TOKEN_STORAGE_KEY, 'expired-token')
    server.use(
      http.get(apiUrl('/auth/me'), () =>
        HttpResponse.json(
          {
            status: 401,
            detail: 'Token de acesso ausente, inválido ou expirado.',
            code: 'AUTH_TOKEN_INVALID',
          },
          { status: 401 },
        ),
      ),
    )
    const { router } = renderRoute('/orders')

    expect(
      await screen.findByText('Sua sessão expirou. Entre novamente para continuar.'),
    ).toBeInTheDocument()
    expect(router.state.location.search).toBe('?reason=expired')
    expect(sessionStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull()
  })

  it('offers a retry when the session cannot be checked for another reason', async () => {
    sessionStorage.setItem(TOKEN_STORAGE_KEY, 'stored-token')
    server.use(http.get(apiUrl('/auth/me'), () => HttpResponse.error(), { once: true }))
    const { user } = renderRoute('/orders')

    expect(await screen.findByText('Não foi possível carregar sua sessão.')).toBeInTheDocument()
    mockMe()
    await user.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(await ordersHeading()).toBeInTheDocument()
  })

  it('ends the session when any authenticated call is rejected with 401', async () => {
    const { router } = renderRoute('/orders/new', { signedInAs: testUser })
    await screen.findByRole('heading', { level: 1, name: 'Novo pedido' })
    // Registered after rendering so it wins over the default fake orders API.
    server.use(
      http.get(apiUrl('/orders'), () =>
        HttpResponse.json(
          { status: 401, code: 'AUTH_TOKEN_INVALID', detail: 'x' },
          { status: 401 },
        ),
      ),
    )

    await act(() => api.get('/orders').catch(() => undefined))

    expect(
      await screen.findByText('Sua sessão expirou. Entre novamente para continuar.'),
    ).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
  })

  it('ignores a late 401 from a token that is no longer the session', async () => {
    renderRoute('/orders', { signedInAs: testUser })
    await ordersHeading()

    act(() => notifyUnauthorized('token-from-a-previous-session'))

    expect(screen.getByRole('heading', { level: 1, name: 'Pedidos' })).toBeInTheDocument()
    expect(sessionStorage.getItem(TOKEN_STORAGE_KEY)).toBe('stored-token')
  })

  it('after "Sair", the next person starts from the orders list with their own data', async () => {
    const { router, user } = renderRoute('/orders/new', { signedInAs: testUser })
    await screen.findByRole('heading', { level: 1, name: 'Novo pedido' })

    await user.click(screen.getByRole('button', { name: 'Conta de Henrique Nogarini' }))
    await user.click(
      within(await screen.findByRole('menu')).getByRole('menuitem', { name: 'Sair' }),
    )
    await loginHeading()
    expect(router.state.location.search).toBe('')

    const other = {
      ...testUser,
      id: 'b1c2',
      name: 'Ana Beatriz Rocha',
      email: 'ana@restaurante.com',
    }
    mockLogin('other-token')
    mockMe(other)
    await user.type(screen.getByLabelText('E-mail'), other.email)
    await user.type(screen.getByLabelText('Senha'), 'senha-forte-2')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await ordersHeading()).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/orders')
    expect(screen.getByRole('button', { name: 'Conta de Ana Beatriz Rocha' })).toHaveTextContent(
      'AR',
    )
  })
})
