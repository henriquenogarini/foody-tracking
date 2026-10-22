import { http as mock, HttpResponse } from 'msw'
import { apiUrl, server } from '@/test/server'
import { onUnauthorized, setAuthToken } from './authToken'
import { api } from './http'

function echoAuthorization() {
  server.use(
    mock.get(apiUrl('/echo'), ({ request }) =>
      HttpResponse.json({ authorization: request.headers.get('Authorization') }),
    ),
  )
}

describe('api client', () => {
  afterEach(() => setAuthToken(null))

  it('calls the configured API base URL', async () => {
    server.use(mock.get(apiUrl('/ping'), () => HttpResponse.json({ pong: true })))
    const { data } = await api.get('/ping')
    expect(data).toEqual({ pong: true })
  })

  it('sends the bearer token when there is a session', async () => {
    echoAuthorization()
    setAuthToken('jwt-123')
    const { data } = await api.get('/echo')
    expect(data.authorization).toBe('Bearer jwt-123')
  })

  it('sends no Authorization header without a session', async () => {
    echoAuthorization()
    const { data } = await api.get('/echo')
    expect(data.authorization).toBeNull()
  })

  describe('on 401', () => {
    const handler = vi.fn()
    let unregister: () => void

    beforeEach(() => {
      unregister = onUnauthorized(handler)
      server.use(
        mock.get(apiUrl('/orders'), () =>
          HttpResponse.json({ code: 'AUTH_TOKEN_INVALID' }, { status: 401 }),
        ),
        mock.post(apiUrl('/auth/login'), () =>
          HttpResponse.json({ code: 'AUTH_INVALID_CREDENTIALS' }, { status: 401 }),
        ),
        mock.get(apiUrl('/forbidden'), () =>
          HttpResponse.json({ code: 'ACCESS_DENIED' }, { status: 403 }),
        ),
      )
    })
    afterEach(() => unregister())

    it('reports a lost session when an authenticated call is rejected', async () => {
      setAuthToken('expired')
      await expect(api.get('/orders')).rejects.toMatchObject({ response: { status: 401 } })
      expect(handler).toHaveBeenCalledOnce()
    })

    it('leaves wrong login credentials to the form', async () => {
      await expect(api.post('/auth/login', {})).rejects.toMatchObject({
        response: { status: 401 },
      })
      expect(handler).not.toHaveBeenCalled()
    })

    it('does not treat other errors as a lost session', async () => {
      setAuthToken('valid')
      await expect(api.get('/forbidden')).rejects.toMatchObject({ response: { status: 403 } })
      expect(handler).not.toHaveBeenCalled()
    })

    it('stops reporting once the handler is unregistered', async () => {
      unregister()
      setAuthToken('expired')
      await expect(api.get('/orders')).rejects.toBeDefined()
      expect(handler).not.toHaveBeenCalled()
    })
  })
})
