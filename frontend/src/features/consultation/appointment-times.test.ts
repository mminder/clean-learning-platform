import { describe, expect, it } from 'vitest'
import { formatDateAndTimes, formatTimes } from './appointment-times'

describe('formatTimes', () => {
  it('gives the start and the end in Zurich', () => {
    expect(formatTimes('2026-11-17T09:00:00Z', '2026-11-17T10:00:00Z')).toBe(
      '10:00–11:00',
    )
  })

  it('gives a session across the change to summer time with its real end', () => {
    expect(formatTimes('2027-03-28T00:30:00Z', '2027-03-28T01:30:00Z')).toBe(
      '01:30–03:30',
    )
  })
})

describe('formatDateAndTimes', () => {
  it("gives the start's Zurich date and the times", () => {
    expect(
      formatDateAndTimes('2026-11-17T09:00:00Z', '2026-11-17T10:00:00Z'),
    ).toBe('17 Nov 2026, 10:00–11:00')
    expect(
      formatDateAndTimes('2026-11-17T23:00:00Z', '2026-11-18T00:00:00Z'),
    ).toBe('18 Nov 2026, 00:00–01:00')
  })
})
