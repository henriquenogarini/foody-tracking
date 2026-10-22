import AddIcon from '@mui/icons-material/Add'
import ListIcon from '@mui/icons-material/FormatListBulleted'
import PersonOutlineIcon from '@mui/icons-material/PersonOutlined'
import {
  AppBar,
  Avatar,
  BottomNavigation,
  BottomNavigationAction,
  Box,
  Button,
  Toolbar,
  useMediaQuery,
  useTheme,
} from '@mui/material'
import { useState } from 'react'
import { Link, Outlet, useMatch } from 'react-router'
import { AccountMenu } from '@/features/auth/components/AccountMenu'
import { useAuth } from '@/features/auth/useAuth'
import { BrandLogo } from '@/shared/components/BrandLogo'
import { formatInitials } from '@/shared/utils/format'

const NAV_ITEMS = [
  { to: '/orders', label: 'Pedidos', icon: <ListIcon /> },
  { to: '/orders/new', label: 'Novo pedido', icon: <AddIcon /> },
]

export function AppLayout() {
  const current = useMatch('/orders/new') ? '/orders/new' : '/orders'
  // noSsr reads the media query on the first render, so the logo never jumps size.
  const isDesktop = useMediaQuery(useTheme().breakpoints.up('md'), { noSsr: true })
  const { user } = useAuth()
  const [accountAnchor, setAccountAnchor] = useState<HTMLElement | null>(null)
  const userName = user?.name ?? ''

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
          <Button
            onClick={(event) => setAccountAnchor(event.currentTarget)}
            aria-haspopup="menu"
            aria-expanded={Boolean(accountAnchor)}
            aria-label={`Conta de ${userName}`}
            sx={{
              display: { xs: 'none', md: 'inline-flex' },
              ml: 'auto',
              px: 1,
              gap: 1.25,
              fontWeight: 600,
              color: 'ink.900',
            }}
          >
            <Avatar
              sx={{
                width: 36,
                height: 36,
                bgcolor: 'brand.100',
                color: 'brand.800',
                fontSize: 13,
                fontWeight: 700,
              }}
            >
              {formatInitials(userName)}
            </Avatar>
            {userName.split(' ')[0]}
          </Button>
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
          // Clears the fixed 72px tab bar on mobile.
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
        <BottomNavigationAction
          value="account"
          label="Conta"
          icon={<PersonOutlineIcon />}
          aria-haspopup="menu"
          aria-expanded={Boolean(accountAnchor)}
          onClick={(event) => setAccountAnchor(event.currentTarget)}
        />
      </BottomNavigation>

      <AccountMenu anchorEl={accountAnchor} onClose={() => setAccountAnchor(null)} />
    </Box>
  )
}
