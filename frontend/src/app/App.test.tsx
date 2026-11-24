import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'
import { App } from './App'
import { Providers } from './Providers'

// openapi-fetch can't build a relative URL outside the browser, so the app gets a client that answers from here.
vi.mock('@/api/client', async (importActual) => ({
  ...(await importActual<typeof import('@/api/client')>()),
  client: {
    GET: vi.fn(async (path: string) =>
      path === '/api/me'
        ? {
            data: {
              id: '00000000-0000-0000-0000-000000000001',
              name: 'Customer 1',
              email: 'customer1@example.com',
              roles: ['CUSTOMER'],
            },
            response: { status: 200 },
          }
        : { data: undefined, response: { status: 404 } },
    ),
  },
}))

function renderAt(path: string) {
  render(
    <Providers>
      <MemoryRouter initialEntries={[path]}>
        <App />
      </MemoryRouter>
    </Providers>,
  )
}

describe('App', () => {
  it('shows the not-found page at an unknown path', () => {
    renderAt('/nope')

    expect(
      screen.getByRole('heading', { name: 'Page not found' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: 'Back to the shop' }),
    ).toHaveAttribute('href', '/')
  })

  it.each(['/expert/cases', '/expert/profile'])(
    'shows the not-found page at %s to a customer',
    async (path) => {
      renderAt(path)

      expect(
        await screen.findByRole('heading', { name: 'Page not found' }),
      ).toBeInTheDocument()
    },
  )
})
