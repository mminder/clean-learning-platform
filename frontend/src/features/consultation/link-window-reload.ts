const DAY_MS = 24 * 60 * 60 * 1000

/** How long to wait before asking again when the server still says closed for a time that has passed. */
const RETRY_MS = 30_000

/**
 * How long the customer's case detail waits before it reloads the case, so that **Join** appears when the next link
 * window opens (spec 06, decision 11): until the soonest `linkAvailableFrom` of an upcoming appointment whose window
 * isn't open. Nothing without such an appointment, or for a time more than a day away (a browser's timer can't wait
 * longer than 24.8 days, and the page reloads before then anyway). 30 seconds once that time has passed on the
 * browser's clock while the server still says closed, so that a browser clock running ahead doesn't reload in a loop.
 */
export function msUntilLinkWindowReload(
  upcomingAppointments: {
    linkAvailableFrom: string
    linkWindowOpen: boolean
  }[],
  now: Date,
): number | undefined {
  const opensAt = upcomingAppointments
    .filter((appointment) => !appointment.linkWindowOpen)
    .map((appointment) => new Date(appointment.linkAvailableFrom).getTime())
  if (opensAt.length === 0) {
    return undefined
  }
  const wait = Math.min(...opensAt) - now.getTime()
  if (wait > DAY_MS) {
    return undefined
  }
  return wait > 0 ? wait : RETRY_MS
}
