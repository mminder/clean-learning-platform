import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ConsultationCasePage } from './ConsultationCasePage'
import type { CustomerConsultationCase } from './queries'

// openapi-fetch can't build a relative URL outside the browser, so the page gets a client that answers from here:
// each answer once, the last one from then on.
const api = vi.hoisted(() => ({ answers: [] as unknown[] }))
vi.mock('@/api/client', async (importActual) => ({
  ...(await importActual<typeof import('@/api/client')>()),
  client: {
    GET: vi.fn(async () => ({
      data: api.answers.length > 1 ? api.answers.shift() : api.answers[0],
      response: { status: 200 },
    })),
  },
}))

type Appointment = CustomerConsultationCase['upcomingAppointments'][number]

// 17 Nov 2026, 10:00-11:00 in Zurich, whose window opens at 09:00.
function appointment(overrides: Partial<Appointment>): Appointment {
  return {
    id: '00000000-0000-0000-0000-000000000001',
    start: '2026-11-17T09:00:00Z',
    end: '2026-11-17T10:00:00Z',
    status: 'BOOKED',
    canCancel: false,
    freeCancellationUntil: '2026-11-16T09:00:00Z',
    creditRefundedIfCancelledNow: false,
    linkAvailableFrom: '2026-11-17T08:00:00Z',
    linkWindowOpen: false,
    teamsLink: null,
    ...overrides,
  }
}

function consultationCase(
  upcomingAppointments: Appointment[],
): CustomerConsultationCase {
  return {
    id: '00000000-0000-0000-0000-0000000000c1',
    offeringName: 'Career coaching',
    purchaseTerms: {
      pricePaid: { amount: '300.00', currency: 'CHF' },
      sessionCount: 3,
      sessionDurationMinutes: 60,
      cancellationDeadlineHours: 24,
    },
    purchasedAt: '2026-11-01T10:00:00Z',
    openCredits: 2,
    totalCredits: 3,
    expert: {
      id: '00000000-0000-0000-0000-0000000000e2',
      displayName: 'Expert 2',
      bio: '',
    },
    upcomingAppointments,
    pastAppointments: [],
    notes: [],
    canBook: true,
    canCancel: false,
  }
}

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/my/cases/c1']}>
        <Routes>
          <Route path="my/cases/:id" element={<ConsultationCasePage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ConsultationCasePage, the call link', () => {
  beforeEach(() => {
    api.answers = []
  })

  it('says when the link will be available before the window opens', async () => {
    api.answers = [consultationCase([appointment({})])]
    renderPage()

    expect(
      await screen.findByText(
        'The link will be available from 09:00 (Zurich time).',
      ),
    ).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Join' })).not.toBeInTheDocument()
  })

  it('says that there is no link yet inside the window without one', async () => {
    api.answers = [
      consultationCase([
        appointment({ linkWindowOpen: true, teamsLink: null }),
      ]),
    ]
    renderPage()

    expect(
      await screen.findByText('The meeting link has not been provided yet.'),
    ).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Join' })).not.toBeInTheDocument()
  })

  it('shows Join with the link, in a new tab, inside the window', async () => {
    api.answers = [
      consultationCase([
        appointment({
          linkWindowOpen: true,
          teamsLink: 'https://example.com/meet/expert-2',
        }),
      ]),
    ]
    renderPage()

    const join = await screen.findByRole('link', { name: 'Join' })
    expect(join).toHaveAttribute('href', 'https://example.com/meet/expert-2')
    expect(join).toHaveAttribute('target', '_blank')
    expect(join).toHaveAttribute('rel', 'noreferrer')
  })

  it('shows Join when the window opens, without a navigation', async () => {
    const opensAt = new Date(Date.now() + 300).toISOString()
    api.answers = [
      consultationCase([appointment({ linkAvailableFrom: opensAt })]),
      consultationCase([
        appointment({
          linkAvailableFrom: opensAt,
          linkWindowOpen: true,
          teamsLink: 'https://example.com/meet/expert-2',
        }),
      ]),
    ]
    renderPage()

    expect(
      await screen.findByText(/The link will be available from/),
    ).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: 'Join' })).toHaveAttribute(
      'href',
      'https://example.com/meet/expert-2',
    )
    expect(
      screen.queryByText(/The link will be available from/),
    ).not.toBeInTheDocument()
  })
})
