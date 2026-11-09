import { Navigate, Outlet, useLocation } from 'react-router'
import type { components } from '@/api/schema'
import { useMe } from '@/features/user/queries'
import { NotFoundPage } from './NotFoundPage'

type Role = components['schemas']['MeResponse']['roles'][number]

/**
 * Guards the routes below it. A visitor goes to the login and comes back after it; a user without the role sees the
 * not-found page. This only hides pages: the backend decides (security.md).
 */
export function RequireRole({ role }: { role: Role }) {
  const me = useMe()
  const location = useLocation()

  if (me.isPending) {
    return null
  }
  if (me.isError) {
    return (
      <p role="alert">The current user could not be loaded. Reload the page.</p>
    )
  }
  if (me.data === null) {
    const redirect = location.pathname + location.search + location.hash
    return (
      <Navigate
        to={`/login?redirect=${encodeURIComponent(redirect)}`}
        replace
      />
    )
  }
  if (!me.data.roles.includes(role)) {
    return <NotFoundPage />
  }
  return <Outlet />
}
