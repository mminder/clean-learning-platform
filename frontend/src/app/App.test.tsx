import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import { App } from './App'
import { Providers } from './Providers'

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
})
