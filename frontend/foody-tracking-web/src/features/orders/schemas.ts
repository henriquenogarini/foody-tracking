import { z } from 'zod'
import { BRAZIL_STATES } from '@/shared/utils/brazil'
import { parseDecimal } from '@/shared/utils/format'
import { phoneToE164 } from '@/shared/utils/masks'
import type { CreateOrderInput } from './types'

// Inputs are strings as typed (masked phone and CEP, "59,90"); the output is the request body.

const required = (label: string, max: number) =>
  z.string().trim().min(1, `Informe ${label}.`).max(max, `Use no máximo ${max} caracteres.`)

const optional = (max: number) =>
  z
    .string()
    .trim()
    .max(max, `Use no máximo ${max} caracteres.`)
    .transform((value) => value || undefined)

export const MAX_ITEMS = 50

const item = z.object({
  description: required('a descrição', 200),
  quantity: z
    .string()
    .trim()
    .regex(/^\d+$/, 'Use um número inteiro.')
    .transform(Number)
    .pipe(z.number().min(1, 'Mínimo 1.').max(999, 'Máximo 999.')),
  unitPrice: z
    .string()
    .transform((value, context) => {
      const price = parseDecimal(value)
      if (price === null) {
        context.addIssue({ code: 'custom', message: 'Informe um valor como 59,90.' })
        return z.NEVER
      }
      return price
    })
    .pipe(
      z
        .number()
        .min(0.01, 'O preço precisa ser maior que zero.')
        .max(99999.99, 'Máximo de R$ 99.999,99.'),
    ),
})

export const createOrderSchema = z
  .object({
    customer: z.object({
      name: required('o nome do cliente', 120),
      phone: z
        .string()
        .trim()
        .refine((value) => {
          const digits = value.replace(/\D/g, '').length
          return digits === 0 || digits === 10 || digits === 11
        }, 'Informe DDD e número, ex.: (11) 98752-1144.')
        .transform((value) => phoneToE164(value) || undefined),
      email: z
        .string()
        .trim()
        .max(254, 'Use no máximo 254 caracteres.')
        .refine(
          (value) => !value || z.email().safeParse(value).success,
          'Informe um e-mail válido.',
        )
        .transform((value) => value || undefined),
    }),
    items: z
      .array(item)
      .min(1, 'Adicione ao menos 1 item.')
      .max(MAX_ITEMS, `Máximo de ${MAX_ITEMS} itens.`),
    deliveryAddress: z.object({
      postalCode: z
        .string()
        .trim()
        .regex(/^\d{5}-?\d{3}$/, 'Informe o CEP como 00000-000.'),
      street: required('a rua', 200),
      number: required('o número', 20),
      complement: optional(100),
      neighborhood: required('o bairro', 100),
      city: required('a cidade', 100),
      // A string (not z.enum) on input, so the form can start with no UF selected.
      state: z
        .string()
        .refine((value) => BRAZIL_STATES.some((uf) => uf === value), 'Selecione a UF.'),
    }),
    notes: optional(500),
  })
  // Empty optionals become undefined, which JSON drops; the annotation checks the body against the contract.
  .transform((values): CreateOrderInput => values)

export type OrderFormValues = z.input<typeof createOrderSchema>

// API errors for any other path (e.g. lat/lng) become a general message.
const RENDERED_PATHS = new Set([
  'customer.name',
  'customer.phone',
  'customer.email',
  'deliveryAddress.postalCode',
  'deliveryAddress.street',
  'deliveryAddress.number',
  'deliveryAddress.complement',
  'deliveryAddress.neighborhood',
  'deliveryAddress.city',
  'deliveryAddress.state',
  'notes',
])

export function isOrderFormPath(path: string): boolean {
  return RENDERED_PATHS.has(path) || /^items\.\d+\.(description|quantity|unitPrice)$/.test(path)
}

export const emptyItem: OrderFormValues['items'][number] = {
  description: '',
  quantity: '1',
  unitPrice: '',
}

export const emptyOrderForm: OrderFormValues = {
  customer: { name: '', phone: '', email: '' },
  items: [emptyItem],
  deliveryAddress: {
    postalCode: '',
    street: '',
    number: '',
    complement: '',
    neighborhood: '',
    city: '',
    state: '',
  },
  notes: '',
}
