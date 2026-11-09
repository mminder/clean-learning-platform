import { describe, expect, it } from 'vitest'
import { ApiError } from '@/lib/api-error'
import { isOfferingGone, purchaseFailure } from './purchase-failure'

function conflict(code: string) {
  return new ApiError('Buying the package failed: 409', 409, {
    title: 'Conflict',
    status: 409,
    instance: '/api/customer/consultation-cases',
    detail: 'Something changed.',
    code,
  })
}

describe('purchaseFailure', () => {
  it('goes back to Choose after PACKAGE_CHANGED', () => {
    expect(purchaseFailure(conflict('PACKAGE_CHANGED'))).toBe('packageChanged')
  })

  it('leaves only Close after OFFERING_NOT_AVAILABLE or a 404', () => {
    expect(purchaseFailure(conflict('OFFERING_NOT_AVAILABLE'))).toBe(
      'offeringGone',
    )
    expect(purchaseFailure(new ApiError('Buying failed', 404))).toBe(
      'offeringGone',
    )
  })

  it('stays on Confirm after any other failure', () => {
    expect(purchaseFailure(conflict('SOMETHING_ELSE'))).toBe('failed')
    expect(purchaseFailure(new ApiError('Buying failed', 500))).toBe('failed')
    expect(purchaseFailure(new TypeError('Failed to fetch'))).toBe('failed')
  })
})

describe('isOfferingGone', () => {
  it('reads a 404 and a 400 as an offering that left the shop', () => {
    expect(isOfferingGone(new ApiError('Loading failed', 404))).toBe(true)
    expect(isOfferingGone(new ApiError('Loading failed', 400))).toBe(true)
    expect(isOfferingGone(new ApiError('Loading failed', 500))).toBe(false)
    expect(isOfferingGone(new TypeError('Failed to fetch'))).toBe(false)
  })
})
