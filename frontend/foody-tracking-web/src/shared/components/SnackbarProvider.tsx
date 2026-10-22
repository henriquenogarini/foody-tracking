import { Alert, Snackbar } from '@mui/material'
import { useCallback, useState, type ReactNode } from 'react'
import { SnackbarContext, type Notify, type SnackbarSeverity } from '@/shared/hooks/useSnackbar'

interface Message {
  key: number
  text: string
  severity: SnackbarSeverity
}

/** One toast at a time; a new message replaces the current one. */
export function SnackbarProvider({ children }: { children: ReactNode }) {
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
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
        // Clear of the 72px mobile tab bar.
        sx={{ bottom: { xs: 88, md: 24 } }}
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
