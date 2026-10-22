import { formatDateTime, formatRelative, formatTime } from '@/shared/utils/format'
import { isTerminal } from './status'
import type { OrderSummary } from './types'

/** 1 → "1 item", 3 → "3 itens" */
export function itemsLabel(count: number): string {
  return `${count} ${count === 1 ? 'item' : 'itens'}`
}

/** "Moema · São Paulo" */
export function placeLabel({ neighborhood, city }: Pick<OrderSummary, 'neighborhood' | 'city'>) {
  return [neighborhood, city].filter(Boolean).join(' · ')
}

/**
 * Orders still moving show how long ago they changed ("há 4 min"); finished ones show when they
 * finished ("13:52", or the date when it was not today), as in the design.
 */
export function updatedLabel(
  { status, updatedAt }: Pick<OrderSummary, 'status' | 'updatedAt'>,
  now: number = Date.now(),
): string {
  if (!isTerminal(status)) return formatRelative(updatedAt, now)
  const sameDay = new Date(updatedAt).toDateString() === new Date(now).toDateString()
  return sameDay ? formatTime(updatedAt) : formatDateTime(updatedAt)
}
