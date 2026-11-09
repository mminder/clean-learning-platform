import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  formatDate,
  formatDateTime,
  formatLocalDate,
  formatTime,
  fromCalendarDay,
  localDateOf,
  toCalendarDay,
  todayInZurich,
} from './date'

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

describe('formatTime', () => {
  it('gives the time in Zurich in winter time (UTC+1)', () => {
    expect(formatTime('2026-11-17T09:00:00Z')).toBe('10:00')
  })

  it('gives the time in Zurich in summer time (UTC+2)', () => {
    expect(formatTime('2026-07-01T08:30:00Z')).toBe('10:30')
  })

  it("gives Zurich's time around midnight UTC", () => {
    expect(formatTime('2026-11-17T22:59:00Z')).toBe('23:59')
    expect(formatTime('2026-11-17T23:00:00Z')).toBe('00:00')
    expect(formatTime('2026-11-17T23:30:00Z')).toBe('00:30')
    expect(formatTime('2026-06-30T21:30:00Z')).toBe('23:30')
    expect(formatTime('2026-06-30T22:00:00Z')).toBe('00:00')
  })

  it('follows the days of the summer-time changes', () => {
    // 28 Mar 2027: 02:00 jumps to 03:00, so 01:30–03:30 lasts 60 minutes.
    expect(formatTime('2027-03-28T00:30:00Z')).toBe('01:30')
    expect(formatTime('2027-03-28T01:30:00Z')).toBe('03:30')
    // 31 Oct 2027: 03:00 goes back to 02:00, so 02:30 happens twice.
    expect(formatTime('2027-10-31T00:30:00Z')).toBe('02:30')
    expect(formatTime('2027-10-31T01:30:00Z')).toBe('02:30')
    expect(formatTime('2027-10-31T02:00:00Z')).toBe('03:00')
  })

  it('shows the hour with two digits and 24 hours, and no seconds', () => {
    expect(formatTime('2026-11-17T08:05:59Z')).toBe('09:05')
    expect(formatTime('2026-11-17T21:45:00Z')).toBe('22:45')
  })
})

describe('localDateOf', () => {
  it("gives Zurich's date in winter time (UTC+1)", () => {
    expect(localDateOf('2026-11-17T09:00:00Z')).toBe('2026-11-17')
  })

  it("gives Zurich's date in summer time (UTC+2)", () => {
    expect(localDateOf('2026-07-01T08:30:00Z')).toBe('2026-07-01')
  })

  it("gives Zurich's date around midnight UTC", () => {
    expect(localDateOf('2026-11-17T22:59:59Z')).toBe('2026-11-17')
    expect(localDateOf('2026-11-17T23:00:00Z')).toBe('2026-11-18')
    expect(localDateOf('2026-11-17T23:30:00Z')).toBe('2026-11-18')
    expect(localDateOf('2026-06-30T21:59:59Z')).toBe('2026-06-30')
    expect(localDateOf('2026-06-30T22:00:00Z')).toBe('2026-07-01')
  })

  it('gives the days of the summer-time changes', () => {
    expect(localDateOf('2027-03-27T22:59:59Z')).toBe('2027-03-27')
    expect(localDateOf('2027-03-27T23:00:00Z')).toBe('2027-03-28')
    expect(localDateOf('2027-03-28T21:59:59Z')).toBe('2027-03-28')
    expect(localDateOf('2027-03-28T22:00:00Z')).toBe('2027-03-29')
    expect(localDateOf('2027-10-30T21:59:59Z')).toBe('2027-10-30')
    expect(localDateOf('2027-10-30T22:00:00Z')).toBe('2027-10-31')
    expect(localDateOf('2027-10-31T22:59:59Z')).toBe('2027-10-31')
    expect(localDateOf('2027-10-31T23:00:00Z')).toBe('2027-11-01')
  })

  it('reads an instant with another offset', () => {
    expect(localDateOf('2026-11-18T00:30:00+01:00')).toBe('2026-11-18')
  })
})

