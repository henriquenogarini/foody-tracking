import type { OrderStatus } from './types'

// The status machine mirrors the API (order/enums/OrderStatus.java). The UI only offers these moves;
// the API stays the authority and answers 409 for anything else.
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

/** Design status colors (theme palette.status). */
export const STATUS_TONE = {
  RECEBIDO: 'received',
  EM_PREPARO: 'preparing',
  SAIU_PARA_ENTREGA: 'out',
  ENTREGUE: 'delivered',
  CANCELADO: 'cancelled',
} as const satisfies Record<OrderStatus, string>

/** Button text for moving an order *to* a status, as in the design. */
export const ACTION_LABEL: Record<Exclude<OrderStatus, 'RECEBIDO'>, string> = {
  EM_PREPARO: 'Iniciar preparo',
  SAIU_PARA_ENTREGA: 'Saiu para entrega',
  ENTREGUE: 'Marcar entregue',
  CANCELADO: 'Cancelar pedido',
}

export function isTerminal(status: OrderStatus): boolean {
  return NEXT_TRANSITIONS[status].length === 0
}

/** The forward move shown as the main button; cancelling lives in the secondary menu. */
export function primaryTransition(status: OrderStatus): Exclude<OrderStatus, 'RECEBIDO'> | null {
  const next = NEXT_TRANSITIONS[status].find((target) => target !== 'CANCELADO')
  return next && next !== 'RECEBIDO' ? next : null
}

export function canCancel(status: OrderStatus): boolean {
  return NEXT_TRANSITIONS[status].includes('CANCELADO')
}
