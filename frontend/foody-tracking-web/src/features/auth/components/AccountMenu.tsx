import LogoutIcon from '@mui/icons-material/Logout'
import { Box, Divider, ListItemIcon, Menu, MenuItem, Typography } from '@mui/material'
import { useAuth } from '@/features/auth/useAuth'

interface AccountMenuProps {
  anchorEl: HTMLElement | null
  onClose: () => void
}

export function AccountMenu({ anchorEl, onClose }: AccountMenuProps) {
  const { user, signOut } = useAuth()

  return (
    <Menu
      anchorEl={anchorEl}
      open={Boolean(anchorEl)}
      onClose={onClose}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      slotProps={{ paper: { sx: { minWidth: 240, mt: 1 } } }}
    >
      <Box sx={{ px: 2, py: 1.5 }}>
        <Typography sx={{ fontWeight: 600 }}>{user?.name}</Typography>
        <Typography variant="body2" color="text.secondary">
          {user?.email}
        </Typography>
      </Box>
      <Divider />
      <MenuItem
        onClick={() => {
          onClose()
          signOut()
        }}
      >
        <ListItemIcon>
          <LogoutIcon fontSize="small" />
        </ListItemIcon>
        Sair
      </MenuItem>
    </Menu>
  )
}
