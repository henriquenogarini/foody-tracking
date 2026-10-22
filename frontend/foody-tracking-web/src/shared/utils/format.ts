// Built once: Intl constructors are expensive. Dates render in the browser's time zone.

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
const date = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short' })
const time = new Intl.DateTimeFormat('pt-BR', { timeStyle: 'short' })
const relative = new Intl.RelativeTimeFormat('pt-BR', { numeric: 'auto', style: 'short' })

const UNITS: [Intl.RelativeTimeFormatUnit, number][] = [
  ['day', 86_400],
  ['hour', 3_600],
  ['minute', 60],
]

export function formatCurrency(value: number): string {
  return currency.format(value)
}

/** "21/10/2026 14:13", without the comma Intl puts between date and time. */
export function formatDateTime(iso: string): string {
  const value = new Date(iso)
  return `${date.format(value)} ${time.format(value)}`
}

export function formatTime(iso: string): string {
  return time.format(new Date(iso))
}

export function formatRelative(iso: string, now: number = Date.now()): string {
  const seconds = (new Date(iso).getTime() - now) / 1000
  for (const [unit, size] of UNITS) {
    if (Math.abs(seconds) >= size) {
      // The short style abbreviates with a period ("há 1 min."); the design writes "há 1 min".
      return relative.format(Math.trunc(seconds / size), unit).replace(/\.$/, '')
    }
  }
  return 'agora'
}

export function formatInitials(name: string): string {
  const words = name.trim().split(/\s+/).filter(Boolean)
  const picked = words.length > 1 ? [words[0], words[words.length - 1]] : words
  // Array.from splits by code point, so a name starting with an emoji keeps it whole.
  return picked.map((word) => Array.from(word)[0].toUpperCase()).join('')
}

/** "59,90", "1.234,5", "R$ 12" or "59.90" → number; anything else → null. At most 2 decimals. */
export function parseDecimal(input: string): number | null {
  let text = input.replace(/R\$/g, '').replace(/\s/g, '')
  if (text.includes(',')) text = text.replace(/\./g, '').replace(',', '.')
  return /^\d+(\.\d{1,2})?$/.test(text) ? Number(text) : null
}
