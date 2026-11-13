import { describe, expect, it } from 'vitest'
import { ApiError } from '@/lib/api-error'
import { bookingFailure } from './booking-failure'

function conflict(code: string) {
  return new ApiError('Booking the appointment failed: 409', 409, {
    title: 'Conflict',
    status: 409,
    instance: '/api/customer/consultation-cases/1/appointments',
    detail: 'Something changed.',
    code,
  })
}

describe('bookingFailure', () => {
  it('goes back to the dates after SLOT_NOT_AVAILABLE', () => {
    expect(bookingFailure(conflict('SLOT_NOT_AVAILABLE'))).toBe('slotTaken')
  })

  it('goes back to the experts after EXPERT_NOT_ELIGIBLE', () => {
    expect(bookingFailure(conflict('EXPERT_NOT_ELIGIBLE'))).toBe(
      'expertNotEligible',
    )
  })

  it("goes on with the case's expert after EXPERT_CHANGED", () => {
    expect(bookingFailure(conflict('EXPERT_CHANGED'))).toBe('expertChanged')
  })

  it('leaves only Close after NO_OPEN_CREDITS or a 404', () => {
    expect(bookingFailure(conflict('NO_OPEN_CREDITS'))).toBe('noOpenCredits')
    expect(bookingFailure(new ApiError('Booking failed', 404))).toBe(
      'caseNotFound',
    )
  })

  it('leaves only Close after CASE_CANCELLED', () => {
    expect(bookingFailure(conflict('CASE_CANCELLED'))).toBe('caseCancelled')
  })

  it('stays on the confirmation after any other failure', () => {
    expect(bookingFailure(conflict('SOMETHING_ELSE'))).toBe('failed')
    expect(bookingFailure(new ApiError('Booking failed', 400))).toBe('failed')
    expect(bookingFailure(new ApiError('Booking failed', 500))).toBe('failed')
    expect(bookingFailure(new TypeError('Failed to fetch'))).toBe('failed')
  })
})
