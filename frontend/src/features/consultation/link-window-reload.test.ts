import { describe, expect, it } from 'vitest'
import { msUntilLinkWindowReload } from './link-window-reload'

// Tue 17 Nov 2026, 09:00 in Zurich: the window of the appointment 10:00–11:00 opens.
const NINE = '2026-11-17T08:00:00Z'
const NOW = new Date('2026-11-17T07:55:00Z')

function waiting(linkAvailableFrom: string) {
  return { linkAvailableFrom, linkWindowOpen: false }
}

function open(linkAvailableFrom: string) {
  return { linkAvailableFrom, linkWindowOpen: true }
}

describe('msUntilLinkWindowReload', () => {
  it('waits until the window opens', () => {
    expect(msUntilLinkWindowReload([waiting(NINE)], NOW)).toBe(5 * 60 * 1000)
  })

  it('waits for the soonest window that is not open yet', () => {
    expect(
      msUntilLinkWindowReload(
        [
          open('2026-11-17T07:00:00Z'),
          waiting('2026-11-18T08:00:00Z'),
          waiting(NINE),
        ],
        NOW,
      ),
    ).toBe(5 * 60 * 1000)
  })

  it('answers nothing without an appointment that waits for its window', () => {
    expect(msUntilLinkWindowReload([], NOW)).toBeUndefined()
    expect(
      msUntilLinkWindowReload([open('2026-11-17T07:00:00Z')], NOW),
    ).toBeUndefined()
  })

  it('waits a day at most', () => {
    const inADay = new Date(new Date(NINE).getTime() - 24 * 60 * 60 * 1000)
    expect(msUntilLinkWindowReload([waiting(NINE)], inADay)).toBe(
      24 * 60 * 60 * 1000,
    )
    expect(
      msUntilLinkWindowReload([waiting(NINE)], new Date(inADay.getTime() - 1)),
    ).toBeUndefined()
  })

  it('tries again in 30 seconds when the time has passed but the server still says closed', () => {
    expect(msUntilLinkWindowReload([waiting(NINE)], new Date(NINE))).toBe(
      30_000,
    )
    expect(
      msUntilLinkWindowReload(
        [waiting(NINE)],
        new Date('2026-11-17T08:00:10Z'),
      ),
    ).toBe(30_000)
  })
})
