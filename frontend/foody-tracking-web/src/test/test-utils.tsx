import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
// RouterProvider from 'react-router' (not 'react-router/dom'): under Vitest the two entry points can
// resolve to different CJS/ESM builds with separate contexts. The /dom variant only adds flushSync.
import { createMemoryRouter, RouterProvider } from 'react-router'
import { routes } from '@/app/router'
import { theme } from '@/app/theme'
import { AuthProvider } from '@/features/auth/AuthProvider'
import { TOKEN_STORAGE_KEY } from '@/features/auth/session'
import type { User } from '@/features/auth/types'
import { apiUrl, server } from './server'

export const testUser: User = {
  id: '3f6c1a2e-8d4b-4c7a-9e1f-2b5d7a9c0e11',
  name: 'Henrique Nogarini',
  email: 'henrique@restaurante.com',
  role: 'USER',
  createdAt: '2026-10-21T13:00:00-03:00',
}

/** Answers GET /auth/me with `user`, as the API does for a valid token. */
export function mockMe(user: User = testUser) {
  server.use(http.get(apiUrl('/auth/me'), () => HttpResponse.json(user)))
}

interface RenderRouteOptions {
  /** Starts with a stored session for this user. */
  signedInAs?: User
}

/** Renders the real route tree at `path` with fresh providers, as the app does at startup. */
export function renderRoute(path: string, { signedInAs }: RenderRouteOptions = {}) {
  if (signedInAs) {
    sessionStorage.setItem(TOKEN_STORAGE_KEY, 'stored-token')
    mockMe(signedInAs)
  }
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  const view = render(
    <ThemeProvider theme={theme}>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <RouterProvider router={router} />
        </AuthProvider>
      </QueryClientProvider>
    </ThemeProvider>,
  )
  return { ...view, router, user: userEvent.setup() }
}
