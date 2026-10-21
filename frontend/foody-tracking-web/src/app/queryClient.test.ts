import { AxiosError, AxiosHeaders } from 'axios'
import { shouldRetry } from './queryClient'

function httpError(status: number) {
  const config = { headers: new AxiosHeaders() }
  return new AxiosError('failed', 'ERR_BAD_RESPONSE', config, null, {
    status,
    statusText: '',
    headers: {},
    config,
    data: null,
  })
}

describe('shouldRetry', () => {
  it.each([400, 401, 403, 404, 409])('never retries a %i', (status) => {
    expect(shouldRetry(0, httpError(status))).toBe(false)
  })

  it('retries a 5xx once', () => {
    expect(shouldRetry(0, httpError(503))).toBe(true)
    expect(shouldRetry(1, httpError(503))).toBe(false)
  })

  it('retries a network failure (no response) once', () => {
    const networkError = new AxiosError('Network Error', 'ERR_NETWORK')
    expect(shouldRetry(0, networkError)).toBe(true)
    expect(shouldRetry(1, networkError)).toBe(false)
  })

  it('retries a non-HTTP error once', () => {
    expect(shouldRetry(0, new Error('boom'))).toBe(true)
    expect(shouldRetry(1, new Error('boom'))).toBe(false)
  })
})
