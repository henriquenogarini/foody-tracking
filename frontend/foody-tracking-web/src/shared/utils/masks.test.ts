import { maskCep, maskPhone, phoneToE164 } from './masks'

describe('maskPhone', () => {
  it.each([
    ['', ''],
    ['1', '(1'],
    ['11', '(11'],
    ['119', '(11) 9'],
    ['119875', '(11) 9875'],
    ['1198752', '(11) 9875-2'],
    ['1134567890', '(11) 3456-7890'],
    ['11987521144', '(11) 98752-1144'],
    ['119875211449999', '(11) 98752-1144'],
    ['(11) 98752-1144', '(11) 98752-1144'],
  ])('masks "%s" as "%s" while typing', (input, expected) => {
    expect(maskPhone(input)).toBe(expected)
  })

  it('formats the E.164 value stored by the API', () => {
    expect(maskPhone('+5511987521144')).toBe('(11) 98752-1144')
  })
})

describe('phoneToE164', () => {
  it.each([
    ['(11) 98752-1144', '+5511987521144'],
    ['(11) 3456-7890', '+551134567890'],
    ['', ''],
    ['   ', ''],
  ])('converts "%s" to "%s"', (input, expected) => {
    expect(phoneToE164(input)).toBe(expected)
  })

  it('round-trips through the display mask', () => {
    expect(phoneToE164(maskPhone('+5511987521144'))).toBe('+5511987521144')
  })
})

describe('maskCep', () => {
  it.each([
    ['', ''],
    ['04650', '04650'],
    ['046501', '04650-1'],
    ['04650140', '04650-140'],
    ['04650-140', '04650-140'],
    ['046501409', '04650-140'],
  ])('masks "%s" as "%s"', (input, expected) => {
    expect(maskCep(input)).toBe(expected)
  })
})
