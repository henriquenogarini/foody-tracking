import { zodResolver } from '@hookform/resolvers/zod'
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft'
import { Alert, Box, Link, Stack, Typography, useMediaQuery, useTheme } from '@mui/material'
import { useId } from 'react'
import { Controller, FormProvider, useForm } from 'react-hook-form'
import { Link as RouterLink, useNavigate } from 'react-router'
import { AddressFields } from '@/features/orders/components/AddressFields'
import { OrderItemsSection } from '@/features/orders/components/OrderItemsSection'
import { OrderSummaryPanel } from '@/features/orders/components/OrderSummaryPanel'
import { useCreateOrder } from '@/features/orders/queries'
import {
  createOrderSchema,
  emptyOrderForm,
  isOrderFormPath,
  type OrderFormValues,
} from '@/features/orders/schemas'
import type { CreateOrderInput } from '@/features/orders/types'
import { toApiError } from '@/shared/api/problem'
import { FormField } from '@/shared/components/FormField'
import { SectionCard } from '@/shared/components/SectionCard'
import { useSnackbar } from '@/shared/hooks/useSnackbar'
import { applyFieldErrors } from '@/shared/utils/forms'
import { maskPhone } from '@/shared/utils/masks'

export default function NewOrderPage() {
  const isDesktop = useMediaQuery(useTheme().breakpoints.up('md'), { noSsr: true })
  const formId = useId()
  const navigate = useNavigate()
  const notify = useSnackbar()
  const createOrder = useCreateOrder()

  const form = useForm<OrderFormValues, unknown, CreateOrderInput>({
    resolver: zodResolver(createOrderSchema),
    mode: 'onTouched',
    defaultValues: emptyOrderForm,
  })
  const {
    control,
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = form

  const onSubmit = handleSubmit((input) =>
    createOrder.mutate(input, {
      onSuccess: (order) => {
        notify(`Pedido #${order.code} criado`)
        navigate(`/orders/${order.id}`)
      },
      // The API validates too; its field errors land on the matching inputs (paths mirror the form).
      onError: (error) => applyFieldErrors(toApiError(error), setError, isOrderFormPath),
    }),
  )

  const apiError = createOrder.error ? toApiError(createOrder.error) : null
  // Errors shown next to an input need no banner; anything else (other paths, 5xx, network) does.
  const showApiError = apiError && !Object.keys(apiError.fieldErrors).some(isOrderFormPath)

  return (
    <FormProvider {...form}>
      <Stack spacing={{ xs: 2, md: 3.5 }} sx={{ pb: { xs: 10, md: 0 } }}>
        <Stack spacing={1}>
          <Link
            component={RouterLink}
            to="/orders"
            underline="hover"
            sx={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 0.5,
              minHeight: 44,
              width: 'fit-content',
              color: 'ink.700',
              fontWeight: 500,
            }}
          >
            <ChevronLeftIcon fontSize="small" />
            Pedidos
          </Link>
          <Typography
            variant="h2"
            component="h1"
            sx={{ fontSize: { xs: 24, md: 32 }, lineHeight: { xs: '30px', md: '38px' } }}
          >
            Novo pedido
          </Typography>
        </Stack>

        {showApiError && <Alert severity="error">{apiError.message}</Alert>}

        <Box
          component="form"
          id={formId}
          noValidate
          onSubmit={onSubmit}
          aria-label="Novo pedido"
          sx={{
            display: 'grid',
            gap: 3,
            gridTemplateColumns: { xs: '1fr', md: 'minmax(0, 1fr) 360px' },
            alignItems: 'start',
          }}
        >
          <Stack spacing={3}>
            <SectionCard title="Cliente">
              <FormField
                label="Nome"
                autoComplete="name"
                error={errors.customer?.name?.message}
                {...register('customer.name')}
              />
              <Box
                sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' } }}
              >
                <Controller
                  control={control}
                  name="customer.phone"
                  render={({ field, fieldState }) => (
                    <FormField
                      label="Telefone"
                      type="tel"
                      inputMode="tel"
                      autoComplete="tel-national"
                      placeholder="(11) 98752-1144"
                      helperText="Opcional"
                      error={fieldState.error?.message}
                      {...field}
                      onChange={(event) => field.onChange(maskPhone(event.target.value))}
                    />
                  )}
                />
                <FormField
                  label="E-mail"
                  type="email"
                  autoComplete="email"
                  placeholder="cliente@exemplo.com"
                  helperText="Opcional"
                  error={errors.customer?.email?.message}
                  {...register('customer.email')}
                />
              </Box>
            </SectionCard>

            <OrderItemsSection />

            <SectionCard title="Endereço de entrega">
              <AddressFields />
            </SectionCard>

            <SectionCard title="Observações">
              <FormField
                label="Instruções para a cozinha ou o entregador"
                multiline
                minRows={3}
                placeholder="Ex.: sem cebola, interfone 03"
                error={errors.notes?.message}
                {...register('notes')}
              />
            </SectionCard>
          </Stack>

          {isDesktop && (
            <OrderSummaryPanel formId={formId} submitting={createOrder.isPending} variant="side" />
          )}
        </Box>

        {!isDesktop && (
          <OrderSummaryPanel formId={formId} submitting={createOrder.isPending} variant="bar" />
        )}
      </Stack>
    </FormProvider>
  )
}
