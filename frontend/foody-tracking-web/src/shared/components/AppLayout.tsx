import AddIcon from '@mui/icons-material/Add'
import ListIcon from '@mui/icons-material/FormatListBulleted'
import {
  AppBar,
  BottomNavigation,
  BottomNavigationAction,
  Box,
  Button,
  Toolbar,
  useMediaQuery,
  useTheme,
} from '@mui/material'
import { Link, Outlet, useMatch } from 'react-router'
import { BrandLogo } from './BrandLogo'

const NAV_ITEMS = [
  { to: '/orders', label: 'Pedidos', icon: <ListIcon /> },
  { to: '/orders/new', label: 'Novo pedido', icon: <AddIcon /> },
]

// Desktop: top bar with links. Mobile (< md): compact top bar + bottom tab bar, as in the design.
export function AppLayout() {
  // Order detail (/orders/:id) still belongs to "Pedidos"; only the form is "Novo pedido".
  const current = useMatch('/orders/new') ? '/orders/new' : '/orders'
  // noSsr reads the media query on the first render, so the logo never jumps size.
  const isDesktop = useMediaQuery(useTheme().breakpoints.up('md'), { noSsr: true })

  return (
    <Box sx={{ minHeight: '100dvh', display: 'flex', flexDirection: 'column' }}>
      <AppBar
        position="sticky"
        color="inherit"
        elevation={0}
        sx={{ borderBottom: 1, borderColor: 'divider' }}
      >
        <Toolbar
          disableGutters
          sx={{ minHeight: { xs: 56, md: 64 }, px: { xs: 2, md: 4 }, gap: 4 }}
        >
          <Box
            component={Link}
            to="/orders"
            aria-label="Foody Tracking, início"
            // 44px tall hit area around the 28-32px logo: the touch-target minimum.
            sx={{
              display: 'flex',
              alignItems: 'center',
              minHeight: 44,
              color: 'inherit',
              textDecoration: 'none',
            }}
          >
            <BrandLogo height={isDesktop ? 32 : 28} />
          </Box>
          <Box
            component="nav"
            aria-label="Principal"
            sx={{ display: { xs: 'none', md: 'flex' }, gap: 0.5 }}
          >
            {NAV_ITEMS.map((item) => {
              const active = item.to === current
              return (
                <Button
                  key={item.to}
                  component={Link}
                  to={item.to}
                  aria-current={active ? 'page' : undefined}
                  sx={{
                    height: 40,
                    px: '14px',
                    fontWeight: 600,
                    color: active ? 'brand.700' : 'ink.700',
                    bgcolor: active ? 'brand.50' : 'transparent',
                    '&:hover': { bgcolor: active ? 'brand.50' : 'surface.100' },
                  }}
                >
                  {item.label}
                </Button>
              )
            })}
          </Box>
        </Toolbar>
      </AppBar>

      <Box
        component="main"
        sx={{
          flex: 1,
          width: 1,
          maxWidth: 1216,
          mx: 'auto',
          px: { xs: 2, md: 4 },
          pt: { xs: 2, md: 5 },
          // Keeps the last row clear of the fixed 72px tab bar on mobile.
          pb: { xs: 12, md: 6 },
        }}
      >
        <Outlet />
      </Box>

      <BottomNavigation
        component="nav"
        aria-label="Navegação inferior"
        value={current}
        showLabels
        sx={{
          display: { md: 'none' },
          position: 'fixed',
          insetInline: 0,
          bottom: 0,
          height: 72,
          borderTop: 1,
          borderColor: 'divider',
          zIndex: 'appBar',
        }}
      >
        {NAV_ITEMS.map((item) => (
          <BottomNavigationAction
            key={item.to}
            component={Link}
            to={item.to}
            value={item.to}
            label={item.label}
            icon={item.icon}
            aria-current={item.to === current ? 'page' : undefined}
          />
        ))}
      </BottomNavigation>
    </Box>
  )
}
