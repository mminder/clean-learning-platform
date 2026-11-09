import { ApiError, isNotFound } from '@/lib/api-error'

/**
 * What a failed booking, or a failed load of the times on a first booking, means for the booking dialog (UC-06, the
 * plan's error table): `slotTaken` goes back to the dates, `expertNotEligible` back to the experts, `expertChanged` on
 * to the dates with the case's expert, `noOpenCredits` and `caseNotFound` leave only **Close**, and `failed` stays
 * where it is.
 */
export type BookingFailure =
  | 'slotTaken'
  | 'expertNotEligible'
  | 'expertChanged'
  | 'noOpenCredits'
  | 'caseNotFound'
  | 'failed'

export function bookingFailure(failure: unknown): BookingFailure {
  const code = failure instanceof ApiError ? failure.problem?.code : undefined
  switch (code) {
    case 'SLOT_NOT_AVAILABLE':
      return 'slotTaken'
    case 'EXPERT_NOT_ELIGIBLE':
      return 'expertNotEligible'
    case 'EXPERT_CHANGED':
      return 'expertChanged'
    case 'NO_OPEN_CREDITS':
      return 'noOpenCredits'
  }
  return isNotFound(failure) ? 'caseNotFound' : 'failed'
}
