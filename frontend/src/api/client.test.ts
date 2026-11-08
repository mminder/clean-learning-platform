import { afterEach, describe, expect, it, vi } from 'vitest'
import { client } from './client'

// openapi-fetch needs an absolute URL outside the browser.
const baseUrl = 'http://localhost'

function stubFetch() {
  return vi.fn(async (request: Request) => {
    void request
    return new Response(null, { status: 204 })
  })
}

function setCookie(cookie: string) {
  document.cookie = cookie
}

afterEach(() => {
  setCookie('XSRF-TOKEN=; max-age=0')
  setCookie('other=; max-age=0')
})

describe('client', () => {
  it('sends the CSRF token from the cookie on a mutating request', async () => {
    setCookie('other=1')
    setCookie('XSRF-TOKEN=7c2b0a5e-token')
    const fetch = stubFetch()

    await client.POST('/api/logout', { baseUrl, fetch })

    expect(fetch.mock.calls[0][0].headers.get('X-XSRF-TOKEN')).toBe(
      '7c2b0a5e-token',
    )
  })

  it('reads the cookie on each request, because a login replaces it', async () => {
    const fetch = stubFetch()
    setCookie('XSRF-TOKEN=before-login')
    await client.POST('/api/logout', { baseUrl, fetch })
    setCookie('XSRF-TOKEN=after-login')
    await client.POST('/api/logout', { baseUrl, fetch })

    expect(fetch.mock.calls[1][0].headers.get('X-XSRF-TOKEN')).toBe(
      'after-login',
    )
  })

  it('sends the CSRF token with a FormData body and leaves its multipart content type', async () => {
    setCookie('XSRF-TOKEN=7c2b0a5e-token')
    const fetch = stubFetch()
    const body = new FormData()
    body.append('file', new File(['image'], 'agile.png', { type: 'image/png' }))

    await client.PUT('/api/admin/consultation-offerings/{id}/image', {
      baseUrl,
      fetch,
      params: { path: { id: '00000000-0000-0000-0000-000000001001' } },
      body: body as unknown as { file: string },
    })

    const request = fetch.mock.calls[0][0]
    expect(request.headers.get('X-XSRF-TOKEN')).toBe('7c2b0a5e-token')
    expect(request.headers.get('Content-Type')).toMatch(
      /^multipart\/form-data; boundary=/,
    )
  })

  it('sends no CSRF header on a GET', async () => {
    setCookie('XSRF-TOKEN=7c2b0a5e-token')
    const fetch = stubFetch()

    await client.GET('/api/me', { baseUrl, fetch })

    expect(fetch.mock.calls[0][0].headers.has('X-XSRF-TOKEN')).toBe(false)
  })

  it('sends no CSRF header without the cookie', async () => {
    const fetch = stubFetch()

    await client.POST('/api/logout', { baseUrl, fetch })

    expect(fetch.mock.calls[0][0].headers.has('X-XSRF-TOKEN')).toBe(false)
  })
})
