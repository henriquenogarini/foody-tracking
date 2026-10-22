import '@testing-library/jest-dom/vitest'
import { setAuthToken } from '@/shared/api/authToken'
import { server } from './server'

// Any request without a handler fails the test: no test may silently hit a real network.
beforeAll(() => server.listen({ onUnhandledFrame: 'error' }))
afterEach(() => {
  server.resetHandlers()
  sessionStorage.clear()
  setAuthToken(null)
})
afterAll(() => server.close())
