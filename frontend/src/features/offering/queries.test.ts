import { describe, expect, it } from 'vitest'
import { toRequest, type ConsultationOffering } from './queries'

describe('toRequest', () => {
  const offering: ConsultationOffering = {
    id: '00000000-0000-0000-0000-000000001001',
    name: 'Career Coaching',
    description: 'One-to-one sessions.',
    status: 'PUBLISHED',
    sessionDurationMinutes: 60,
    cancellationDeadlineHours: 24,
    packages: [
      {
        id: '00000000-0000-0000-0000-000000001011',
        sessionCount: 3,
        price: { amount: '450.00', currency: 'CHF' },
      },
    ],
    eligibleExpertIds: ['00000000-0000-0000-0000-000000000202'],
  }

  it('keeps each package with its id and its price as the amount, and the eligible experts', () => {
    expect(toRequest(offering)).toEqual({
      name: 'Career Coaching',
      description: 'One-to-one sessions.',
      sessionDurationMinutes: 60,
      cancellationDeadlineHours: 24,
      packages: [
        {
          id: '00000000-0000-0000-0000-000000001011',
          sessionCount: 3,
          price: '450.00',
        },
      ],
      eligibleExpertIds: ['00000000-0000-0000-0000-000000000202'],
    })
  })

  it('leaves out what the form does not edit', () => {
    const request = toRequest(offering)
    expect(request).not.toHaveProperty('id')
    expect(request).not.toHaveProperty('status')
  })
})
