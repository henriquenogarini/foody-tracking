const digitsOf = (value: string) => value.replace(/\D/g, '')

export function maskPhone(value: string): string {
  const all = digitsOf(value)
  const digits = (value.trim().startsWith('+55') ? all.slice(2) : all).slice(0, 11)
  if (digits.length === 0) return ''
  if (digits.length <= 2) return `(${digits}`
  const area = `(${digits.slice(0, 2)}) `
  if (digits.length <= 6) return area + digits.slice(2)
  // Landlines have 8 digits after the area code, mobiles 9.
  const split = digits.length === 11 ? 7 : 6
  return `${area}${digits.slice(2, split)}-${digits.slice(split)}`
}

/** To E.164 ("+5511987521144"). Empty stays empty: the API accepts "". */
export function phoneToE164(masked: string): string {
  const digits = digitsOf(masked)
  return digits ? `+55${digits}` : ''
}

export function maskCep(value: string): string {
  const digits = digitsOf(value).slice(0, 8)
  return digits.length > 5 ? `${digits.slice(0, 5)}-${digits.slice(5)}` : digits
}
