// Input masks for Brazilian phone numbers and CEPs. They reformat on every keystroke, so they accept
// partial input, and they also format stored values for display.

const digitsOf = (value: string) => value.replace(/\D/g, '')

/** "11987521144", "+5511987521144" or partial input → "(11) 98752-1144" / "(11) 3456-7890". */
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

/** Masked phone → E.164 the API stores ("+5511987521144"). Empty stays empty: the API accepts "". */
export function phoneToE164(masked: string): string {
  const digits = digitsOf(masked)
  return digits ? `+55${digits}` : ''
}

/** "04650140" or partial input → "04650-140" (the API accepts it with or without the dash). */
export function maskCep(value: string): string {
  const digits = digitsOf(value).slice(0, 8)
  return digits.length > 5 ? `${digits.slice(0, 5)}-${digits.slice(5)}` : digits
}
