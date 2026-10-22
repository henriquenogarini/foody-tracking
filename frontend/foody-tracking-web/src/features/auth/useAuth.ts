import { createContext, useContext } from 'react'
import type { User } from './types'

export type AuthStatus = 'loading' | 'authenticated' | 'anonymous' | 'error'

export type SessionEnd = 'expired' | 'signedOut' | null

export interface AuthContextValue {
  status: AuthStatus
  user: User | null
  sessionEnd: SessionEnd
  signIn: (token: string) => void
  signOut: () => void
  acknowledgeSessionEnd: () => void
  retry: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth must be used inside <AuthProvider>')
  return value
}
