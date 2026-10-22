// sessionStorage survives a reload but not the tab. If storage throws (private mode), the token stays in memory.

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
    // Memory-only session.
  }
}
