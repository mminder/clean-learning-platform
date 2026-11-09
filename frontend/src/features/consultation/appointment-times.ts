import { formatDate, formatTime } from '@/lib/date'

/** A slot's or an appointment's times in Zurich, start–end: `10:00–11:00`. */
export function formatTimes(start: string, end: string): string {
  return `${formatTime(start)}–${formatTime(end)}`
}

/** A slot's or an appointment's date and times in Zurich: `17 Nov 2026, 10:00–11:00`. */
export function formatDateAndTimes(start: string, end: string): string {
  return `${formatDate(start)}, ${formatTimes(start, end)}`
}
