import {
  Box,
  Card,
  Divider,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import { itemsLabel } from '@/features/orders/display'
import type { Order } from '@/features/orders/types'
import { formatCurrency } from '@/shared/utils/format'

interface OrderItemsCardProps {
  order: Pick<Order, 'items' | 'total'>
  /** Desktop table (Item, Qtd., Unitário, Subtotal) or the compact phone list. */
  variant: 'table' | 'list'
}

const head = { typography: 'subtitle2', color: 'ink.700', bgcolor: 'surface.100', py: 1.5 }
const money = { fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap' } as const

/** Items with the server-computed subtotals and total. */
export function OrderItemsCard({ order, variant }: OrderItemsCardProps) {
  const totalLine = (
    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', gap: 2 }}>
      <Typography variant="subtitle1">Total</Typography>
      <Typography
        sx={{
          typography: 'h2',
          fontSize: { xs: 24, md: 32 },
          color: 'brand.700',
          ...money,
        }}
      >
        {formatCurrency(order.total)}
      </Typography>
    </Box>
  )
  const header = (
    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
      <Typography variant="h4" component="h2">
        Itens
      </Typography>
      <Typography variant="body2" color="text.secondary">
        {itemsLabel(order.items.length)}
      </Typography>
    </Box>
  )

  if (variant === 'list') {
    return (
      <Card component="section" sx={{ p: 2 }}>
        <Stack spacing={2}>
          {header}
          <Stack component="ul" spacing={1.25} sx={{ listStyle: 'none', m: 0, p: 0 }}>
            {order.items.map((item, index) => (
              <Box
                component="li"
                key={index}
                sx={{ display: 'flex', justifyContent: 'space-between', gap: 2 }}
              >
                <Typography sx={{ overflowWrap: 'anywhere' }}>
                  {item.quantity}× {item.description}
                </Typography>
                <Typography sx={money}>{formatCurrency(item.subtotal)}</Typography>
              </Box>
            ))}
          </Stack>
          <Divider />
          {totalLine}
        </Stack>
      </Card>
    )
  }

  return (
    <Card component="section" sx={{ overflow: 'hidden' }}>
      <Box sx={{ px: 3, py: 2.5 }}>{header}</Box>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell sx={{ ...head, pl: 3 }}>Item</TableCell>
            <TableCell sx={head} align="right">
              Qtd.
            </TableCell>
            <TableCell sx={head} align="right">
              Unitário
            </TableCell>
            <TableCell sx={{ ...head, pr: 3 }} align="right">
              Subtotal
            </TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {order.items.map((item, index) => (
            <TableRow key={index}>
              <TableCell sx={{ pl: 3, py: 2, overflowWrap: 'anywhere' }}>
                {item.description}
              </TableCell>
              <TableCell align="right" sx={money}>
                {item.quantity}
              </TableCell>
              <TableCell align="right" sx={money}>
                {formatCurrency(item.unitPrice)}
              </TableCell>
              <TableCell align="right" sx={{ ...money, pr: 3, fontWeight: 700 }}>
                {formatCurrency(item.subtotal)}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
      <Box sx={{ px: 3, py: 2.5, bgcolor: 'surface.100' }}>{totalLine}</Box>
    </Card>
  )
}
