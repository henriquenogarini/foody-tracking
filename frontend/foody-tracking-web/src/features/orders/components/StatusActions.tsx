import MoreVertIcon from '@mui/icons-material/MoreVert'
import { Box, Button, IconButton, Menu, MenuItem } from '@mui/material'
import { useState, type MouseEvent } from 'react'
import { useUpdateStatus } from '@/features/orders/queries'
import { ACTION_LABEL, canCancel, primaryTransition } from '@/features/orders/status'
import type { OrderStatus } from '@/features/orders/types'
import { ConfirmDialog } from '@/shared/components/ConfirmDialog'

interface StatusActionsProps {
  order: { id: string; code: number; status: OrderStatus }
  /** `row`: compact button + menu (table); `block`: full-width button + menu (mobile card). */
  layout?: 'row' | 'block'
}

// Clicks inside a clickable row or card must not also open the order.
const stop = (event: MouseEvent) => event.stopPropagation()

/** Next valid status as one click, cancelling behind a confirmation. Nothing for finished orders. */
export function StatusActions({ order, layout = 'row' }: StatusActionsProps) {
  const updateStatus = useUpdateStatus()
  const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null)
  const [confirmingCancel, setConfirmingCancel] = useState(false)

  const next = primaryTransition(order.status)
  const cancellable = canCancel(order.status)
  if (!next && !cancellable) return null

  const move = (status: OrderStatus) =>
    updateStatus.mutate({ id: order.id, status }, { onSettled: () => setConfirmingCancel(false) })

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
          loading={updateStatus.isPending && updateStatus.variables?.status === next}
          disabled={updateStatus.isPending}
          onClick={() => move(next)}
        >
          {ACTION_LABEL[next]}
        </Button>
      )}
      {cancellable && (
        <IconButton
          aria-label={`Mais ações do pedido #${order.code}`}
          aria-haspopup="menu"
          onClick={(event) => setMenuAnchor(event.currentTarget)}
          disabled={updateStatus.isPending}
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
            setConfirmingCancel(true)
          }}
        >
          {ACTION_LABEL.CANCELADO}
        </MenuItem>
      </Menu>
      <ConfirmDialog
        open={confirmingCancel}
        title={`Cancelar o pedido #${order.code}?`}
        description="O pedido sai da operação e não pode voltar. O cliente precisa ser avisado."
        confirmLabel="Cancelar pedido"
        destructive
        loading={updateStatus.isPending}
        onConfirm={() => move('CANCELADO')}
        onClose={() => setConfirmingCancel(false)}
      />
    </Box>
  )
}
