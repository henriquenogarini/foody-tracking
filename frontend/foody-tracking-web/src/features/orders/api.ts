import { api } from '@/shared/api/http'
import type { PageResponse } from '@/shared/api/types'
import type {
  CreateOrderInput,
  Order,
  OrderListParams,
  OrderStatus,
  OrderSummary,
  StatusHistoryEntry,
} from './types'

export async function listOrders({ status, q, page, size }: OrderListParams) {
  const { data } = await api.get<PageResponse<OrderSummary>>('/orders', {
    // Empty values are dropped so the API applies no filter.
    params: { status: status || undefined, q: q || undefined, page, size },
  })
  return data
}

/** Orders in one status, read from the page total (the API has no count endpoint). */
export async function countOrders(status: OrderStatus): Promise<number> {
  const { totalElements } = await listOrders({ status, page: 0, size: 1 })
  return totalElements
}

export async function updateOrderStatus(id: string, status: OrderStatus): Promise<Order> {
  const { data } = await api.patch<Order>(`/orders/${id}/status`, { status })
  return data
}

export async function createOrder(input: CreateOrderInput): Promise<Order> {
  const { data } = await api.post<Order>('/orders', input)
  return data
}

export async function getOrder(id: string): Promise<Order> {
  const { data } = await api.get<Order>(`/orders/${id}`)
  return data
}

export async function getOrderHistory(id: string): Promise<StatusHistoryEntry[]> {
  const { data } = await api.get<StatusHistoryEntry[]>(`/orders/${id}/history`)
  return data
}
