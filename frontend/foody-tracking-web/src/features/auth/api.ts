import { api } from '@/shared/api/http'
import type { LoginInput, PasswordResetInput, RegisterInput, TokenResponse, User } from './types'

export async function login(input: LoginInput): Promise<TokenResponse> {
  const { data } = await api.post<TokenResponse>('/auth/login', input)
  return data
}

/** Creates the account; the API returns the user but no token, so callers log in afterwards. */
export async function register(input: RegisterInput): Promise<User> {
  const { data } = await api.post<User>('/auth/register', input)
  return data
}

export async function getMe(): Promise<User> {
  const { data } = await api.get<User>('/auth/me')
  return data
}

/** Demo reset without e-mail confirmation: always 204, whether or not the e-mail exists. */
export async function resetPassword(input: PasswordResetInput): Promise<void> {
  await api.post('/auth/password-reset', input)
}
