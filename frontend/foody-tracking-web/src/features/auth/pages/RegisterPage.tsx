import { zodResolver } from '@hookform/resolvers/zod'
import MailOutlineIcon from '@mui/icons-material/MailOutlined'
import PersonOutlineIcon from '@mui/icons-material/PersonOutlined'
import { Alert, Button, Link, Stack } from '@mui/material'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link as RouterLink } from 'react-router'
import { login, register as createAccount } from '@/features/auth/api'
import { AuthLayout } from '@/features/auth/components/AuthLayout'
import { PasswordField } from '@/features/auth/components/PasswordField'
import { registerSchema, type RegisterData, type RegisterValues } from '@/features/auth/schemas'
import { useAuth } from '@/features/auth/useAuth'
import { toApiError } from '@/shared/api/problem'
import { FormField } from '@/shared/components/FormField'
import { applyFieldErrors } from '@/shared/utils/forms'

// Account created but the automatic sign-in failed: resubmitting would only say "e-mail já cadastrado".
class SignInAfterSignUpError extends Error {}

export default function RegisterPage() {
  const { signIn } = useAuth()
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<RegisterValues, unknown, RegisterData>({
    resolver: zodResolver(registerSchema),
    mode: 'onTouched',
    defaultValues: { name: '', email: '', password: '', confirmPassword: '' },
  })

  const mutation = useMutation({
    mutationFn: async ({ name, email, password }: RegisterData) => {
      await createAccount({ name, email, password })
      try {
        return await login({ email, password })
      } catch (cause) {
        throw new SignInAfterSignUpError('Account created, sign-in failed', { cause })
      }
    },
    onSuccess: ({ accessToken }) => signIn(accessToken),
    onError: (error) => {
      const apiError = toApiError(error)
      if (error instanceof SignInAfterSignUpError) return
      if (apiError.code === 'USER_EMAIL_ALREADY_EXISTS') {
        setError('email', { type: 'server', message: apiError.message }, { shouldFocus: true })
      } else {
        applyFieldErrors(apiError, setError, ['name', 'email', 'password'])
      }
    },
  })
  const accountCreated = mutation.error instanceof SignInAfterSignUpError
  const apiError = mutation.error && !accountCreated ? toApiError(mutation.error) : null
  const showApiError =
    apiError &&
    apiError.code !== 'USER_EMAIL_ALREADY_EXISTS' &&
    Object.keys(apiError.fieldErrors).length === 0

  return (
    <AuthLayout
      title="Crie sua conta"
      subtitle="Leva um minuto. Depois é só cadastrar o primeiro pedido."
      footer={
        <>
          Já tem conta?{' '}
          <Link component={RouterLink} to="/login" sx={{ fontWeight: 700 }}>
            Entrar
          </Link>
        </>
      }
    >
      <Stack
        component="form"
        noValidate
        spacing={2.25}
        onSubmit={handleSubmit((values) => mutation.mutate(values))}
      >
        {accountCreated && (
          <Alert severity="info">
            Conta criada, mas não foi possível entrar agora.{' '}
            <Link component={RouterLink} to="/login" sx={{ fontWeight: 700 }}>
              Entre com seu e-mail e senha
            </Link>
            .
          </Alert>
        )}
        {showApiError && <Alert severity="error">{apiError.message}</Alert>}
        <FormField
          label="Nome completo"
          autoComplete="name"
          placeholder="Como está no seu documento"
          icon={<PersonOutlineIcon fontSize="small" />}
          error={errors.name?.message}
          {...register('name')}
        />
        <FormField
          label="E-mail"
          type="email"
          autoComplete="email"
          placeholder="voce@restaurante.com"
          icon={<MailOutlineIcon fontSize="small" />}
          error={errors.email?.message}
          {...register('email')}
        />
        <PasswordField
          label="Senha"
          autoComplete="new-password"
          helperText="Mínimo de 8 caracteres."
          error={errors.password?.message}
          {...register('password')}
        />
        <PasswordField
          label="Confirmar senha"
          autoComplete="new-password"
          placeholder="Repita a senha"
          error={errors.confirmPassword?.message}
          {...register('confirmPassword')}
        />
        <Button
          type="submit"
          variant="contained"
          fullWidth
          loading={mutation.isPending}
          sx={{ height: 48, fontSize: 16 }}
        >
          Criar conta
        </Button>
      </Stack>
    </AuthLayout>
  )
}
