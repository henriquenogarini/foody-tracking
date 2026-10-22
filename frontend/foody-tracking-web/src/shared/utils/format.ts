// pt-BR display formats from the design. The API sends ISO-8601 with a -03:00 offset; dates render in
// the browser's time zone. Formatters are built once: Intl constructors are comparatively expensive.

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
const date = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short' })
const time = new Intl.DateTimeFormat('pt-BR', { timeStyle: 'short' })
const relative = new Intl.RelativeTimeFormat('pt-BR', { numeric: 'auto', style: 'short' })

const UNITS: [Intl.RelativeTimeFormatUnit, number][] = [
  ['day', 86_400],
  ['hour', 3_600],
  ['minute', 60],
]

/** 59.9 → "R$ 59,90" */
export function formatCurrency(value: number): string {
  return currency.format(value)
}

/** → "21/10/2026 14:13" (Intl puts a comma between date and time; the design does not). */
export function formatDateTime(iso: string): string {
  const value = new Date(iso)
  return `${date.format(value)} ${time.format(value)}`
}

/** → "14:13" */
export function formatTime(iso: string): string {
  return time.format(new Date(iso))
}

/** → "agora", "há 3 min", "há 2 h", "ontem", "há 5 dias" */
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

/** "Henrique Nogarini" → "HN", "Admin" → "A" (avatar initials: first and last word). */
export function formatInitials(name: string): string {
  const words = name.trim().split(/\s+/).filter(Boolean)
  const picked = words.length > 1 ? [words[0], words[words.length - 1]] : words
  // Array.from splits by code point, so a name starting with an emoji keeps it whole.
  return picked.map((word) => Array.from(word)[0].toUpperCase()).join('')
}
