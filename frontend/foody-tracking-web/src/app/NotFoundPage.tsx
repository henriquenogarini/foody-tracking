import { Box, Button, Stack, Typography } from '@mui/material'
import { Link } from 'react-router'
import { BrandLogo } from '@/shared/components/BrandLogo'

export default function NotFoundPage() {
  return (
    <Box component="main" sx={{ minHeight: '100dvh', display: 'grid', placeItems: 'center', p: 2 }}>
      <Stack spacing={3} sx={{ alignItems: 'center', textAlign: 'center', maxWidth: 420 }}>
        <BrandLogo height={40} />
        <Typography variant="h2" component="h1">
          Página não encontrada
        </Typography>
        <Typography color="text.secondary">
          O endereço acessado não existe ou foi movido.
        </Typography>
        <Button variant="contained" component={Link} to="/orders">
          Voltar para pedidos
        </Button>
      </Stack>
    </Box>
  )
}
