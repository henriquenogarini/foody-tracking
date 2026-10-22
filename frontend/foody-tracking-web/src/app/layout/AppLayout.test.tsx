import { screen, within } from '@testing-library/react'
import { renderRoute, testUser } from '@/test/test-utils'

function topNav() {
  return within(screen.getByRole('navigation', { name: 'Principal' }))
}

function bottomNav() {
  return within(screen.getByRole('navigation', { name: 'Navegação inferior' }))
}

const layoutReady = () => screen.findByRole('navigation', { name: 'Principal' })

describe('AppLayout', () => {
  it('shows the brand linking home', async () => {
    renderRoute('/orders', { signedInAs: testUser })
    const home = await screen.findByRole('link', { name: 'Foody Tracking, início' })
    expect(home).toHaveAttribute('href', '/orders')
    expect(within(home).getByRole('img', { name: 'Foody Tracking' })).toBeInTheDocument()
  })

  it.each([
    ['/orders', 'Pedidos'],
    ['/orders/7b1e4c2a-5d3f-4a8b-9c6d-1e2f3a4b5c6d', 'Pedidos'],
    ['/orders/new', 'Novo pedido'],
  ])('on %s marks "%s" as the current page in both navigations', async (path, label) => {
    renderRoute(path, { signedInAs: testUser })
    await layoutReady()

    for (const nav of [topNav(), bottomNav()]) {
      expect(nav.getByRole('link', { name: label })).toHaveAttribute('aria-current', 'page')
      const others = nav.getAllByRole('link').filter((link) => link.textContent !== label)
      others.forEach((link) => expect(link).not.toHaveAttribute('aria-current'))
    }
  })

  it('navigates between sections', async () => {
    const { router, user } = renderRoute('/orders', { signedInAs: testUser })

    await layoutReady()
    await user.click(topNav().getByRole('link', { name: 'Novo pedido' }))
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Novo pedido' }),
    ).toBeInTheDocument()

    await user.click(bottomNav().getByRole('link', { name: 'Pedidos' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Pedidos' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/orders')
  })

  it('shows the signed-in user and signs out from the account menu', async () => {
    const { router, user } = renderRoute('/orders', { signedInAs: testUser })

    const account = await screen.findByRole('button', { name: 'Conta de Henrique Nogarini' })
    expect(account).toHaveTextContent('HN')
    expect(account).toHaveTextContent('Henrique')

    await user.click(account)
    const menu = await screen.findByRole('menu')
    expect(within(menu).getByText('henrique@restaurante.com')).toBeInTheDocument()

    await user.click(within(menu).getByRole('menuitem', { name: 'Sair' }))
    expect(await screen.findByRole('heading', { name: 'Bem-vindo de volta' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
    expect(sessionStorage.length).toBe(0)
  })

  it('opens the same account menu from the mobile "Conta" tab', async () => {
    const { user } = renderRoute('/orders', { signedInAs: testUser })
    await layoutReady()
    await user.click(bottomNav().getByRole('button', { name: 'Conta' }))
    expect(
      within(await screen.findByRole('menu')).getByText('Henrique Nogarini'),
    ).toBeInTheDocument()
  })
})
