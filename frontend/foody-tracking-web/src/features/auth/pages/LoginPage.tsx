import { zodResolver } from '@hookform/resolvers/zod'
import MailOutlineIcon from '@mui/icons-material/MailOutlined'
import { Alert, Box, Button, Link, Stack } from '@mui/material'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link as RouterLink, useSearchParams } from 'react-router'
import { login } from '@/features/auth/api'
import { AuthLayout } from '@/features/auth/components/AuthLayout'
import { PasswordField } from '@/features/auth/components/PasswordField'
import { loginSchema, type LoginValues } from '@/features/auth/schemas'
import { useAuth } from '@/features/auth/useAuth'
import { toApiError } from '@/shared/api/problem'
import { FormField } from '@/shared/components/FormField'
import { applyFieldErrors } from '@/shared/utils/forms'

export default function LoginPage() {
  const { signIn } = useAuth()
  const [searchParams] = useSearchParams()
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    mode: 'onTouched',
    defaultValues: { email: '', password: '' },
  })

  // GuestRoute redirects on success; this page never navigates itself.
  const mutation = useMutation({
    mutationFn: login,
    onSuccess: ({ accessToken }) => signIn(accessToken),
    onError: (error) => applyFieldErrors(toApiError(error), setError, ['email', 'password']),
  })
  const apiError = mutation.error ? toApiError(mutation.error) : null
  const showApiError = apiError && Object.keys(apiError.fieldErrors).length === 0

  return (
    <AuthLayout
      title="Bem-vindo de volta"
      subtitle="Entre para acompanhar os pedidos da sua operação em tempo real."
      footer={
        <>
          Ainda não tem conta?{' '}
          <Link component={RouterLink} to="/register" sx={{ fontWeight: 700 }}>
            Criar conta
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
        {searchParams.get('reason') === 'expired' && !mutation.isError && (
          <Alert severity="warning">Sua sessão expirou. Entre novamente para continuar.</Alert>
        )}
        {showApiError && <Alert severity="error">{apiError.message}</Alert>}
        <FormField
          label="E-mail"
          type="email"
          autoComplete="email"
          placeholder="voce@restaurante.com"
          icon={<MailOutlineIcon fontSize="small" />}
          error={errors.email?.message}
          {...register('email')}
        />
        <Stack spacing={0.25}>
          <PasswordField
            label="Senha"
            autoComplete="current-password"
            error={errors.password?.message}
            {...register('password')}
          />
          <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
            <Link
              component={RouterLink}
              to="/forgot-password"
              variant="body2"
              sx={{ display: 'inline-flex', alignItems: 'center', minHeight: 44 }}
            >
              Esqueci a senha
            </Link>
          </Box>
        </Stack>
        <Button
          type="submit"
          variant="contained"
          fullWidth
          loading={mutation.isPending}
          sx={{ height: 48, fontSize: 16 }}
        >
          Entrar
        </Button>
      </Stack>
    </AuthLayout>
  )
}
