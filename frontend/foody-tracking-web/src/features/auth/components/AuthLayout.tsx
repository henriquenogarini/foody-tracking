import { Box, Stack, Typography, useTheme } from '@mui/material'
import type { ReactNode } from 'react'
import { BrandLogo } from '@/shared/components/BrandLogo'

interface AuthLayoutProps {
  title: string
  subtitle: string
  children: ReactNode
  /** Link line under the form ("Ainda não tem conta? Criar conta"). */
  footer: ReactNode
}

// Route illustration from the design's orange panel (decorative).
const illustration = (
  <Box
    component="svg"
    viewBox="0 0 520 420"
    aria-hidden="true"
    sx={{ width: 1, maxWidth: 520, height: 'auto', display: 'block' }}
  >
    <path
      d="M60 360C60 260 170 250 230 210 290 170 330 150 420 90"
      fill="none"
      stroke="currentColor"
      strokeOpacity=".35"
      strokeWidth="3"
      strokeDasharray="2 14"
      strokeLinecap="round"
    />
    <path
      d="M60 360C60 260 170 250 230 210"
      fill="none"
      stroke="currentColor"
      strokeWidth="10"
      strokeLinecap="round"
    />
    <circle cx="60" cy="360" r="14" fill="currentColor" />
    <circle cx="230" cy="210" r="9" fill="currentColor" />
    <path
      d="M420 60a30 30 0 0 1 30 30c0 22-30 48-30 48s-30-26-30-48a30 30 0 0 1 30-30zM420 76a14 14 0 1 0 0 28 14 14 0 1 0 0-28z"
      fill="currentColor"
      fillRule="evenodd"
    />
    {[
      { x: 86, y: 120, bar: 80, line: 52 },
      { x: 290, y: 270, bar: 64, line: 90 },
    ].map((card) => (
      <g key={card.x} transform={`translate(${card.x} ${card.y})`}>
        <rect width="150" height="56" rx="12" fill="currentColor" fillOpacity=".16" />
        <rect
          x="14"
          y="20"
          width={card.bar}
          height="8"
          rx="4"
          fill="currentColor"
          fillOpacity=".85"
        />
        <rect
          x="14"
          y="34"
          width={card.line}
          height="6"
          rx="3"
          fill="currentColor"
          fillOpacity=".5"
        />
        <circle cx="128" cy="28" r="10" fill="currentColor" />
      </g>
    ))}
  </Box>
)

// Desktop: orange brand panel + form column. Mobile: thin orange strip, centered stacked logo and title.
export function AuthLayout({ title, subtitle, children, footer }: AuthLayoutProps) {
  const { palette } = useTheme()

  return (
    <Box
      sx={{
        minHeight: '100dvh',
        display: 'flex',
        bgcolor: 'background.default',
        // Thin brand strip on mobile, where the orange panel is hidden. The color goes inside the
        // shorthand: the responsive value is emitted in a media query that would reset a separate color.
        borderTop: { xs: `8px solid ${palette.primary.main}`, md: 'none' },
      }}
    >
      <Box
        component="aside"
        sx={{
          display: { xs: 'none', md: 'flex' },
          flex: '1 1 440px',
          minWidth: 0,
          flexDirection: 'column',
          justifyContent: 'space-between',
          gap: 5,
          p: '48px 56px',
          bgcolor: 'primary.main',
          color: 'surface.0',
        }}
      >
        <BrandLogo variant="inverse" height={40} />
        <Stack spacing={4} sx={{ maxWidth: 520 }}>
          {illustration}
          <Stack spacing={1.5}>
            <Typography variant="h2" component="p" sx={{ fontSize: 36, lineHeight: '40px' }}>
              Do pedido ao portão, em um relance.
            </Typography>
            <Typography sx={{ fontSize: 16, lineHeight: '24px', opacity: 0.92, maxWidth: 440 }}>
              Cadastre pedidos, avance cada etapa e veja o que está na rua sem recarregar a página.
            </Typography>
          </Stack>
        </Stack>
        <Typography variant="body2" sx={{ fontWeight: 500, opacity: 0.8 }}>
          Teste técnico · Foody Delivery · 2026
        </Typography>
      </Box>

      <Box
        component="main"
        sx={{
          flex: '999 1 560px',
          minWidth: 0,
          display: 'flex',
          justifyContent: 'center',
          alignItems: { xs: 'stretch', md: 'center' },
          px: 3,
          py: { xs: 5, md: 6 },
        }}
      >
        <Stack spacing={3.5} useFlexGap sx={{ width: 1, maxWidth: 440 }}>
          <Stack spacing={1} useFlexGap sx={{ textAlign: { xs: 'center', md: 'left' } }}>
            <Box sx={{ display: { xs: 'none', md: 'block' }, mb: 2.5 }}>
              <BrandLogo height={36} />
            </Box>
            <Box sx={{ display: { xs: 'flex', md: 'none' }, justifyContent: 'center', mb: 4 }}>
              <BrandLogo height={68} stacked />
            </Box>
            <Typography
              variant="h1"
              sx={{ fontSize: { xs: 28, md: 40 }, lineHeight: { xs: '34px', md: '44px' } }}
            >
              {title}
            </Typography>
            <Typography sx={{ fontSize: 16, lineHeight: '24px', color: 'ink.700' }}>
              {subtitle}
            </Typography>
          </Stack>
          {children}
          <Typography
            sx={{
              typography: 'body1',
              color: 'ink.700',
              textAlign: 'center',
              fontWeight: 500,
              // Mobile design pins the link line to the bottom of the screen.
              mt: { xs: 'auto', md: 0 },
              '& a': { display: 'inline-flex', alignItems: 'center', minHeight: 44 },
            }}
          >
            {footer}
          </Typography>
        </Stack>
      </Box>
    </Box>
  )
}
