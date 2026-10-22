import { forgotPasswordSchema, loginSchema, registerSchema } from './schemas'

function messages(result: {
  success: boolean
  error?: { issues: { path: PropertyKey[]; message: string }[] }
}) {
  return Object.fromEntries((result.error?.issues ?? []).map((i) => [i.path.join('.'), i.message]))
}

const validRegister = {
  name: 'Henrique Nogarini',
  email: 'henrique@restaurante.com',
  password: 'senha-forte-1',
  confirmPassword: 'senha-forte-1',
}

describe('loginSchema', () => {
  it('accepts an e-mail with surrounding spaces, like the API', () => {
    const result = loginSchema.safeParse({ email: '  henrique@restaurante.com ', password: 'x' })
    expect(result.success && result.data.email).toBe('henrique@restaurante.com')
  })

  it('requires both fields', () => {
    expect(messages(loginSchema.safeParse({ email: '', password: '' }))).toEqual({
      email: 'Informe seu e-mail.',
      password: 'Informe sua senha.',
    })
  })

  it('rejects a malformed e-mail', () => {
    expect(messages(loginSchema.safeParse({ email: 'henrique@', password: 'x' }))).toEqual({
      email: 'Informe um e-mail válido.',
    })
  })
})

describe('registerSchema', () => {
  it('accepts a valid sign-up', () => {
    expect(registerSchema.safeParse(validRegister).success).toBe(true)
  })

  it('rejects mismatched passwords on the confirmation field', () => {
    const result = registerSchema.safeParse({ ...validRegister, confirmPassword: 'outra-senha' })
    expect(messages(result)).toEqual({
      confirmPassword: 'As senhas não coincidem. Digite a mesma senha nos dois campos.',
    })
  })

  it('enforces the API password limits: 8 characters, at most 72 UTF-8 bytes', () => {
    const short = registerSchema.safeParse({
      ...validRegister,
      password: '1234567',
      confirmPassword: '1234567',
    })
    expect(messages(short).password).toBe('A senha precisa ter pelo menos 8 caracteres.')

    // 36 characters of 2 bytes each = 72 bytes: still accepted; one more character is not.
    const atLimit = 'ç'.repeat(36)
    expect(
      registerSchema.safeParse({ ...validRegister, password: atLimit, confirmPassword: atLimit })
        .success,
    ).toBe(true)
    const overLimit = 'ç'.repeat(37)
    const tooLong = registerSchema.safeParse({
      ...validRegister,
      password: overLimit,
      confirmPassword: overLimit,
    })
    expect(messages(tooLong).password).toBe('A senha pode ter no máximo 72 caracteres.')
  })

  it('requires a name of at most 100 characters', () => {
    expect(messages(registerSchema.safeParse({ ...validRegister, name: '   ' })).name).toBe(
      'Informe seu nome.',
    )
    expect(
      messages(registerSchema.safeParse({ ...validRegister, name: 'a'.repeat(101) })).name,
    ).toBe('O nome pode ter no máximo 100 caracteres.')
  })
})

describe('forgotPasswordSchema', () => {
  it('validates the new password and its confirmation', () => {
    const result = forgotPasswordSchema.safeParse({
      email: 'henrique@restaurante.com',
      newPassword: 'nova-senha-1',
      confirmPassword: 'nova-senha-2',
    })
    expect(messages(result)).toEqual({
      confirmPassword: 'As senhas não coincidem. Digite a mesma senha nos dois campos.',
    })
  })
})
