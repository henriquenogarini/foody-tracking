import ErrorOutlineIcon from '@mui/icons-material/ErrorOutlineOutlined'
import { Box, InputAdornment, OutlinedInput, Stack, type OutlinedInputProps } from '@mui/material'
import { useId, type ReactNode, type Ref } from 'react'

interface FormFieldProps extends Omit<OutlinedInputProps, 'error' | 'ref' | 'inputRef'> {
  label: string
  /** Validation message; also marks the input invalid. */
  error?: string
  helperText?: string
  icon?: ReactNode
  /** Goes to the <input> itself, so react-hook-form's register() can be spread in. */
  ref?: Ref<HTMLInputElement>
}

// Design form field: static label above a 44px input, optional leading icon, help or error text below.
export function FormField({ label, error, helperText, icon, id, ref, ...input }: FormFieldProps) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  const message = error ?? helperText
  const messageId = message ? `${inputId}-message` : undefined

  return (
    <Stack spacing={1}>
      <Box
        component="label"
        htmlFor={inputId}
        sx={{ typography: 'subtitle2', color: 'ink.700', width: 'fit-content' }}
      >
        {label}
      </Box>
      <OutlinedInput
        id={inputId}
        inputRef={ref}
        size="small"
        fullWidth
        error={Boolean(error)}
        startAdornment={
          icon ? (
            <InputAdornment position="start" sx={{ color: 'ink.500' }}>
              {icon}
            </InputAdornment>
          ) : undefined
        }
        {...input}
        slotProps={{
          ...input.slotProps,
          input: { 'aria-describedby': messageId, ...input.slotProps?.input },
        }}
      />
      {message && (
        <Box
          id={messageId}
          sx={{
            typography: 'body2',
            color: error ? 'error.main' : 'text.secondary',
            display: 'flex',
            alignItems: 'center',
            gap: 0.75,
          }}
        >
          {error && <ErrorOutlineIcon sx={{ fontSize: 16 }} aria-hidden />}
          {message}
        </Box>
      )}
    </Stack>
  )
}
