import { z } from 'zod'

// Mirrors the API's rules (CLAUDE.md §5): e-mail ≤ 254, name ≤ 100, password 8–72 characters and at
// most 72 UTF-8 bytes (the BCrypt limit). The API stays the authority; this only saves a round trip.
const PASSWORD_MAX_BYTES = 72

const email = z
  .string()
  .trim()
  .min(1, 'Informe seu e-mail.')
  .max(254, 'O e-mail pode ter no máximo 254 caracteres.')
  .pipe(z.email('Informe um e-mail válido.'))

const newPassword = z
  .string()
  .min(8, 'A senha precisa ter pelo menos 8 caracteres.')
  .refine(
    (value) => new TextEncoder().encode(value).length <= PASSWORD_MAX_BYTES,
    'A senha pode ter no máximo 72 caracteres.',
  )

const PASSWORD_MISMATCH = 'As senhas não coincidem. Digite a mesma senha nos dois campos.'

export const loginSchema = z.object({
  email,
  password: z.string().min(1, 'Informe sua senha.'),
})

export const registerSchema = z
  .object({
    name: z
      .string()
      .trim()
      .min(1, 'Informe seu nome.')
      .max(100, 'O nome pode ter no máximo 100 caracteres.'),
    email,
    password: newPassword,
    confirmPassword: z.string(),
  })
  .refine((values) => values.password === values.confirmPassword, {
    path: ['confirmPassword'],
    error: PASSWORD_MISMATCH,
  })

export const forgotPasswordSchema = z
  .object({
    email,
    newPassword,
    confirmPassword: z.string(),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    path: ['confirmPassword'],
    error: PASSWORD_MISMATCH,
  })

// Form state uses the input types; submit handlers receive the parsed output (trimmed e-mail and name).
export type LoginValues = z.input<typeof loginSchema>
export type RegisterValues = z.input<typeof registerSchema>
export type RegisterData = z.output<typeof registerSchema>
export type ForgotPasswordValues = z.input<typeof forgotPasswordSchema>
export type ForgotPasswordData = z.output<typeof forgotPasswordSchema>
