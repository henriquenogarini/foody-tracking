import { Box, Card, Stack, Typography } from '@mui/material'
import type { ReactNode } from 'react'

interface SectionCardProps {
  title: string
  aside?: ReactNode
  children: ReactNode
}

export function SectionCard({ title, aside, children }: SectionCardProps) {
  return (
    <Card component="section" sx={{ p: { xs: 2, md: 3 } }}>
      <Stack spacing={2.5}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
          <Typography variant="h4" component="h2">
            {title}
          </Typography>
          {aside && (
            <Typography variant="body2" color="text.secondary">
              {aside}
            </Typography>
          )}
        </Box>
        {children}
      </Stack>
    </Card>
  )
}
