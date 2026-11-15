import type { components } from '@/api/schema'

type Role = components['schemas']['MeResponse']['roles'][number]

/**
 * Where a user lands after login, by the first matching role (UC-01): a user with EXPERT on My cases, then one with
 * ADMIN on the consultation offerings until the admin's Cases (UC-18) exists, then one with CUSTOMER on My purchases,
 * otherwise the shop (spec 06, decision 1).
 */
export function landingPath(roles: readonly Role[]): string {
  if (roles.includes('EXPERT')) {
    return '/expert/cases'
  }
  if (roles.includes('ADMIN')) {
    return '/admin/consultations'
  }
  return roles.includes('CUSTOMER') ? '/my' : '/'
}

/**
 * A path on this site. Resolved against the site, it stays on it: this rejects `//example.com`, and what browsers
 * read as `//`, such as `/\example.com`.
 */
export function isLocalPath(path: string): boolean {
  const origin = window.location.origin
  return path.startsWith('/') && new URL(path, origin).origin === origin
}

/** The page to open after login: the redirect if it's on this site, otherwise the user's landing page. */
export function pathAfterLogin(
  redirect: string | null,
  roles: readonly Role[],
): string {
  return redirect !== null && isLocalPath(redirect)
    ? redirect
    : landingPath(roles)
}
