import { AxiosError, AxiosHeaders } from 'axios'
import { toApiError } from './problem'

function responseError(status: number, data: unknown) {
  const config = { headers: new AxiosHeaders() }
  return new AxiosError('failed', 'ERR_BAD_RESPONSE', config, null, {
    status,
    statusText: '',
    headers: {},
    config,
    data,
  })
}

// Shape sent by the API (ProblemDetailFactory); its timestamp is UTC.
function problem(status: number, code: string | undefined, detail: string, extra = {}) {
  return {
    type: 'about:blank',
    title: 'Error',
    status,
    detail,
    instance: '/api/v1/orders',
    ...(code ? { code } : {}),
    timestamp: '2026-10-21T17:13:27.123456Z',
    ...extra,
  }
}

describe('toApiError', () => {
  it('keeps the code and pt-BR detail sent by the API', () => {
    const error = responseError(
      409,
      problem(
        409,
        'ORDER_INVALID_TRANSITION',
        'Transição de ENTREGUE para RECEBIDO não é permitida',
      ),
    )
    expect(toApiError(error)).toEqual({
      status: 409,
      code: 'ORDER_INVALID_TRANSITION',
      message: 'Transição de ENTREGUE para RECEBIDO não é permitida',
      fieldErrors: {},
    })
  })

  it('maps validation errors to react-hook-form paths, keeping every message', () => {
    const error = responseError(
      400,
      problem(400, 'VALIDATION_ERROR', 'Um ou mais campos são inválidos.', {
        errors: [
          { field: 'customer.name', message: 'não deve estar em branco' },
          { field: 'items[0].unitPrice', message: 'deve ser maior ou igual a 0.01' },
          { field: 'items[0].unitPrice', message: 'valor numérico fora do limite' },
          { field: 'deliveryAddress.state', message: 'deve ser a sigla da UF' },
        ],
      }),
    )
    expect(toApiError(error)).toMatchObject({
      code: 'VALIDATION_ERROR',
      message: 'Um ou mais campos são inválidos.',
      fieldErrors: {
        'customer.name': 'não deve estar em branco',
        'items.0.unitPrice': 'deve ser maior ou igual a 0.01; valor numérico fora do limite',
        'deliveryAddress.state': 'deve ser a sigla da UF',
      },
    })
  })

  it.each([
    [404, 'NOT_FOUND'],
    [405, 'METHOD_NOT_ALLOWED'],
    [415, 'UNSUPPORTED_MEDIA_TYPE'],
    [401, 'AUTH_TOKEN_INVALID'],
  ])('derives the code from status %i when the body has none', (status, code) => {
    const error = responseError(status, problem(status, undefined, 'Recurso não encontrado.'))
    expect(toApiError(error)).toMatchObject({ status, code, message: 'Recurso não encontrado.' })
  })

  it('treats an unknown code like a missing one', () => {
    const error = responseError(404, problem(404, 'SOMETHING_NEW', 'Pedido não encontrado.'))
    expect(toApiError(error).code).toBe('NOT_FOUND')
  })

  it('falls back to a generic message when a 5xx has no ProblemDetail body', () => {
    expect(toApiError(responseError(502, '<html>Bad Gateway</html>'))).toEqual({
      status: 502,
      code: 'INTERNAL_ERROR',
      message: 'Erro interno. Tente novamente mais tarde.',
      fieldErrors: {},
    })
  })

  it('reports an unreachable server', () => {
    const error = new AxiosError('Network Error', AxiosError.ERR_NETWORK)
    expect(toApiError(error)).toMatchObject({
      status: null,
      code: 'NETWORK_ERROR',
      message: 'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.',
    })
  })

  it('reports a timeout', () => {
    const error = new AxiosError('timeout of 15000ms exceeded', AxiosError.ECONNABORTED)
    expect(toApiError(error)).toMatchObject({ status: null, code: 'TIMEOUT' })
  })

  it('handles errors that did not come from the HTTP client', () => {
    expect(toApiError(new TypeError('x is undefined'))).toEqual({
      status: null,
      code: 'UNKNOWN',
      message: 'Algo deu errado. Tente novamente.',
      fieldErrors: {},
    })
  })
})
