// Mirrors the API contract (CLAUDE.md §5). Money is a JSON number, dates ISO-8601 with offset; optional
// fields come back as null.

export const ORDER_STATUSES = [
  'RECEBIDO',
  'EM_PREPARO',
  'SAIU_PARA_ENTREGA',
  'ENTREGUE',
  'CANCELADO',
] as const

export type OrderStatus = (typeof ORDER_STATUSES)[number]

export interface OrderSummary {
  id: string
  code: number
  status: OrderStatus
  customerName: string
  total: number
  itemsCount: number
  neighborhood: string
  city: string
  createdAt: string
  updatedAt: string
}

export interface Customer {
  name: string
  phone: string | null
  email: string | null
}

export interface OrderItem {
  description: string
  quantity: number
  unitPrice: number
  subtotal: number
}

export interface DeliveryAddress {
  street: string
  number: string
  complement: string | null
  neighborhood: string
  city: string
  state: string
  postalCode: string
  lat: number | null
  lng: number | null
}

export interface Order {
  id: string
  code: number
  status: OrderStatus
  customer: Customer
  items: OrderItem[]
  deliveryAddress: DeliveryAddress
  notes: string | null
  total: number
  createdAt: string
  updatedAt: string
}

export interface OrderListParams {
  status?: OrderStatus
  /** Customer name contains, or the exact code ("1042" / "#1042"). */
  q?: string
  /** 0-based. */
  page: number
  size: number
}

/** POST /orders body. Optional fields are omitted when empty; total and subtotals are computed by the API. */
export interface CreateOrderInput {
  customer: { name: string; phone?: string; email?: string }
  items: { description: string; quantity: number; unitPrice: number }[]
  deliveryAddress: Omit<DeliveryAddress, 'complement' | 'lat' | 'lng'> & { complement?: string }
  notes?: string
}

/** One status change. The first entry is the creation (fromStatus null); oldest first. */
export interface StatusHistoryEntry {
  fromStatus: OrderStatus | null
  toStatus: OrderStatus
  changedAt: string
  changedBy: string
  /** Current name of who made the change; null if that user no longer exists. */
  changedByName: string | null
}
