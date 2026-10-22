import { Navigate, Outlet, useLocation } from 'react-router'
import { SessionGate } from '@/features/auth/components/SessionGate'
import { useAuth } from './useAuth'

export function ProtectedRoute() {
  const { status, sessionEnd } = useAuth()
  const location = useLocation()

  if (status === 'anonymous') {
    // After "Sair" the next person to sign in starts from the orders list, not from this page.
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
