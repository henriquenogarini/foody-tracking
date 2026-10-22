import { Box, Card, Link, Typography } from '@mui/material'
import { Link as RouterLink, useNavigate } from 'react-router'
import { OrderStatusChip } from '@/features/orders/components/OrderStatusChip'
import { StatusActions } from '@/features/orders/components/StatusActions'
import { itemsLabel, placeLabel, updatedLabel } from '@/features/orders/display'
import type { OrderSummary } from '@/features/orders/types'
import { formatCurrency } from '@/shared/utils/format'

/** Mobile list item from the design: code and status, customer, items and total, next action. */
export function OrderCard({ order }: { order: OrderSummary }) {
  const navigate = useNavigate()

  return (
    <Card
      component="article"
      onClick={() => navigate(`/orders/${order.id}`)}
      sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 1.5, cursor: 'pointer' }}
    >
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Link
          component={RouterLink}
          to={`/orders/${order.id}`}
          onClick={(event) => event.stopPropagation()}
          underline="hover"
          aria-label={`Pedido #${order.code}`}
          sx={{
            color: 'ink.900',
            fontWeight: 700,
            fontVariantNumeric: 'tabular-nums',
            minHeight: 44,
            display: 'inline-flex',
            alignItems: 'center',
          }}
        >
          #{order.code}
        </Link>
        <OrderStatusChip status={order.status} />
      </Box>
      <Box>
        <Typography variant="subtitle1">{order.customerName}</Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.25 }}>
          {placeLabel(order)}
        </Typography>
      </Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
        <Typography variant="body2" color="text.secondary">
          {itemsLabel(order.itemsCount)} · {updatedLabel(order)}
        </Typography>
        <Typography
          sx={{ fontWeight: 700, color: 'brand.700', fontVariantNumeric: 'tabular-nums' }}
        >
          {formatCurrency(order.total)}
        </Typography>
      </Box>
      <StatusActions order={order} layout="block" />
    </Card>
  )
}
