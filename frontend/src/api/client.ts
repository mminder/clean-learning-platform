import createClient from 'openapi-fetch'
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
