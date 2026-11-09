import { Link, Outlet } from 'react-router'
import { Button } from '@/components/ui/button'
import { useLogout, useMe } from '@/features/user/queries'

export function Layout() {
  const me = useMe()
  const isAdmin = me.data?.roles.includes('ADMIN') ?? false
  const isCustomer = me.data?.roles.includes('CUSTOMER') ?? false
  const isExpert = me.data?.roles.includes('EXPERT') ?? false

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
            {isCustomer && (
              <Link
                to="/my"
                className="ml-6 text-sm font-medium hover:underline"
              >
                My purchases
              </Link>
            )}
            {isExpert && (
              <Link
                to="/expert/availability"
                className="ml-6 text-sm font-medium hover:underline"
              >
                Availability
              </Link>
            )}
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
        onClick={() => logout.mutate()}
      >
        Log out
      </Button>
    </div>
  )
}
