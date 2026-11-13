import { ApiError, isNotFound } from '@/lib/api-error'

/**
 * What a failed cancellation means for its dialog (UC-07, the plan's error table): `outcomeChanged` shows the reloaded
 * outcome and asks again, `notCancellable` and `notFound` leave only **Close**, and `failed` stays as it is. Task 03
 * adds the case's codes.
 */
export type CancellationFailure =
  'outcomeChanged' | 'notCancellable' | 'notFound' | 'failed'

export function cancellationFailure(failure: unknown): CancellationFailure {
  const code = failure instanceof ApiError ? failure.problem?.code : undefined
  switch (code) {
    case 'DEADLINE_PASSED':
      return 'outcomeChanged'
    case 'APPOINTMENT_CANCELLED':
    case 'APPOINTMENT_STARTED':
      return 'notCancellable'
  }
  return isNotFound(failure) ? 'notFound' : 'failed'
}
