import type { components } from '@/api/schema'

type Role = components['schemas']['MeResponse']['roles'][number]

/**
 * Where a user lands after login. UC-01's landing pages come with UC-11, UC-18 and UC-05; until then, a user with
 * ADMIN lands on the consultation offerings and everyone else on the shop (spec 02, decision 3).
 */
export function landingPath(roles: readonly Role[]): string {
  return roles.includes('ADMIN') ? '/admin/consultations' : '/'
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
