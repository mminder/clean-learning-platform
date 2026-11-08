import { useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router'
import { Button } from '@/components/ui/button'
import { useLogout, useMe } from '@/features/user/queries'

export function Layout() {
  const me = useMe()
  const isAdmin = me.data?.roles.includes('ADMIN') ?? false

  return (
    <div className="min-h-screen">
      <header className="border-b">
        <div className="mx-auto flex min-h-14 max-w-5xl flex-wrap items-center gap-x-6 gap-y-2 px-4 py-2">
          <span className="text-lg font-semibold whitespace-nowrap">
            Clean Learning Platform
          </span>
          <nav>
            <Link to="/" className="text-sm font-medium hover:underline">
              Shop
            </Link>
            {isAdmin && (
              <Link
                to="/admin/consultations"
                className="ml-6 text-sm font-medium hover:underline"
              >
                Consultations
              </Link>
            )}
          </nav>
          <div className="ml-auto">
            <CurrentUser />
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}

/** **Log in** for a visitor, or the user's name and **Log out** (UC-01). */
function CurrentUser() {
  const me = useMe()
  const logout = useLogout()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { key: locationKey } = useLocation()
  const resetOnNextLocation = useRef(false)

  // After a logout no data of the user stays; what's on the screen loads again for the visitor. The reset waits for
  // the new page: it empties `['me']`, and `RequireRole` on the page being left would send the visitor to the login
  // instead of the shop. Navigation runs in a transition, so the effect is what knows that the page has changed.
  useEffect(() => {
    if (resetOnNextLocation.current) {
      resetOnNextLocation.current = false
      void queryClient.resetQueries()
    }
  }, [locationKey, queryClient])

  if (me.isPending || me.isError) {
    return null
  }
  if (me.data === null) {
    // No redirect: a login from the header opens the landing page (UC-01). A login started from a protected page
    // or from Buy returns there with `?redirect=`.
    return (
      <Link to="/login" className="text-sm font-medium hover:underline">
        Log in
      </Link>
    )
  }
  return (
    <div className="flex items-center gap-3">
      <span className="text-sm whitespace-nowrap">{me.data.name}</span>
      <Button
        variant="outline"
        size="sm"
        disabled={logout.isPending}
        onClick={() =>
          logout.mutate(undefined, {
            onSuccess: () => {
              resetOnNextLocation.current = true
              navigate('/')
            },
          })
        }
      >
        Log out
      </Button>
    </div>
  )
}
