import { ApiError, isBadRequest, isNotFound } from '@/lib/api-error'

/**
 * What a failed purchase means for the buy dialog (UC-03): `packageChanged` goes back to **Choose** with the
 * reloaded packages, `offeringGone` leaves only **Close**, and `failed` stays on **Confirm**. The first two mean the
 * shop the dialog was opened from is stale.
 */
export type PurchaseFailure = 'packageChanged' | 'offeringGone' | 'failed'

export function purchaseFailure(failure: unknown): PurchaseFailure {
  const code = failure instanceof ApiError ? failure.problem?.code : undefined
  if (code === 'PACKAGE_CHANGED') {
    return 'packageChanged'
  }
  if (code === 'OFFERING_NOT_AVAILABLE' || isNotFound(failure)) {
    return 'offeringGone'
  }
  return 'failed'
}

/** Reloading the offering failed because it left the shop: a 404, or a malformed id (400), as the detail page reads it. */
export function isOfferingGone(failure: unknown): boolean {
  return isNotFound(failure) || isBadRequest(failure)
}
