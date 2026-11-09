import { TZDate } from '@date-fns/tz'
import { format } from 'date-fns'

/** Every point in time shows in the business time zone, whatever the browser's (use cases, cross-cutting rules). */
const ZURICH = 'Europe/Zurich'

/** An instant from the API (ISO-8601 with offset) as Zurich's date, for example `9 Nov 2026`. */
export function formatDate(instant: string): string {
  return format(new TZDate(instant, ZURICH), 'd MMM yyyy')
}

/**
 * An instant from the API (ISO-8601 with offset) as Zurich's date and time, for example `9 Nov 2026, 15:03`. A page
 * that shows a time adds "(Zurich time)" once, next to it.
 */
export function formatDateTime(instant: string): string {
  return format(new TZDate(instant, ZURICH), 'd MMM yyyy, HH:mm')
}

/** A local date from the API (ISO-8601, `yyyy-MM-dd`), a Zurich calendar date, as text: `2026-11-17` → `17 Nov 2026`. */
export function formatLocalDate(localDate: string): string {
  return format(toCalendarDay(localDate), 'd MMM yyyy')
}

/** Today's date in Zurich, as the API writes a local date: `2026-11-09`. */
export function todayInZurich(): string {
  return format(TZDate.tz(ZURICH), 'yyyy-MM-dd')
}

/**
 * A local date from the API as the calendar's day, whose `timeZone` is Zurich: the start of that day in Zurich.
 */
export function toCalendarDay(localDate: string): Date {
  const [year, month, day] = localDate.split('-').map(Number)
  return new TZDate(year, month - 1, day, ZURICH)
}

/** The calendar's day as a local date for the API: the Zurich date of any time of that day. */
export function fromCalendarDay(date: Date): string {
  return format(new TZDate(date.getTime(), ZURICH), 'yyyy-MM-dd')
}
