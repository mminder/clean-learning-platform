import { formatDateTime } from '@/lib/date'
import { formatMoney } from '@/lib/money'
import { formatDateAndTimes } from './appointment-times'
import type {
  CancellationOutcome,
  CustomerConsultationCase,
  RefundEligibility,
} from './queries'

type Money = CustomerConsultationCase['purchaseTerms']['pricePaid']

/**
 * The case's refund texts (UC-08, spec 05 decision 5), from the server's `refundIfCancelledNow` or `cancellation` and
 * the price paid. The PoC says "would be refunded": no money moves. The dialog writes "The purchase price would
 * **not** be refunded because <reason> Cancel anyway?" around `nonRefundableReason`, with a bold "not".
 */

/** The refund-eligible outcome, in the dialog and on the cancelled case: `… of CHF 450.00 would be refunded.` */
export function refundedText(pricePaid: Money): string {
  return `The purchase price of ${formatMoney(pricePaid)} would be refunded.`
}

/** The cancelled case's line when it wasn't refund-eligible. */
export const NOT_REFUNDED = 'The purchase price would not be refunded.'

/**
 * Why cancelling the case wouldn't refund it: the first reason that applies, in UC-08's order, as the server decided
 * it. With `DEADLINE_PASSED`, it names the soonest such appointment and its free-cancellation time, in Zurich time.
 */
export function nonRefundableReason(refund: RefundEligibility): string {
  switch (refund.reason) {
    case 'SESSION_TOOK_PLACE':
      return 'a session has already taken place.'
    case 'CREDIT_FORFEITED':
      return 'the credit of a session cancelled late was forfeited.'
    case 'DEADLINE_PASSED': {
      const appointment = refund.appointment
      // The server sends the appointment with this reason, always.
      if (!appointment) {
        return 'the free cancellation deadline of your appointment passed.'
      }
      return `the free cancellation deadline of your appointment on ${formatDateAndTimes(appointment.start, appointment.end)} passed on ${formatDateTime(appointment.freeCancellationUntil)}.`
    }
    default:
      return ''
  }
}

/** The cancelled case's refund line, in place of the credits (spec 05, decision 6). */
export function cancelledCaseRefundText(
  cancellation: CancellationOutcome,
  pricePaid: Money,
): string {
  return cancellation.refundEligible ? refundedText(pricePaid) : NOT_REFUNDED
}
