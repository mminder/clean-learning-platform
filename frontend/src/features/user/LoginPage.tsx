import { useNavigate, useSearchParams } from 'react-router'
import type { components } from '@/api/schema'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { pathAfterLogin } from './landing'
import { useLogin, usePocUsers } from './queries'

type Role = components['schemas']['PocUserResponse']['roles'][number]

const roleLabels: Record<Role, string> = {
  CUSTOMER: 'Customer',
  EXPERT: 'Expert',
  ADMIN: 'Admin',
}

/** UC-01's PoC login: pick one of the seeded users. */
export function LoginPage() {
  return (
    <div className="max-w-md space-y-6">
      <div className="space-y-2">
        <h1 className="text-2xl font-semibold">Log in</h1>
        <p className="text-muted-foreground">
          Pick a user to log in as. This is the proof of concept's login.
        </p>
      </div>
      <PocUsers />
    </div>
  )
}

function PocUsers() {
  const users = usePocUsers()
  const login = useLogin()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  function logIn(userId: string) {
    login.mutate(userId, {
      onSuccess: (me) =>
        navigate(pathAfterLogin(searchParams.get('redirect'), me.roles), {
          replace: true,
        }),
    })
  }

  if (users.isPending) {
    return <p className="text-muted-foreground">Loading users…</p>
  }
  if (users.isError) {
    return (
      <div role="alert" className="space-y-3">
        <p>The users could not be loaded.</p>
        <Button variant="outline" onClick={() => users.refetch()}>
          Try again
        </Button>
      </div>
    )
  }
  return (
    <div className="space-y-3">
      {login.isError && (
        <p role="alert" className="text-destructive">
          Logging in failed. Please try again.
        </p>
      )}
      <ul className="space-y-2">
        {users.data.map((user) => (
          <li key={user.id}>
            <Button
              variant="outline"
              className="h-auto w-full justify-between py-3"
              disabled={login.isPending}
              onClick={() => logIn(user.id)}
            >
              <span className="font-medium">{user.name}</span>
              <span className="flex gap-1">
                {user.roles.map((role) => (
                  <Badge key={role} variant="secondary">
                    {roleLabels[role]}
                  </Badge>
                ))}
              </span>
            </Button>
          </li>
        ))}
      </ul>
    </div>
  )
}
