import '@testing-library/jest-dom/vitest'
import { setAuthToken } from '@/shared/api/authToken'
import { server } from './server'

// jsdom has no matchMedia: answer width queries from window.innerWidth (see setViewportWidth).
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

beforeAll(() => server.listen({ onUnhandledFrame: 'error' }))
afterEach(() => {
  server.resetHandlers()
  sessionStorage.clear()
  setAuthToken(null)
  window.innerWidth = 1024
})
afterAll(() => server.close())
