import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/lib/api-error'
import { client, dataOrThrow, okOrThrow } from './client'

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

describe('dataOrThrow and okOrThrow', () => {
  const problem = { title: 'Conflict', status: 409, code: 'CHANGED' }

  function answer(status: number, body?: object) {
    return client.GET('/api/me', {
      baseUrl,
      fetch: async () =>
        new Response(body ? JSON.stringify(body) : null, {
          status,
          headers: { 'Content-Type': 'application/problem+json' },
        }),
    })
  }

  it('dataOrThrow returns the body of a 2xx answer', async () => {
    const result = await answer(200, { id: 'u1' })

    expect(dataOrThrow(result, 'Loading the user')).toEqual({ id: 'u1' })
  })

  it('dataOrThrow throws an ApiError with the status, message and problem of a 409', async () => {
    const result = await answer(409, problem)

    const thrown = catchError(() => dataOrThrow(result, 'Loading the user'))

    expect(thrown).toBeInstanceOf(ApiError)
    expect(thrown).toMatchObject({
      message: 'Loading the user failed: 409',
      status: 409,
      problem,
    })
  })

  it('dataOrThrow throws for a 401 with an empty body and no problem', async () => {
    const result = await client.GET('/api/me', {
      baseUrl,
      fetch: async () =>
        new Response(null, { status: 401, headers: { 'Content-Length': '0' } }),
    })

    const thrown = catchError(() => dataOrThrow(result, 'Loading the user'))

    expect(thrown).toBeInstanceOf(ApiError)
    expect(thrown).toMatchObject({
      message: 'Loading the user failed: 401',
      status: 401,
      problem: undefined,
    })
  })

  it('dataOrThrow leaves the problem of a 500 undefined', async () => {
    const result = await answer(500, problem)

    const thrown = catchError(() => dataOrThrow(result, 'Loading the user'))

    expect(thrown).toMatchObject({ status: 500, problem: undefined })
  })

  it('okOrThrow returns for a 2xx answer', async () => {
    expect(okOrThrow(await answer(204), 'Deleting the user')).toBeUndefined()
  })

  it('okOrThrow throws an ApiError with the status, message and problem of a 409', async () => {
    const result = await answer(409, problem)

    const thrown = catchError(() => okOrThrow(result, 'Deleting the user'))

    expect(thrown).toBeInstanceOf(ApiError)
    expect(thrown).toMatchObject({
      message: 'Deleting the user failed: 409',
      status: 409,
      problem,
    })
  })

  it('okOrThrow leaves the problem of a 500 undefined', async () => {
    const result = await answer(500, problem)

    const thrown = catchError(() => okOrThrow(result, 'Deleting the user'))

    expect(thrown).toMatchObject({ status: 500, problem: undefined })
  })
})

function catchError(fn: () => unknown): unknown {
  try {
    fn()
  } catch (error) {
    return error
  }
  return undefined
}
