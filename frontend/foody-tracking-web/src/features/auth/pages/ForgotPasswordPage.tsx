import { zodResolver } from '@hookform/resolvers/zod'
import MailOutlineIcon from '@mui/icons-material/MailOutlined'
import { Alert, Button, Link, Stack } from '@mui/material'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link as RouterLink } from 'react-router'
import { resetPassword } from '@/features/auth/api'
import { AuthLayout } from '@/features/auth/components/AuthLayout'
import { PasswordField } from '@/features/auth/components/PasswordField'
import {
  forgotPasswordSchema,
  type ForgotPasswordData,
  type ForgotPasswordValues,
} from '@/features/auth/schemas'
import { toApiError } from '@/shared/api/problem'
import { FormField } from '@/shared/components/FormField'
import { applyFieldErrors } from '@/shared/utils/forms'

// Demo reset without e-mail confirmation (backend README decision 30). The API always answers 204 so it
// never reveals whether an account exists; the success message says the same.
export default function ForgotPasswordPage() {
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<ForgotPasswordValues, unknown, ForgotPasswordData>({
    resolver: zodResolver(forgotPasswordSchema),
    mode: 'onTouched',
    defaultValues: { email: '', newPassword: '', confirmPassword: '' },
  })

  const mutation = useMutation({
    mutationFn: ({ email, newPassword }: ForgotPasswordData) =>
      resetPassword({ email, newPassword }),
    onError: (error) => applyFieldErrors(toApiError(error), setError, ['email', 'newPassword']),
  })
  const apiError = mutation.error ? toApiError(mutation.error) : null
  // 404 means the feature is switched off on the server (APP_SECURITY_PASSWORD_RESET_ENABLED=false).
  let errorMessage: string | null = null
  if (apiError?.status === 404) errorMessage = 'Recuperação de senha indisponível no momento.'
  else if (apiError && Object.keys(apiError.fieldErrors).length === 0)
    errorMessage = apiError.message

  const backToLogin = (
    <Link component={RouterLink} to="/login" sx={{ fontWeight: 700 }}>
      Voltar para o login
    </Link>
  )

  if (mutation.isSuccess) {
    return (
      <AuthLayout
        title="Senha redefinida"
        subtitle="Se o e-mail estiver cadastrado, a senha foi alterada. Entre com a nova senha."
        footer={backToLogin}
      >
        <Button
          variant="contained"
          fullWidth
          component={RouterLink}
          to="/login"
          sx={{ height: 48, fontSize: 16 }}
        >
          Ir para o login
        </Button>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout
      title="Recuperar senha"
      subtitle="Informe o e-mail da conta e escolha uma nova senha."
      footer={<>Lembrou a senha? {backToLogin}</>}
    >
      <Stack
        component="form"
        noValidate
        spacing={2.25}
        onSubmit={handleSubmit((values) => mutation.mutate(values))}
      >
        {errorMessage && <Alert severity="error">{errorMessage}</Alert>}
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
          label="Nova senha"
          autoComplete="new-password"
          placeholder="Mínimo de 8 caracteres"
          error={errors.newPassword?.message}
          {...register('newPassword')}
        />
        <PasswordField
          label="Confirmar nova senha"
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
          Redefinir senha
        </Button>
      </Stack>
    </AuthLayout>
  )
}
