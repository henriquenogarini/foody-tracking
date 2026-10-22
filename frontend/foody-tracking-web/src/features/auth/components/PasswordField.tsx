import LockOutlinedIcon from '@mui/icons-material/LockOutlined'
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined'
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined'
import { IconButton, InputAdornment } from '@mui/material'
import { useState, type ComponentProps } from 'react'
import { FormField } from '@/shared/components/FormField'

type PasswordFieldProps = Omit<ComponentProps<typeof FormField>, 'type' | 'icon' | 'endAdornment'>

/** Password input with the design's lock icon and a show/hide toggle. */
export function PasswordField(props: PasswordFieldProps) {
  const [visible, setVisible] = useState(false)

  return (
    <FormField
      {...props}
      type={visible ? 'text' : 'password'}
      icon={<LockOutlinedIcon fontSize="small" />}
      endAdornment={
        <InputAdornment position="end">
          <IconButton
            edge="end"
            aria-label={visible ? 'Ocultar senha' : 'Mostrar senha'}
            aria-pressed={visible}
            onClick={() => setVisible((current) => !current)}
            // 44px touch target, like the design's input adornment.
            sx={{ color: 'ink.500', width: 44, height: 44 }}
          >
            {visible ? (
              <VisibilityOffOutlinedIcon fontSize="small" />
            ) : (
              <VisibilityOutlinedIcon fontSize="small" />
            )}
          </IconButton>
        </InputAdornment>
      }
    />
  )
}
