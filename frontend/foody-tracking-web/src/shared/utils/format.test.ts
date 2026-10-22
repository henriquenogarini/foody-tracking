import { formatCurrency, formatDateTime, formatRelative, formatTime } from './format'

// Intl separates "R$" from the amount with a no-break space.
const NBSP = ' '

describe('formatCurrency', () => {
  it.each([
    [59.9, `R$${NBSP}59,90`],
    [0, `R$${NBSP}0,00`],
    [99999.99, `R$${NBSP}99.999,99`],
  ])('formats %d as BRL', (value, expected) => {
    expect(formatCurrency(value)).toBe(expected)
  })
})

describe('date and time (tests run in America/Sao_Paulo)', () => {
  const iso = '2026-10-21T14:13:27.123-03:00'

  it('formats date and time like the design, without a comma', () => {
    expect(formatDateTime(iso)).toBe('21/10/2026 14:13')
  })

  it('formats only the time', () => {
    expect(formatTime(iso)).toBe('14:13')
  })

  it('converts other offsets to the local time zone', () => {
    expect(formatTime('2026-10-21T17:13:00Z')).toBe('14:13')
  })
})

describe('formatRelative', () => {
  const now = new Date('2026-10-21T14:00:00-03:00').getTime()
  const ago = (seconds: number) => new Date(now - seconds * 1000).toISOString()

  it.each([
    [0, 'agora'],
    [59, 'agora'],
    [60, 'há 1 min'],
    [3 * 60 + 59, 'há 3 min'],
    [59 * 60, 'há 59 min'],
    [2 * 3600, 'há 2 h'],
    [26 * 3600, 'ontem'],
    [5 * 86400, 'há 5 dias'],
  ])('%is ago → "%s"', (seconds, expected) => {
    expect(formatRelative(ago(seconds), now)).toBe(expected)
  })

  it('handles timestamps slightly in the future (clock skew) as now', () => {
    expect(formatRelative(new Date(now + 5_000).toISOString(), now)).toBe('agora')
  })
})
