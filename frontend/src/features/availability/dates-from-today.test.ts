import { describe, expect, it } from 'vitest'
import { datesFromToday } from './dates-from-today'

describe('datesFromToday', () => {
  it('leaves out the dates before today and keeps today and later ones, in their order', () => {
    expect(
      datesFromToday(
        ['2026-11-08', '2026-11-09', '2026-11-10', '2026-11-11', '2026-12-01'],
        '2026-11-10',
      ),
    ).toEqual(['2026-11-10', '2026-11-11', '2026-12-01'])
  })

  it('compares across months and years', () => {
    expect(datesFromToday(['2026-09-30', '2026-10-01'], '2026-10-01')).toEqual([
      '2026-10-01',
    ])
    expect(
      datesFromToday(['2026-12-31', '2027-01-01', '2027-01-02'], '2027-01-01'),
    ).toEqual(['2027-01-01', '2027-01-02'])
  })

  it('gives no dates when every date is before today', () => {
    expect(datesFromToday(['2026-11-08', '2026-11-09'], '2026-11-10')).toEqual(
      [],
    )
    expect(datesFromToday([], '2026-11-10')).toEqual([])
  })
})
