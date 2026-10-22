import { Stack, Typography } from '@mui/material'
import type { ReactNode } from 'react'

interface EmptyStateProps {
  title: string
  description?: string
  icon?: ReactNode
  /** Call to action, e.g. "Criar primeiro pedido". */
  action?: ReactNode
  /** h1 when the empty state is the whole page (e.g. "Pedido não encontrado"). */
  headingLevel?: 'h1' | 'h2'
}

export function EmptyState({
  title,
  description,
  icon,
  action,
  headingLevel = 'h2',
}: EmptyStateProps) {
  return (
    <Stack spacing={1.5} sx={{ alignItems: 'center', textAlign: 'center', py: 8, px: 2 }}>
      {icon && <Stack sx={{ color: 'ink.300', '& svg': { fontSize: 48 } }}>{icon}</Stack>}
      <Typography variant="h4" component={headingLevel}>
        {title}
      </Typography>
      {description && (
        <Typography color="text.secondary" sx={{ maxWidth: 420 }}>
          {description}
        </Typography>
      )}
      {action && <Stack sx={{ pt: 1 }}>{action}</Stack>}
    </Stack>
  )
}
