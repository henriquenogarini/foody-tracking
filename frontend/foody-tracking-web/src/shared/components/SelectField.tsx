import ErrorOutlineIcon from '@mui/icons-material/ErrorOutlineOutlined'
import { Box, NativeSelect, OutlinedInput, Stack } from '@mui/material'
import { useId, type ComponentProps, type Ref } from 'react'

interface SelectFieldProps extends Omit<ComponentProps<'select'>, 'ref'> {
  label: string
  options: readonly string[]
  error?: string
  ref?: Ref<HTMLSelectElement>
}

// Native select (the phone's own picker) with the same label and error layout as FormField.
export function SelectField({ label, options, error, id, ref, ...select }: SelectFieldProps) {
  const generatedId = useId()
  const selectId = id ?? generatedId
  const messageId = error ? `${selectId}-message` : undefined

  return (
    <Stack spacing={1}>
      <Box
        component="label"
        htmlFor={selectId}
        sx={{ typography: 'subtitle2', color: 'ink.700', width: 'fit-content' }}
      >
        {label}
      </Box>
      <NativeSelect
        input={<OutlinedInput size="small" fullWidth error={Boolean(error)} />}
        inputProps={{
          ...select,
          id: selectId,
          inputRef: ref,
          'aria-invalid': Boolean(error),
          'aria-describedby': messageId,
        }}
      >
        <option value="" disabled>
          Selecione
        </option>
        {options.map((option) => (
          <option key={option} value={option}>
            {option}
          </option>
        ))}
      </NativeSelect>
      {error && (
        <Box
          id={messageId}
          sx={{
            typography: 'body2',
            color: 'error.main',
            display: 'flex',
            alignItems: 'center',
            gap: 0.75,
          }}
        >
          <ErrorOutlineIcon sx={{ fontSize: 16 }} aria-hidden />
          {error}
        </Box>
      )}
    </Stack>
  )
}
