import ErrorOutlineIcon from '@mui/icons-material/ErrorOutlineOutlined'
import { Box, InputAdornment, OutlinedInput, Stack, type OutlinedInputProps } from '@mui/material'
import { useId, type ReactNode, type Ref } from 'react'

const visuallyHidden = {
  position: 'absolute',
  // Strings on purpose: in sx a bare 1 means 100%.
  width: '1px',
  height: '1px',
  overflow: 'hidden',
  clip: 'rect(0 0 0 0)',
  whiteSpace: 'nowrap',
} as const

interface FormFieldProps extends Omit<OutlinedInputProps, 'error' | 'ref' | 'inputRef'> {
  label: string
  error?: string
  helperText?: string
  icon?: ReactNode
  hideLabel?: boolean
  ref?: Ref<HTMLInputElement>
}

export function FormField({
  label,
  error,
  helperText,
  icon,
  hideLabel = false,
  id,
  ref,
  ...input
}: FormFieldProps) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  const message = error ?? helperText
  const messageId = message ? `${inputId}-message` : undefined

  return (
    <Stack spacing={1}>
      <Box
        component="label"
        htmlFor={inputId}
        sx={
          hideLabel
            ? visuallyHidden
            : { typography: 'subtitle2', color: 'ink.700', width: 'fit-content' }
        }
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
