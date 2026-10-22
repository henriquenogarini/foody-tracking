import { Box, Stack, Typography, useTheme } from '@mui/material'
import type { StepState, TimelineStep } from '@/features/orders/timeline'
import { formatDateTime } from '@/shared/utils/format'

/** Vertical status history ("Linha do tempo"), oldest first. */
export function StatusTimeline({ steps }: { steps: TimelineStep[] }) {
  const { palette } = useTheme()
  // Design dots: green when done, orange with a halo for the current step, grey ahead, red if cancelled.
  const dot: Record<StepState, object> = {
    done: { bgcolor: 'success.main' },
    current: { bgcolor: 'brand.500', boxShadow: `0 0 0 4px ${palette.brand[100]}` },
    pending: { bgcolor: 'ink.300' },
    cancelled: { bgcolor: 'error.main' },
  }

  return (
    <Box component="ol" sx={{ listStyle: 'none', m: 0, p: 0 }}>
      {steps.map((step, index) => {
        const last = index === steps.length - 1
        const pending = step.state === 'pending'
        return (
          <Box
            component="li"
            key={`${step.status}-${index}`}
            aria-current={step.state === 'current' ? 'step' : undefined}
            sx={{ display: 'grid', gridTemplateColumns: '24px 1fr', columnGap: 1.5 }}
          >
            <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', pt: 0.75 }}>
              <Box
                sx={{ width: 10, height: 10, borderRadius: 999, flex: 'none', ...dot[step.state] }}
              />
              {!last && (
                <Box sx={{ width: 2, flex: 1, minHeight: 24, my: 0.5, bgcolor: 'divider' }} />
              )}
            </Box>
            <Stack spacing={0.25} sx={{ pb: last ? 0 : 2.5 }}>
              <Typography
                variant="subtitle1"
                sx={{ color: pending ? 'text.secondary' : 'ink.900' }}
              >
                {step.label}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {step.description}
              </Typography>
              {step.at && (
                <Typography
                  variant="body2"
                  component="time"
                  dateTime={step.at}
                  sx={{ color: 'ink.700', fontVariantNumeric: 'tabular-nums' }}
                >
                  {formatDateTime(step.at)}
                </Typography>
              )}
            </Stack>
          </Box>
        )
      })}
    </Box>
  )
}
