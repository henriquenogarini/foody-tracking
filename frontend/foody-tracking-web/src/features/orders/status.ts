import type { OrderStatus } from './types'

// Mirrors the API's status machine, which answers 409 for any other move.
export const NEXT_TRANSITIONS: Record<OrderStatus, readonly OrderStatus[]> = {
  RECEBIDO: ['EM_PREPARO', 'CANCELADO'],
  EM_PREPARO: ['SAIU_PARA_ENTREGA', 'CANCELADO'],
  SAIU_PARA_ENTREGA: ['ENTREGUE'],
  ENTREGUE: [],
  CANCELADO: [],
}

export const STATUS_LABEL: Record<OrderStatus, string> = {
  RECEBIDO: 'Recebido',
  EM_PREPARO: 'Em preparo',
  SAIU_PARA_ENTREGA: 'Saiu para entrega',
  ENTREGUE: 'Entregue',
  CANCELADO: 'Cancelado',
}

export const STATUS_TONE = {
  RECEBIDO: 'received',
  EM_PREPARO: 'preparing',
  SAIU_PARA_ENTREGA: 'out',
  ENTREGUE: 'delivered',
  CANCELADO: 'cancelled',
} as const satisfies Record<OrderStatus, string>

export const ACTION_LABEL: Record<Exclude<OrderStatus, 'RECEBIDO'>, string> = {
  EM_PREPARO: 'Iniciar preparo',
  SAIU_PARA_ENTREGA: 'Saiu para entrega',
  ENTREGUE: 'Marcar entregue',
  CANCELADO: 'Cancelar pedido',
}

export const DETAIL_ACTION_LABEL: typeof ACTION_LABEL = {
  ...ACTION_LABEL,
  ENTREGUE: 'Marcar como entregue',
}

export function isTerminal(status: OrderStatus): boolean {
  return NEXT_TRANSITIONS[status].length === 0
}

export function primaryTransition(status: OrderStatus): Exclude<OrderStatus, 'RECEBIDO'> | null {
  const next = NEXT_TRANSITIONS[status].find((target) => target !== 'CANCELADO')
  return next && next !== 'RECEBIDO' ? next : null
}

export function canCancel(status: OrderStatus): boolean {
  return NEXT_TRANSITIONS[status].includes('CANCELADO')
}
