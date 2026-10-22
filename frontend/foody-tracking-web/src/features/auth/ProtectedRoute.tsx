import { Navigate, Outlet, useLocation } from 'react-router'
import { SessionGate } from '@/features/auth/components/SessionGate'
import { useAuth } from './useAuth'

/** Renders the private routes only for a signed-in user; anyone else goes to /login. */
export function ProtectedRoute() {
  const { status, sessionEnd } = useAuth()
  const location = useLocation()

  if (status === 'anonymous') {
    // After an expiry or a first visit the user comes back to this page once signed in; after
    // "Sair" they do not, so the next person to sign in starts from the orders list.
    if (sessionEnd === 'signedOut') return <Navigate to="/login" replace />
    return (
      <Navigate
        to={sessionEnd === 'expired' ? '/login?reason=expired' : '/login'}
        replace
        state={{ from: location }}
      />
    )
  }
  if (status !== 'authenticated') return <SessionGate />
  return <Outlet />
}
