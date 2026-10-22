import {
  Box,
  Link,
  Skeleton,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import { Link as RouterLink, useNavigate } from 'react-router'
import { OrderStatusChip } from '@/features/orders/components/OrderStatusChip'
import { StatusActions } from '@/features/orders/components/StatusActions'
import { itemsLabel, placeLabel, updatedLabel } from '@/features/orders/display'
import type { OrderSummary } from '@/features/orders/types'
import { formatCurrency } from '@/shared/utils/format'

interface OrdersTableProps {
  orders: OrderSummary[] | undefined
  skeletonRows?: number
}

const COLUMNS = [
  { label: 'Código', width: 96 },
  { label: 'Cliente' },
  { label: 'Itens', width: 96 },
  { label: 'Total', width: 120 },
  { label: 'Status', width: 176 },
  { label: 'Atualizado', width: 150 },
  { label: 'Ações', width: 220, align: 'right' as const },
]

const cell = { px: 2, py: 1.75, borderColor: 'divider' }

// The whole row opens the order; the code is the keyboard-accessible link.
export function OrdersTable({ orders, skeletonRows = 5 }: OrdersTableProps) {
  const navigate = useNavigate()

  return (
    <Box sx={{ overflowX: 'auto' }}>
      <Table sx={{ minWidth: 880 }}>
        <TableHead>
          <TableRow>
            {COLUMNS.map((column) => (
              <TableCell
                key={column.label}
                align={column.align}
                sx={{
                  ...cell,
                  py: 1.5,
                  width: column.width,
                  bgcolor: 'surface.100',
                  color: 'ink.700',
                  typography: 'subtitle2',
                }}
              >
                {column.label}
              </TableCell>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {orders
            ? orders.map((order) => (
                <TableRow
                  key={order.id}
                  hover
                  onClick={() => navigate(`/orders/${order.id}`)}
                  sx={{
                    cursor: 'pointer',
                    '&:last-child td': { borderBottom: 0 },
                    '&.MuiTableRow-hover:hover': { bgcolor: 'surface.100' },
                  }}
                >
                  <TableCell sx={cell}>
                    <Link
                      component={RouterLink}
                      to={`/orders/${order.id}`}
                      onClick={(event) => event.stopPropagation()}
                      underline="hover"
                      sx={{ color: 'ink.900', fontWeight: 700, fontVariantNumeric: 'tabular-nums' }}
                    >
                      #{order.code}
                    </Link>
                  </TableCell>
                  <TableCell sx={cell}>
                    <Typography sx={{ fontWeight: 600 }}>{order.customerName}</Typography>
                    <Typography variant="body2" color="text.secondary">
                      {placeLabel(order)}
                    </Typography>
                  </TableCell>
                  <TableCell sx={{ ...cell, fontVariantNumeric: 'tabular-nums' }}>
                    {itemsLabel(order.itemsCount)}
                  </TableCell>
                  <TableCell
                    sx={{
                      ...cell,
                      fontWeight: 700,
                      color: 'brand.700',
                      fontVariantNumeric: 'tabular-nums',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {formatCurrency(order.total)}
                  </TableCell>
                  <TableCell sx={cell}>
                    <OrderStatusChip status={order.status} />
                  </TableCell>
                  <TableCell
                    sx={{
                      ...cell,
                      color: 'text.secondary',
                      typography: 'body2',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {updatedLabel(order)}
                  </TableCell>
                  <TableCell sx={cell} align="right">
                    <StatusActions order={order} />
                  </TableCell>
                </TableRow>
              ))
            : Array.from({ length: skeletonRows }, (_, index) => (
                <TableRow key={index}>
                  {COLUMNS.map((column) => (
                    <TableCell key={column.label} sx={cell}>
                      <Skeleton />
                    </TableCell>
                  ))}
                </TableRow>
              ))}
        </TableBody>
      </Table>
    </Box>
  )
}
