import createClient from 'openapi-fetch'
import { ApiError } from '@/lib/api-error'
import { problemOf, type ProblemDetail } from '@/lib/problem'
import type { paths } from './schema'

const CSRF_COOKIE = 'XSRF-TOKEN'
const CSRF_HEADER = 'X-XSRF-TOKEN'
const MUTATING_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

// The API is on the SPA's own origin: Vite proxies /api in development, and the jar serves both in production.
export const client = createClient<paths>()

// Spring Security's CSRF protection for SPAs: the backend sets the XSRF-TOKEN cookie, and every mutating request
// sends its value back in the X-XSRF-TOKEN header. The cookie is read on each request, because a login replaces it.
client.use({
  onRequest({ request }) {
    if (!MUTATING_METHODS.has(request.method)) {
      return
    }
    const token = readCookie(CSRF_COOKIE)
    if (token !== undefined) {
      request.headers.set(CSRF_HEADER, token)
    }
  },
})

function readCookie(name: string): string | undefined {
  for (const cookie of document.cookie.split(';')) {
    const [key, ...value] = cookie.trim().split('=')
    if (key === name) {
      return decodeURIComponent(value.join('='))
    }
  }
  return undefined
}

/** What openapi-fetch returns: the body of a 2xx answer, or the body of an error answer, which a route may not type. */
interface Result<T, E> {
  data?: T
  error?: E
  response: Response
}

/**
 * The body of a 2xx answer, or an `ApiError` ("<action> failed: <status>") with the problem of a 400 or 409. It checks
 * `data`, not `error`: a 401 has an empty body, and openapi-fetch then returns `error: undefined`.
 */
export function dataOrThrow<T, E extends ProblemDetail | undefined>(
  result: Result<T, E>,
  action: string,
): T {
  if (result.data === undefined) {
    throw failed(result, action)
  }
  return result.data
}

/** For an answer without a body: throws the same `ApiError` as `dataOrThrow` unless the answer is 2xx. */
export function okOrThrow<E extends ProblemDetail | undefined>(
  result: Result<unknown, E>,
  action: string,
): void {
  if (!result.response.ok) {
    throw failed(result, action)
  }
}

function failed(
  result: Result<unknown, ProblemDetail | undefined>,
  action: string,
) {
  const { status } = result.response
  return new ApiError(
    `${action} failed: ${status}`,
    status,
    problemOf(status, result.error),
  )
}
