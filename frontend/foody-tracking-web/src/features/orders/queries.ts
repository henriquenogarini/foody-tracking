import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toApiError } from '@/shared/api/problem'
import { useSnackbar } from '@/shared/hooks/useSnackbar'
import { countOrders, listOrders, updateOrderStatus } from './api'
import { STATUS_LABEL } from './status'
import { ORDER_STATUSES, type Order, type OrderListParams, type OrderStatus } from './types'

export type StatusCounts = Record<OrderStatus, number> & { total: number }

/** "Tracking" without WebSockets: lists and counts refresh while the tab is visible. */
export const POLL_INTERVAL_MS = 10_000

// One root key, so a status change invalidates every list, count and detail at once.
export const orderKeys = {
  all: ['orders'] as const,
  list: (params: OrderListParams) => [...orderKeys.all, 'list', params] as const,
  counts: () => [...orderKeys.all, 'counts'] as const,
}

export function useOrdersQuery(params: OrderListParams) {
  return useQuery({
    queryKey: orderKeys.list(params),
    queryFn: () => listOrders(params),
    refetchInterval: POLL_INTERVAL_MS,
    refetchIntervalInBackground: false,
    // Keeps the current page on screen while the next filter/page loads (no flash of skeletons).
    placeholderData: keepPreviousData,
  })
}

/** Orders per status plus their sum (the "Todos" filter). */
export function useStatusCountsQuery() {
  return useQuery({
    queryKey: orderKeys.counts(),
    queryFn: async (): Promise<StatusCounts> => {
      const totals = await Promise.all(ORDER_STATUSES.map(countOrders))
      const byStatus = Object.fromEntries(ORDER_STATUSES.map((s, i) => [s, totals[i]]))
      return {
        ...(byStatus as Record<OrderStatus, number>),
        total: totals.reduce((a, b) => a + b, 0),
      }
    },
    refetchInterval: POLL_INTERVAL_MS,
    refetchIntervalInBackground: false,
  })
}

export function useUpdateStatus() {
  const queryClient = useQueryClient()
  const notify = useSnackbar()

  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: OrderStatus }) =>
      updateOrderStatus(id, status),
    onSuccess: (order: Order) => notify(`Pedido #${order.code}: ${STATUS_LABEL[order.status]}`),
    onError: (error) => {
      // 409 means someone else moved the order first: show the API's reason and refresh.
      notify(toApiError(error).message, 'error')
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: orderKeys.all }),
  })
}
