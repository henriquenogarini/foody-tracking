import AddIcon from '@mui/icons-material/Add'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutlineOutlined'
import {
  Box,
  Button,
  IconButton,
  InputAdornment,
  Stack,
  useMediaQuery,
  useTheme,
} from '@mui/material'
import { useFieldArray, useFormContext } from 'react-hook-form'
import { itemsLabel } from '@/features/orders/display'
import { emptyItem, MAX_ITEMS, type OrderFormValues } from '@/features/orders/schemas'
import { FormField } from '@/shared/components/FormField'
import { SectionCard } from '@/shared/components/SectionCard'

const DESKTOP_COLUMNS = 'minmax(0, 1fr) 112px 160px 44px'

// The item count lives here, so typing in a line re-renders this section, not the whole page.
export function OrderItemsSection() {
  const isDesktop = useMediaQuery(useTheme().breakpoints.up('md'), { noSsr: true })
  const {
    control,
    register,
    setFocus,
    formState: { errors },
  } = useFormContext<OrderFormValues>()
  const { fields, append, remove } = useFieldArray({ control, name: 'items' })
  const listError = errors.items?.message ?? errors.items?.root?.message

  // The removed button goes with its row: focus the line that takes its place.
  const removeItem = (index: number) => {
    remove(index)
    // Deferred until the remaining rows re-render under their shifted names.
    setTimeout(() => setFocus(`items.${Math.min(index, fields.length - 2)}.description`))
  }

  return (
    <SectionCard title="Itens" aside={itemsLabel(fields.length)}>
      <Stack spacing={2}>
        {isDesktop && (
          <Box
            aria-hidden
            sx={{
              display: 'grid',
              gridTemplateColumns: DESKTOP_COLUMNS,
              gap: 2,
              typography: 'subtitle2',
              color: 'ink.700',
            }}
          >
            <span>Descrição</span>
            <span>Qtd.</span>
            <span>Preço unit.</span>
          </Box>
        )}
        {fields.map((field, index) => {
          const itemErrors = errors.items?.[index]
          const position = fields.length > 1 ? ` do item ${index + 1}` : ''
          const removable = fields.length > 1

          return (
            <Box
              key={field.id}
              sx={{
                display: 'grid',
                gridTemplateColumns: { xs: '1fr', md: DESKTOP_COLUMNS },
                gap: 2,
                p: { xs: 2, md: 0 },
                border: { xs: 1, md: 0 },
                borderColor: 'divider',
                borderRadius: 1.4,
              }}
            >
              <FormField
                label={`Descrição${position}`}
                hideLabel={isDesktop}
                placeholder="Ex.: Pizza calabresa G"
                error={itemErrors?.description?.message}
                {...register(`items.${index}.description`)}
              />
              <Box
                sx={{
                  display: { xs: 'grid', md: 'contents' },
                  gridTemplateColumns: '1fr 1fr',
                  gap: 2,
                }}
              >
                <FormField
                  label={`Qtd.${position}`}
                  hideLabel={isDesktop}
                  inputMode="numeric"
                  error={itemErrors?.quantity?.message}
                  {...register(`items.${index}.quantity`)}
                />
                <FormField
                  label={`Preço unit.${position}`}
                  hideLabel={isDesktop}
                  inputMode="decimal"
                  placeholder="0,00"
                  startAdornment={<InputAdornment position="start">R$</InputAdornment>}
                  error={itemErrors?.unitPrice?.message}
                  {...register(`items.${index}.unitPrice`)}
                />
              </Box>
              {isDesktop ? (
                removable ? (
                  <IconButton
                    aria-label={`Remover item ${index + 1}`}
                    onClick={() => removeItem(index)}
                    sx={{ width: 44, height: 44, color: 'ink.500', alignSelf: 'start' }}
                  >
                    <DeleteOutlineIcon />
                  </IconButton>
                ) : (
                  <span />
                )
              ) : (
                removable && (
                  <Button
                    variant="text"
                    startIcon={<DeleteOutlineIcon />}
                    onClick={() => removeItem(index)}
                    aria-label={`Remover item ${index + 1}`}
                    sx={{ justifySelf: 'end', color: 'error.main' }}
                  >
                    Remover
                  </Button>
                )
              )}
            </Box>
          )
        })}
        {listError && (
          <Box role="alert" sx={{ typography: 'body2', color: 'error.main' }}>
            {listError}
          </Box>
        )}
        <Box>
          <Button
            variant="outlined"
            startIcon={<AddIcon />}
            onClick={() => append(emptyItem)}
            disabled={fields.length >= MAX_ITEMS}
          >
            Adicionar item
          </Button>
        </Box>
      </Stack>
    </SectionCard>
  )
}
