import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { apiUrl, server } from '@/test/server'
import { renderRoute } from '@/test/test-utils'

async function openLogin() {
  const view = renderRoute('/login')
  await screen.findByRole('heading', { name: 'Bem-vindo de volta' })
  return view
}

describe('LoginPage', () => {
  it('validates the fields before calling the API', async () => {
    const { user } = await openLogin()
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByText('Informe seu e-mail.')).toBeInTheDocument()
    expect(screen.getByText('Informe sua senha.')).toBeInTheDocument()
    expect(screen.getByLabelText('E-mail')).toHaveAttribute('aria-invalid', 'true')
  })

  it('shows the API message for wrong credentials and stays on the page', async () => {
    server.use(
      http.post(apiUrl('/auth/login'), () =>
        HttpResponse.json(
          { status: 401, detail: 'E-mail ou senha inválidos.', code: 'AUTH_INVALID_CREDENTIALS' },
          { status: 401 },
        ),
      ),
    )
    const { user, router } = await openLogin()
    await user.type(screen.getByLabelText('E-mail'), 'henrique@restaurante.com')
    await user.type(screen.getByLabelText('Senha'), 'errada-123')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('E-mail ou senha inválidos.')
    expect(router.state.location.pathname).toBe('/login')
  })

  it('toggles password visibility', async () => {
    const { user } = await openLogin()
    const password = screen.getByLabelText('Senha')
    expect(password).toHaveAttribute('type', 'password')

    await user.click(screen.getByRole('button', { name: 'Mostrar senha' }))
    expect(password).toHaveAttribute('type', 'text')
    await user.click(screen.getByRole('button', { name: 'Ocultar senha' }))
    expect(password).toHaveAttribute('type', 'password')
  })

  it('links to sign-up and password recovery', async () => {
    await openLogin()
    expect(screen.getByRole('link', { name: 'Criar conta' })).toHaveAttribute('href', '/register')
    expect(screen.getByRole('link', { name: 'Esqueci a senha' })).toHaveAttribute(
      'href',
      '/forgot-password',
    )
  })
})
