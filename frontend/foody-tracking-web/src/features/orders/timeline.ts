import { STATUS_LABEL } from './status'
import type { OrderStatus, StatusHistoryEntry } from './types'

export type StepState = 'done' | 'current' | 'pending' | 'cancelled'

export interface TimelineStep {
  status: OrderStatus
  label: string
  description: string
  state: StepState
  at?: string
}

const HAPPY_PATH: readonly OrderStatus[] = [
  'RECEBIDO',
  'EM_PREPARO',
  'SAIU_PARA_ENTREGA',
  'ENTREGUE',
]

function describe(entry: StatusHistoryEntry): string {
  // No changedAt: the change was just made here and the history has not caught up yet.
  const who = entry.changedAt ? (entry.changedByName ?? 'usuário removido') : 'você'
  switch (entry.toStatus) {
    case 'RECEBIDO':
      return `Pedido criado por ${who}`
    case 'EM_PREPARO':
      return 'Cozinha iniciou o preparo'
    case 'SAIU_PARA_ENTREGA':
      return 'Entregador a caminho'
    case 'ENTREGUE':
      return 'Pedido entregue ao cliente'
    case 'CANCELADO':
      return `Cancelado por ${who}`
  }
}

// Right after a change the order is newer than its history; its status is then appended as the
// current step, so the timeline never contradicts the status shown above it.
export function buildTimeline(status: OrderStatus, history: StatusHistoryEntry[]): TimelineStep[] {
  const latest = history.at(-1)?.toStatus
  const pendingEntry: StatusHistoryEntry[] =
    latest && latest !== status
      ? [
          {
            fromStatus: latest,
            toStatus: status,
            changedAt: '',
            changedBy: '',
            changedByName: null,
          },
        ]
      : []
  const entries = [...history, ...pendingEntry]

  const happened = entries.map((entry, index): TimelineStep => {
    const last = index === entries.length - 1
    let state: StepState = 'done'
    if (entry.toStatus === 'CANCELADO') state = 'cancelled'
    else if (last && entry.toStatus !== 'ENTREGUE') state = 'current'
    return {
      status: entry.toStatus,
      label: STATUS_LABEL[entry.toStatus],
      description: describe(entry),
      state,
      at: entry.changedAt || undefined,
    }
  })
  if (status === 'CANCELADO') return happened

  const ahead = HAPPY_PATH.slice(HAPPY_PATH.indexOf(status) + 1).map((pending): TimelineStep => ({
    status: pending,
    label: STATUS_LABEL[pending],
    description: 'Aguardando',
    state: 'pending',
  }))
  return [...happened, ...ahead]
}
