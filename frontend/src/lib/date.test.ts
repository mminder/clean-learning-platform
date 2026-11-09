import { describe, expect, it } from 'vitest'
import { formatDate, formatDateTime } from './date'

describe('formatDate', () => {
  it('gives the date in Zurich in winter time (UTC+1)', () => {
    expect(formatDate('2026-11-09T14:03:12.345Z')).toBe('9 Nov 2026')
  })

  it('gives the date in Zurich in summer time (UTC+2)', () => {
    expect(formatDate('2026-07-01T10:30:00Z')).toBe('1 Jul 2026')
  })

  it("gives Zurich's date around midnight UTC", () => {
    expect(formatDate('2026-11-09T22:59:59Z')).toBe('9 Nov 2026')
    expect(formatDate('2026-11-09T23:00:00Z')).toBe('10 Nov 2026')
    expect(formatDate('2026-06-30T21:59:59Z')).toBe('30 Jun 2026')
    expect(formatDate('2026-06-30T22:00:00Z')).toBe('1 Jul 2026')
  })

  it('reads an instant with another offset', () => {
    expect(formatDate('2026-11-10T00:30:00+01:00')).toBe('10 Nov 2026')
  })
})

describe('formatDateTime', () => {
  it('gives the date and time in Zurich in winter time (UTC+1)', () => {
    expect(formatDateTime('2026-11-09T14:03:12.345Z')).toBe('9 Nov 2026, 15:03')
  })

  it('gives the date and time in Zurich in summer time (UTC+2)', () => {
    expect(formatDateTime('2026-07-01T10:30:00Z')).toBe('1 Jul 2026, 12:30')
  })

  it("gives Zurich's date and time around midnight UTC", () => {
    expect(formatDateTime('2026-11-09T23:30:00Z')).toBe('10 Nov 2026, 00:30')
    expect(formatDateTime('2026-11-10T00:15:00Z')).toBe('10 Nov 2026, 01:15')
    expect(formatDateTime('2026-06-30T22:15:00Z')).toBe('1 Jul 2026, 00:15')
    expect(formatDateTime('2026-06-30T23:59:00Z')).toBe('1 Jul 2026, 01:59')
  })

  it('follows the change to summer time and back', () => {
    expect(formatDateTime('2026-03-29T00:59:00Z')).toBe('29 Mar 2026, 01:59')
    expect(formatDateTime('2026-03-29T01:00:00Z')).toBe('29 Mar 2026, 03:00')
    expect(formatDateTime('2026-10-25T00:59:00Z')).toBe('25 Oct 2026, 02:59')
    expect(formatDateTime('2026-10-25T01:00:00Z')).toBe('25 Oct 2026, 02:00')
  })

  it('shows the hour with two digits and 24 hours', () => {
    expect(formatDateTime('2026-11-09T08:05:00Z')).toBe('9 Nov 2026, 09:05')
    expect(formatDateTime('2026-11-09T21:45:00Z')).toBe('9 Nov 2026, 22:45')
  })
})
