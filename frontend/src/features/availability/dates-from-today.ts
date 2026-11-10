/**
 * The dates from `today` on, in their order. Both are local dates of the API (`yyyy-MM-dd`), whose text sorts as the
 * dates do. A save never changes a date before today (UC-13), and the API refuses one with a 400, so the form leaves
 * out the ones it still holds, such as yesterday on a page left open past midnight.
 */
export function datesFromToday(dates: string[], today: string): string[] {
  return dates.filter((date) => date >= today)
}
