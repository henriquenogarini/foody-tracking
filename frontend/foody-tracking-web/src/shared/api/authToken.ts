// The HTTP client needs the session token, but shared/ must not import the auth feature (CLAUDE.md §3).
// This module is the extension point instead: auth writes the token here (and owns its persistence) and
// registers what a lost session means; http.ts only reads the token and reports the 401.

let token: string | null = null
let unauthorizedHandler: (() => void) | undefined

export function getAuthToken(): string | null {
  return token
}

export function setAuthToken(value: string | null): void {
  token = value
}

/** Registers the reaction to a rejected authenticated call. Returns the unregister function. */
export function onUnauthorized(handler: () => void): () => void {
  unauthorizedHandler = handler
  return () => {
    if (unauthorizedHandler === handler) unauthorizedHandler = undefined
  }
}

export function notifyUnauthorized(): void {
  unauthorizedHandler?.()
}
