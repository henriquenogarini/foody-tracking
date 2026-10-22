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
  /** Customer name contains, or exact code ("1042" / "#1042"). */
  q?: string
  /** 0-based. */
  page: number
  size: number
}

export interface CreateOrderInput {
  customer: { name: string; phone?: string; email?: string }
  items: { description: string; quantity: number; unitPrice: number }[]
  deliveryAddress: Omit<DeliveryAddress, 'complement' | 'lat' | 'lng'> & { complement?: string }
  notes?: string
}

/** Oldest first; the first entry is the creation (fromStatus null). */
export interface StatusHistoryEntry {
  fromStatus: OrderStatus | null
  toStatus: OrderStatus
  changedAt: string
  changedBy: string
  /** Null if that user no longer exists. */
  changedByName: string | null
}
