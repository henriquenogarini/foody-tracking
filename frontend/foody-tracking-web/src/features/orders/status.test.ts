import { canCancel, isTerminal, NEXT_TRANSITIONS, primaryTransition } from './status'
import { ORDER_STATUSES } from './types'

describe('order status machine (mirrors the API)', () => {
  it('allows exactly the API transitions', () => {
    expect(NEXT_TRANSITIONS).toEqual({
      RECEBIDO: ['EM_PREPARO', 'CANCELADO'],
      EM_PREPARO: ['SAIU_PARA_ENTREGA', 'CANCELADO'],
      SAIU_PARA_ENTREGA: ['ENTREGUE'],
      ENTREGUE: [],
      CANCELADO: [],
    })
  })

  it.each([
    ['RECEBIDO', 'EM_PREPARO', true],
    ['EM_PREPARO', 'SAIU_PARA_ENTREGA', true],
    ['SAIU_PARA_ENTREGA', 'ENTREGUE', false],
    ['ENTREGUE', null, false],
    ['CANCELADO', null, false],
  ] as const)('%s → main action %s, cancellable: %s', (status, next, cancellable) => {
    expect(primaryTransition(status)).toBe(next)
    expect(canCancel(status)).toBe(cancellable)
  })

  it('treats only delivered and cancelled orders as finished', () => {
    expect(ORDER_STATUSES.filter(isTerminal)).toEqual(['ENTREGUE', 'CANCELADO'])
  })
})
