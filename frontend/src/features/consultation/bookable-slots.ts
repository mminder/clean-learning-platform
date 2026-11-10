import { localDateOf } from '@/lib/date'
import type { BookableSlot } from './queries'

/**
 * The slots by their Zurich date (`localDateOf` of the start), in the slots' order: the booking dialog's calendar
 * enables these dates, and its time step lists one date's slots. The API answers the slots soonest first, so the dates
 * come soonest first, too.
 */
export function slotsByDate(
  slots: BookableSlot[],
): Map<string, BookableSlot[]> {
  const byDate = new Map<string, BookableSlot[]>()
  for (const slot of slots) {
    const date = localDateOf(slot.start)
    byDate.set(date, [...(byDate.get(date) ?? []), slot])
  }
  return byDate
}
