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

// Runs during the first render: child effects run before the parent's, so an effect would be too late.
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

  // A late 401 carrying an older token (from before the current sign-in) is ignored.
  useEffect(
    () =>
      onUnauthorized((rejectedToken) => {
        if (rejectedToken === getAuthToken()) endSession('expired')
      }),
    [endSession],
  )

  const value = useMemo<AuthContextValue>(() => {
    // A failed background refetch must not end a working session.
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
