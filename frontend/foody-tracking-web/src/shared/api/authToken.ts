// Lets auth hand the token to http.ts without shared/ importing the auth feature.

let token: string | null = null
let unauthorizedHandler: ((rejectedToken: string) => void) | undefined

export function getAuthToken(): string | null {
  return token
}

export function setAuthToken(value: string | null): void {
  token = value
}

/** The handler gets the token the rejected call carried, so a late 401 from an older session can be ignored. */
export function onUnauthorized(handler: (rejectedToken: string) => void): () => void {
  unauthorizedHandler = handler
  return () => {
    if (unauthorizedHandler === handler) unauthorizedHandler = undefined
  }
}

export function notifyUnauthorized(rejectedToken: string): void {
  unauthorizedHandler?.(rejectedToken)
}
