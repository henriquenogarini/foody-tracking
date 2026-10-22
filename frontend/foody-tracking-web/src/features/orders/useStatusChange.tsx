import { useState, type ReactElement } from 'react'
import { useUpdateStatus } from '@/features/orders/queries'
import { canCancel, primaryTransition } from '@/features/orders/status'
import type { OrderStatus } from '@/features/orders/types'
import { ConfirmDialog } from '@/shared/components/ConfirmDialog'

interface StatusChange {
  next: ReturnType<typeof primaryTransition>
  cancellable: boolean
  busy: boolean
  applying: OrderStatus | undefined
  advance: () => void
  askToCancel: () => void
  /** Render once per order. */
  confirmDialog: ReactElement
}

/** One mutation and one cancel confirmation per order, however many buttons the screen draws for it. */
export function useStatusChange(order: {
  id: string
  code: number
  status: OrderStatus
}): StatusChange {
  const updateStatus = useUpdateStatus()
  const [confirmingCancel, setConfirmingCancel] = useState(false)
  const next = primaryTransition(order.status)

  const move = (status: OrderStatus) =>
    updateStatus.mutate({ id: order.id, status }, { onSettled: () => setConfirmingCancel(false) })

  return {
    next,
    cancellable: canCancel(order.status),
    busy: updateStatus.isPending,
    applying: updateStatus.isPending ? updateStatus.variables?.status : undefined,
    advance: () => next && move(next),
    askToCancel: () => setConfirmingCancel(true),
    confirmDialog: (
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
    ),
  }
}
