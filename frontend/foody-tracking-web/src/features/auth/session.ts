// Token persistence (CLAUDE.md §4.2): sessionStorage, so the session survives a reload but ends with the
// tab. Storage can throw (private mode, blocked site data); the app then just keeps the token in memory.

export const TOKEN_STORAGE_KEY = 'foody.token'

export function readStoredToken(): string | null {
  try {
    return sessionStorage.getItem(TOKEN_STORAGE_KEY)
  } catch {
    return null
  }
}

export function storeToken(token: string | null): void {
  try {
    if (token) sessionStorage.setItem(TOKEN_STORAGE_KEY, token)
    else sessionStorage.removeItem(TOKEN_STORAGE_KEY)
  } catch {
    // Memory-only session: nothing else to do.
  }
}
