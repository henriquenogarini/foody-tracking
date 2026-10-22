import CheckIcon from '@mui/icons-material/Check'
import CloseIcon from '@mui/icons-material/Close'
import { Box, useTheme } from '@mui/material'
import { STATUS_LABEL, STATUS_TONE } from '@/features/orders/status'
import type { OrderStatus } from '@/features/orders/types'

export function OrderStatusChip({ status }: { status: OrderStatus }) {
  const tone = useTheme().palette.status[STATUS_TONE[status]]
  const Icon = status === 'ENTREGUE' ? CheckIcon : status === 'CANCELADO' ? CloseIcon : null

  return (
    <Box
      component="span"
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 0.75,
        height: 28,
        px: 1.5,
        borderRadius: 0.6, // 6px (theme radius is 10)
        bgcolor: tone.bg,
        color: tone.fg,
        typography: 'subtitle2',
        whiteSpace: 'nowrap',
      }}
    >
      {Icon && <Icon sx={{ fontSize: 14 }} aria-hidden />}
      {STATUS_LABEL[status]}
    </Box>
  )
}
