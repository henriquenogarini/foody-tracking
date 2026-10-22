export type UserRole = 'USER' | 'ADMIN'

export interface User {
  id: string
  name: string
  email: string
  role: UserRole
  createdAt: string
}

export interface LoginInput {
  email: string
  password: string
}

export interface RegisterInput {
  name: string
  email: string
  password: string
}

export interface PasswordResetInput {
  email: string
  newPassword: string
}

export interface TokenResponse {
  accessToken: string
  tokenType: 'Bearer'
  /** Seconds until the token expires. */
  expiresIn: number
}
