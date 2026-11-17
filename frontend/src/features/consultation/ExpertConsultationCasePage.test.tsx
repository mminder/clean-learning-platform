import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { components } from '@/api/schema'
import { ExpertConsultationCasePage } from './ExpertConsultationCasePage'

type ExpertConsultationCase =
  components['schemas']['ExpertConsultationCaseResponse']

// openapi-fetch can't build a relative URL outside the browser, so the page gets a client that answers from here.
const api = vi.hoisted(() => ({ answer: undefined as unknown }))
vi.mock('@/api/client', () => ({
  client: {
    GET: vi.fn(async () => ({ data: api.answer, response: { status: 200 } })),
  },
}))

function expertCase(teamsLink: string | null): ExpertConsultationCase {
  return {
    id: '00000000-0000-0000-0000-0000000000c1',
    customerName: 'Customer 1',
    customerEmail: 'customer1@example.com',
    offeringName: 'Career coaching',
    sessionCount: 3,
    sessionDurationMinutes: 60,
    cancellationDeadlineHours: 24,
    purchasedAt: '2026-11-01T10:00:00Z',
    openCredits: 2,
    totalCredits: 3,
    upcomingAppointments: [
      {
        id: '00000000-0000-0000-0000-000000000001',
        start: '2026-11-17T09:00:00Z',
        end: '2026-11-17T10:00:00Z',
        status: 'BOOKED',
      },
    ],
    pastAppointments: [],
    cancelledAppointments: [],
    notes: [],
    teamsLink,
  }
}

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/expert/cases/c1']}>
        <Routes>
          <Route
            path="expert/cases/:id"
            element={<ExpertConsultationCasePage />}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ExpertConsultationCasePage, the call link', () => {
  beforeEach(() => {
    api.answer = undefined
  })

  it('asks for a Teams link in the profile without one', async () => {
    api.answer = expertCase(null)
    renderPage()

    expect(
      await screen.findByText(/Add your Teams link in/),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'your profile' })).toHaveAttribute(
      'href',
      '/expert/profile',
    )
    expect(screen.queryByRole('link', { name: 'Join' })).not.toBeInTheDocument()
  })

  it('shows Join with the expert’s own link', async () => {
    api.answer = expertCase('https://example.com/meet/expert-2')
    renderPage()

    expect(await screen.findByRole('link', { name: 'Join' })).toHaveAttribute(
      'href',
      'https://example.com/meet/expert-2',
    )
    expect(screen.queryByText(/Add your Teams link in/)).not.toBeInTheDocument()
  })
})
