import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { apiUrl, server } from '@/test/server'
import { renderRoute } from '@/test/test-utils'

async function submitReset(user: ReturnType<typeof renderRoute>['user']) {
  await user.type(screen.getByLabelText('E-mail'), 'henrique@restaurante.com')
  await user.type(screen.getByLabelText('Nova senha'), 'nova-senha-1')
  await user.type(screen.getByLabelText('Confirmar nova senha'), 'nova-senha-1')
  await user.click(screen.getByRole('button', { name: 'Redefinir senha' }))
}

async function openReset() {
  const view = renderRoute('/forgot-password')
  await screen.findByRole('heading', { name: 'Recuperar senha' })
  return view
}

describe('ForgotPasswordPage', () => {
  it('resets the password and points back to the login', async () => {
    const received: unknown[] = []
    server.use(
      http.post(apiUrl('/auth/password-reset'), async ({ request }) => {
        received.push(await request.json())
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const { user } = await openReset()
    await submitReset(user)

    expect(await screen.findByRole('heading', { name: 'Senha redefinida' })).toBeInTheDocument()
    expect(
      screen.getByText(
        'Se o e-mail estiver cadastrado, a senha foi alterada. Entre com a nova senha.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ir para o login' })).toHaveAttribute('href', '/login')
    expect(received).toEqual([{ email: 'henrique@restaurante.com', newPassword: 'nova-senha-1' }])
  })

  it('explains when the feature is switched off on the server', async () => {
    server.use(
      http.post(apiUrl('/auth/password-reset'), () =>
        HttpResponse.json(
          { status: 404, detail: 'Recurso não encontrado.', code: 'NOT_FOUND' },
          { status: 404 },
        ),
      ),
    )
    const { user } = await openReset()
    await submitReset(user)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Recuperação de senha indisponível no momento.',
    )
  })
})
