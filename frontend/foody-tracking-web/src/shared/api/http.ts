import axios, { isAxiosError } from 'axios'
import { getAuthToken, notifyUnauthorized } from './authToken'

const baseURL = import.meta.env.VITE_API_URL
if (!baseURL) {
  throw new Error('VITE_API_URL is not set: copy .env.example to .env (see README).')
}

// Errors are rejected as AxiosError; callers convert them with toApiError() where they are shown.
export const api = axios.create({ baseURL, timeout: 15_000 })

api.interceptors.request.use((config) => {
  const token = getAuthToken()
  if (token) config.headers.set('Authorization', `Bearer ${token}`)
  return config
})

api.interceptors.response.use(undefined, (error: unknown) => {
  // Only a 401 that carried a token ends the session; a login 401 must reach the form.
  const authorization = isAxiosError(error) ? error.config?.headers.get('Authorization') : null
  if (isAxiosError(error) && error.response?.status === 401 && typeof authorization === 'string') {
    notifyUnauthorized(authorization.replace(/^Bearer /, ''))
  }
  return Promise.reject(error)
})
