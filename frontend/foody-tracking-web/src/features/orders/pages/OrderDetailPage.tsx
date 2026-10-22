import ChevronLeftIcon from '@mui/icons-material/ChevronLeft'
import PlaceOutlinedIcon from '@mui/icons-material/PlaceOutlined'
import SearchOffIcon from '@mui/icons-material/SearchOff'
import {
  Alert,
  Box,
  Button,
  Card,
  IconButton,
  Link,
  Skeleton,
  Stack,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material'
import { Link as RouterLink, useParams } from 'react-router'
import { OrderItemsCard } from '@/features/orders/components/OrderItemsCard'
import { OrderStatusChip } from '@/features/orders/components/OrderStatusChip'
import { StatusTimeline } from '@/features/orders/components/StatusTimeline'
import { useOrderHistoryQuery, useOrderQuery } from '@/features/orders/queries'
import CheckIcon from '@mui/icons-material/Check'
import CloseIcon from '@mui/icons-material/Close'
import { DETAIL_ACTION_LABEL } from '@/features/orders/status'
import { buildTimeline } from '@/features/orders/timeline'
import type { Order } from '@/features/orders/types'
import { useStatusChange } from '@/features/orders/useStatusChange'
import { toApiError } from '@/shared/api/problem'
import { EmptyState } from '@/shared/components/EmptyState'
import { SectionCard } from '@/shared/components/SectionCard'
import { formatDateTime, formatRelative, formatTime } from '@/shared/utils/format'
import { maskPhone } from '@/shared/utils/masks'

function createdLabel(createdAt: string): string {
  const today = new Date(createdAt).toDateString() === new Date().toDateString()
  return today ? `Criado às ${formatTime(createdAt)}` : `Criado em ${formatDateTime(createdAt)}`
}

function addressLines({ deliveryAddress: a }: Order): [string, string] {
  const street = [`${a.street}, ${a.number}`, a.complement].filter(Boolean).join(' · ')
  return [street, `${a.neighborhood} · ${a.city}/${a.state} · ${a.postalCode}`]
}

const backLink = (
  <Link
    component={RouterLink}
    to="/orders"
    underline="hover"
    sx={{
      display: 'inline-flex',
      alignItems: 'center',
      gap: 0.5,
      minHeight: 44,
      width: 'fit-content',
      color: 'ink.700',
      fontWeight: 500,
    }}
  >
    <ChevronLeftIcon fontSize="small" />
    Pedidos
  </Link>
)

export default function OrderDetailPage() {
  const { id = '' } = useParams()
  const isDesktop = useMediaQuery(useTheme().breakpoints.up('md'), { noSsr: true })
  const orderQuery = useOrderQuery(id)
  const historyQuery = useOrderHistoryQuery(id)
  const order = orderQuery.data

  if (!order) {
    if (orderQuery.isError) {
      const error = toApiError(orderQuery.error)
      // 404 (missing or someone else's) and 400 (not an id) both mean "no such order".
      if (error.status === 404 || error.status === 400) {
        return (
          <Card>
            <EmptyState
              headingLevel="h1"
              icon={<SearchOffIcon />}
              title="Pedido não encontrado"
              description="Ele pode ter sido digitado errado ou não pertencer à sua conta."
              action={
                <Button variant="contained" component={RouterLink} to="/orders">
                  Ver pedidos
                </Button>
              }
            />
          </Card>
        )
      }
      return (
        <Stack spacing={2}>
          {backLink}
          <Alert
            severity="error"
            action={
              <Button color="inherit" size="small" onClick={() => void orderQuery.refetch()}>
                Tentar novamente
              </Button>
            }
          >
            {error.message}
          </Alert>
        </Stack>
      )
    }
    return (
      <Stack spacing={3} role="status" aria-label="Carregando pedido">
        <Skeleton width={240} height={44} />
        <Skeleton variant="rounded" height={240} sx={{ borderRadius: 1.4 }} />
        <Skeleton variant="rounded" height={160} sx={{ borderRadius: 1.4 }} />
      </Stack>
    )
  }

  return <OrderDetail order={order} history={historyQuery} isDesktop={isDesktop} />
}

interface OrderDetailProps {
  order: Order
  history: ReturnType<typeof useOrderHistoryQuery>
  isDesktop: boolean
}

// Split from the page so the status-change hook only runs once the order exists.
function OrderDetail({ order, history: historyQuery, isDesktop }: OrderDetailProps) {
  const { next, cancellable, busy, applying, advance, askToCancel, confirmDialog } =
    useStatusChange(order)
  const finished = !next && !cancellable

  const nextButton = next && (
    <Button
      variant="contained"
      fullWidth={!isDesktop}
      startIcon={next === 'ENTREGUE' ? <CheckIcon /> : undefined}
      loading={applying === next}
      disabled={busy}
      onClick={advance}
      sx={{ height: 48, flex: { xs: 1, md: 'none' } }}
    >
      {DETAIL_ACTION_LABEL[next]}
    </Button>
  )

  const [street, place] = addressLines(order)
  const updated = `atualizado ${formatRelative(order.updatedAt)}`

  const customerCard = (
    <SectionCard title="Cliente">
      <Stack spacing={0.5}>
        <Typography variant="subtitle1">{order.customer.name}</Typography>
        {order.customer.phone && (
          <Typography color="text.secondary">{maskPhone(order.customer.phone)}</Typography>
        )}
        {order.customer.email && (
          <Typography color="text.secondary" sx={{ overflowWrap: 'anywhere' }}>
            {order.customer.email}
          </Typography>
        )}
      </Stack>
    </SectionCard>
  )
  const addressCard = (
    <SectionCard title="Endereço de entrega">
      <Box sx={{ display: 'flex', gap: 1.5 }}>
        <PlaceOutlinedIcon sx={{ color: 'brand.700', mt: 0.25 }} aria-hidden />
        <Box>
          <Typography>{street}</Typography>
          <Typography variant="body2" color="text.secondary">
            {place}
          </Typography>
        </Box>
      </Box>
    </SectionCard>
  )
  const notesCard = order.notes && (
    <SectionCard title="Observações">
      <Typography sx={{ whiteSpace: 'pre-line', overflowWrap: 'anywhere' }}>
        {order.notes}
      </Typography>
    </SectionCard>
  )
  const timelineCard = (
    <SectionCard title="Linha do tempo">
      {historyQuery.data ? (
        <StatusTimeline steps={buildTimeline(order.status, historyQuery.data)} />
      ) : historyQuery.isError ? (
        <Typography variant="body2" color="text.secondary">
          Não foi possível carregar o histórico.
        </Typography>
      ) : (
        <Skeleton variant="rounded" height={160} />
      )}
      {isDesktop && cancellable && (
        <Box sx={{ borderTop: 1, borderColor: 'divider', pt: 1.5 }}>
          <Button
            variant="text"
            startIcon={<CloseIcon />}
            onClick={askToCancel}
            disabled={busy}
            sx={{ color: 'error.main' }}
          >
            Cancelar pedido
          </Button>
        </Box>
      )}
    </SectionCard>
  )

  return (
    <Stack spacing={{ xs: 2, md: 3.5 }} sx={{ pb: { xs: finished ? 0 : 10, md: 0 } }}>
      <Stack spacing={1}>
        {backLink}
        <Box
          sx={{
            display: 'flex',
            flexWrap: 'wrap',
            gap: 2,
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, alignItems: 'center' }}>
            <Typography
              variant="h2"
              component="h1"
              sx={{ fontSize: { xs: 24, md: 40 }, lineHeight: { xs: '30px', md: '44px' } }}
            >
              Pedido #{order.code}
            </Typography>
            {isDesktop && <OrderStatusChip status={order.status} />}
            {isDesktop && (
              <Typography color="text.secondary">
                {createdLabel(order.createdAt)} · {updated}
              </Typography>
            )}
          </Box>
          {isDesktop && nextButton}
        </Box>
        {!isDesktop && (
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <OrderStatusChip status={order.status} />
            <Typography variant="body2" color="text.secondary">
              {updated}
            </Typography>
          </Box>
        )}
      </Stack>

      {isDesktop ? (
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: 'minmax(0, 1fr) 360px',
            gap: 3,
            alignItems: 'start',
          }}
        >
          <Stack spacing={3}>
            <OrderItemsCard order={order} variant="table" />
            <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 3 }}>
              {customerCard}
              {addressCard}
            </Box>
            {notesCard}
          </Stack>
          <Box sx={{ position: 'sticky', top: 88 }}>{timelineCard}</Box>
        </Box>
      ) : (
        <Stack spacing={2}>
          <OrderItemsCard order={order} variant="list" />
          {customerCard}
          {addressCard}
          {notesCard}
          {timelineCard}
        </Stack>
      )}

      {!isDesktop && !finished && (
        <Box
          sx={{
            position: 'fixed',
            insetInline: 0,
            bottom: 72,
            zIndex: 'appBar',
            px: 2,
            py: 1.5,
            bgcolor: 'surface.0',
            borderTop: 1,
            borderColor: 'divider',
          }}
        >
          <Box sx={{ display: 'flex', gap: 1.5 }}>
            {cancellable && (
              <IconButton
                aria-label={`Cancelar o pedido #${order.code}`}
                onClick={askToCancel}
                disabled={busy}
                sx={{
                  width: 52,
                  height: 48,
                  flex: 'none',
                  border: 1,
                  borderColor: 'border.strong',
                  borderRadius: 1,
                  color: 'error.main',
                }}
              >
                <CloseIcon />
              </IconButton>
            )}
            {nextButton}
          </Box>
        </Box>
      )}
      {confirmDialog}
    </Stack>
  )
}
