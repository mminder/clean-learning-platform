import { describe, expect, it } from 'vitest'
import { ApiError } from '@/lib/api-error'
import { cancellationFailure } from './cancellation-failure'

function conflict(code: string) {
  return new ApiError('Cancelling the appointment failed: 409', 409, {
    title: 'Conflict',
    status: 409,
    instance: '/api/customer/consultation-cases/1/appointments/2/cancel',
    detail: 'Something changed.',
    code,
  })
}

describe('cancellationFailure', () => {
  it('shows the new outcome to confirm again after DEADLINE_PASSED', () => {
    expect(cancellationFailure(conflict('DEADLINE_PASSED'))).toBe(
      'outcomeChanged',
    )
  })

  it('leaves only Close after APPOINTMENT_CANCELLED or APPOINTMENT_STARTED', () => {
    expect(cancellationFailure(conflict('APPOINTMENT_CANCELLED'))).toBe(
      'notCancellable',
    )
    expect(cancellationFailure(conflict('APPOINTMENT_STARTED'))).toBe(
      'notCancellable',
    )
  })

  it('leaves only Close after a 404', () => {
    expect(cancellationFailure(new ApiError('Cancelling failed', 404))).toBe(
      'notFound',
    )
  })

  it('stays as it is after any other failure', () => {
    expect(cancellationFailure(conflict('SOMETHING_ELSE'))).toBe('failed')
    expect(cancellationFailure(new ApiError('Cancelling failed', 400))).toBe(
      'failed',
    )
    expect(cancellationFailure(new ApiError('Cancelling failed', 500))).toBe(
      'failed',
    )
    expect(cancellationFailure(new TypeError('Failed to fetch'))).toBe('failed')
  })
})
