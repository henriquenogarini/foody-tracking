import { screen, within } from '@testing-library/react'
import { renderRoute } from '@/test/test-utils'

function topNav() {
  return within(screen.getByRole('navigation', { name: 'Principal' }))
}

function bottomNav() {
  return within(screen.getByRole('navigation', { name: 'Navegação inferior' }))
}

describe('AppLayout', () => {
  it('shows the brand linking home', async () => {
    renderRoute('/orders')
    const home = await screen.findByRole('link', { name: 'Foody Tracking, início' })
    expect(home).toHaveAttribute('href', '/orders')
    expect(within(home).getByRole('img', { name: 'Foody Tracking' })).toBeInTheDocument()
  })

  it.each([
    ['/orders', 'Pedidos'],
    ['/orders/7b1e4c2a-5d3f-4a8b-9c6d-1e2f3a4b5c6d', 'Pedidos'],
    ['/orders/new', 'Novo pedido'],
  ])('on %s marks "%s" as the current page in both navigations', async (path, label) => {
    renderRoute(path)
    await screen.findByRole('navigation', { name: 'Principal' })

    for (const nav of [topNav(), bottomNav()]) {
      expect(nav.getByRole('link', { name: label })).toHaveAttribute('aria-current', 'page')
      const others = nav.getAllByRole('link').filter((link) => link.textContent !== label)
      others.forEach((link) => expect(link).not.toHaveAttribute('aria-current'))
    }
  })

  it('navigates between sections', async () => {
    const { router, user } = renderRoute('/orders')

    await user.click(await topNav().findByRole('link', { name: 'Novo pedido' }))
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Novo pedido' }),
    ).toBeInTheDocument()

    await user.click(bottomNav().getByRole('link', { name: 'Pedidos' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Pedidos' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/orders')
  })
})
