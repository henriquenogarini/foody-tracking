import { Box, useTheme } from '@mui/material'

interface BrandLogoProps {
  variant?: 'default' | 'inverse'
  height?: number
  stacked?: boolean
}

export function BrandLogo({ variant = 'default', height = 32, stacked = false }: BrandLogoProps) {
  const { palette, typography } = useTheme()
  const inverse = variant === 'inverse'
  const tile = inverse ? palette.surface[0] : palette.brand[500]
  const glyph = inverse ? palette.brand[500] : palette.surface[0]

  return (
    <Box
      role="img"
      aria-label="Foody Tracking"
      sx={{
        display: 'inline-flex',
        flexDirection: stacked ? 'column' : 'row',
        alignItems: 'center',
        gap: `${height * (stacked ? 0.24 : 0.28)}px`,
        color: inverse ? palette.surface[0] : palette.ink[900],
      }}
    >
      <svg viewBox="0 0 64 64" width={height} height={height} aria-hidden="true">
        <rect width="64" height="64" rx="18" fill={tile} />
        <path
          d="M19 47C19 37 27 36 33 32 35 30.6 36.6 29.2 38 28"
          fill="none"
          stroke={glyph}
          strokeWidth="5.5"
          strokeLinecap="round"
        />
        <circle cx="19" cy="47" r="4.75" fill={glyph} />
        <path
          d="M43 9.5a10 10 0 0 1 10 10c0 7.2-10 15.5-10 15.5s-10-8.3-10-15.5a10 10 0 0 1 10-10zM43 15a4.5 4.5 0 1 0 0 9 4.5 4.5 0 1 0 0-9z"
          fill={glyph}
          fillRule="evenodd"
        />
      </svg>
      <Box
        component="span"
        aria-hidden="true"
        sx={{
          fontFamily: typography.h1.fontFamily,
          fontSize: height * (stacked ? 0.42 : 0.625),
          lineHeight: 1,
          letterSpacing: '-0.015em',
          whiteSpace: 'nowrap',
        }}
      >
        <Box component="span" sx={{ fontWeight: 700 }}>
          Foody
        </Box>{' '}
        <Box component="span" sx={{ fontWeight: 500 }}>
          Tracking
        </Box>
      </Box>
    </Box>
  )
}
