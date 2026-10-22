import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useReducer, type ReactNode } from 'react'
import { getAuthToken, onUnauthorized, setAuthToken } from '@/shared/api/authToken'
import { getMe } from './api'
import { readStoredToken, storeToken } from './session'
import { AuthContext, type AuthContextValue, type AuthStatus, type SessionEnd } from './useAuth'

interface SessionState {
  token: string | null
  end: SessionEnd
}

type SessionAction =
  | { type: 'signedIn'; token: string }
  | { type: 'ended'; reason: 'expired' | 'signedOut' }
  | { type: 'acknowledged' }

function reducer(state: SessionState, action: SessionAction): SessionState {
  switch (action.type) {
    case 'signedIn':
      return { token: action.token, end: null }
    case 'ended':
      return state.token ? { token: null, end: action.reason } : state
    case 'acknowledged':
      return state.end ? { ...state, end: null } : state
  }
}

// The stored token is handed to the HTTP client during the first render, before any child query runs
// (child effects run before the parent's, so an effect here would be too late). It is idempotent.
function restoreSession(): SessionState {
  const token = readStoredToken()
  setAuthToken(token)
  return { token, end: null }
}

const meQueryKey = ['auth', 'me'] as const

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [session, dispatch] = useReducer(reducer, undefined, restoreSession)

  const {
    data: me,
    isError,
    refetch,
  } = useQuery({
    queryKey: meQueryKey,
    queryFn: getMe,
    enabled: session.token !== null,
    staleTime: Infinity,
  })

  // Side effects live here, not in the reducer, which must stay pure.
  const endSession = useCallback(
    (reason: 'expired' | 'signedOut') => {
      storeToken(null)
      setAuthToken(null)
      queryClient.clear()
      dispatch({ type: 'ended', reason })
    },
    [queryClient],
  )

  const signIn = useCallback((token: string) => {
    storeToken(token)
    setAuthToken(token)
    dispatch({ type: 'signedIn', token })
  }, [])

  const signOut = useCallback(() => endSession('signedOut'), [endSession])
  const acknowledgeSessionEnd = useCallback(() => dispatch({ type: 'acknowledged' }), [])
  const retry = useCallback(() => void refetch(), [refetch])

  // A subscription, not data fetching: an authenticated call rejected with 401 ends the session, unless
  // it carried an older token (a request from before the current sign-in that came back late).
  useEffect(
    () =>
      onUnauthorized((rejectedToken) => {
        if (rejectedToken === getAuthToken()) endSession('expired')
      }),
    [endSession],
  )

  const value = useMemo<AuthContextValue>(() => {
    // Data first: a failed background refetch must not take a working session away.
    let status: AuthStatus = 'loading'
    if (session.token === null) status = 'anonymous'
    else if (me) status = 'authenticated'
    else if (isError) status = 'error'
    return {
      status,
      user: status === 'authenticated' ? (me ?? null) : null,
      sessionEnd: session.end,
      signIn,
      signOut,
      acknowledgeSessionEnd,
      retry,
    }
  }, [session, me, isError, signIn, signOut, acknowledgeSessionEnd, retry])

  return <AuthContext value={value}>{children}</AuthContext>
}
