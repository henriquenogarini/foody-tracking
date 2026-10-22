import { formatDateTime, formatRelative, formatTime } from '@/shared/utils/format'
import { isTerminal } from './status'
import type { OrderSummary } from './types'

export function itemsLabel(count: number): string {
  return `${count} ${count === 1 ? 'item' : 'itens'}`
}

export function placeLabel({ neighborhood, city }: Pick<OrderSummary, 'neighborhood' | 'city'>) {
  return [neighborhood, city].filter(Boolean).join(' · ')
}

export function updatedLabel(
  { status, updatedAt }: Pick<OrderSummary, 'status' | 'updatedAt'>,
  now: number = Date.now(),
): string {
  if (!isTerminal(status)) return formatRelative(updatedAt, now)
  const sameDay = new Date(updatedAt).toDateString() === new Date(now).toDateString()
  return sameDay ? formatTime(updatedAt) : formatDateTime(updatedAt)
}
