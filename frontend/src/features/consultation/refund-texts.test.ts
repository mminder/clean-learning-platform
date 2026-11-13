import { describe, expect, it } from 'vitest'
import {
  cancelledCaseRefundText,
  nonRefundableReason,
  refundedText,
} from './refund-texts'

const PRICE = { amount: '450.00', currency: 'CHF' }

describe('refundedText', () => {
  it('names the price paid', () => {
    expect(refundedText(PRICE)).toBe(
      'The purchase price of CHF 450.00 would be refunded.',
    )
  })
})

describe('nonRefundableReason', () => {
  it('gives each reason in UC-08 words', () => {
    expect(
      nonRefundableReason({
        refundEligible: false,
        reason: 'SESSION_TOOK_PLACE',
      }),
    ).toBe('a session has already taken place.')
    expect(
      nonRefundableReason({
        refundEligible: false,
        reason: 'CREDIT_FORFEITED',
      }),
    ).toBe('the credit of a session cancelled late was forfeited.')
  })

  it('names the appointment whose deadline passed, in Zurich time', () => {
    expect(
      nonRefundableReason({
        refundEligible: false,
        reason: 'DEADLINE_PASSED',
        appointment: {
          start: '2026-11-17T09:00:00Z',
          end: '2026-11-17T10:00:00Z',
          freeCancellationUntil: '2026-11-16T09:00:00Z',
        },
      }),
    ).toBe(
      'the free cancellation deadline of your appointment on 17 Nov 2026, 10:00–11:00 passed on 16 Nov 2026, 10:00.',
    )
  })

  it('has no reason for a refund-eligible case', () => {
    expect(nonRefundableReason({ refundEligible: true, reason: null })).toBe('')
  })
})

describe('cancelledCaseRefundText', () => {
  it('says whether the price would be refunded', () => {
    expect(
      cancelledCaseRefundText(
        { cancelledAt: '2026-11-15T11:00:00Z', refundEligible: true },
        PRICE,
      ),
    ).toBe('The purchase price of CHF 450.00 would be refunded.')
    expect(
      cancelledCaseRefundText(
        { cancelledAt: '2026-11-15T11:00:00Z', refundEligible: false },
        PRICE,
      ),
    ).toBe('The purchase price would not be refunded.')
  })
})
