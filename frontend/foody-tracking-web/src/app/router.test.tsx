import { screen } from '@testing-library/react'
import { renderRoute, testUser } from '@/test/test-utils'

describe('routes', () => {
  it('redirects / to the orders list', async () => {
    const { router } = renderRoute('/', { signedInAs: testUser })
    expect(await screen.findByRole('heading', { level: 1, name: 'Pedidos' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/orders')
  })

  it.each([
    ['/orders/new', 'Novo pedido'],
    ['/orders/7b1e4c2a-5d3f-4a8b-9c6d-1e2f3a4b5c6d', 'Detalhe do pedido'],
  ])('renders %s inside the app layout', async (path, heading) => {
    renderRoute(path, { signedInAs: testUser })
    expect(await screen.findByRole('heading', { level: 1, name: heading })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Principal' })).toBeInTheDocument()
  })

  it.each([
    ['/login', 'Bem-vindo de volta'],
    ['/register', 'Crie sua conta'],
    ['/forgot-password', 'Recuperar senha'],
  ])('renders the public page %s without the app navigation', async (path, heading) => {
    renderRoute(path)
    expect(await screen.findByRole('heading', { level: 1, name: heading })).toBeInTheDocument()
    expect(screen.queryByRole('navigation', { name: 'Principal' })).not.toBeInTheDocument()
  })

  it('shows the not-found page for unknown paths, with a way back', async () => {
    const { router, user } = renderRoute('/does-not-exist', { signedInAs: testUser })
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Página não encontrada' }),
    ).toBeInTheDocument()

    await user.click(screen.getByRole('link', { name: 'Voltar para pedidos' }))
    expect(router.state.location.pathname).toBe('/orders')
  })
})
