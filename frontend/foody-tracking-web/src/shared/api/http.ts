import axios, { isAxiosError } from 'axios'
import { getAuthToken, notifyUnauthorized } from './authToken'

const baseURL = import.meta.env.VITE_API_URL
if (!baseURL) {
  throw new Error('VITE_API_URL is not set: copy .env.example to .env (see README).')
}

// Single client for the whole app. Errors are rejected untouched (an AxiosError): callers turn them
// into an ApiError with toApiError() where they are shown, and the query retry policy reads the status.
export const api = axios.create({ baseURL, timeout: 15_000 })

api.interceptors.request.use((config) => {
  const token = getAuthToken()
  if (token) config.headers.set('Authorization', `Bearer ${token}`)
  return config
})

api.interceptors.response.use(undefined, (error: unknown) => {
  // Only an authenticated call that comes back 401 means the session is gone. A 401 from login
  // (no token sent) is just wrong credentials and must reach the form, not log anyone out.
  const authorization = isAxiosError(error) ? error.config?.headers.get('Authorization') : null
  if (isAxiosError(error) && error.response?.status === 401 && typeof authorization === 'string') {
    notifyUnauthorized(authorization.replace(/^Bearer /, ''))
  }
  return Promise.reject(error)
})
