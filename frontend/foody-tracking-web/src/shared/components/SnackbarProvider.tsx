import { Alert, Snackbar, useMediaQuery, useTheme } from '@mui/material'
import { useCallback, useState, type ReactNode } from 'react'
import { SnackbarContext, type Notify, type SnackbarSeverity } from '@/shared/hooks/useSnackbar'

interface Message {
  key: number
  text: string
  severity: SnackbarSeverity
}

export function SnackbarProvider({ children }: { children: ReactNode }) {
  const isDesktop = useMediaQuery(useTheme().breakpoints.up('md'), { noSsr: true })
  const [message, setMessage] = useState<Message | null>(null)
  const notify = useCallback<Notify>(
    (text, severity = 'success') => setMessage({ key: Date.now(), text, severity }),
    [],
  )
  const close = () => setMessage(null)

  return (
    <SnackbarContext value={notify}>
      {children}
      <Snackbar
        key={message?.key}
        open={message !== null}
        autoHideDuration={5000}
        onClose={(_, reason) => reason !== 'clickaway' && close()}
        // On phones the actions sit at the bottom, so the toast goes on top, below the 56px app bar.
        anchorOrigin={{ vertical: isDesktop ? 'bottom' : 'top', horizontal: 'center' }}
        sx={{ top: { xs: 64 } }}
      >
        <Alert
          severity={message?.severity ?? 'success'}
          variant="filled"
          onClose={close}
          sx={{ width: 1 }}
        >
          {message?.text}
        </Alert>
      </Snackbar>
    </SnackbarContext>
  )
}
