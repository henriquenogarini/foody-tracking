import { buildTimeline } from './timeline'
import type { StatusHistoryEntry } from './types'

const entry = (
  fromStatus: StatusHistoryEntry['fromStatus'],
  toStatus: StatusHistoryEntry['toStatus'],
  minute: number,
  changedByName: string | null = 'Henrique',
): StatusHistoryEntry => ({
  fromStatus,
  toStatus,
  changedAt: `2026-10-21T13:${String(minute).padStart(2, '0')}:00-03:00`,
  changedBy: '3f6c1a2e-8d4b-4c7a-9e1f-2b5d7a9c0e11',
  changedByName,
})

const created = entry(null, 'RECEBIDO', 41)
const preparing = entry('RECEBIDO', 'EM_PREPARO', 44)
const out = entry('EM_PREPARO', 'SAIU_PARA_ENTREGA', 50)

describe('buildTimeline', () => {
  it('shows what happened, the current step and what is ahead', () => {
    const steps = buildTimeline('SAIU_PARA_ENTREGA', [created, preparing, out])
    expect(steps.map((s) => [s.label, s.state, s.description])).toEqual([
      ['Recebido', 'done', 'Pedido criado por Henrique'],
      ['Em preparo', 'done', 'Cozinha iniciou o preparo'],
      ['Saiu para entrega', 'current', 'Entregador a caminho'],
      ['Entregue', 'pending', 'Aguardando'],
    ])
    expect(steps[2].at).toBe(out.changedAt)
    expect(steps[3].at).toBeUndefined()
  })

  it('ends with every step done once delivered', () => {
    const delivered = entry('SAIU_PARA_ENTREGA', 'ENTREGUE', 58)
    const steps = buildTimeline('ENTREGUE', [created, preparing, out, delivered])
    expect(steps.map((s) => s.state)).toEqual(['done', 'done', 'done', 'done'])
  })

  it('stops at the cancellation, with who cancelled', () => {
    const cancelled = entry('RECEBIDO', 'CANCELADO', 45, 'Ana')
    const steps = buildTimeline('CANCELADO', [created, cancelled])
    expect(steps.map((s) => [s.label, s.state, s.description])).toEqual([
      ['Recebido', 'done', 'Pedido criado por Henrique'],
      ['Cancelado', 'cancelled', 'Cancelado por Ana'],
    ])
  })

  it('names a user who no longer exists', () => {
    const steps = buildTimeline('RECEBIDO', [entry(null, 'RECEBIDO', 41, null)])
    expect(steps[0]).toMatchObject({
      state: 'current',
      description: 'Pedido criado por usuário removido',
    })
    expect(steps).toHaveLength(4)
  })

  it('follows the order when the history has not caught up with a change yet', () => {
    // The order already says "Saiu para entrega" (updated from the PATCH response); history is older.
    const steps = buildTimeline('SAIU_PARA_ENTREGA', [created, preparing])
    expect(steps.map((s) => [s.label, s.state])).toEqual([
      ['Recebido', 'done'],
      ['Em preparo', 'done'],
      ['Saiu para entrega', 'current'],
      ['Entregue', 'pending'],
    ])
    expect(steps[2].at).toBeUndefined()
  })

  it('shows a fresh cancellation before the history reports it', () => {
    const steps = buildTimeline('CANCELADO', [created])
    expect(steps.map((s) => [s.label, s.state, s.description])).toEqual([
      ['Recebido', 'done', 'Pedido criado por Henrique'],
      ['Cancelado', 'cancelled', 'Cancelado por você'],
    ])
  })
})
