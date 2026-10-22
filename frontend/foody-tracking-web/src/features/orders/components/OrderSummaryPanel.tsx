import { Box, Button, Card, Divider, Stack, Typography } from '@mui/material'
import { useWatch } from 'react-hook-form'
import { Link as RouterLink } from 'react-router'
import { itemsLabel } from '@/features/orders/display'
import type { OrderFormValues } from '@/features/orders/schemas'
import { formatCurrency, parseDecimal } from '@/shared/utils/format'

interface OrderSummaryPanelProps {
  formId: string
  submitting: boolean
  variant: 'side' | 'bar'
}

// Display only: the API computes the real total.
function useLiveSummary() {
  const items = useWatch<OrderFormValues, 'items'>({ name: 'items' })
  const lines = items.map((item, index) => {
    const quantity = /^\d+$/.test(item.quantity.trim()) ? Number(item.quantity) : 0
    const price = parseDecimal(item.unitPrice) ?? 0
    return {
      key: index,
      label: `${quantity}× ${item.description.trim() || 'Item sem descrição'}`,
      subtotal: Math.round(quantity * price * 100) / 100,
      filled: Boolean(item.description.trim()) && quantity > 0 && price > 0,
    }
  })
  const total = lines.reduce((sum, line) => sum + line.subtotal, 0)
  return { lines: lines.filter((line) => line.filled), total, count: items.length }
}

export function OrderSummaryPanel({ formId, submitting, variant }: OrderSummaryPanelProps) {
  const { lines, total, count } = useLiveSummary()
  const submit = (
    <Button
      type="submit"
      form={formId}
      variant="contained"
      fullWidth
      loading={submitting}
      sx={{ height: 48, fontSize: 16 }}
    >
      Criar pedido
    </Button>
  )

  if (variant === 'bar') {
    return (
      <Box
        sx={{
          position: 'fixed',
          insetInline: 0,
          bottom: 72,
          zIndex: 'appBar',
          display: 'flex',
          alignItems: 'center',
          gap: 2,
          px: 2,
          py: 1.5,
          bgcolor: 'surface.0',
          borderTop: 1,
          borderColor: 'divider',
        }}
      >
        <Box sx={{ flex: 'none' }}>
          <Typography variant="body2" color="text.secondary">
            Total · {itemsLabel(count)}
          </Typography>
          <Typography
            aria-live="polite"
            sx={{ typography: 'h3', fontSize: 20, lineHeight: '26px', color: 'brand.700' }}
          >
            {formatCurrency(total)}
          </Typography>
        </Box>
        {submit}
      </Box>
    )
  }

  return (
    <Card sx={{ p: 3, position: 'sticky', top: 88 }}>
      <Stack spacing={2.5}>
        <Typography variant="h4" component="h2">
          Resumo
        </Typography>
        {lines.length > 0 && (
          <Stack component="ul" spacing={1.25} sx={{ listStyle: 'none', m: 0, p: 0 }}>
            {lines.map((line) => (
              <Box
                component="li"
                key={line.key}
                sx={{ display: 'flex', justifyContent: 'space-between', gap: 2 }}
              >
                <Typography sx={{ minWidth: 0, overflowWrap: 'anywhere' }}>{line.label}</Typography>
                <Typography sx={{ flex: 'none', fontVariantNumeric: 'tabular-nums' }}>
                  {formatCurrency(line.subtotal)}
                </Typography>
              </Box>
            ))}
          </Stack>
        )}
        <Divider />
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
          <Typography variant="subtitle1">Total</Typography>
          <Typography
            aria-live="polite"
            sx={{
              typography: 'h2',
              fontSize: 32,
              color: 'brand.700',
              fontVariantNumeric: 'tabular-nums',
            }}
          >
            {formatCurrency(total)}
          </Typography>
        </Box>
        <Typography variant="body2" color="text.secondary">
          O pedido entra como{' '}
          <Box component="strong" sx={{ color: 'status.received.fg' }}>
            Recebido
          </Box>
          . O total é calculado pelo servidor.
        </Typography>
        <Stack spacing={1}>
          {submit}
          <Button variant="text" component={RouterLink} to="/orders" fullWidth>
            Cancelar
          </Button>
        </Stack>
      </Stack>
    </Card>
  )
}
