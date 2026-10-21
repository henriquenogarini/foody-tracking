import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
// RouterProvider from 'react-router' (not 'react-router/dom'): under Vitest the two entry points can
// resolve to different CJS/ESM builds with separate contexts. The /dom variant only adds flushSync.
import { createMemoryRouter, RouterProvider } from 'react-router'
import { routes } from '@/app/router'
import { theme } from '@/app/theme'

/** Renders the real route tree at `path`, with a fresh, non-retrying query cache per test. */
export function renderRoute(path: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  const user = userEvent.setup()
  const view = render(
    <ThemeProvider theme={theme}>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </ThemeProvider>,
  )
  return { ...view, router, user }
}
