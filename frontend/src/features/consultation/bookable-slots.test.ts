import { describe, expect, it } from 'vitest'
import { slotsByDate } from './bookable-slots'

function slot(start: string, end: string) {
  return { start, end, freeCancellationUntil: start }
}

describe('slotsByDate', () => {
  it('groups the slots by their Zurich date, keeping their order', () => {
    const nine = slot('2026-11-17T08:00:00Z', '2026-11-17T09:00:00Z')
    const ten = slot('2026-11-17T09:00:00Z', '2026-11-17T10:00:00Z')
    const nextDay = slot('2026-11-18T09:00:00Z', '2026-11-18T10:00:00Z')

    const byDate = slotsByDate([nine, ten, nextDay])

    expect([...byDate.keys()]).toEqual(['2026-11-17', '2026-11-18'])
    expect(byDate.get('2026-11-17')).toEqual([nine, ten])
    expect(byDate.get('2026-11-18')).toEqual([nextDay])
  })

  it("puts a slot after midnight in Zurich on Zurich's next day, also in summer", () => {
    const lateWinter = slot('2026-11-17T22:30:00Z', '2026-11-17T23:30:00Z')
    const afterMidnightWinter = slot(
      '2026-11-17T23:00:00Z',
      '2026-11-18T00:00:00Z',
    )
    const afterMidnightSummer = slot(
      '2026-06-30T22:00:00Z',
      '2026-06-30T23:00:00Z',
    )

    const byDate = slotsByDate([
      afterMidnightSummer,
      lateWinter,
      afterMidnightWinter,
    ])

    expect(byDate.get('2026-07-01')).toEqual([afterMidnightSummer])
    expect(byDate.get('2026-11-17')).toEqual([lateWinter])
    expect(byDate.get('2026-11-18')).toEqual([afterMidnightWinter])
  })

  it('has no date without slots', () => {
    expect(slotsByDate([]).size).toBe(0)
  })
})
