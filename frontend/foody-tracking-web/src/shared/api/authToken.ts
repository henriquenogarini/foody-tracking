// The HTTP client needs the session token, but shared/ must not import the auth feature (CLAUDE.md §3).
// This module is the extension point instead: auth writes the token here (and owns its persistence) and
// registers what a lost session means; http.ts only reads the token and reports the 401.

let token: string | null = null
let unauthorizedHandler: ((rejectedToken: string) => void) | undefined

export function getAuthToken(): string | null {
  return token
}

export function setAuthToken(value: string | null): void {
  token = value
}

/**
 * Registers the reaction to a rejected authenticated call; it receives the token that call carried, so a
 * late 401 from a previous session can be told apart from the current one. Returns the unregister function.
 */
export function onUnauthorized(handler: (rejectedToken: string) => void): () => void {
  unauthorizedHandler = handler
  return () => {
    if (unauthorizedHandler === handler) unauthorizedHandler = undefined
  }
}

export function notifyUnauthorized(rejectedToken: string): void {
  unauthorizedHandler?.(rejectedToken)
}
