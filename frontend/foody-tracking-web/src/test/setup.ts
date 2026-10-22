import '@testing-library/jest-dom/vitest'
import { setAuthToken } from '@/shared/api/authToken'
import { server } from './server'

// jsdom has no matchMedia. This one answers min/max-width queries against window.innerWidth, so tests
// pick the desktop (default 1024px) or mobile layout with setViewportWidth() from test-utils.
window.matchMedia = (query: string) => {
  const min = /min-width:\s*([\d.]+)px/.exec(query)
  const max = /max-width:\s*([\d.]+)px/.exec(query)
  const width = window.innerWidth
  const matches = (!min || width >= Number(min[1])) && (!max || width <= Number(max[1]))
  return {
    matches,
    media: query,
    onchange: null,
    addEventListener: () => {},
    removeEventListener: () => {},
    addListener: () => {},
    removeListener: () => {},
    dispatchEvent: () => false,
  }
}

// Any request without a handler fails the test: no test may silently hit a real network.
beforeAll(() => server.listen({ onUnhandledFrame: 'error' }))
afterEach(() => {
  server.resetHandlers()
  sessionStorage.clear()
  setAuthToken(null)
  window.innerWidth = 1024
})
afterAll(() => server.close())
