import { useEffect } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router'
import { SessionGate } from '@/features/auth/components/SessionGate'
import { useAuth } from './useAuth'

function returnPath(state: unknown): string {
  if (typeof state !== 'object' || state === null || !('from' in state)) return '/orders'
  const from = state.from
  if (typeof from !== 'object' || from === null || !('pathname' in from)) return '/orders'
  const search = 'search' in from && typeof from.search === 'string' ? from.search : ''
  return typeof from.pathname === 'string' ? from.pathname + search : '/orders'
}

export function GuestRoute() {
  const { status, sessionEnd, acknowledgeSessionEnd } = useAuth()
  const location = useLocation()

  // Forget the end reason once shown, so a later anonymous visit is not reported as an expiry.
  useEffect(() => {
    if (sessionEnd) acknowledgeSessionEnd()
  }, [sessionEnd, acknowledgeSessionEnd])

  if (status === 'authenticated') return <Navigate to={returnPath(location.state)} replace />
  if (status !== 'anonymous') return <SessionGate />
  return <Outlet />
}
