import { useEffect } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router'
import { SessionGate } from '@/features/auth/components/SessionGate'
import { useAuth } from './useAuth'

// ProtectedRoute stores where the user was going; after signing in they land there instead of /orders.
function returnPath(state: unknown): string {
  if (typeof state !== 'object' || state === null || !('from' in state)) return '/orders'
  const from = state.from
  if (typeof from !== 'object' || from === null || !('pathname' in from)) return '/orders'
  const search = 'search' in from && typeof from.search === 'string' ? from.search : ''
  return typeof from.pathname === 'string' ? from.pathname + search : '/orders'
}

/** Login, sign-up and password reset: a signed-in user is sent into the app instead. */
export function GuestRoute() {
  const { status, sessionEnd, acknowledgeSessionEnd } = useAuth()
  const location = useLocation()

  // The reason the session ended has reached the guest pages (the login shows ?reason=expired from the
  // URL); forget it so a later anonymous visit to a private page is not reported as an expiry again.
  useEffect(() => {
    if (sessionEnd) acknowledgeSessionEnd()
  }, [sessionEnd, acknowledgeSessionEnd])

  if (status === 'authenticated') return <Navigate to={returnPath(location.state)} replace />
  if (status !== 'anonymous') return <SessionGate />
  return <Outlet />
}
