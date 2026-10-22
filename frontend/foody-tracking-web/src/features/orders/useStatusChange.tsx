import { useState, type ReactElement } from 'react'
import { useUpdateStatus } from '@/features/orders/queries'
import { canCancel, primaryTransition } from '@/features/orders/status'
import type { OrderStatus } from '@/features/orders/types'
import { ConfirmDialog } from '@/shared/components/ConfirmDialog'

interface StatusChange {
  /** The forward move offered as the main action, if any. */
  next: ReturnType<typeof primaryTransition>
  cancellable: boolean
  /** True while any change for this order is in flight: every button of the order disables. */
  busy: boolean
  /** The status being applied, to show a spinner on the right button. */
  applying: OrderStatus | undefined
  advance: () => void
  askToCancel: () => void
  /** Render once per order; it asks before cancelling. */
  confirmDialog: ReactElement
}

/**
 * One order's status actions: a single mutation and a single cancel confirmation, however many buttons
 * the screen draws for it (list row, detail header, timeline card, phone bar).
 */
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
