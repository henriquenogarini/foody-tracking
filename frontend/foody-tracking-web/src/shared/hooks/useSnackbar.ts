import { createContext, useContext } from 'react'

export type SnackbarSeverity = 'success' | 'info' | 'warning' | 'error'

export type Notify = (message: string, severity?: SnackbarSeverity) => void

export const SnackbarContext = createContext<Notify | null>(null)

/** Short feedback toast ("Pedido criado", conflict messages). */
export function useSnackbar(): Notify {
  const notify = useContext(SnackbarContext)
  if (!notify) throw new Error('useSnackbar must be used inside <SnackbarProvider>')
  return notify
}
