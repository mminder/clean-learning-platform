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
