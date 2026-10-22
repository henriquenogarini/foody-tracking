import { createContext, useContext } from 'react'
import type { User } from './types'

export type AuthStatus = 'loading' | 'authenticated' | 'anonymous' | 'error'

/** Why the last session ended, until a guest page acknowledges it: drives the login redirect. */
export type SessionEnd = 'expired' | 'signedOut' | null

export interface AuthContextValue {
  status: AuthStatus
  /** Set only while `status` is `authenticated`. */
  user: User | null
  sessionEnd: SessionEnd
  signIn: (token: string) => void
  signOut: () => void
  /** Clears `sessionEnd` once the guest pages have shown it. */
  acknowledgeSessionEnd: () => void
  /** Retries loading the user after a non-auth failure (`status` is `error`). */
  retry: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth must be used inside <AuthProvider>')
  return value
}
