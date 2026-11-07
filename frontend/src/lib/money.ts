import type { components } from '@/api/schema'

type Money = components['schemas']['MoneyResponse']

const amountFormat = new Intl.NumberFormat('en-US', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

/** Formats money from the API, for example `CHF 1,800.00`. */
export function formatMoney({ amount, currency }: Money): string {
  // Intl formats the decimal string exactly, without turning it into a float first.
  return `${currency} ${amountFormat.format(amount as `${number}`)}`
}
