import { Box, ButtonBase } from '@mui/material'
import { STATUS_LABEL } from '@/features/orders/status'
import type { StatusCounts } from '@/features/orders/queries'
import { ORDER_STATUSES, type OrderStatus } from '@/features/orders/types'

interface StatusFilterProps {
  value: OrderStatus | undefined
  counts: StatusCounts | undefined
  onChange: (status: OrderStatus | undefined) => void
}

/** Design filter pills: "Todos" plus one per status, each with its count; the active one is dark. */
export function StatusFilter({ value, counts, onChange }: StatusFilterProps) {
  const options: { status: OrderStatus | undefined; label: string; count?: number }[] = [
    { status: undefined, label: 'Todos', count: counts?.total },
    ...ORDER_STATUSES.map((status) => ({
      status,
      label: STATUS_LABEL[status],
      count: counts?.[status],
    })),
  ]

  return (
    <Box
      role="group"
      aria-label="Filtrar por status"
      sx={{
        display: 'flex',
        gap: 1,
        // One scrollable row on phones, wrapping on wider screens.
        flexWrap: { xs: 'nowrap', md: 'wrap' },
        overflowX: { xs: 'auto', md: 'visible' },
        mx: { xs: -2, md: 0 },
        px: { xs: 2, md: 0 },
        pb: { xs: 0.5, md: 0 },
        scrollbarWidth: 'none',
      }}
    >
      {options.map((option) => {
        const active = option.status === value
        return (
          <ButtonBase
            key={option.label}
            aria-pressed={active}
            // Without it the name reads "Todos5": the count is a separate element.
            aria-label={
              option.count === undefined
                ? option.label
                : `${option.label}, ${option.count} ${option.count === 1 ? 'pedido' : 'pedidos'}`
            }
            onClick={() => onChange(option.status)}
            sx={{
              flex: 'none',
              // 36px as designed on desktop; 44px touch target on phones and tablets.
              height: { xs: 44, md: 36 },
              px: 1.75,
              gap: 1,
              borderRadius: 999,
              border: 1,
              borderColor: active ? 'ink.900' : 'divider',
              bgcolor: active ? 'ink.900' : 'surface.0',
              color: active ? 'surface.0' : 'ink.700',
              typography: 'body1',
              fontWeight: 600,
              fontSize: 14,
              '&.Mui-focusVisible': { outline: 2, outlineColor: 'primary.main', outlineOffset: 2 },
            }}
          >
            {option.label}
            {option.count !== undefined && (
              <Box component="span" sx={{ fontWeight: 700, opacity: 0.7 }}>
                {option.count}
              </Box>
            )}
          </ButtonBase>
        )
      })}
    </Box>
  )
}
