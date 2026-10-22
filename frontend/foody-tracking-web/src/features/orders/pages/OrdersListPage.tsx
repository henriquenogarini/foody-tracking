import AddIcon from '@mui/icons-material/Add'
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined'
import SearchIcon from '@mui/icons-material/Search'
import {
  Alert,
  Box,
  Button,
  Card,
  Fab,
  InputAdornment,
  OutlinedInput,
  Pagination,
  Skeleton,
  Stack,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material'
import { useEffect, useRef, useState } from 'react'
import { Navigate, Link as RouterLink, useSearchParams } from 'react-router'
import { OrderCard } from '@/features/orders/components/OrderCard'
import { OrdersSummary } from '@/features/orders/components/OrdersSummary'
import { OrdersTable } from '@/features/orders/components/OrdersTable'
import { StatusFilter } from '@/features/orders/components/StatusFilter'
import { useOrdersQuery, useStatusCountsQuery } from '@/features/orders/queries'
import { ORDER_STATUSES, type OrderStatus } from '@/features/orders/types'
import { toApiError } from '@/shared/api/problem'
import { EmptyState } from '@/shared/components/EmptyState'
import { formatRelative } from '@/shared/utils/format'

const PAGE_SIZE = 10
const SEARCH_DEBOUNCE_MS = 300

function parseStatus(value: string | null): OrderStatus | undefined {
  return ORDER_STATUSES.find((status) => status === value)
}

/** 1-based page from the URL; anything that is not a positive integer means the first page. */
function parsePage(value: string | null): number {
  const page = Number(value)
  return Number.isInteger(page) && page > 1 ? page : 1
}

export default function OrdersListPage() {
  const isDesktop = useMediaQuery(useTheme().breakpoints.up('md'), { noSsr: true })

  // Filters live in the URL (?status=EM_PREPARO&q=ana&page=2): shareable and reload-safe. Status and
  // page changes are history entries (back undoes them); typing replaces the entry instead.
  const [searchParams, setSearchParams] = useSearchParams()
  const status = parseStatus(searchParams.get('status'))
  const q = searchParams.get('q') ?? ''
  const page = parsePage(searchParams.get('page'))

  const updateParams = (changes: Record<string, string | undefined>, replace = false) =>
    setSearchParams(
      (current) => {
        const next = new URLSearchParams(current)
        for (const [key, value] of Object.entries(changes)) {
          if (value) next.set(key, value)
          else next.delete(key)
        }
        return next
      },
      { replace },
    )

  // The field updates on every key; the URL (and the request) only after a short pause.
  const [searchInput, setSearchInput] = useState(q)
  // Follows the URL when it changes from outside the field (back button, a link to /orders).
  // Storing the previous value and adjusting during render is React's pattern for this, not an effect.
  const [syncedQ, setSyncedQ] = useState(q)
  if (q !== syncedQ) {
    setSyncedQ(q)
    if (q !== searchInput.trim()) setSearchInput(q)
  }
  const searchTimer = useRef<ReturnType<typeof setTimeout>>(undefined)
  useEffect(() => () => clearTimeout(searchTimer.current), [])
  const onSearchChange = (value: string) => {
    setSearchInput(value)
    clearTimeout(searchTimer.current)
    searchTimer.current = setTimeout(
      () => updateParams({ q: value.trim() || undefined, page: undefined }, true),
      SEARCH_DEBOUNCE_MS,
    )
  }

  const ordersQuery = useOrdersQuery({ status, q: q || undefined, page: page - 1, size: PAGE_SIZE })
  const { data: counts } = useStatusCountsQuery()
  const result = ordersQuery.data
  // While a new filter loads, the previous rows stay (dimmed) instead of flashing skeletons.
  const stale = ordersQuery.isPlaceholderData
  const filtering = Boolean(status || q)

  const clearFilters = () => {
    clearTimeout(searchTimer.current)
    setSearchInput('')
    updateParams({ status: undefined, q: undefined, page: undefined })
  }

  // A page past the end (old link, or the last order on it changed status): go to the last page.
  if (result && !stale && result.content.length === 0 && result.totalElements > 0) {
    const lastPage = new URLSearchParams(searchParams)
    if (result.totalPages > 1) lastPage.set('page', String(result.totalPages))
    else lastPage.delete('page')
    return <Navigate to={{ search: lastPage.toString() }} replace />
  }

  let content
  if (!result && ordersQuery.isError) {
    content = (
      <Alert
        severity="error"
        action={
          <Button color="inherit" size="small" onClick={() => void ordersQuery.refetch()}>
            Tentar novamente
          </Button>
        }
      >
        {toApiError(ordersQuery.error).message}
      </Alert>
    )
  } else if (result && !stale && result.totalElements === 0) {
    content = (
      <Card>
        {filtering ? (
          <EmptyState
            icon={<SearchIcon />}
            title="Nenhum pedido encontrado"
            description="Nenhum pedido corresponde aos filtros atuais."
            action={
              <Button variant="outlined" onClick={clearFilters}>
                Limpar filtros
              </Button>
            }
          />
        ) : (
          <EmptyState
            icon={<ReceiptLongOutlinedIcon />}
            title="Nenhum pedido ainda"
            description="Cadastre o primeiro pedido para acompanhar cada etapa até a entrega."
            action={
              <Button variant="contained" component={RouterLink} to="/orders/new">
                Criar primeiro pedido
              </Button>
            }
          />
        )}
      </Card>
    )
  } else {
    const from = result ? result.page * result.size + 1 : 0
    const to = result ? from + result.content.length - 1 : 0
    const footer = result && (
      <Box
        sx={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: 1.5,
          justifyContent: 'space-between',
          alignItems: 'center',
          px: { md: 2 },
          py: 1.5,
          borderTop: { md: 1 },
          borderColor: { md: 'divider' },
        }}
      >
        <Typography variant="body2" color="text.secondary">
          Mostrando {from}–{to} de {result.totalElements} pedidos
        </Typography>
        {result.totalPages > 1 && (
          <Pagination
            count={result.totalPages}
            page={page}
            onChange={(_, value) => updateParams({ page: value > 1 ? String(value) : undefined })}
            shape="rounded"
            size={isDesktop ? 'medium' : 'large'}
            getItemAriaLabel={(type, item) =>
              type === 'page'
                ? `Página ${item}`
                : type === 'next'
                  ? 'Próxima página'
                  : 'Página anterior'
            }
            sx={{
              '& .Mui-selected': { bgcolor: 'ink.900', color: 'surface.0' },
              '& .Mui-selected:hover': { bgcolor: 'ink.700' },
            }}
          />
        )}
      </Box>
    )
    content = isDesktop ? (
      <Card sx={{ overflow: 'hidden', opacity: stale ? 0.6 : 1, transition: 'opacity 150ms' }}>
        <OrdersTable orders={result?.content} />
        {footer}
      </Card>
    ) : (
      <Stack spacing={1.5} sx={{ opacity: stale ? 0.6 : 1, transition: 'opacity 150ms' }}>
        {result
          ? result.content.map((order) => <OrderCard key={order.id} order={order} />)
          : Array.from({ length: 3 }, (_, index) => (
              <Skeleton key={index} variant="rounded" height={180} sx={{ borderRadius: 1.4 }} />
            ))}
        {footer}
      </Stack>
    )
  }

  return (
    <Stack spacing={{ xs: 2, md: 3.5 }}>
      <Box
        sx={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: 2,
          justifyContent: 'space-between',
          alignItems: { xs: 'baseline', md: 'flex-end' },
        }}
      >
        <Stack spacing={0.75}>
          <Typography
            variant="h2"
            component="h1"
            sx={{ fontSize: { xs: 24, md: 32 }, lineHeight: { xs: '30px', md: '38px' } }}
          >
            Pedidos
          </Typography>
          <Typography sx={{ color: 'ink.700', display: { xs: 'none', md: 'block' } }}>
            Atualizado automaticamente a cada 10 segundos
            {ordersQuery.dataUpdatedAt > 0 &&
              ` · última vez ${formatRelative(new Date(ordersQuery.dataUpdatedAt).toISOString())}`}
          </Typography>
        </Stack>
        {isDesktop ? (
          <Button
            variant="contained"
            component={RouterLink}
            to="/orders/new"
            startIcon={<AddIcon />}
          >
            Novo pedido
          </Button>
        ) : (
          counts && (
            <Typography variant="body2" color="text.secondary">
              {counts.total} {counts.total === 1 ? 'pedido' : 'pedidos'}
            </Typography>
          )
        )}
      </Box>

      <OrdersSummary counts={counts} />

      <Box
        sx={{
          display: 'flex',
          flexDirection: { xs: 'column', md: 'row' },
          gap: { xs: 1.5, md: 2 },
          justifyContent: 'space-between',
          alignItems: { md: 'center' },
        }}
      >
        <StatusFilter
          value={status}
          counts={counts}
          onChange={(value) => updateParams({ status: value, page: undefined })}
        />
        <OutlinedInput
          type="search"
          value={searchInput}
          onChange={(event) => onSearchChange(event.target.value)}
          placeholder="Buscar por cliente ou código"
          size="small"
          startAdornment={
            <InputAdornment position="start" sx={{ color: 'ink.500' }}>
              <SearchIcon fontSize="small" />
            </InputAdornment>
          }
          slotProps={{ input: { 'aria-label': 'Buscar pedidos', maxLength: 100 } }}
          sx={{ width: { xs: 1, md: 280 }, flex: 'none' }}
        />
      </Box>

      {content}

      {!isDesktop && (
        <Fab
          variant="extended"
          color="primary"
          component={RouterLink}
          to="/orders/new"
          sx={{
            position: 'fixed',
            right: 16,
            // Above the 72px bottom tab bar.
            bottom: 72 + 16,
            height: 52,
            px: 2.5,
            gap: 1,
            borderRadius: 999,
            textTransform: 'none',
            typography: 'button',
          }}
        >
          <AddIcon />
          Novo pedido
        </Fab>
      )}
    </Stack>
  )
}
