import MoreVertIcon from '@mui/icons-material/MoreVert'
import { Box, Button, IconButton, Menu, MenuItem } from '@mui/material'
import { useState, type MouseEvent } from 'react'
import { ACTION_LABEL } from '@/features/orders/status'
import type { OrderStatus } from '@/features/orders/types'
import { useStatusChange } from '@/features/orders/useStatusChange'

interface StatusActionsProps {
  order: { id: string; code: number; status: OrderStatus }
  layout?: 'row' | 'block'
}

// Clicks inside a clickable row or card must not also open the order.
const stop = (event: MouseEvent) => event.stopPropagation()

export function StatusActions({ order, layout = 'row' }: StatusActionsProps) {
  const { next, cancellable, busy, applying, advance, askToCancel, confirmDialog } =
    useStatusChange(order)
  const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null)
  if (!next && !cancellable) return null

  return (
    <Box
      onClick={stop}
      sx={{ display: 'flex', gap: 0.5, alignItems: 'center', justifyContent: 'flex-end' }}
    >
      {next && (
        <Button
          variant="outlined"
          size={layout === 'row' ? 'small' : 'medium'}
          fullWidth={layout === 'block'}
          loading={applying === next}
          disabled={busy}
          onClick={advance}
        >
          {ACTION_LABEL[next]}
        </Button>
      )}
      {cancellable && (
        <IconButton
          aria-label={`Mais ações do pedido #${order.code}`}
          aria-haspopup="menu"
          onClick={(event) => setMenuAnchor(event.currentTarget)}
          disabled={busy}
          sx={{ width: 44, height: 44, color: 'ink.700' }}
        >
          <MoreVertIcon />
        </IconButton>
      )}
      <Menu
        anchorEl={menuAnchor}
        open={Boolean(menuAnchor)}
        onClose={() => setMenuAnchor(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <MenuItem
          sx={{ color: 'error.main' }}
          onClick={() => {
            setMenuAnchor(null)
            askToCancel()
          }}
        >
          {ACTION_LABEL.CANCELADO}
        </MenuItem>
      </Menu>
      {confirmDialog}
    </Box>
  )
}
