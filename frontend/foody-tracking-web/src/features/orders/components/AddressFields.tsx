import { Box } from '@mui/material'
import { Controller, useFormContext } from 'react-hook-form'
import type { OrderFormValues } from '@/features/orders/schemas'
import { FormField } from '@/shared/components/FormField'
import { SelectField } from '@/shared/components/SelectField'
import { BRAZIL_STATES } from '@/shared/utils/brazil'
import { maskCep } from '@/shared/utils/masks'

export function AddressFields() {
  const {
    control,
    register,
    formState: { errors },
  } = useFormContext<OrderFormValues>()
  const fieldErrors = errors.deliveryAddress

  return (
    <Box
      sx={{
        display: 'grid',
        gap: 2,
        gridTemplateColumns: { xs: '1fr 1fr', md: 'repeat(6, minmax(0, 1fr))' },
        '& > :nth-of-type(1), & > :nth-of-type(3)': { gridColumn: { xs: 'span 1', md: 'span 2' } },
        '& > :nth-of-type(2), & > :nth-of-type(4)': { gridColumn: { xs: 'span 2', md: 'span 4' } },
        '& > :nth-of-type(5)': { gridColumn: { xs: 'span 2', md: 'span 3' } },
        '& > :nth-of-type(6)': { gridColumn: { xs: 'span 1', md: 'span 2' } },
        '& > :nth-of-type(7)': { gridColumn: { xs: 'span 1', md: 'span 1' } },
      }}
    >
      <Controller
        control={control}
        name="deliveryAddress.postalCode"
        render={({ field, fieldState }) => (
          <FormField
            label="CEP"
            inputMode="numeric"
            autoComplete="postal-code"
            placeholder="00000-000"
            error={fieldState.error?.message}
            {...field}
            onChange={(event) => field.onChange(maskCep(event.target.value))}
          />
        )}
      />
      <FormField
        label="Rua"
        autoComplete="address-line1"
        error={fieldErrors?.street?.message}
        {...register('deliveryAddress.street')}
      />
      <FormField
        label="Número"
        error={fieldErrors?.number?.message}
        {...register('deliveryAddress.number')}
      />
      <FormField
        label="Complemento"
        helperText="Opcional"
        autoComplete="address-line2"
        error={fieldErrors?.complement?.message}
        {...register('deliveryAddress.complement')}
      />
      <FormField
        label="Bairro"
        error={fieldErrors?.neighborhood?.message}
        {...register('deliveryAddress.neighborhood')}
      />
      <FormField
        label="Cidade"
        autoComplete="address-level2"
        error={fieldErrors?.city?.message}
        {...register('deliveryAddress.city')}
      />
      <SelectField
        label="UF"
        options={BRAZIL_STATES}
        error={fieldErrors?.state?.message}
        {...register('deliveryAddress.state')}
      />
    </Box>
  )
}
