import { setupServer } from 'msw/node'

/** API mock shared by every test; handlers are added per test with server.use(). */
export const server = setupServer()

/** Absolute URL of an API path, as the HTTP client will request it in tests. */
export const apiUrl = (path: string) => `${import.meta.env.VITE_API_URL}${path}`
