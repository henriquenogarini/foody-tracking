import { Alert, Box, Button, CircularProgress } from '@mui/material'
import { useAuth } from '@/features/auth/useAuth'

export function SessionGate() {
  const { status, retry, signOut } = useAuth()

  return (
    <Box sx={{ minHeight: '100dvh', display: 'grid', placeItems: 'center', p: 2 }}>
      {status === 'error' ? (
        <Alert
          severity="error"
          action={
            <>
              <Button color="inherit" size="small" onClick={retry}>
                Tentar novamente
              </Button>
              {/* Way out when the API stays down; a reload would land here again. */}
              <Button color="inherit" size="small" onClick={signOut}>
                Sair
              </Button>
            </>
          }
        >
          Não foi possível carregar sua sessão.
        </Alert>
      ) : (
        <CircularProgress aria-label="Carregando sessão" />
      )}
    </Box>
  )
}