describe('formatLocalDate', () => {
  it('gives a date of the API as text, in winter and in summer', () => {
    expect(formatLocalDate('2026-11-17')).toBe('17 Nov 2026')
    expect(formatLocalDate('2026-07-01')).toBe('1 Jul 2026')
  })

  it('gives the days of the summer-time changes', () => {
    expect(formatLocalDate('2026-03-29')).toBe('29 Mar 2026')
    expect(formatLocalDate('2026-10-25')).toBe('25 Oct 2026')
  })

  it('gives the first and the last day of a year', () => {
    expect(formatLocalDate('2026-01-01')).toBe('1 Jan 2026')
    expect(formatLocalDate('2026-12-31')).toBe('31 Dec 2026')
  })
})

describe('todayInZurich', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  function todayAt(instant: string) {
    vi.setSystemTime(new Date(instant))
    return todayInZurich()
  }

  it("gives Zurich's date in winter time (UTC+1)", () => {
    expect(todayAt('2026-11-09T14:00:00Z')).toBe('2026-11-09')
  })

  it("gives Zurich's date in summer time (UTC+2)", () => {
    expect(todayAt('2026-07-01T10:30:00Z')).toBe('2026-07-01')
  })

  it("gives Zurich's date around midnight UTC", () => {
    expect(todayAt('2026-11-09T22:59:59Z')).toBe('2026-11-09')
    expect(todayAt('2026-11-09T23:00:00Z')).toBe('2026-11-10')
    expect(todayAt('2026-11-10T00:30:00Z')).toBe('2026-11-10')
    expect(todayAt('2026-06-30T21:59:59Z')).toBe('2026-06-30')
    expect(todayAt('2026-06-30T22:00:00Z')).toBe('2026-07-01')
  })
})

describe('toCalendarDay', () => {
  // TZDate's toISOString() keeps Zurich's offset, so the instants are compared.
  it('gives the start of the day in Zurich in winter time (UTC+1)', () => {
    expect(toCalendarDay('2026-11-17').getTime()).toBe(
      Date.parse('2026-11-16T23:00:00.000Z'),
    )
  })

  it('gives the start of the day in Zurich in summer time (UTC+2)', () => {
    expect(toCalendarDay('2026-07-01').getTime()).toBe(
      Date.parse('2026-06-30T22:00:00.000Z'),
    )
  })

  it('gives the start of the days of the summer-time changes', () => {
    expect(toCalendarDay('2026-03-29').getTime()).toBe(
      Date.parse('2026-03-28T23:00:00.000Z'),
    )
    expect(toCalendarDay('2026-10-25').getTime()).toBe(
      Date.parse('2026-10-24T22:00:00.000Z'),
    )
  })
})

describe('fromCalendarDay', () => {
  it("gives the calendar's day as a date of the API, in winter and in summer", () => {
    expect(fromCalendarDay(new Date('2026-11-16T23:00:00Z'))).toBe('2026-11-17')
    expect(fromCalendarDay(new Date('2026-06-30T22:00:00Z'))).toBe('2026-07-01')
  })

  it("gives Zurich's day around midnight UTC", () => {
    expect(fromCalendarDay(new Date('2026-11-16T22:59:59Z'))).toBe('2026-11-16')
    expect(fromCalendarDay(new Date('2026-11-17T00:30:00Z'))).toBe('2026-11-17')
    expect(fromCalendarDay(new Date('2026-06-30T21:59:59Z'))).toBe('2026-06-30')
  })

  it('gives the same day for any time of it, such as noon', () => {
    expect(fromCalendarDay(new Date('2026-11-17T11:00:00Z'))).toBe('2026-11-17')
    expect(fromCalendarDay(new Date('2026-11-17T22:59:59Z'))).toBe('2026-11-17')
  })

  it('is the reverse of toCalendarDay', () => {
    for (const date of [
      '2026-01-01',
      '2026-03-29',
      '2026-07-01',
      '2026-10-25',
      '2026-11-17',
      '2026-12-31',
    ]) {
      expect(fromCalendarDay(toCalendarDay(date))).toBe(date)
    }
  })
})
