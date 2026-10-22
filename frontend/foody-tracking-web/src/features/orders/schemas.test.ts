import { createOrderSchema, emptyOrderForm, isOrderFormPath, type OrderFormValues } from './schemas'

const valid: OrderFormValues = {
  customer: { name: ' José da Silva ', phone: '(11) 98752-1144', email: '' },
  items: [
    { description: 'Pizza calabresa G', quantity: '1', unitPrice: '59,90' },
    { description: 'Guaraná 2L', quantity: '2', unitPrice: '14,95' },
  ],
  deliveryAddress: {
    postalCode: '04650-140',
    street: 'Rua José Neves',
    number: '590',
    complement: '',
    neighborhood: 'Vila São Paulo',
    city: 'São Paulo',
    state: 'SP',
  },
  notes: '',
}

function errorsOf(values: OrderFormValues) {
  const result = createOrderSchema.safeParse(values)
  return Object.fromEntries((result.error?.issues ?? []).map((i) => [i.path.join('.'), i.message]))
}

describe('createOrderSchema', () => {
  it('turns the form into the exact API body', () => {
    const body = JSON.parse(JSON.stringify(createOrderSchema.parse(valid)))
    expect(body).toEqual({
      customer: { name: 'José da Silva', phone: '+5511987521144' },
      items: [
        { description: 'Pizza calabresa G', quantity: 1, unitPrice: 59.9 },
        { description: 'Guaraná 2L', quantity: 2, unitPrice: 14.95 },
      ],
      deliveryAddress: {
        postalCode: '04650-140',
        street: 'Rua José Neves',
        number: '590',
        neighborhood: 'Vila São Paulo',
        city: 'São Paulo',
        state: 'SP',
      },
    })
  })

  it('reports every required field of an empty form', () => {
    expect(errorsOf(emptyOrderForm)).toEqual({
      'customer.name': 'Informe o nome do cliente.',
      'items.0.description': 'Informe a descrição.',
      'items.0.unitPrice': 'Informe um valor como 59,90.',
      'deliveryAddress.postalCode': 'Informe o CEP como 00000-000.',
      'deliveryAddress.street': 'Informe a rua.',
      'deliveryAddress.number': 'Informe o número.',
      'deliveryAddress.neighborhood': 'Informe o bairro.',
      'deliveryAddress.city': 'Informe a cidade.',
      'deliveryAddress.state': 'Selecione a UF.',
    })
  })

  it.each([
    ['0', 'Mínimo 1.'],
    ['1000', 'Máximo 999.'],
    ['1,5', 'Use um número inteiro.'],
  ])('rejects quantity "%s"', (quantity, message) => {
    const values = { ...valid, items: [{ ...valid.items[0], quantity }] }
    expect(errorsOf(values)['items.0.quantity']).toBe(message)
  })

  it.each([
    ['0,00', 'O preço precisa ser maior que zero.'],
    ['100.000,00', 'Máximo de R$ 99.999,99.'],
    ['12,345', 'Informe um valor como 59,90.'],
  ])('rejects price "%s"', (unitPrice, message) => {
    const values = { ...valid, items: [{ ...valid.items[0], unitPrice }] }
    expect(errorsOf(values)['items.0.unitPrice']).toBe(message)
  })

  it('needs at least one item', () => {
    expect(errorsOf({ ...valid, items: [] }).items).toBe('Adicione ao menos 1 item.')
  })

  it('accepts optional phone and e-mail only when complete', () => {
    expect(errorsOf({ ...valid, customer: { ...valid.customer, phone: '(11) 9875' } })).toEqual({
      'customer.phone': 'Informe DDD e número, ex.: (11) 98752-1144.',
    })
    expect(errorsOf({ ...valid, customer: { ...valid.customer, email: 'jose@' } })).toEqual({
      'customer.email': 'Informe um e-mail válido.',
    })
  })

  it('enforces the API length limits', () => {
    const errors = errorsOf({
      ...valid,
      customer: { ...valid.customer, name: 'a'.repeat(121) },
      notes: 'b'.repeat(501),
    })
    expect(errors['customer.name']).toBe('Use no máximo 120 caracteres.')
    expect(errors.notes).toBe('Use no máximo 500 caracteres.')
  })

  it.each(['04650140', '04650-140'])('accepts CEP "%s"', (postalCode) => {
    const values = { ...valid, deliveryAddress: { ...valid.deliveryAddress, postalCode } }
    expect(errorsOf(values)).toEqual({})
  })

  it.each(['0465-0140', '0465014', 'abcde-fgh'])('rejects CEP "%s"', (postalCode) => {
    const values = { ...valid, deliveryAddress: { ...valid.deliveryAddress, postalCode } }
    expect(errorsOf(values)['deliveryAddress.postalCode']).toBe('Informe o CEP como 00000-000.')
  })

  it('accepts only real UFs', () => {
    const values = { ...valid, deliveryAddress: { ...valid.deliveryAddress, state: 'XX' } }
    expect(errorsOf(values)['deliveryAddress.state']).toBe('Selecione a UF.')
  })
})

describe('isOrderFormPath', () => {
  it.each([
    ['customer.phone', true],
    ['items.12.unitPrice', true],
    ['deliveryAddress.state', true],
    ['deliveryAddress.lat', false],
    ['items', false],
  ])('%s → %s', (path, rendered) => {
    expect(isOrderFormPath(path)).toBe(rendered)
  })
})
