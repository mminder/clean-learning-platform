import { describe, expect, it } from 'vitest'
import { formatMoney } from './money'

describe('formatMoney', () => {
  it('puts the currency first and keeps two decimals', () => {
    expect(formatMoney({ amount: '450.00', currency: 'CHF' })).toBe(
      'CHF 450.00',
    )
  })

  it('separates thousands with a comma', () => {
    expect(formatMoney({ amount: '1800.00', currency: 'CHF' })).toBe(
      'CHF 1,800.00',
    )
    expect(formatMoney({ amount: '1234567.50', currency: 'CHF' })).toBe(
      'CHF 1,234,567.50',
    )
  })

  it('formats zero', () => {
    expect(formatMoney({ amount: '0.00', currency: 'CHF' })).toBe('CHF 0.00')
  })

  it('formats the largest amount exactly', () => {
    expect(formatMoney({ amount: '99999999.99', currency: 'CHF' })).toBe(
      'CHF 99,999,999.99',
    )
  })
})
