import { Box, Card, Skeleton, Typography } from '@mui/material'
import type { StatusCounts } from '@/features/orders/queries'

interface OrdersSummaryProps {
  counts: StatusCounts | undefined
}

// All-time totals from the per-status counts (owner decision, CLAUDE.md §1.2: the API has no
// date-filtered statistics, so the design's "hoje"/average-time cards become overall figures).
export function OrdersSummary({ counts }: OrdersSummaryProps) {
  const cancelledShare = counts?.total ? Math.round((counts.CANCELADO / counts.total) * 100) : 0
  const cards = [
    {
      label: 'Em andamento',
      value: counts && counts.RECEBIDO + counts.EM_PREPARO,
      meta: 'Recebidos e em preparo',
    },
    {
      label: 'Na rua',
      value: counts?.SAIU_PARA_ENTREGA,
      meta: 'Saiu para entrega',
      highlight: true,
    },
    { label: 'Entregues', value: counts?.ENTREGUE, meta: 'Total entregue' },
    { label: 'Cancelados', value: counts?.CANCELADO, meta: `${cancelledShare}% dos pedidos` },
  ]

  return (
    <Box
      component="section"
      aria-label="Resumo dos pedidos"
      sx={{
        // Mobile design has no summary cards: the filter chips carry the counts there.
        display: { xs: 'none', md: 'grid' },
        gridTemplateColumns: 'repeat(4, minmax(0, 1fr))',
        gap: 2,
      }}
    >
      {cards.map((card) => (
        <Card
          key={card.label}
          sx={{ px: 2.5, py: 2, display: 'flex', flexDirection: 'column', gap: 0.5 }}
        >
          <Typography variant="subtitle2" sx={{ color: 'ink.700' }}>
            {card.label}
          </Typography>
          <Typography
            component="p"
            sx={{
              typography: 'h1',
              fontSize: 28,
              lineHeight: '32px',
              letterSpacing: '-0.01em',
              fontVariantNumeric: 'tabular-nums',
              color: card.highlight ? 'brand.700' : 'ink.900',
            }}
          >
            {card.value ?? <Skeleton width={32} />}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {card.meta}
          </Typography>
        </Card>
      ))}
    </Box>
  )
}
