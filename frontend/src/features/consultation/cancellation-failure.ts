import { ApiError, isNotFound } from '@/lib/api-error'

/**
 * What a failed cancellation, of an appointment or a case, means for its dialog (UC-07, UC-08, the plan's error
 * table): `outcomeChanged` shows the reloaded outcome and asks again, `notCancellable` and `notFound` leave only
 * **Close**, and `failed` stays as it is.
 */
export type CancellationFailure =
  'outcomeChanged' | 'notCancellable' | 'notFound' | 'failed'

export function cancellationFailure(failure: unknown): CancellationFailure {
  const code = failure instanceof ApiError ? failure.problem?.code : undefined
  switch (code) {
    case 'DEADLINE_PASSED':
    case 'REFUND_ELIGIBILITY_CHANGED':
      return 'outcomeChanged'
    case 'APPOINTMENT_CANCELLED':
    case 'APPOINTMENT_STARTED':
    case 'CASE_CANCELLED':
    case 'CASE_COMPLETED':
      return 'notCancellable'
  }
  return isNotFound(failure) ? 'notFound' : 'failed'
}
