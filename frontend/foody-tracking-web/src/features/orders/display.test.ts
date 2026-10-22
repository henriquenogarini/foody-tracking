import { itemsLabel, placeLabel, updatedLabel } from './display'

describe('itemsLabel', () => {
  it.each([
    [1, '1 item'],
    [2, '2 itens'],
    [50, '50 itens'],
  ])('%i → %s', (count, label) => {
    expect(itemsLabel(count)).toBe(label)
  })
})

describe('placeLabel', () => {
  it('joins neighborhood and city like the design', () => {
    expect(placeLabel({ neighborhood: 'Moema', city: 'São Paulo' })).toBe('Moema · São Paulo')
  })
})

describe('updatedLabel (tests run in America/Sao_Paulo)', () => {
  const now = new Date('2026-10-21T14:00:00-03:00').getTime()

  it('shows elapsed time while the order is moving', () => {
    expect(
      updatedLabel({ status: 'EM_PREPARO', updatedAt: '2026-10-21T13:51:00-03:00' }, now),
    ).toBe('há 9 min')
  })

  it('shows the time a finished order finished today', () => {
    expect(updatedLabel({ status: 'ENTREGUE', updatedAt: '2026-10-21T13:52:00-03:00' }, now)).toBe(
      '13:52',
    )
  })

  it('shows date and time when it finished on another day', () => {
    expect(updatedLabel({ status: 'CANCELADO', updatedAt: '2026-10-20T18:05:00-03:00' }, now)).toBe(
      '20/10/2026 18:05',
    )
  })
})
